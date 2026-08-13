package com.ivanovsky.passnotes.presentation.core.compose.cells

@JvmInline
value class IntCellId(val id: Int)

@JvmInline
value class GeneratorBaseId(val id: Int)

fun Int.toId(): IntCellId =
    IntCellId(this)

fun Int.toBaseId(): GeneratorBaseId =
    GeneratorBaseId(this)