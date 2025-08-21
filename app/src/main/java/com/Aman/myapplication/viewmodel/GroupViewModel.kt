// Updated GroupViewModel.kt with better error handling and debugging

package com.Aman.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Aman.myapplication.Friend
import com.Aman.myapplication.Group
import com.Aman.myapplication.GroupMember
import com.Aman.myapplication.GroupRepository
import com.Aman.myapplication.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel(private val groupRepository: GroupRepository) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun createGroup(name: String, description: String?, userId: String, selectedFriends: List<String> = emptyList()) {
        viewModelScope.launch {
            Log.d("GroupViewModel", "Starting group creation: name=$name, userId=$userId, friends=${selectedFriends.size}")

            _isLoading.value = true
            _error.value = null

            try {
                // Validate inputs
                if (name.isBlank()) {
                    _error.value = "Group name cannot be empty"
                    _isLoading.value = false
                    return@launch
                }

                if (userId.isBlank()) {
                    _error.value = "User ID is required"
                    _isLoading.value = false
                    return@launch
                }

                Log.d("GroupViewModel", "Calling repository.createGroup...")

                groupRepository.createGroup(name, description, userId).fold(
                    onSuccess = { group ->
                        Log.d("GroupViewModel", "Group created successfully: ${group.id}")

                        // Add selected friends to the group
                        var allMembersAdded = true
                        var addedCount = 0

                        selectedFriends.forEach { friendId ->
                            Log.d("GroupViewModel", "Adding friend $friendId to group ${group.id}")
                            groupRepository.addMemberToGroup(group.id, friendId).fold(
                                onSuccess = {
                                    addedCount++
                                    Log.d("GroupViewModel", "Successfully added friend $friendId")
                                },
                                onFailure = { exception ->
                                    allMembersAdded = false
                                    Log.e("GroupViewModel", "Failed to add friend $friendId: ${exception.message}")
                                }
                            )
                        }

                        val message = when {
                            selectedFriends.isEmpty() -> "Group created successfully!"
                            allMembersAdded -> "Group created and all ${selectedFriends.size} members added successfully!"
                            addedCount > 0 -> "Group created! $addedCount of ${selectedFriends.size} members added."
                            else -> "Group created! Could not add any members."
                        }

                        Log.d("GroupViewModel", "Final result: $message")
                        _successMessage.value = message

                        // Refresh the groups list
                        loadUserGroups(userId)
                    },
                    onFailure = { exception ->
                        Log.e("GroupViewModel", "Failed to create group: ${exception.message}", exception)
                        _error.value = exception.message ?: "Failed to create group"
                    }
                )
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Unexpected error during group creation: ${e.message}", e)
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
                Log.d("GroupViewModel", "Group creation process completed")
            }
        }
    }

    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            Log.d("GroupViewModel", "Loading groups for user: $userId")
            _isLoading.value = true
            _error.value = null

            groupRepository.getUserGroups(userId).fold(
                onSuccess = { groups ->
                    Log.d("GroupViewModel", "Loaded ${groups.size} groups")
                    _groups.value = groups
                },
                onFailure = { exception ->
                    Log.e("GroupViewModel", "Failed to load groups: ${exception.message}", exception)
                    _error.value = exception.message ?: "Failed to load groups"
                }
            )
            _isLoading.value = false
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            groupRepository.getGroupMembers(groupId).fold(
                onSuccess = { members ->
                    _groupMembers.value = members
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load group members"
                }
            )
            _isLoading.value = false
        }
    }

    fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Load both group info and members
            groupRepository.getGroupById(groupId).fold(
                onSuccess = { group ->
                    _currentGroup.value = group
                    loadGroupMembers(groupId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load group details"
                    _isLoading.value = false
                }
            )
        }
    }

    fun loadUserFriends(userId: String) {
        viewModelScope.launch {
            Log.d("GroupViewModel", "Loading friends for user: $userId")
            _error.value = null

            groupRepository.getUserFriends(userId).fold(
                onSuccess = { friends ->
                    Log.d("GroupViewModel", "Loaded ${friends.size} friends")
                    _friends.value = friends
                },
                onFailure = { exception ->
                    Log.e("GroupViewModel", "Failed to load friends: ${exception.message}")
                    _error.value = exception.message ?: "Failed to load friends"
                }
            )
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _error.value = null

            if (query.isNotBlank()) {
                groupRepository.searchUsers(query).fold(
                    onSuccess = { users ->
                        _searchResults.value = users
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to search users"
                    }
                )
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun sendFriendRequest(userId: String, friendEmail: String) {
        viewModelScope.launch {
            _error.value = null

            groupRepository.sendFriendRequest(userId, friendEmail).fold(
                onSuccess = {
                    _successMessage.value = "Friend request sent!"
                    // Optionally refresh search results or friends list
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to send friend request"
                }
            )
        }
    }

    fun addMemberToGroup(groupId: String, userId: String, role: String = "member") {
        viewModelScope.launch {
            _error.value = null

            groupRepository.addMemberToGroup(groupId, userId, role).fold(
                onSuccess = {
                    _successMessage.value = "Member added successfully!"
                    loadGroupMembers(groupId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to add member"
                }
            )
        }
    }

    fun removeMemberFromGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            _error.value = null

            groupRepository.removeMemberFromGroup(groupId, userId).fold(
                onSuccess = {
                    _successMessage.value = "Member removed successfully!"
                    loadGroupMembers(groupId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to remove member"
                }
            )
        }
    }

    fun updateGroup(groupId: String, name: String, description: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            groupRepository.updateGroup(groupId, name, description).fold(
                onSuccess = { group ->
                    _currentGroup.value = group
                    _successMessage.value = "Group updated successfully!"
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update group"
                }
            )
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}