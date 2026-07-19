# Java-to-Kotlin migration plan

## Goal and scope

Migrate all 30 Java source files currently in `:app` (3,068 lines) to Kotlin without changing application behavior or public contracts. Keep each migration small enough to review, compile, and commit independently. Do not combine the mechanical language conversion with unrelated refactoring.

## Migration rules

- [ ] Record a clean baseline by running `./gradlew app:compileFdroidDebugKotlin` and `./gradlew test` before the first migration.
- [ ] Convert one checklist item at a time unless a tightly coupled group cannot compile separately.
- [ ] Preserve the package, class name, visibility, annotations, Room table/column/query definitions, overloads, checked-exception behavior, and Java-callable API.
- [ ] Derive Kotlin nullability from `@NonNull`/`@Nullable`, implementations, and call sites; do not rely only on the IDE converter's platform types.
- [ ] Preserve Java interoperability where callers still require it (`companion object`, `const val`, `@JvmStatic`, `@JvmField`, `@JvmOverloads`, or explicit getter/setter names).
- [ ] Prefer idiomatic Kotlin only when it does not alter equality, hashing, mutability, serialization, Room construction, default values, or exception behavior.
- [ ] Replace each `.java` file with the same-package `.kt` file in one change; verify that no duplicate declaration remains.
- [ ] After every item, run `./gradlew app:compileFdroidDebugKotlin`. Run relevant existing tests when the migrated type has direct coverage. Do not add tests unless separately requested.
- [ ] Before committing a phase, inspect the diff for accidental API or behavior changes and run `./gradlew app:spotlessApply`, then compile again.

## Phase 1: Small, dependency-light types and utilities

These provide quick validation of Kotlin/Java interop and remove leaf Java types first.

- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/OnConflictStrategy.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/SyncStrategy.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/util/ObjectUtils.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/util/CollectionUtils.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/util/DateUtils.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/util/LocaleUtils.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/util/InputMethodUtils.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/BaseRemoteFileOutputStream.java`

Pay particular attention to static utility call sites: preserve source compatibility with `@JvmStatic` while Java callers remain. Preserve nullable return values and the `OutputStream` contract.

## Phase 2: Remote filesystem exception hierarchy

Convert parent classes before subclasses and preserve constructor messages and inheritance exactly.

- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/exception/RemoteFSException.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/exception/RemoteFSApiException.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/exception/RemoteFSAuthException.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/exception/RemoteFSFileNotFoundException.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/exception/RemoteFSNetworkException.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/exception/InternalCacheException.java`

## Phase 3: Core result and persistence models

These types have broad call-site impact. Preserve their existing mutable/JavaBean shape until all consumers have migrated; do not automatically turn them into data or sealed classes.

- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/entity/OperationError.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/entity/OperationResult.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/entity/RemoteFile.java`

For `OperationError`, retain `Serializable`, constants, factory methods, equality, and hash code semantics. For generic `OperationResult`, verify factory-method type inference, deferred/error state transitions, and nullable payload behavior from both Kotlin and remaining Java callers. For `RemoteFile`, retain its no-argument construction path, mutable accessors, Room annotations/defaults, and equality/hash code behavior.

## Phase 4: DAO and repository contracts

Convert interfaces before their larger implementations. Kotlin nullability here becomes part of the implementation contract, so compare every signature with all implementors and callers.

- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/db/dao/RemoteFileDao.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/db/dao/UsedFileDao.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/encdb/dao/GroupDao.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/encdb/dao/NoteDao.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/encdb/EncryptedDatabase.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/EncryptedDatabaseRepository.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/FileSystemProvider.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/FileSystemSyncProcessor.java`

For Room DAOs, retain every annotation and SQL string verbatim and verify KSP generation during compilation. Check JVM signatures for overloaded methods and property-like getters such as `getDatabase()`, `getFile()`, and `getSyncProcessor()` before converting them to Kotlin properties.

## Phase 5: Observer infrastructure and output streams

- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/ObserverBus.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/OfflineFileOutputStream.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/RemoteFileOutputStream.java`

For `ObserverBus`, preserve nested interface names, registration/removal behavior, main-thread dispatch, collection/thread-safety choices, and reflection-based behavior. For both streams, preserve buffering, temporary-file ownership, `flush()`/`close()` ordering, upload/offline state changes, cleanup, logging, and exact `IOException` propagation.

## Phase 6: Large remote filesystem implementations

Migrate these last because they depend on most preceding types. Split review by method even if each class must be replaced atomically.

- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/RemoteFileSyncProcessor.java`
- [ ] `app/src/main/java/com/ivanovsky/passnotes/data/repository/file/remote/RemoteFileSystemProvider.java`

For each class:

- [ ] First perform a syntax-focused conversion with no algorithmic refactoring.
- [ ] Compare every overridden method against its newly migrated Kotlin interface.
- [ ] Audit nullable cache/database/API values before using `!!`; prefer explicit failure paths matching the Java behavior.
- [ ] Verify synchronization, locks, callbacks, observer notifications, revision/conflict decisions, file cleanup, and network-to-domain error mapping method by method.
- [ ] Preserve static constants/factories used by remaining Java or generated code.

## Final verification and cleanup

- [ ] Confirm the inventory is empty with `rg --files -g '*.java'` (or document any intentionally generated/vendor Java files if introduced later).
- [ ] Search for stale Java-only suppressions, `@NonNull`/`@Nullable` imports, redundant `@Jvm*` bridges, and obsolete converter artifacts; remove only those proven unnecessary.
- [ ] Run `./gradlew app:spotlessApply` and review any formatting changes.
- [ ] Run `./gradlew app:compileFdroidDebugKotlin`.
- [ ] Run `./gradlew test`.
- [ ] Run `./gradlew app:assembleFdroidDebug`.
- [ ] Review the complete migration diff without changing code during that review; record any issues found for follow-up.
- [ ] Confirm `git status` contains only intended migration and formatting changes before the final commit/PR.

## Progress

- Total Java files: **30**
- Migrated: **0 / 30**
- Remaining: **30 / 30**

Update these totals whenever a file checklist item is completed.
