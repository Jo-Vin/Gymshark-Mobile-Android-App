# Engineering Guidelines

## Overview

This repository contains an Android application built with Kotlin and Jetpack Compose.

The objective is to deliver a reliable, maintainable and well-tested application using established Android development practices.

Engineering decisions should favour clarity, simplicity and consistency with the existing codebase.

## Architecture

- Follow the existing architectural patterns and project structure.
- Maintain clear separation of concerns between the UI, domain and data layers where appropriate.
- Keep business logic separate from UI rendering.
- Use repositories to abstract data sources where appropriate.
- Prefer unidirectional data flow and explicit UI state management.
- Avoid unnecessary abstractions, interfaces or architectural layers.
- Design components with testability and maintainability in mind.

## Kotlin

- Follow Kotlin coding conventions and established idioms.
- Prefer immutable data and `val` unless mutation is necessary.
- Use null safety rather than unnecessary non-null assertions.
- Prefer straightforward, readable implementations over clever or overly complex solutions.
- Keep functions focused and responsibilities clearly defined.
- Use descriptive names that communicate intent.

## Jetpack Compose

- Build reusable composables with clearly defined responsibilities.
- Prefer stateless composables where practical.
- Hoist state to the appropriate owner.
- Keep expensive operations outside composable functions.
- Use stable keys for lazy list and grid items where appropriate.
- Consider recomposition behaviour and rendering performance.
- Support varying screen sizes and content lengths.
- Consider accessibility, including content descriptions and touch targets.

## Data and Networking

- Separate network representations from UI models where appropriate.
- Reuse existing data sources, parsers and mapping logic.
- Handle loading, empty and error states explicitly.
- Avoid blocking the main thread with network or computational work.
- Use Kotlin coroutines and Flow where appropriate.
- Handle failures predictably and avoid silently swallowing exceptions.

## Testing

- Write unit tests for meaningful application behaviour.
- Follow the existing testing conventions and frameworks.
- Prefer deterministic tests with clearly defined inputs and expected outputs.
- Cover important edge cases and failure scenarios.
- Test observable behaviour rather than unnecessary implementation details.
- Keep production code testable without excessive mocking.
- Ensure existing tests continue to pass following changes.

## Dependencies

- Prefer existing project dependencies where suitable.
- Introduce new libraries only when they provide clear value.
- Avoid adding dependencies for functionality that can be implemented simply with existing tools.

## Code Changes

- Understand the relevant existing implementation before modifying it.
- Keep changes focused on the requirements being addressed.
- Preserve unrelated functionality.
- Avoid speculative refactoring or unnecessary complexity.
- Maintain consistent formatting and naming conventions.
- Update tests when behaviour changes.
- Remove unused code and imports introduced by changes.

## Quality and Verification

Before considering a change complete:

1. Confirm the implementation meets the intended requirements.
2. Review the changes for correctness and simplicity.
3. Run relevant tests and build checks where available.
4. Consider edge cases, error handling and potential regressions.
5. Ensure the resulting code remains understandable and maintainable.

## Engineering Principles

- **Simplicity:** Prefer the simplest solution that satisfies the requirements.
- **Consistency:** Follow established patterns unless there is a clear reason to improve them.
- **Testability:** Design behaviour so it can be verified reliably.
- **Maintainability:** Optimise for code that another developer can understand and extend.
- **Pragmatism:** Apply engineering principles proportionately to the complexity of the problem.