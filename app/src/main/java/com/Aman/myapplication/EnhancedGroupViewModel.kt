package com.Aman.myapplication


import android.content.Context
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

class EnhancedGroupViewModel(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val realtimeRepository: RealtimeRepository,
    private val contactRepository: ContactRepository,
    private val userRepository: SupabaseAuthRepository
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

    // Expense and chat properties
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

    // Contact properties
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    // Real-time properties
    private val _isTyping = MutableStateFlow<Set<String>>(emptySet())
    val isTyping: StateFlow<Set<String>> = _isTyping.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    private var currentGroupId: String? = null

    init {
        // Subscribe to real-time updates
        subscribeToRealtimeUpdates()
        checkContactsPermission()
    }

    private fun subscribeToRealtimeUpdates() {
        viewModelScope.launch {
            // Subscribe to message updates
            realtimeRepository.messageUpdates.collect { newMessage ->
                Log.d("EnhancedGroupViewModel", "Received new message: ${newMessage.id}")

                // Add message to current list
                val currentMessages = _groupMessages.value.toMutableList()

                // Check if message already exists (avoid duplicates)
                if (currentMessages.none { it.id == newMessage.id }) {
                    // Fetch complete message with sender details
                    try {
                        val completeMessage = loadMessageWithDetails(newMessage)
                        currentMessages.add(completeMessage)
                        _groupMessages.value = currentMessages.sortedBy { it.created_at }

                        // Update unread count if not current user
                        updateUnreadCount(newMessage.group_id, newMessage.sender_id)

                        // Show notification or play sound
                        handleNewMessageNotification(completeMessage)
                    } catch (e: Exception) {
                        Log.e("EnhancedGroupViewModel", "Error loading message details: ${e.message}")
                        currentMessages.add(newMessage)
                        _groupMessages.value = currentMessages.sortedBy { it.created_at }
                    }
                }
            }
        }

        viewModelScope.launch {
            // Subscribe to expense updates
            realtimeRepository.expenseUpdates.collect { newExpense ->
                Log.d("EnhancedGroupViewModel", "Received new expense: ${newExpense.id}")

                // Refresh expenses and balances
                currentGroupId?.let { groupId ->
                    loadGroupExpenses(groupId)
                    calculateGroupBalances(groupId)
                }
            }
        }

        viewModelScope.launch {
            // Subscribe to member updates
            realtimeRepository.memberUpdates.collect { newMember ->
                Log.d("EnhancedGroupViewModel", "Received new member: ${newMember.id}")

                // Refresh group members
                currentGroupId?.let { groupId ->
                    loadGroupMembers(groupId)
                }
            }
        }
    }

    private suspend fun loadMessageWithDetails(message: GroupMessage): GroupMessage {
        val sender = userRepository.getUserById(message.sender_id)
        val expense = message.expense_id?.let { expenseId ->
            expenseRepository.getExpenseById(expenseId).getOrNull()
        }

        return message.copy(sender = sender, expense = expense)
    }

    private fun updateUnreadCount(groupId: String, senderId: String) {
        // Only update if not current user (you'll need to pass current user ID)
        val currentCounts = _unreadCounts.value.toMutableMap()
        val currentCount = currentCounts[groupId] ?: 0
        currentCounts[groupId] = currentCount + 1
        _unreadCounts.value = currentCounts
    }

    private fun handleNewMessageNotification(message: GroupMessage) {
        // You can implement notification logic here
        // Show toast, play sound, send system notification, etc.
        _successMessage.value = "New message from ${message.sender?.full_name}: ${message.message.take(50)}..."
    }

    // Enhanced methods with real-time support

    fun joinGroupRealtime(groupId: String) {
        viewModelScope.launch {
            try {
                currentGroupId = groupId
                realtimeRepository.subscribeToGroupMessages(groupId)
                realtimeRepository.subscribeToGroupExpenses(groupId)
                realtimeRepository.subscribeToGroupMembers(groupId)

                Log.d("EnhancedGroupViewModel", "Joined real-time channels for group: $groupId")
            } catch (e: Exception) {
                Log.e("EnhancedGroupViewModel", "Error joining real-time channels: ${e.message}")
            }
        }
    }

    fun leaveGroupRealtime() {
        viewModelScope.launch {
            try {
                realtimeRepository.unsubscribeFromGroup()
                currentGroupId = null
                Log.d("EnhancedGroupViewModel", "Left real-time channels")
            } catch (e: Exception) {
                Log.e("EnhancedGroupViewModel", "Error leaving real-time channels: ${e.message}")
            }
        }
    }

    // Contact-related methods

    fun checkContactsPermission() {
        _hasContactsPermission.value = contactRepository.hasContactsPermission()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoadingContacts.value = true
            _error.value = null

            try {
                contactRepository.getContacts().fold(
                    onSuccess = { contacts ->
                        Log.d("EnhancedGroupViewModel", "Loaded ${contacts.size} contacts")

                        // Check which contacts are app users
                        val contactsWithAppStatus = contactRepository.checkAppUsers(contacts, userRepository)
                        _contacts.value = contactsWithAppStatus
                    },
                    onFailure = { exception ->
                        Log.e("EnhancedGroupViewModel", "Failed to load contacts: ${exception.message}")
                        _error.value = exception.message ?: "Failed to load contacts"
                    }
                )
            } catch (e: Exception) {
                Log.e("EnhancedGroupViewModel", "Unexpected error loading contacts: ${e.message}")
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoadingContacts.value = false
            }
        }
    }

    fun inviteContactToGroup(contact: Contact, groupName: String, inviterName: String) {
        viewModelScope.launch {
            try {
                val invitationData = InvitationData(
                    contact = contact,
                    groupName = groupName,
                    inviterName = inviterName
                )

                val success = if (contact.isAppUser) {
                    // Send in-app invitation or direct group invite
                    inviteAppUserToGroup(contact, groupName)
                } else {
                    // Send WhatsApp/SMS invitation
                    contactRepository.sendWhatsAppInvitation(invitationData)
                }

                if (success) {
                    _successMessage.value = "Invitation sent to ${contact.name}!"
                } else {
                    _error.value = "Failed to send invitation to ${contact.name}"
                }

            } catch (e: Exception) {
                Log.e("EnhancedGroupViewModel", "Error sending invitation: ${e.message}")
                _error.value = "Failed to send invitation: ${e.message}"
            }
        }
    }

    private suspend fun inviteAppUserToGroup(contact: Contact, groupName: String): Boolean {
        return try {
            // If user is already an app user, you can add them directly to the group
            contact.userId?.let { userId ->
                currentGroupId?.let { groupId ->
                    addMemberToGroup(groupId, userId)
                    true
                } ?: false
            } ?: false
        } catch (e: Exception) {
            Log.e("EnhancedGroupViewModel", "Error adding app user to group: ${e.message}")
            false
        }
    }

    // Enhanced messaging with typing indicators

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
                    Log.d("EnhancedGroupViewModel", "Message sent successfully")
                    // Real-time will handle updating the UI
                    stopTyping(groupId, senderId)
                },
                onFailure = { exception ->
                    Log.e("EnhancedGroupViewModel", "Failed to send message: ${exception.message}")
                    _error.value = exception.message ?: "Failed to send message"
                }
            )
        }
    }

    fun startTyping(groupId: String, userId: String) {
        val currentTyping = _isTyping.value.toMutableSet()
        currentTyping.add(userId)
        _isTyping.value = currentTyping

        // Auto-stop typing after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            stopTyping(groupId, userId)
        }
    }

    fun stopTyping(groupId: String, userId: String) {
        val currentTyping = _isTyping.value.toMutableSet()
        currentTyping.remove(userId)
        _isTyping.value = currentTyping
    }

    // Enhanced expense creation with real-time updates
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
                        Log.d("EnhancedGroupViewModel", "Expense created successfully: ${expense.id}")
                        _successMessage.value = "Expense added successfully!"

                        // Real-time will handle updating expenses
                        // But we can also refresh to ensure consistency
                        calculateGroupBalances(groupId)
                    },
                    onFailure = { exception ->
                        Log.e("EnhancedGroupViewModel", "Failed to create expense: ${exception.message}")
                        _error.value = exception.message ?: "Failed to create expense"
                    }
                )
            } catch (e: Exception) {
                Log.e("EnhancedGroupViewModel", "Unexpected error creating expense: ${e.message}", e)
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoadingExpenses.value = false
            }
        }
    }

    // Existing methods with enhancements...

    fun createGroup(name: String, description: String?, userId: String, selectedFriends: List<String> = emptyList()) {
        viewModelScope.launch {
            Log.d("EnhancedGroupViewModel", "Starting group creation: name=$name, userId=$userId, friends=${selectedFriends.size}")

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
                        Log.d("EnhancedGroupViewModel", "Group created successfully: ${group.id}")

                        var allMembersAdded = true
                        var addedCount = 0

                        selectedFriends.forEach { friendId ->
                            groupRepository.addMemberToGroup(group.id, friendId).fold(
                                onSuccess = {
                                    addedCount++
                                    Log.d("EnhancedGroupViewModel", "Successfully added friend $friendId")
                                },
                                onFailure = { exception ->
                                    allMembersAdded = false
                                    Log.e("EnhancedGroupViewModel", "Failed to add friend $friendId: ${exception.message}")
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

                        // Join real-time channels for the new group
                        joinGroupRealtime(group.id)
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

    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            Log.d("EnhancedGroupViewModel", "Loading groups for user: $userId")
            _isLoading.value = true
            _error.value = null

            groupRepository.getUserGroups(userId).fold(
                onSuccess = { groups ->
                    Log.d("EnhancedGroupViewModel", "Loaded ${groups.size} groups")
                    _groups.value = groups
                },
                onFailure = { exception ->
                    Log.e("EnhancedGroupViewModel", "Failed to load groups: ${exception.message}", exception)
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

                    // Join real-time for this group
                    joinGroupRealtime(groupId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load group details"
                    _isLoading.value = false
                }
            )
        }
    }

    fun loadGroupExpenses(groupId: String) {
        viewModelScope.launch {
            _isLoadingExpenses.value = true
            _error.value = null

            expenseRepository.getGroupExpenses(groupId).fold(
                onSuccess = { expenses ->
                    Log.d("EnhancedGroupViewModel", "Loaded ${expenses.size} expenses")
                    _groupExpenses.value = expenses
                },
                onFailure = { exception ->
                    Log.e("EnhancedGroupViewModel", "Failed to load expenses: ${exception.message}")
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
                    Log.d("EnhancedGroupViewModel", "Calculated balances for ${balances.size} users")
                    _userBalances.value = balances
                },
                onFailure = { exception ->
                    Log.e("EnhancedGroupViewModel", "Failed to calculate balances: ${exception.message}")
                    _error.value = exception.message ?: "Failed to calculate balances"
                }
            )
        }
    }

    fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            _isLoadingMessages.value = true

            expenseRepository.getGroupMessages(groupId).fold(
                onSuccess = { messages ->
                    Log.d("EnhancedGroupViewModel", "Loaded ${messages.size} messages")
                    _groupMessages.value = messages

                    // Mark messages as read for this group
                    markMessagesAsRead(groupId)
                },
                onFailure = { exception ->
                    Log.e("EnhancedGroupViewModel", "Failed to load messages: ${exception.message}")
                    _error.value = exception.message ?: "Failed to load messages"
                }
            )
            _isLoadingMessages.value = false
        }
    }

    fun markMessagesAsRead(groupId: String) {
        val currentCounts = _unreadCounts.value.toMutableMap()
        currentCounts[groupId] = 0
        _unreadCounts.value = currentCounts
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

    fun loadUserFriends(userId: String) {
        viewModelScope.launch {
            Log.d("EnhancedGroupViewModel", "Loading friends for user: $userId")
            _error.value = null

            groupRepository.getUserFriends(userId).fold(
                onSuccess = { friends ->
                    Log.d("EnhancedGroupViewModel", "Loaded ${friends.size} friends")
                    _friends.value = friends
                },
                onFailure = { exception ->
                    Log.e("EnhancedGroupViewModel", "Failed to load friends: ${exception.message}")
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
                    // Real-time will handle updating the members list
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

    fun clearContacts() {
        _contacts.value = emptyList()
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

    override fun onCleared() {
        super.onCleared()
        // Clean up real-time subscriptions
        viewModelScope.launch {
            leaveGroupRealtime()
        }
    }
}