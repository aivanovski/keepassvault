package com.ivanovsky.passnotes.data.repository.keepass.model

data class DatabaseFixture(
    val hash: HashAlgorithm,
    val requestedSize: DatabaseSize,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DatabaseFixture

        if (hash != other.hash) return false
        if (requestedSize != other.requestedSize) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + requestedSize.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}