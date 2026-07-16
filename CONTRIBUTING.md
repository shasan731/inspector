# Contributing

Use a focused branch and keep unrelated changes out of the same pull request. Java 17 and the checked-in Gradle Wrapper are required; Android Studio is optional.

Before opening a pull request, run:

```bash
./gradlew detekt lintDebug testDebugUnitTest assembleDebug
```

New hardware behavior must be behind a repository interface, lifecycle-aware, permission-safe, and accompanied by a fake that works in CI. Never infer success from API invocation alone when human observation is required. Missing optional hardware must be unsupported rather than failed.

Keep user-visible text in both English and Bangla resources. Add tests for score, persistence, import, entitlement, and state-transition changes. Do not commit personal inspection data, secrets, keystores, generated reports, full-resolution evidence, or vendor-restricted identifiers.

By contributing, you agree that your contribution is licensed under Apache License 2.0.

