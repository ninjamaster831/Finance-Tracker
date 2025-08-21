// Updated GroupViewModel.kt
package com.Aman.myapplication.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Aman.myapplication.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // Existing properties...
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

    // New properties for expenses and chat
    private val _groupExpenses = MutableStateFlow<List<GroupExpense>>(emptyList())
    val groupExpenses: StateFlow<List<GroupExpense>> = _groupExpenses.asStateFlow()

    private val _userBalances = MutableStateFlow<List<UserBalance>>(emptyList())
    val userBalances: StateFlow<List<UserBalance>> = _userBalances.asStateFlow()

    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages.asStateFlow()

    private val _isLoadingExpenses = MutableStateFlow(false)
    val isLoadingExpenses: StateFlow<Boolean> = _isLoadingExpenses.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    // Existing methods... (keep all existing methods)

    fun createGroup(name: String, description: String?, userId: String, selectedFriends: List<String> = emptyList()) {
        viewModelScope.launch {
            Log.d("GroupViewModel", "Starting group creation: name=$name, userId=$userId, friends=${selectedFriends.size}")

            _isLoading.value = true
            _error.value = null

            try {
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

                groupRepository.createGroup(name, description, userId).fold(
                    onSuccess = { group ->
                        Log.d("GroupViewModel", "Group created successfully: ${group.id}")

                        var allMembersAdded = true
                        var addedCount = 0

                        selectedFriends.forEach { friendId ->
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

                        _successMessage.value = message
                        loadUserGroups(userId)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to create group"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New methods for expenses

    fun createExpense(
        groupId: String,
        title: String,
        description: String?,
        amount: Double,
        category: String,
        paidBy: String,
        splitWith: List<String>
    ) {
        viewModelScope.launch {
            _isLoadingExpenses.value = true
            _error.value = null

            try {
                if (title.isBlank()) {
                    _error.value = "Expense title cannot be empty"
                    return@launch
                }

                if (amount <= 0) {
                    _error.value = "Amount must be greater than 0"
                    return@launch
                }

                if (splitWith.isEmpty()) {
                    _error.value = "Please select at least one person to split with"
                    return@launch
                }

                val request = CreateExpenseRequest(
                    group_id = groupId,
                    paid_by = paidBy,
                    title = title,
                    description = description,
                    amount = amount,
                    category = category,
                    date = System.currentTimeMillis(),
                    split_with = splitWith
                )

                expenseRepository.createExpense(request).fold(
                    onSuccess = { expense ->
                        Log.d("GroupViewModel", "Expense created successfully: ${expense.id}")
                        _successMessage.value = "Expense added successfully!"

                        // Refresh expenses and balances
                        loadGroupExpenses(groupId)
                        calculateGroupBalances(groupId)
                        loadGroupMessages(groupId) // Refresh messages to show the expense message
                    },
                    onFailure = { exception ->
                        Log.e("GroupViewModel", "Failed to create expense: ${exception.message}")
                        _error.value = exception.message ?: "Failed to create expense"
                    }
                )
            } catch (e: Exception) {
                Log.e("GroupViewModel", "Unexpected error creating expense: ${e.message}", e)
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoadingExpenses.value = false
            }
        }
    }

    fun loadGroupExpenses(groupId: String) {
        viewModelScope.launch {
            _isLoadingExpenses.value = true
            _error.value = null

            expenseRepository.getGroupExpenses(groupId).fold(
                onSuccess = { expenses ->
                    Log.d("GroupViewModel", "Loaded ${expenses.size} expenses")
                    _groupExpenses.value = expenses
                },
                onFailure = { exception ->
                    Log.e("GroupViewModel", "Failed to load expenses: ${exception.message}")
                    _error.value = exception.message ?: "Failed to load expenses"
                }
            )
            _isLoadingExpenses.value = false
        }
    }

    fun calculateGroupBalances(groupId: String) {
        viewModelScope.launch {
            expenseRepository.calculateGroupBalances(groupId).fold(
                onSuccess = { balances ->
                    Log.d("GroupViewModel", "Calculated balances for ${balances.size} users")
                    _userBalances.value = balances
                },
                onFailure = { exception ->
                    Log.e("GroupViewModel", "Failed to calculate balances: ${exception.message}")
                    _error.value = exception.message ?: "Failed to calculate balances"
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun settleSplit(splitId: String, groupId: String) {
        viewModelScope.launch {
            expenseRepository.settleSplit(splitId).fold(
                onSuccess = {
                    _successMessage.value = "Split settled successfully!"
                    loadGroupExpenses(groupId)
                    calculateGroupBalances(groupId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to settle split"
                }
            )
        }
    }

    // New methods for chat

    fun sendMessage(groupId: String, senderId: String, message: String) {
        viewModelScope.launch {
            if (message.isBlank()) return@launch

            val request = CreateMessageRequest(
                group_id = groupId,
                sender_id = senderId,
                message = message.trim(),
                message_type = "text"
            )

            expenseRepository.sendMessage(request).fold(
                onSuccess = { newMessage ->
                    Log.d("GroupViewModel", "Message sent successfully")
                    // Add the new message to the current list for immediate UI update
                    val currentMessages = _groupMessages.value.toMutableList()
                    currentMessages.add(newMessage)
                    _groupMessages.value = currentMessages
                },
                onFailure = { exception ->
                    Log.e("GroupViewModel", "Failed to send message: ${exception.message}")
                    _error.value = exception.message ?: "Failed to send message"
                }
            )
        }
    }

    fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            _isLoadingMessages.value = true

            expenseRepository.getGroupMessages(groupId).fold(
                onSuccess = { messages ->
                    Log.d("GroupViewModel", "Loaded ${messages.size} messages")
                    _groupMessages.value = messages
                },
                onFailure = { exception ->
                    Log.e("GroupViewModel", "Failed to load messages: ${exception.message}")
                    _error.value = exception.message ?: "Failed to load messages"
                }
            )
            _isLoadingMessages.value = false
        }
    }

    // Clear methods
    fun clearExpenses() {
        _groupExpenses.value = emptyList()
    }

    fun clearMessages() {
        _groupMessages.value = emptyList()
    }

    fun clearBalances() {
        _userBalances.value = emptyList()
    }

    // Existing methods (keep all of them)...
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