# Trapeze Project Guide

## Project Overview
A Pure-Compose driven architectural library implementing the MESA framework (Modular, Explicit, State-driven, Architecture). The library facilitates a rigid UDF flow where the UI is a stateless projection of a single State object.

## Libraries
- **Trapeze**: Core architecture (`TrapezeStateHolder`, `TrapezeState`, `TrapezeScreen`, `TrapezeEvent`, `TrapezeContent`).
- **TrapezeNavigation**: Navigation layer (`TrapezeNavigator`, `TrapezeNavHost`, `TrapezeInterop`).
- **Strata**: Business logic layer (`StrataInteractor`, `StrataSubjectInteractor`, `StrataResult`, `strataLaunch`).

## MESA Pillars
- **Modular:** Feature isolation by design; components are decoupled and portable.
- **Explicit:** All interactions are defined through the Screen, State, and Event contracts.
- **State-driven:** The State object is the Single Source of Truth (SSoT) and contains the event processing hook.
- **Architecture:** Provides the structural "Trapeze" to swing between Logic and UI.

## Technical Contract
- **Screen:** A `Parcelable` destination key with typed arguments.
- **State:** An immutable data class containing all display data AND an `eventSink: (TrapezeEvent) -> Unit`.
- **Logic (StateHolder):** Responsible for producing the State object via `@Composable produceState(screen: T): S`.
- **UI (TrapezeUi):** A stateless Composable that consumes the State: `@Composable (Modifier, State) -> Unit`.
- **Glue (TrapezeContent):** The entry point that bridges a Screen to its StateHolder and UI.

## Coding Standards
- **UDF Flow:** UI sends Events to the `eventSink` -> StateHolder processes -> New State is emitted -> UI recomposes.
- **Injection Agnostic:** The library accepts StateHolder instances; consumers choose the injection strategy (Manual, Dagger, Metro, etc.).
- **No ViewModels:** Logic belongs in `TrapezeStateHolder` to maintain MESA alignment.
- **Stateless UI:** Composables must never hold business logic or persistent state.
- **License Headers:** All source files must include the Apache 2.0 license header. The year should be `2026` (if current year) or `2026-<currentYear>`.

## Clean Architecture & Modules
- **API (`:features:foo:api`):** Public interfaces (UseCase definitions, Screen arguments). Stable API.
- **Domain (`:features:foo:domain`):** Business logic implementation (UseCases, Interactors). Internal details.
- **Data (`:features:foo:data`):** Repository implementations and data sources.
- **Presentation (`:features:foo:presentation`):** UI, StateHolder, and DI bindings.

## Navigation (TrapezeNavigation)
- **TrapezeNavHost:** Root container managing backstack and screen restoration.
- **TrapezeNavigator:** Injectable interface for navigation actions (`navigate`, `pop`, `popTo`, `replace`).
- **TrapezeInterop:** For communicating with the host Activity or global handlers (Toasts, Dialogs).

## Strata Patterns
- **Interactors:**
    - `StrataInteractor<P, R>`: For one-shot async work. Returns `StrataResult<R>`.
    - `StrataSubjectInteractor<P, T>`: For observing flows. Requires `invoke(params)` to start emission.
- **Error Handling:** Use `strataLaunch` in StateHolders. Use `.onFailure { }` on Interactor results. Failures are provided as `StrataException`.
- **Triggering:** `StrataSubjectInteractor` flows MUST be triggered by invoking them in the UI/Logic layer (e.g., `LaunchedEffect`), not in UseCase `init` blocks.

## Event Safety
- **Sink:** Use `eventSink: (Event) -> Unit` in State.
- **Wrapper:** In StateHolder, wrap the sink using `wrapEventSink` helper to ensure CoroutineScope is active before delivery.

## Dependency Injection (Metro)
- **Graph:** Use `AppGraph` (AppScope) for global dependencies.
- **Binding:** Use `@ContributesBinding(AppScope::class)` in Domain/Data to expose implementations.
- **Injection:** Inject interfaces. Use `Lazy<T>` for heavy dependencies or to delay initialization.