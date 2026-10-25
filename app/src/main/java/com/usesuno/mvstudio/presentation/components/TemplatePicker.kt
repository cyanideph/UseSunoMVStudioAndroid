package com.usesuno.mvstudio.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.usesuno.mvstudio.domain.model.Template

@Composable
fun TemplatePicker(
    selectedTemplate: Template,
    onTemplateSelected: (Template) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Template.values().forEach { template ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = template.name.replace("_", " "),
                        color = if (template == selectedTemplate) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                onClick = { onTemplateSelected(template) }
            )
        }
    }
}
