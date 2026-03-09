package com.tishukoff.feature.taskstate.api

import kotlinx.coroutines.flow.Flow

/**
 * Stages of the task finite state machine.
 */
enum class TaskStage {
    IDLE,
    PLANNING,
    PLAN_APPROVED,
    EXECUTION,
    VALIDATION,
    DONE,
    ;

    companion object {

        /** Map of allowed transitions between stages. */
        val allowedTransitions: Map<TaskStage, Set<TaskStage>> = mapOf(
            IDLE to setOf(PLANNING),
            PLANNING to setOf(PLAN_APPROVED),
            PLAN_APPROVED to setOf(EXECUTION),
            EXECUTION to setOf(VALIDATION),
            VALIDATION to setOf(DONE),
        )
    }
}

/**
 * Reason why the FSM is currently paused.
 */
enum class PauseReason {
    STAGE_COMPLETE,
    USER_REQUESTED,
}

/**
 * Result of a state transition attempt.
 */
sealed interface TransitionResult {
    data class Success(val newState: TaskState) : TransitionResult
    data class Denied(val from: TaskStage, val to: TaskStage, val reason: String) : TransitionResult
}

/**
 * Complete state of the task FSM.
 */
data class TaskState(
    val stage: TaskStage = TaskStage.IDLE,
    val isPaused: Boolean = false,
    val pauseReason: PauseReason? = null,
    val taskDescription: String = "",
    val planningResult: String? = null,
    val executionResult: String? = null,
    val validationResult: String? = null,
    val userClarifications: Map<TaskStage, String> = emptyMap(),
)

/**
 * Finite state machine that drives a task through planning, execution, and validation stages.
 */
interface TaskStateMachine {

    /** Observable state of the FSM. */
    val taskState: Flow<TaskState>

    /** Start a new task — begins with [TaskStage.PLANNING]. */
    suspend fun startTask(description: String): TransitionResult

    /** Approve the plan after PLANNING stage completes. Transitions to EXECUTION. */
    suspend fun approvePlan(clarification: String? = null): TransitionResult

    /** Resume execution after a stage-complete pause. Optionally attach a clarification. */
    suspend fun resumeTask(clarification: String? = null): TransitionResult

    /** Pause the currently running stage. */
    fun pauseTask()

    /** Reset the FSM back to [TaskStage.IDLE]. */
    fun resetTask()
}
