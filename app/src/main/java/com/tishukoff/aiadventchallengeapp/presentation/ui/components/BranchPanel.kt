package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme
import com.tishukoff.feature.agent.api.BranchInfo

@Composable
fun BranchPanel(
    branches: List<BranchInfo>,
    currentBranchId: String?,
    onCreateCheckpoint: (String) -> Unit,
    onCreateBranch: (checkpointId: String, name: String) -> Unit,
    onSwitchBranch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCheckpointDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var checkpointName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Branches",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row {
                AssistChip(
                    onClick = { showCheckpointDialog = true },
                    label = { Text("Checkpoint") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.then(
                                Modifier.padding(0.dp)
                            ),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                AssistChip(
                    onClick = { showBranchDialog = true },
                    label = { Text("Branch") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                )
            }
        }

        if (branches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                branches.forEach { branch ->
                    FilterChip(
                        selected = branch.id == currentBranchId,
                        onClick = { onSwitchBranch(branch.id) },
                        label = {
                            Text("${branch.name} (${branch.messageCount})")
                        },
                    )
                }
            }
        }
    }

    if (showCheckpointDialog) {
        AlertDialog(
            onDismissRequest = {
                showCheckpointDialog = false
                checkpointName = ""
            },
            title = { Text("Create Checkpoint") },
            text = {
                OutlinedTextField(
                    value = checkpointName,
                    onValueChange = { checkpointName = it },
                    label = { Text("Checkpoint name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (checkpointName.isNotBlank()) {
                            onCreateCheckpoint(checkpointName)
                            checkpointName = ""
                            showCheckpointDialog = false
                        }
                    },
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCheckpointDialog = false
                        checkpointName = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showBranchDialog) {
        AlertDialog(
            onDismissRequest = {
                showBranchDialog = false
                branchName = ""
            },
            title = { Text("Create Branch") },
            text = {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (branchName.isNotBlank()) {
                            val latestCheckpoint = branches.lastOrNull()
                            onCreateBranch(
                                latestCheckpoint?.id ?: "",
                                branchName,
                            )
                            branchName = ""
                            showBranchDialog = false
                        }
                    },
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBranchDialog = false
                        branchName = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BranchPanelEmptyPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        BranchPanel(
            branches = emptyList(),
            currentBranchId = null,
            onCreateCheckpoint = {},
            onCreateBranch = { _, _ -> },
            onSwitchBranch = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BranchPanelWithBranchesPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        BranchPanel(
            branches = listOf(
                BranchInfo(id = "1", name = "main", checkpointName = "start", messageCount = 12),
                BranchInfo(id = "2", name = "explore", checkpointName = "v1", messageCount = 6),
                BranchInfo(id = "3", name = "alt", checkpointName = "v1", messageCount = 3),
            ),
            currentBranchId = "2",
            onCreateCheckpoint = {},
            onCreateBranch = { _, _ -> },
            onSwitchBranch = {},
        )
    }
}
