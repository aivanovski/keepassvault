package com.ivanovsky.passnotes.domain.usecases.diff

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.extensions.toEither
import timber.log.Timber

class ApplyDiffUseCase(
    private val dbRepository: EncryptedDatabaseRepository,
    private val settings: Settings
) {

    fun applyDiff(
        db: EncryptedDatabase,
        diff: List<DiffEvent<EncryptedDatabaseElement>>
    ): Either<OperationError, EncryptedDatabase> =
        either {
            val sortedDiff = DiffSorter().sort(diff)

            Timber.d(
                "Applying %s diff events to database %s".format(
                    diff.size,
                    db.getFile().name
                )
            )

            val events = mutableListOf<DiffEvent<EncryptedDatabaseElement>>()
                .apply {
                    addAll(sortedDiff)
                }

            var index = 0
            while (index < events.size) {
                val event = events[index]

                applyEvent(db, event).bind()

                index++
            }

            db.commit().bind()

            db
        }

    private fun applyEvent(
        db: EncryptedDatabase,
        event: DiffEvent<EncryptedDatabaseElement>
    ): Either<OperationError, Unit> =
        either {
            Timber.d("    apply event: $event")

            val entity = event.getEntity()

            @Suppress("UNCHECKED_CAST")
            when (entity) {
                is Group -> applyGroupEvent(db, event as DiffEvent<Group>).bind()
                is Note -> applyNoteEvent(db, event as DiffEvent<Note>).bind()
                is Property -> applyPropertyEvent(db, event as DiffEvent<Property>).bind()
            }
        }

    private fun applyGroupEvent(
        db: EncryptedDatabase,
        event: DiffEvent<Group>
    ): Either<OperationError, Unit> =
        either {
            val group = event.getEntity()

            when (event) {
                is DiffEvent.Insert<*> -> {
                    db.groupDao.insert(
                        GroupEntity(
                            uid = group.uid,
                            parentUid = group.parentUid,
                            title = group.title,
                            autotypeEnabled = group.autotypeEnabled,
                            searchEnabled = group.searchEnabled
                        ),
                        false
                    ).toEither().bind()
                }

                is DiffEvent.Delete<*> -> {
                    db.groupDao.remove(group.uid).toEither().bind()
                }

                is DiffEvent.Update -> {
                    raise(newDbError("Invalid diff event: $event", Stacktrace()))
                }
            }
        }

    private fun applyNoteEvent(
        db: EncryptedDatabase,
        event: DiffEvent<Note>
    ): Either<OperationError, Unit> =
        either {
            val note = event.getEntity()

            when (event) {
                is DiffEvent.Insert<*> -> {
                    db.noteDao.insert(
                        listOf(note),
                        false
                    ).toEither().bind()
                }

                is DiffEvent.Delete<*> -> {
                    val noteUid = note.uid
                        ?: raise(newDbError("Invalid delete event: $event", Stacktrace()))

                    db.noteDao.remove(noteUid).toEither().bind()
                }

                is DiffEvent.Update -> {
                    raise(newDbError("Invalid diff event: $event", Stacktrace()))
                }
            }
        }

    private fun applyPropertyEvent(
        db: EncryptedDatabase,
        event: DiffEvent<Property>
    ): Either<OperationError, Unit> =
        either {
            when (event) {
                is DiffEvent.Insert -> {
                    val property = event.entity
                    val noteUid = event.parentUuid
                        ?: raise(newDbError("Invalid event: $event", Stacktrace()))

                    val note = db.noteDao.getNoteByUid(noteUid).toEither().bind()

                    val newProperties = note.properties.plus(property)

                    db.noteDao.update(
                        note.copy(properties = newProperties),
                        false
                    ).toEither().bind()
                }

                is DiffEvent.Delete -> {
                    val property = event.entity
                    val noteUid = event.parentUuid
                        ?: raise(newDbError("Invalid event: $event", Stacktrace()))

                    val note = db.noteDao.getNoteByUid(noteUid).toEither().bind()

                    val newProperties = note.properties.filter { prop ->
                        prop.name != property.name
                    }

                    db.noteDao.update(
                        note.copy(properties = newProperties),
                        false
                    ).toEither().bind()
                }

                is DiffEvent.Update -> {
                    val oldProperty = event.oldEntity
                    val newProperty = event.newEntity

                    val noteUid = event.newParentUuid
                        ?: raise(newDbError("Invalid event: $event", Stacktrace()))

                    val note = db.noteDao.getNoteByUid(noteUid).toEither().bind()

                    val propertyIndex = note.properties.indexOfFirst { prop ->
                        prop.name == oldProperty.name
                    }

                    if (propertyIndex in note.properties.indices) {
                        val newProperties = note.properties.toMutableList()
                            .apply {
                                set(propertyIndex, newProperty)
                            }

                        db.noteDao.update(
                            note.copy(properties = newProperties),
                            false
                        ).toEither().bind()
                    }
                }
            }
        }
}