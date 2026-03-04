package com.tishukoff.feature.taskstate.impl.di

import com.tishukoff.feature.taskstate.api.TaskStateMachine
import com.tishukoff.feature.taskstate.impl.TaskStateMachineImpl
import org.koin.dsl.module

val taskStateModule = module {
    single<TaskStateMachine> { TaskStateMachineImpl(agent = get()) }
}
