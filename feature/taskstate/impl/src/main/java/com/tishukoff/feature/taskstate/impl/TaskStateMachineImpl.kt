package com.tishukoff.feature.taskstate.impl

import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.taskstate.api.PauseReason
import com.tishukoff.feature.taskstate.api.TaskStage
import com.tishukoff.feature.taskstate.api.TaskState
import com.tishukoff.feature.taskstate.api.TaskStateMachine
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

    override suspend fun startTask(description: String) {
        _taskState.value = TaskState(
            stage = TaskStage.PLANNING,
            taskDescription = description,
        )
        runStage(TaskStage.PLANNING)
    }

    override suspend fun resumeTask(clarification: String?) {
        val current = _taskState.value
        if (!current.isPaused) return

        val nextStage = when (current.stage) {
            TaskStage.PLANNING -> TaskStage.EXECUTION
            TaskStage.EXECUTION -> TaskStage.VALIDATION
            TaskStage.VALIDATION -> TaskStage.DONE
            else -> return
        }

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
    }

    override fun pauseTask() {
        val current = _taskState.value
        if (current.stage in listOf(TaskStage.IDLE, TaskStage.DONE)) return
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
