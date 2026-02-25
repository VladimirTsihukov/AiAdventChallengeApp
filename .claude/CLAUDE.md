## Tech Stack
- Android, Kotlin, Jetpack Compose, Coroutines, MVI
- Clean Architecture with Use Cases
- Koin for DI, Retrofit for networking

## Code Style
- Prefer `val` over `var`
- Use `sealed interface` for UI states
- Always add KDoc for public APIs

## Architecture
- Always follow Clean Architecture with layers: domain, data, presentation
- Domain layer: UseCases, Repository interfaces, models — no Android dependencies
- Data layer: Repository implementations, data sources, mappers
- Presentation layer: ViewModel, UI State, Compose screens (MVI pattern)

## Multi-module Structure
- Split every feature into two modules:
    - `:feature:api` — public contracts (interfaces, models, navigation routes)
    - `:feature:impl` — implementation, internal, not accessible from outside
- Other modules depend only on `:api`, never on `:impl` directly
- Cross-module communication only through interfaces defined in `:api`

## Coding Rules
- Don't add unnecessary comments — code should be self-explanatory
- Never use `!!` operator, always handle nullability properly
- Prefer extension functions over utility classes
- Write tests for Use Cases and ViewModels

## Don'ts
- Don't refactor code that wasn't asked to change
- Don't add dependencies without asking first
- Don't use deprecated APIs
