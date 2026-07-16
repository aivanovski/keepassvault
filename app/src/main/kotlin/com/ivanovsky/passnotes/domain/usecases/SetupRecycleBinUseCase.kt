package com.ivanovsky.passnotes.domain.usecases

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.toEither
import kotlinx.coroutines.withContext

class SetupRecycleBinUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun setupRecycleBinGroup(): Either<OperationError, Group> =
        withContext(dispatchers.IO) {
            either {
                val db = getDbUseCase.getDatabase().toEither().bind()

                val root = db.groupDao.rootGroup.toEither().bind()
                val children = db.groupDao.getChildGroups(root.uid).toEither().bind()

                val recycleBin = children
                    .firstOrNull { group -> group.title == RECYCLE_BIN_NAME }
                    ?: children.firstOrNull { group ->
                        group.title.equals(
                            RECYCLE_BIN_NAME,
                            ignoreCase = true
                        )
                    }

                if (recycleBin == null) {
                    val groupUid = db.groupDao.insert(
                        GroupEntity(
                            parentUid = root.uid,
                            title = RECYCLE_BIN_NAME,
                            autotypeEnabled = InheritableBooleanOption.DISABLED,
                            searchEnabled = InheritableBooleanOption.DISABLED
                        )
                    ).toEither().bind()

                    db.groupDao.getGroupByUid(groupUid).toEither().bind()
                } else {
                    recycleBin
                }
            }
        }

    companion object {
        const val RECYCLE_BIN_NAME = "Recycle bin"
    }
}