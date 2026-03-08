package com.tishukoff.feature.taskstate.impl

import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.taskstate.api.PauseReason
import com.tishukoff.feature.taskstate.api.TaskStage
import com.tishukoff.feature.taskstate.api.TaskState
import com.tishukoff.feature.taskstate.api.TaskStateMachine
import com.tishukoff.feature.taskstate.api.TransitionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal class TaskStateMachineImpl(
    private val agent: Agent,
) : TaskStateMachine {

    private val _taskState = MutableStateFlow(TaskState())
    override val taskState: Flow<TaskState> = _taskState.asStateFlow()

    private var stageJob: Job? = null

    override suspend fun startTask(description: String): TransitionResult {
        val current = _taskState.value
        val result = validateTransition(current.stage, TaskStage.PLANNING)
        if (result != null) return result

        _taskState.value = TaskState(
            stage = TaskStage.PLANNING,
            taskDescription = description,
        )
        runStage(TaskStage.PLANNING)
        return TransitionResult.Success(_taskState.value)
    }

    override suspend fun approvePlan(clarification: String?): TransitionResult {
        val current = _taskState.value
        if (!current.isPaused) {
            return TransitionResult.Denied(
                from = current.stage,
                to = TaskStage.PLAN_APPROVED,
                reason = "Нельзя утвердить план — этап ещё выполняется",
            )
        }

        val result = validateTransition(current.stage, TaskStage.PLAN_APPROVED)
        if (result != null) return result

        val updatedClarifications = if (clarification != null) {
            current.userClarifications + (TaskStage.EXECUTION to clarification)
        } else {
            current.userClarifications
        }

        _taskState.update {
            it.copy(
                stage = TaskStage.PLAN_APPROVED,
                isPaused = false,
                pauseReason = null,
                userClarifications = updatedClarifications,
            )
        }

        // PLAN_APPROVED automatically transitions to EXECUTION
        _taskState.update {
            it.copy(stage = TaskStage.EXECUTION)
        }
        runStage(TaskStage.EXECUTION)
        return TransitionResult.Success(_taskState.value)
    }

    override suspend fun resumeTask(clarification: String?): TransitionResult {
        val current = _taskState.value
        if (!current.isPaused) {
            return TransitionResult.Denied(
                from = current.stage,
                to = current.stage,
                reason = "Задача не на паузе",
            )
        }

        val nextStage = when (current.stage) {
            TaskStage.EXECUTION -> TaskStage.VALIDATION
            TaskStage.VALIDATION -> TaskStage.DONE
            TaskStage.PLANNING -> {
                return TransitionResult.Denied(
                    from = TaskStage.PLANNING,
                    to = TaskStage.EXECUTION,
                    reason = "Нельзя перейти к выполнению без утверждения плана. Используйте 'Утвердить план'",
                )
            }
            else -> {
                return TransitionResult.Denied(
                    from = current.stage,
                    to = current.stage,
                    reason = "Нет допустимого перехода из состояния ${current.stage.name}",
                )
            }
        }

        val result = validateTransition(current.stage, nextStage)
        if (result != null) return result

        val updatedClarifications = if (clarification != null) {
            current.userClarifications + (nextStage to clarification)
        } else {
            current.userClarifications
        }

        _taskState.update {
            it.copy(
                stage = nextStage,
                isPaused = false,
                pauseReason = null,
                userClarifications = updatedClarifications,
            )
        }

        if (nextStage != TaskStage.DONE) {
            runStage(nextStage)
        }
        return TransitionResult.Success(_taskState.value)
    }

    override fun pauseTask() {
        val current = _taskState.value
        if (current.stage in listOf(TaskStage.IDLE, TaskStage.DONE, TaskStage.PLAN_APPROVED)) return
        if (current.isPaused) return

        stageJob?.cancel()
        stageJob = null

        _taskState.update {
            it.copy(
                isPaused = true,
                pauseReason = PauseReason.USER_REQUESTED,
            )
        }
    }

    override fun resetTask() {
        stageJob?.cancel()
        stageJob = null
        _taskState.value = TaskState()
    }

    private fun validateTransition(from: TaskStage, to: TaskStage): TransitionResult.Denied? {
        val allowed = TaskStage.allowedTransitions[from] ?: emptySet()
        if (to !in allowed) {
            return TransitionResult.Denied(
                from = from,
                to = to,
                reason = "Переход ${from.name} → ${to.name} запрещён",
            )
        }
        return null
    }

    private suspend fun runStage(stage: TaskStage) = coroutineScope {
        val currentSettings = agent.settings.first()
        val state = _taskState.value

        val systemPrompt = when (stage) {
            TaskStage.PLANNING -> TaskStagePrompts.planning()
            TaskStage.EXECUTION -> TaskStagePrompts.execution(
                planningResult = state.planningResult.orEmpty(),
            )
            TaskStage.VALIDATION -> TaskStagePrompts.validation(
                taskDescription = state.taskDescription,
                executionResult = state.executionResult.orEmpty(),
            )
            else -> return@coroutineScope
        }

        agent.updateSettings(currentSettings.copy(systemPrompt = systemPrompt))

        val userMessage = buildUserMessage(stage, state)
        agent.addUserMessage(userMessage)

        try {
            val response = agent.processRequest()
            ensureActive()

            when (stage) {
                TaskStage.PLANNING -> _taskState.update { it.copy(planningResult = response.text) }
                TaskStage.EXECUTION -> _taskState.update { it.copy(executionResult = response.text) }
                TaskStage.VALIDATION -> _taskState.update { it.copy(validationResult = response.text) }
                else -> Unit
            }

            _taskState.update {
                it.copy(
                    isPaused = true,
                    pauseReason = PauseReason.STAGE_COMPLETE,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            agent.updateSettings(currentSettings)
        }
    }

    private fun buildUserMessage(stage: TaskStage, state: TaskState): String {
        val clarification = state.userClarifications[stage]
        val base = when (stage) {
            TaskStage.PLANNING -> state.taskDescription
            TaskStage.EXECUTION -> "Выполни план по задаче: ${state.taskDescription}"
            TaskStage.VALIDATION -> "Проверь результат выполнения задачи: ${state.taskDescription}"
            else -> ""
        }
        return if (clarification != null) {
            "$base\n\nУточнение от пользователя: $clarification"
        } else {
            base
        }
    }
}
