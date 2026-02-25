package com.devoxx.genie.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.FileReferenceUiModel
import com.devoxx.genie.ui.compose.theme.DevoxxBlue
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor

@Composable
fun FileReferencesSection(
    files: List<FileReferenceUiModel>,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (files.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val typography = DevoxxGenieThemeAccessor.typography

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = if (expanded) "\u25BC" else "\u25B6",
                style = typography.caption.copy(color = DevoxxBlue),
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                text = "Files (${files.size})",
                style = typography.caption.copy(color = DevoxxBlue),
            )
        }

        // File list
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                files.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFileClick(file.filePath) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "\uD83D\uDCC4",
                            style = typography.caption,
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = file.fileName,
                            style = typography.caption.copy(
                                color = DevoxxBlue,
                                textDecoration = TextDecoration.Underline,
                            ),
                        )
                    }
                }
            }
        }
    }
}
