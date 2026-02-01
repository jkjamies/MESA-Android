package com.jkjamies.trapeze.counter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jkjamies.trapeze.TrapezeContent
import com.jkjamies.trapeze.counter.common.AppInterop
import com.jkjamies.trapeze.counter.common.AppInteropEvent
import com.jkjamies.trapeze.counter.feature.counter.CounterScreen
import com.jkjamies.trapeze.counter.feature.counter.CounterStateHolder
import com.jkjamies.trapeze.counter.feature.counter.CounterUi
import com.jkjamies.trapeze.counter.feature.summary.SummaryScreen
import com.jkjamies.trapeze.counter.feature.summary.SummaryStateHolder
import com.jkjamies.trapeze.counter.feature.summary.SummaryUi
import com.jkjamies.trapeze.counter.theme.TrapezeTheme
import com.jkjamies.trapeze.navigation.LocalTrapezeNavigator
import com.jkjamies.trapeze.navigation.TrapezeNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrapezeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrapezeNavHost(
                        initialScreen = CounterScreen(initialCount = 0)
                    ) { screen ->
                        val navigator = LocalTrapezeNavigator.current
                        
                        // NOTE: In a real app Interop might also be provided via CompositionLocal or Dependency Injection
                        val interop = remember {
                            object : AppInterop {
                                override fun send(event: AppInteropEvent) {
                                    Toast.makeText(this@MainActivity, "App Interop: $event", Toast.LENGTH_SHORT).show()
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
                             is SummaryScreen -> {
                                 TrapezeContent(
                                     modifier = Modifier.padding(innerPadding),
                                     screen = screen,
                                     stateHolder = SummaryStateHolder(navigator),
                                     ui = ::SummaryUi
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