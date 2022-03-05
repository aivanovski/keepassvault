package com.ivanovsky.passnotes.domain.entity

enum class SortType {
    DEFAULT,
    TITLE,
    MODIFICATION_DATE,
    CREATION_DATE;

    companion object {

        fun default(): SortType = TITLE

        fun getByName(name: String): SortType? {
            return values().firstOrNull { it.name == name }
        }
    }
}