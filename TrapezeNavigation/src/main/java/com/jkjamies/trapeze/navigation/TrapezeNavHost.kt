package com.jkjamies.trapeze.navigation
import androidx.compose.runtime.Composable
import com.jkjamies.trapeze.TrapezeScreen
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

val LocalTrapezeNavigator = staticCompositionLocalOf<TrapezeNavigator> { 
    error("No TrapezeNavigator provided") 
}

@Composable
fun TrapezeNavHost(
    initialScreen: TrapezeScreen,
    modifier: Modifier = Modifier,
    content: @Composable (TrapezeScreen) -> Unit
) {
    var stack by remember { mutableStateOf(listOf(initialScreen)) }
    val currentScreen = stack.last()
    val saveableStateHolder = rememberSaveableStateHolder()
    
    val navigator = remember {
        object : TrapezeNavigator {
            override fun navigate(screen: TrapezeScreen) {
                stack = stack + screen
            }
            override fun pop() {
                if (stack.size > 1) {
                    stack = stack.dropLast(1)
                }
            }
        }
    }
    
    CompositionLocalProvider(LocalTrapezeNavigator provides navigator) {
        saveableStateHolder.SaveableStateProvider(key = currentScreen) {
             content(currentScreen)
        }
    }
}
