# Project Overview
KeePassVault is an Android application to view and modify KeePass database files.
Users can synchronize KeePass file with remove server using WebDav or Git.
Project consists of several modules:
- `:app` - The main Android application, written in Kotlin (some legacy parts are in Java)
- `:keepass-rs-android` - Provides Kotlin interface for calling JNI wrapper for keepass-rs module
- `:keepass-rs` - The Rust JNI wrapper over Rust crate keepass-rs

# Technology Stack
- Languages: Kotlin (some legacy parts on Java), Rust (for decoding database)
- UI Framework: Compose, View for old screens
- Architecture: MVVM + Clean Architecture
- Navigation: Cicerone
- Dependency Injection: Koin

# Android app directory Structure
The project follows Clean Architecture pattern
- /data - responsible for work with KeePass file, server synchronization, encrypting
- /domain - reusable UseCases and Interactors across the app, plus not ui related logic (eg crash reporting, logging)
- /presentation - UI for screens
- /injection - Modules for DependencyInjections

# Commands to build and test
- Verification: ./gradlew app:assembleFdroidDebug
- Build the app: ./gradlew app:assembleFdroidDebug
- Fix code formatting: ./gradlew app:spotlessApply
- Build: ./gradlew test

# Continuous Integration
The workflows are defined in .github/workflows/*.yaml

# Key Guidelines for Agents
- Don't write tests until it is requested
- Before creating a PR:
    - Do a code review for the changes. Don't change anything in the code, just highlight issues may found in the diff
    - Validate that app compiles
    - Run tests
    - Validate code formatting and commit changes if there are some
