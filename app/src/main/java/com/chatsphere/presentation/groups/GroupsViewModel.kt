package com.chatsphere.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatsphere.core.common.AppResult
import com.chatsphere.core.common.UiState
import com.chatsphere.domain.usecase.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val name: String = "",
    val memberIds: String = "",
    val isCreating: Boolean = false,
    val message: String? = null
) : UiState

@HiltViewModel
class GroupsViewModel @Inject constructor(private val createGroupUseCase: CreateGroupUseCase) : ViewModel() {
    private val _state = MutableStateFlow(GroupsUiState())
    val state = _state.asStateFlow()

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateMembers(value: String) = _state.update { it.copy(memberIds = value) }

    fun createGroup() = viewModelScope.launch {
        _state.update { it.copy(isCreating = true, message = null) }
        val members = state.value.memberIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        when (createGroupUseCase(state.value.name, members)) {
            is AppResult.Success -> _state.update { it.copy(isCreating = false, message = "Group created") }
            is AppResult.Error -> _state.update { it.copy(isCreating = false, message = "Unable to create group") }
            AppResult.Loading -> Unit
        }
    }
}
