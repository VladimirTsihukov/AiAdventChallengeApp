package com.tishukoff.feature.mcp.impl.presentation.scheduler

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.mcp.api.McpTool
import com.tishukoff.feature.mcp.impl.data.McpClientWrapper
import com.tishukoff.feature.mcp.impl.data.SchedulerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SchedulerViewModel(
    private val repository: SchedulerRepository,
    private val mcpClientWrapper: McpClientWrapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SchedulerUiState>(SchedulerUiState.Loading)
    val uiState: StateFlow<SchedulerUiState> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(CreateTaskForm())
    val form: StateFlow<CreateTaskForm> = _form.asStateFlow()

    private val _availableTools = MutableStateFlow<List<McpTool>>(emptyList())
    val availableTools: StateFlow<List<McpTool>> = _availableTools.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadTasks()
        startAutoRefresh()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = SchedulerUiState.Loading
            try {
                val tools = mcpClientWrapper.listTools()
                _availableTools.value = tools

                val tasks = repository.listTasks()
                _uiState.value = SchedulerUiState.Content(tasks = tasks)
            } catch (e: Exception) {
                Log.d(TAG, "loadTasks error: ${e.message}")
                val message = e.message.orEmpty()
                if (message.contains("Not connected") || message.contains("closed")) {
                    _uiState.value = SchedulerUiState.NotConnected
                } else {
                    _uiState.value = SchedulerUiState.Error(message = message.ifBlank { "Failed to load tasks" })
                }
            }
        }
    }

    fun showCreateDialog() {
        _form.value = CreateTaskForm()
        _uiState.update { state ->
            if (state is SchedulerUiState.Content) {
                state.copy(showCreateDialog = true)
            } else state
        }
    }

    fun hideCreateDialog() {
        _uiState.update { state ->
            if (state is SchedulerUiState.Content) {
                state.copy(showCreateDialog = false)
            } else state
        }
    }

    fun updateFormName(name: String) {
        _form.update { it.copy(name = name) }
    }

    fun updateFormToolName(toolName: String) {
        _form.update { it.copy(toolName = toolName, toolArguments = emptyMap()) }
    }

    fun updateFormToolArgument(key: String, value: String) {
        _form.update { it.copy(toolArguments = it.toolArguments + (key to value)) }
    }

    fun updateFormInterval(interval: String) {
        _form.update { it.copy(intervalMinutes = interval) }
    }

    fun updateFormDuration(duration: String) {
        _form.update { it.copy(durationMinutes = duration) }
    }

    fun createTask() {
        val currentForm = _form.value
        val interval = currentForm.intervalMinutes.toIntOrNull()
        val duration = currentForm.durationMinutes.toIntOrNull()

        Log.d(TAG, "createTask: name='${currentForm.name}', tool='${currentForm.toolName}', " +
                "interval=$interval, duration=$duration, args=${currentForm.toolArguments}")

        if (currentForm.name.isBlank() || currentForm.toolName.isBlank()
            || interval == null || duration == null
            || interval <= 0 || duration <= 0
        ) {
            Log.d(TAG, "createTask: validation failed")
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                if (state is SchedulerUiState.Content) state.copy(isCreating = true) else state
            }
            try {
                repository.createTask(
                    name = currentForm.name,
                    toolName = currentForm.toolName,
                    toolArguments = currentForm.toolArguments,
                    intervalMinutes = interval,
                    durationMinutes = duration,
                )
                val tasks = repository.listTasks()
                _uiState.value = SchedulerUiState.Content(tasks = tasks)
            } catch (e: Exception) {
                Log.d(TAG, "createTask error: ${e.message}")
                _uiState.update { state ->
                    if (state is SchedulerUiState.Content) {
                        state.copy(isCreating = false, error = e.message)
                    } else state
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                val tasks = repository.listTasks()
                _uiState.update { state ->
                    if (state is SchedulerUiState.Content) {
                        state.copy(tasks = tasks, expandedTaskId = null, taskResults = emptyList())
                    } else state
                }
            } catch (e: Exception) {
                Log.d(TAG, "deleteTask error: ${e.message}")
            }
        }
    }

    fun toggleTask(taskId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleTask(taskId, isActive)
                val tasks = repository.listTasks()
                _uiState.update { state ->
                    if (state is SchedulerUiState.Content) {
                        state.copy(tasks = tasks)
                    } else state
                }
            } catch (e: Exception) {
                Log.d(TAG, "toggleTask error: ${e.message}")
            }
        }
    }

    fun expandTask(taskId: String) {
        val state = _uiState.value
        if (state !is SchedulerUiState.Content) return

        if (state.expandedTaskId == taskId) {
            _uiState.update {
                (it as SchedulerUiState.Content).copy(
                    expandedTaskId = null,
                    taskResults = emptyList(),
                )
            }
            return
        }

        _uiState.update {
            (it as SchedulerUiState.Content).copy(
                expandedTaskId = taskId,
                isLoadingResults = true,
                taskResults = emptyList(),
            )
        }

        viewModelScope.launch {
            try {
                val results = repository.getTaskResults(taskId)
                _uiState.update { state2 ->
                    if (state2 is SchedulerUiState.Content) {
                        state2.copy(taskResults = results, isLoadingResults = false)
                    } else state2
                }
            } catch (e: Exception) {
                Log.d(TAG, "expandTask error: ${e.message}")
                _uiState.update { state2 ->
                    if (state2 is SchedulerUiState.Content) {
                        state2.copy(isLoadingResults = false)
                    } else state2
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { state ->
            if (state is SchedulerUiState.Content) state.copy(error = null) else state
        }
    }

    private fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                refreshSilently()
            }
        }
    }

    private suspend fun refreshSilently() {
        val state = _uiState.value
        if (state !is SchedulerUiState.Content) return

        try {
            val tasks = repository.listTasks()
            val expandedId = state.expandedTaskId
            val results = if (expandedId != null) {
                repository.getTaskResults(expandedId)
            } else {
                state.taskResults
            }

            _uiState.update { current ->
                if (current is SchedulerUiState.Content) {
                    current.copy(tasks = tasks, taskResults = results)
                } else current
            }
        } catch (e: Exception) {
            Log.d(TAG, "refreshSilently error: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    private companion object {
        const val TAG = "SchedulerViewModel"
        const val REFRESH_INTERVAL_MS = 30_000L
    }
}
