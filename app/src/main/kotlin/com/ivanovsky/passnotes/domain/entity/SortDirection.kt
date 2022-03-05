package com.ivanovsky.passnotes.domain.entity

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    companion object {

        fun default(): SortDirection = ASCENDING

        fun getByName(name: String): SortDirection? {
            return values().firstOrNull { it.name == name }
        }
    }
}