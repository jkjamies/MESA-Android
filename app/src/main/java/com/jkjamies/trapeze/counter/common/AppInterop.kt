package com.jkjamies.trapeze.counter.common

import com.jkjamies.trapeze.TrapezeInterop
import com.jkjamies.trapeze.TrapezeInteropEvent

interface AppInterop : TrapezeInterop {
    fun send(event: AppInteropEvent)
    override fun send(event: TrapezeInteropEvent) {
        if (event is AppInteropEvent) {
            send(event)
        }
    }
}

interface AppInteropEvent : TrapezeInteropEvent
