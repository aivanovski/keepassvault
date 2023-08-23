package com.ivanovsky.passnotes.domain.entity

enum class PasswordResource(val symbols: String) {
    UPPERCASE("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
    LOWERCASE("abcdefghijklmnopqrstuvwxyz"),
    DIGITS("0123456789"),
    MINUS("-"),
    UNDERSCORE("_"),
    SPACE(" "),
    SPECIAL("&/,^@.#:%\\\"='$!*`;+"),
    BRACKETS("[](){}<>")
}