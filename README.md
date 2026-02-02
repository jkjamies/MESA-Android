# Trapeze Framework

A type-safe, MESA-inspired architecture for Jetpack Compose. This framework enforces a strict separation between logic (StateHolder), presentation (UI), and identity (Screen).

## 📚 Libraries

- **Trapeze**: The core architecture types (`TrapezeStateHolder`, `TrapezeState`, `TrapezeScreen`, `TrapezeEvent`, `TrapezeContent`).
- **TrapezeNavigation**: The navigation layer (`TrapezeNavigator`, `TrapezeNavHost`, `TrapezeInterop`).
- **Strata**: The business logic and concurrency layer (`StrataInteractor`, `StrataResult`, `strataLaunch`).

---

## 🏗️ Trapeze Architecture

Trapeze implements the MESA pattern (Modular, Explicit, State-driven, Architecture).

### 1. Identity (The Screen)
The `TrapezeScreen` is a `Parcelable` data structure that uniquely identifies a destination. It carries strict, typed arguments.
```kotlin
@Parcelize
data class DictionaryScreen(val word: String) : TrapezeScreen, Parcelable
```

### 2. Logic (The StateHolder)
The `TrapezeStateHolder` is the brain. It manages lifecycle, holds business logic, and produces the single source of truth (`State`).
- **Input**: The `Screen` arguments.
- **Output**: A `State` object.
- **Dependencies**: Injected via Metro/Dagger.

### 3. State (The Contract)
An immutable data class containing:
- **Data**: All fields needed for rendering.
- **Events**: A single `eventSink: (Event) -> Unit` lambda to handle user interactions.

### 4. UI (The View)
A stateless Composable function. It is a pure projection of the `State`.

### 5. The Weld (TrapezeContent)
Connects the pieces together at runtime, typically within the NavGraph.

---

## 💻 Example: Counter Feature

```kotlin
import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.jkjamies.trapeze.*
import kotlinx.parcelize.Parcelize

// 1. Definition (Screen, State, Event)
@Parcelize
data class CounterScreen(val initialValue: Int) : TrapezeScreen, Parcelable

data class CounterState(
    val count: Int,
    val eventSink: (CounterEvent) -> Unit
) : TrapezeState

sealed interface CounterEvent : TrapezeEvent {
    data object Increment : CounterEvent
}

// 2. StateHolder Implementation
class CounterStateHolder : TrapezeStateHolder<CounterScreen, CounterState, CounterEvent>() {
    @Composable
    override fun produceState(screen: CounterScreen): CounterState {
        // use rememberSaveable to persist state across navigation/process death
        var count by rememberSaveable { mutableIntStateOf(screen.initialValue) }

        return CounterState(
            count = count,
            eventSink = { event ->
                when (event) {
                    CounterEvent.Increment -> count++
                }
            }
        )
    }
}

// 3. UI Implementation
@Composable
fun CounterUi(modifier: Modifier = Modifier, state: CounterState) {
    Button(
        onClick = { state.eventSink(CounterEvent.Increment) },
        modifier = modifier
    ) {
        Text("Count: ${state.count}")
    }
}
```

---

## � Trapeze Navigation

Type-safe navigation integrated with Trapeze.

### TrapezeNavHost
The root container. Manages the backstack and screen restoration.
```kotlin
TrapezeNavHost(initialScreen = HomeScreen) { screen ->
    when(screen) {
        is HomeScreen -> TrapezeContent(screen, HomeStateHolder(), ::HomeUi)
        is DetailScreen -> TrapezeContent(screen, DetailStateHolder(), ::DetailUi)
    }
}
```

### TrapezeNavigator
Injectable interface for navigation actions. 

**Capabilities:**
- `navigate(screen)`: Push a new screen.
- `pop()`: Go back one level.
- `popTo(screen)`: Pop until a specific screen found.
- `replace(screen)`: Replace current screen.

### Interop
For communicating with the host Activity or global handlers (e.g., Toasts, Dialogs).
1. Define `TrapezeInteropEvent`.
2. Inject `TrapezeInterop` into StateHolder.
3. Send events: `interop.send(MyEvent)`.
4. Handle in `NavHost` via composition local or callback.

---

## �🧠 Strata (Business Logic)

Strata standardizes all asynchronous operations and error handling. It forces business logic out of the UI and StateHolder.

### Interactors
All logic units extend `StrataInteractor`.

#### 1. StrataInteractor (One-Shot)
For operations that complete once (e.g., API calls, DB writes).
```kotlin
class SaveNote : StrataInteractor<String, Unit>() {
    override suspend fun doWork(params: String) {
        db.save(params)
    }
}
```

#### 2. StrataSubjectInteractor (Streams)
For observing data over time. Requires an explicit trigger to start emission.
```kotlin
class ObserveNote : StrataSubjectInteractor<String, Note>() {
    override fun createObservable(params: String): Flow<Note> = db.observe(params)
}
```

### Execution & Error Handling
StateHolders use `strataLaunch` to execute interactors safely. Results are wrapped in `StrataResult`.

```kotlin
// In StateHolder
fun save(content: String) {
    strataLaunch {
        // Returns StrataResult<Unit>
        // You can handle failure without needing explicit onSuccess
        saveNote(content).onFailure { error -> 
            // error is StrataException
            notifyError(error)
        }
    }
}

// Triggering a Subject
LaunchedEffect(Unit) {
    observeNote(noteId) // Starts the flow
}
val note by observeNote.flow.collectAsState(initial = null)
```

---

## 🔌 Dependency Injection

Trapeze is **injection-agnostic**. You can use Manual DI, Dagger, Hilt, Koin, or any other framework.

### Why we recommend Metro
The examples in this repo leverage **Metro**, a highly optimized Dagger-compatible graph. 
- **Speed**: 40-70% faster build times than Dagger/Hilt.
- **Modern**: Built on KSP and fully compatible with the Kotlin K2 compiler.
- **Standard**: Uses standard `javax.inject` annotations.

If using Metro:
- Bind implementations using `@ContributesBinding(AppScope::class)`.
- Use `Lazy<T>` to delay initialization of heavy Interactors.
