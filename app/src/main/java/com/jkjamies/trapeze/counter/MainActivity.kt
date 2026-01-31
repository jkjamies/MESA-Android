package com.jkjamies.trapeze.counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import com.jkjamies.trapeze.TrapezeContent
import com.jkjamies.trapeze.counter.theme.TrapezeTheme
import com.jkjamies.trapeze.navigation.TrapezeNavHost
import com.jkjamies.trapeze.navigation.TrapezeNavigator
import com.jkjamies.trapeze.TrapezeScreen
import com.jkjamies.trapeze.TrapezeInterop
import com.jkjamies.trapeze.TrapezeInteropEvent
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrapezeTheme {
                Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
                    TrapezeNavHost(
                        initialScreen = CounterScreen("Enter your email")
                    ) { screen ->
                        val navigator = com.jkjamies.trapeze.navigation.LocalTrapezeNavigator.current
                        
                        // NOTE: In a real app Interop might also be provided via CompositionLocal or Dependency Injection
                        val interop = remember {
                            object : TrapezeInterop {
                                override fun send(event: TrapezeInteropEvent) {
                                    Toast.makeText(this@MainActivity, "Interop Event: $event", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        when(screen) {
                             is CounterScreen -> {
                                 TrapezeContent(
                                     modifier = Modifier.padding(innerPadding),
                                     screen = screen,
                                     stateHolder = CounterStateHolder(interop, navigator),
                                     ui = ::CounterUi
                                 )
                             }
                             else -> {} // Handle potential other screens
                        }
                    }
                }
            }
        }
    }
}