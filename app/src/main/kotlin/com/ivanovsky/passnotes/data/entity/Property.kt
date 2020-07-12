package com.ivanovsky.passnotes.data.entity

data class Property(
    val type: PropertyType? = null,
    val name: String? = null,
    val value: String? = null,
    val protected: Boolean = false
)