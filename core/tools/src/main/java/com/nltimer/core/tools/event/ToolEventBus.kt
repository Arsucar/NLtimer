package com.nltimer.core.tools.event

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class ToolEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ToolExecutedEvent>(
        extraBufferCapacity = 64,
    )
    val events: SharedFlow<ToolExecutedEvent> = _events.asSharedFlow()

    internal suspend fun emit(event: ToolExecutedEvent) {
        _events.emit(event)
    }
}
