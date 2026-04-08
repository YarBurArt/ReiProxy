package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.yarburart.reiproxy.ui.components.EmptyState
import io.yarburart.reiproxy.ui.components.ScreenTitle

data class Project(val name: String, val description: String)

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val projects = remember {
        mutableStateListOf(
            Project("Target App", "Mobile banking app - staging"),
            Project("API Gateway", "Internal microservices gateway"),
        )
    }
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
                    selectedIndex?.let { projects.removeAt(it) }
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
                itemsIndexed(projects) { index, project ->
                    val isSelected = selectedIndex == index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedIndex = if (selectedIndex == index) null else index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(project.name)
                            Text(
                                text = project.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            projects.add(Project(newProjectName, newProjectDesc))
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
