package com.ivanovsky.passnotes.presentation.core.compose.cells

class IllegalCellModelException(
    model: Any
) : IllegalArgumentException(
    "Unable to create CellViewModel for model: ${model::class.qualifiedName}"
)