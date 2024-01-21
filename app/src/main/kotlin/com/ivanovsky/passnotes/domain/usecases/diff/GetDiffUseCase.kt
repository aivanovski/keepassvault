package com.ivanovsky.passnotes.domain.usecases.diff

import com.github.aivanovski.keepasstreediff.PathDiffer
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import kotlinx.coroutines.withContext

class GetDiffUseCase(
    private val dispatchers: DispatcherProvider
) {

    suspend fun getDiff(
        lhs: KotpassDatabase,
        rhs: KotpassDatabase
    ): List<DiffListItem> =
        withContext(dispatchers.Default) {
            val diff = PathDiffer().diff(
                lhs = lhs.buildNodeTree(),
                rhs = rhs.buildNodeTree()
            )
                .map { event -> event.toInternalDiffEvent() }

            DiffTransformer().transform(
                lhs = lhs,
                rhs = rhs,
                diff = diff
            )
        }
}