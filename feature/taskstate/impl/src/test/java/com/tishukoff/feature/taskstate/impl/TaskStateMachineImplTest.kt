package com.tishukoff.feature.taskstate.impl

import com.tishukoff.feature.taskstate.api.PauseReason
import com.tishukoff.feature.taskstate.api.TaskStage
import com.tishukoff.feature.taskstate.api.TaskState
import com.tishukoff.feature.taskstate.api.TransitionResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskStateMachineImplTest {

    private lateinit var fakeAgent: FakeAgent
    private lateinit var fsm: TaskStateMachineImpl

    @Before
    fun setUp() {
        fakeAgent = FakeAgent()
        fsm = TaskStateMachineImpl(agent = fakeAgent)
    }

    @Test
    fun `initial state is IDLE`() = runTest {
        val state = fsm.taskState.first()
        assertEquals(TaskStage.IDLE, state.stage)
    }

    @Test
    fun `startTask transitions from IDLE to PLANNING`() = runTest {
        fakeAgent.nextResponse = "Step 1. Do this\nStep 2. Do that"

        val result = fsm.startTask("Build REST API")

        assertTrue(result is TransitionResult.Success)
        val state = fsm.taskState.first()
        assertEquals(TaskStage.PLANNING, state.stage)
        assertTrue(state.isPaused)
        assertEquals(PauseReason.STAGE_COMPLETE, state.pauseReason)
    }

    @Test
    fun `startTask from non-IDLE is denied`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")

        val result = fsm.startTask("another task")

        assertTrue(result is TransitionResult.Denied)
        val denied = result as TransitionResult.Denied
        assertEquals(TaskStage.PLANNING, denied.from)
        assertEquals(TaskStage.PLANNING, denied.to)
    }

    @Test
    fun `resumeTask from PLANNING is denied - must approve plan first`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")

        val result = fsm.resumeTask()

        assertTrue(result is TransitionResult.Denied)
        val denied = result as TransitionResult.Denied
        assertEquals(TaskStage.PLANNING, denied.from)
        assertTrue(denied.reason.contains("утверждения плана"))
    }

    @Test
    fun `approvePlan transitions PLANNING to EXECUTION`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")

        fakeAgent.nextResponse = "execution result"
        val result = fsm.approvePlan()

        assertTrue(result is TransitionResult.Success)
        val state = fsm.taskState.first()
        assertEquals(TaskStage.EXECUTION, state.stage)
        assertTrue(state.isPaused)
    }

    @Test
    fun `approvePlan from EXECUTION is denied`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")
        fakeAgent.nextResponse = "execution"
        fsm.approvePlan()

        val result = fsm.approvePlan()

        assertTrue(result is TransitionResult.Denied)
    }

    @Test
    fun `resumeTask from EXECUTION transitions to VALIDATION`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")
        fakeAgent.nextResponse = "execution"
        fsm.approvePlan()

        fakeAgent.nextResponse = "validation result"
        val result = fsm.resumeTask()

        assertTrue(result is TransitionResult.Success)
        val state = fsm.taskState.first()
        assertEquals(TaskStage.VALIDATION, state.stage)
        assertTrue(state.isPaused)
    }

    @Test
    fun `resumeTask from VALIDATION transitions to DONE`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")
        fakeAgent.nextResponse = "execution"
        fsm.approvePlan()
        fakeAgent.nextResponse = "validation"
        fsm.resumeTask()

        val result = fsm.resumeTask()

        assertTrue(result is TransitionResult.Success)
        val state = fsm.taskState.first()
        assertEquals(TaskStage.DONE, state.stage)
    }

    @Test
    fun `cannot skip from IDLE to EXECUTION`() = runTest {
        val result = fsm.resumeTask()

        assertTrue(result is TransitionResult.Denied)
    }

    @Test
    fun `cannot skip from PLANNING to VALIDATION`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")

        // Try to resume (which would go to EXECUTION) — denied because plan not approved
        val result = fsm.resumeTask()
        assertTrue(result is TransitionResult.Denied)

        // State should remain PLANNING
        val state = fsm.taskState.first()
        assertEquals(TaskStage.PLANNING, state.stage)
    }

    @Test
    fun `full happy path IDLE - PLANNING - APPROVED - EXECUTION - VALIDATION - DONE`() = runTest {
        fakeAgent.nextResponse = "plan"
        val r1 = fsm.startTask("Build feature")
        assertTrue(r1 is TransitionResult.Success)
        assertEquals(TaskStage.PLANNING, fsm.taskState.first().stage)

        fakeAgent.nextResponse = "implemented"
        val r2 = fsm.approvePlan()
        assertTrue(r2 is TransitionResult.Success)
        assertEquals(TaskStage.EXECUTION, fsm.taskState.first().stage)

        fakeAgent.nextResponse = "all checks passed"
        val r3 = fsm.resumeTask()
        assertTrue(r3 is TransitionResult.Success)
        assertEquals(TaskStage.VALIDATION, fsm.taskState.first().stage)

        val r4 = fsm.resumeTask()
        assertTrue(r4 is TransitionResult.Success)
        assertEquals(TaskStage.DONE, fsm.taskState.first().stage)
    }

    @Test
    fun `pause and resume works correctly`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")

        // After startTask completes, FSM is paused with STAGE_COMPLETE
        val state = fsm.taskState.first()
        assertTrue(state.isPaused)
        assertEquals(PauseReason.STAGE_COMPLETE, state.pauseReason)

        // Approve plan to continue
        fakeAgent.nextResponse = "execution"
        val result = fsm.approvePlan()
        assertTrue(result is TransitionResult.Success)
        assertEquals(TaskStage.EXECUTION, fsm.taskState.first().stage)
    }

    @Test
    fun `resetTask returns to IDLE`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")
        fakeAgent.nextResponse = "execution"
        fsm.approvePlan()

        fsm.resetTask()

        val state = fsm.taskState.first()
        assertEquals(TaskStage.IDLE, state.stage)
        assertEquals(false, state.isPaused)
    }

    @Test
    fun `clarification is passed through approvePlan`() = runTest {
        fakeAgent.nextResponse = "plan"
        fsm.startTask("task")

        fakeAgent.nextResponse = "execution"
        fsm.approvePlan(clarification = "Add error handling")

        val state = fsm.taskState.first()
        assertEquals("Add error handling", state.userClarifications[TaskStage.EXECUTION])
    }

    @Test
    fun `resumeTask when not paused is denied`() = runTest {
        // IDLE state, not paused
        val result = fsm.resumeTask()
        assertTrue(result is TransitionResult.Denied)
    }

    @Test
    fun `approvePlan when not paused is denied`() = runTest {
        val result = fsm.approvePlan()
        assertTrue(result is TransitionResult.Denied)
    }
}
