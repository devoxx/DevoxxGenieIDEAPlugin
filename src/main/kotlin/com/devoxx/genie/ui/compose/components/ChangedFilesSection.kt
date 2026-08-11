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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.model.ChangedFileUiModel
import com.devoxx.genie.ui.compose.theme.DevoxxBlue
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor

private val AddedGreen = Color(0xFF3FB950)
private val RemovedRed = Color(0xFFF85149)

/**
 * Lists the files an agent run changed, each opening a diff of the file before the run
 * against its current state (issue #705). Starts expanded — unlike the file-reference
 * section this is the review surface, so hiding it by default would defeat the point.
 */
@Composable
fun ChangedFilesSection(
    files: List<ChangedFileUiModel>,
    onChangedFileClick: (ChangedFileUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (files.isEmpty()) return

    var expanded by remember { mutableStateOf(true) }
    val typography = DevoxxGenieThemeAccessor.typography

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = if (expanded) "▼" else "▶",
                style = typography.caption.copy(color = DevoxxBlue),
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                text = if (files.size == 1) "Agent changed 1 file" else "Agent changed ${files.size} files",
                style = typography.caption.copy(color = DevoxxBlue),
            )
        }

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
                            .then(
                                if (file.diffable) {
                                    Modifier.clickable { onChangedFileClick(file) }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "📄",
                            style = typography.caption,
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = file.fileName,
                            style = typography.caption.copy(
                                color = if (file.diffable) DevoxxBlue else typography.caption.color,
                                textDecoration = if (file.diffable) TextDecoration.Underline else TextDecoration.None,
                            ),
                        )
                        if (file.linesAdded > 0 || file.linesRemoved > 0) {
                            Spacer(Modifier.width(8.dp))
                            BasicText(
                                text = "+${file.linesAdded}",
                                style = typography.caption.copy(color = AddedGreen),
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                text = "-${file.linesRemoved}",
                                style = typography.caption.copy(color = RemovedRed),
                            )
                        }
                    }
                }
            }
        }
    }
}
