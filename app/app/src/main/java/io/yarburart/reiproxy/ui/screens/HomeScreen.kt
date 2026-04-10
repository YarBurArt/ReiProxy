package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import io.yarburart.reiproxy.data.Project
import io.yarburart.reiproxy.ui.components.EmptyState
import io.yarburart.reiproxy.ui.components.ScreenTitle

@PreviewScreenSizes
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    projects: List<Project> = emptyList(),
    activeProjectId: Long? = null,
    historyCount: Int = 0,
    proxyRunning: Boolean = false,
    onProjectSelected: (Long?) -> Unit = {},
    onProjectCreated: (String, String) -> Unit = { _, _ -> },
    onProjectDeleted: (Project) -> Unit = {},
    onStartProxy: () -> Unit = {},
    onStopProxy: () -> Unit = {},
) {
    var showDetail by remember { mutableStateOf<Long?>(null) }

    if (showDetail != null) {
        val project = projects.find { it.id == showDetail }
        if (project != null) {
            ProjectDetailView(
                modifier = modifier,
                project = project,
                isActive = activeProjectId == project.id,
                historyCount = historyCount,
                proxyRunning = proxyRunning,
                onBack = { showDetail = null },
                onSetActive = { onProjectSelected(project.id) },
                onStartProxy = onStartProxy,
                onStopProxy = onStopProxy,
            )
        } else {
            showDetail = null
        }
    } else {
        ProjectListView(
            modifier = modifier,
            projects = projects,
            activeProjectId = activeProjectId,
            onProjectSelected = { project ->
                showDetail = project.id
            },
            onProjectActivated = { id -> onProjectSelected(id) },
            onProjectCreated = onProjectCreated,
            onProjectDeleted = onProjectDeleted,
        )
    }
}

@Composable
private fun ProjectListView(
    modifier: Modifier = Modifier,
    projects: List<Project> = emptyList(),
    activeProjectId: Long? = null,
    onProjectSelected: (Project) -> Unit = {},
    onProjectActivated: (Long?) -> Unit = {},
    onProjectCreated: (String, String) -> Unit = { _, _ -> },
    onProjectDeleted: (Project) -> Unit = {},
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectDesc by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle("Projects", modifier = Modifier.weight(1f))
            IconButton(onClick = { showCreateDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(
                onClick = {
                    selectedIndex?.let { idx ->
                        if (idx < projects.size) {
                            onProjectDeleted(projects[idx])
                        }
                    }
                    selectedIndex = null
                },
                enabled = selectedIndex != null
            ) {
                Text("−", style = MaterialTheme.typography.headlineMedium)
            }
        }

        if (projects.isEmpty()) {
            EmptyState("No projects yet. Tap + to create one.")
        } else {
            LazyColumn {
                items(projects) { project ->
                    val idx = projects.indexOf(project)
                    val isSelected = selectedIndex == idx
                    val isActive = activeProjectId == project.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedIndex = if (selectedIndex == idx) null else idx
                                if (selectedIndex == idx) {
                                    onProjectSelected(project)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = project.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (isActive) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newProjectName = ""
                newProjectDesc = ""
            },
            title = { Text("New Project") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProjectDesc,
                        onValueChange = { newProjectDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            onProjectCreated(newProjectName, newProjectDesc)
                            showCreateDialog = false
                            newProjectName = ""
                            newProjectDesc = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newProjectName = ""
                    newProjectDesc = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeaderLocal(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ProjectDetailView(
    modifier: Modifier = Modifier,
    project: Project,
    isActive: Boolean,
    historyCount: Int,
    proxyRunning: Boolean,
    onBack: () -> Unit = {},
    onSetActive: () -> Unit = {},
    onStartProxy: () -> Unit = {},
    onStopProxy: () -> Unit = {},
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Back button + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Project Details", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Project info
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Name", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(project.name, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            Text("Description", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(project.description, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Captured Requests", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$historyCount requests", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Active status of project
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Active Project", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (isActive) "Yes — History, Repeat, Automate will use this"
                        else "No — Select to use this project",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (!isActive) { Button(onClick = onSetActive) { Text("Activate") }
                } else {
                    Text("✓", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Proxy control
        SectionHeaderLocal("Proxy")
        Text(
            text = if (proxyRunning) "● Running" else "○ Stopped",
            color = if (proxyRunning)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = if (proxyRunning) onStopProxy else onStartProxy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (proxyRunning) "Stop Proxy" else "Start Proxy")
        }
    }
}
