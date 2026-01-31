# Trapeze Project Guide

## Project Overview
A Pure-Compose driven architectural library implementing the MESA framework. The library facilitates a rigid UDF flow where the UI is a stateless projection of a single State object.

## MESA Pillars
- **Modular:** Feature isolation by design; components are decoupled and portable.
- **Explicit:** All interactions are defined through the Screen, State, and Event contracts.
- **State-driven:** The State object is the Single Source of Truth (SSoT) and contains the event processing hook.
- **Architecture:** Provides the structural "Trapeze" to swing between Logic and UI.

## Technical Contract
- **State:** An immutable data class containing all display data AND an `eventSink: (TrapezeEvent) -> Unit`.
- **Logic (StateHolder):** Responsible for producing the State object.
- **UI (TrapezeUi):** A stateless Composable that consumes the State.
- **Glue (TrapezeContent):** The entry point that bridges a Screen to its StateHolder and UI.

## Coding Standards
- **UDF Flow:** UI sends Events to the `eventSink` -> StateHolder processes -> New State is emitted -> UI recomposes.
- **Injection Agnostic:** The library accepts StateHolder instances; consumers choose the injection strategy (Manual, Dagger, etc.).
- **No ViewModels:** Logic belongs in `TrapezeStateHolder` to maintain MESA alignment.
- **Stateless UI:** Composables must never hold business logic or persistent state.

## The Trapeze Contract
1. **Screen:** `Parcelable` destination key.
2. **Event:** Sealed interface of user intents.
3. **State:** Data class with values and the `eventSink` closure.
4. **StateHolder:** `@Composable produceState(screen: T): S`.
5. **UI:** `@Composable (Modifier, State) -> Unit`.