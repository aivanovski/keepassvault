package com.ivanovsky.passnotes.presentation.core.compose

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (option: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = {
            isExpanded = !isExpanded
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            textStyle = PrimaryTextStyle(),
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            value = selectedOption,
            label = {
                Text(text = label)
            },
            onValueChange = { },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            colors = AppTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            for (option in options) {
                DropdownMenuItem(
                    onClick = {
                        isExpanded = false
                        onOptionSelected.invoke(option)
                    },
                    text = {
                        Text(
                            text = option
                        )
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}