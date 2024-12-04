package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun contentDescription(description: String): Modifier {
    return remember {
        Modifier.semantics {
            contentDescription = description
        }
    }
}