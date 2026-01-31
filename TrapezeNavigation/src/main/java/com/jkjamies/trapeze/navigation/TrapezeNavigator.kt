package com.jkjamies.trapeze.navigation


import com.jkjamies.trapeze.TrapezeScreen

interface TrapezeNavigator {
    fun navigate(screen: TrapezeScreen)
    fun pop()
}
