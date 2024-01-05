package com.ivanovsky.passnotes.domain.entity

enum class SearchType {
    FUZZY,
    STRICT;

    companion object {

        fun default(): SearchType = STRICT

        fun getByName(name: String): SearchType? {
            return values().firstOrNull { it.name == name }
        }
    }
}