package com.chatsphere.presentation.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CreateGroupScreen(viewModel: GroupsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::updateName,
            label = { Text("Group name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.memberIds,
            onValueChange = viewModel::updateMembers,
            label = { Text("Member IDs, comma separated") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Button(onClick = viewModel::createGroup, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("Create group")
        }
        state.message?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
    }
}
