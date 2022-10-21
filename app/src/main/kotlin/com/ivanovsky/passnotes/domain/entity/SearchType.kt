package com.ivanovsky.passnotes.domain.entity

enum class SearchType {
    FUZZY,
    STRICT;

    companion object {

        fun default(): SearchType = FUZZY

        fun getByName(name: String): SearchType? {
            return SearchType.values().firstOrNull { it.name == name }
        }
    }
}