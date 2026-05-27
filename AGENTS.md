# Project Overview
KeePassVault is an Android application written in Kotlin.
It provides possibility to view and modify KeePass database files.
Users can synchronize KeePass file with remove server using WebDav or Git.

# Technology Stack
- Language: Kotlin (some legacy parts on Java)
- UI Framework: Compose for new screens, View for old
- Architecture: MVVM + Clean Architecture
- Navigation: Cicerone
- Dependency Injection: Koin

# Directory Structure
The project follows Clean Architecture pattern
- /data - responsible for work with KeePass file, server synchronization, encrypting
- /domain - UseCases + Interactors and not ui related logic
- /presentation - UI for screens
- /injection - Modules for DependencyInjections

# Commands to build and test
- Verification: ./gradlew app:compileFdroidDebugKotlin
- Build the app: ./gradlew app:assembleFdroidDebug
- Fix code formatting: ./gradlew app:spotlessApply
- Build: ./gradlew test

# Continuous Integration
The workflows are defined in .github/workflows/*.yaml

# Key Guidelines for Agents
- Before creating a PR:
    - Validate that app compiles
    - Run tests
    - Validate code formatting
