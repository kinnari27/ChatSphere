package com.chatsphere.presentation.groups

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chatsphere.presentation.theme.ChatSphereTheme

@Composable
fun CreateGroupScreen(viewModel: GroupsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    CreateGroupContent(
        state = state,
        onUpdateName = viewModel::updateName,
        onUpdateMembers = viewModel::updateMembers,
        onCreateGroup = viewModel::createGroup
    )
}

@Composable
fun CreateGroupContent(
    state: GroupsUiState,
    onUpdateName: (String) -> Unit,
    onUpdateMembers: (String) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onUpdateName,
            label = { Text("Group name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.memberIds,
            onValueChange = onUpdateMembers,
            label = { Text("Member IDs, comma separated") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Button(onClick = onCreateGroup, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("Create group")
        }
        state.message?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CreateGroupPreview() {
    ChatSphereTheme {
        Surface {
            CreateGroupContent(
                state = GroupsUiState(name = "Awesome Group"),
                onUpdateName = {},
                onUpdateMembers = {},
                onCreateGroup = {}
            )
        }
    }
}
