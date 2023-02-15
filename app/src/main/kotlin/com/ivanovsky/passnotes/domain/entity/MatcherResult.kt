package com.ivanovsky.passnotes.domain.entity

data class MatcherResult<T>(
    val entry: T,
    val title: String,
    val highlights: List<Int>
)