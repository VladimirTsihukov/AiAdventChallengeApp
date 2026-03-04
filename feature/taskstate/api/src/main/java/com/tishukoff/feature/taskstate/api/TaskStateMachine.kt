package com.tishukoff.feature.taskstate.api

import kotlinx.coroutines.flow.Flow

/**
 * Stages of the task finite state machine.
 */
enum class TaskStage {
    IDLE,
    PLANNING,
    EXECUTION,
    VALIDATION,
    DONE,
}

/**
 * Reason why the FSM is currently paused.
 */
enum class PauseReason {
    STAGE_COMPLETE,
    USER_REQUESTED,
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
    suspend fun startTask(description: String)

    /** Resume execution after a stage-complete pause. Optionally attach a clarification. */
    suspend fun resumeTask(clarification: String? = null)

    /** Pause the currently running stage. */
    fun pauseTask()

    /** Reset the FSM back to [TaskStage.IDLE]. */
    fun resetTask()
}
