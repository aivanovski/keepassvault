package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.data.entity.SyncResolution
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.util.isNewerThan

class SyncStrategyResolver {

    fun resolve(
        localModified: Long?,
        cachedRemoteModified: Long?,
        remoteModified: Long?,
        syncStrategy: SyncStrategy
    ): SyncResolution {
        return when (syncStrategy) {
            SyncStrategy.LAST_MODIFICATION_WINS -> resolveForLastModificationWins(
                localModified,
                remoteModified
            )
            SyncStrategy.LAST_REMOTE_MODIFICATION_WINS -> resolveForLastRemoteModificationWind(
                localModified,
                cachedRemoteModified,
                remoteModified
            )
        }
    }

    private fun resolveForLastModificationWins(
        localModified: Long?,
        remoteModified: Long?
    ): SyncResolution {
        return when {
            remoteModified.isNewerThan(localModified) -> {
                SyncResolution.REMOTE
            }
            localModified.isNewerThan(remoteModified) -> {
                SyncResolution.LOCAL
            }
            localModified == remoteModified -> {
                SyncResolution.EQUALS
            }
            else -> {
                SyncResolution.ERROR
            }
        }
    }

    private fun resolveForLastRemoteModificationWind(
        localModified: Long?,
        cachedRemoteModified: Long?,
        remoteModified: Long?
    ): SyncResolution {
        return when {
            remoteModified.isNewerThan(localModified) -> {
                SyncResolution.REMOTE
            }
            localModified.isNewerThan(remoteModified) -> {
                if (remoteModified.isNewerThan(cachedRemoteModified)) {
                    SyncResolution.ERROR
                } else {
                    SyncResolution.LOCAL
                }
            }
            localModified == remoteModified -> {
                SyncResolution.EQUALS
            }
            else -> {
                SyncResolution.ERROR
            }
        }
    }
}