package com.ivanovsky.passnotes.presentation.core.compose.cells

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class CellIdGenerator(
    startFrom: GeneratorBaseId
) {

    private var id = AtomicInt(startFrom.id)

    fun nextId(): IntCellId {
        return id.fetchAndIncrement().toId()
    }
}