package com.ivanovsky.passnotes.domain.entity

enum class LoggingType {
    RELEASE,
    DEBUG,
    FILE;

    companion object {
        fun getByName(name: String): LoggingType? = values().firstOrNull { it.name == name }
    }
}