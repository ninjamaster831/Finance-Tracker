package com.Aman.myapplication

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RealtimeRepository(private val supabase: SupabaseClient) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _messageUpdates = MutableSharedFlow<GroupMessage>()
    val messageUpdates: Flow<GroupMessage> = _messageUpdates.asSharedFlow()

    private val _expenseUpdates = MutableSharedFlow<GroupExpense>()
    val expenseUpdates: Flow<GroupExpense> = _expenseUpdates.asSharedFlow()

    private val _memberUpdates = MutableSharedFlow<GroupMember>()
    val memberUpdates: Flow<GroupMember> = _memberUpdates.asSharedFlow()

    private var messageChannel: RealtimeChannel? = null
    private var expenseChannel: RealtimeChannel? = null
    private var memberChannel: RealtimeChannel? = null

    suspend fun subscribeToGroupMessages(groupId: String) {
        try {
            Log.d("RealtimeRepository", "Subscribing to messages for group: $groupId")

            messageChannel = supabase.realtime.channel("group_messages_$groupId")
            messageChannel?.subscribe()

            Log.d("RealtimeRepository", "Successfully subscribed to group messages")

        } catch (e: Exception) {
            Log.e("RealtimeRepository", "Error subscribing to messages: ${e.message}")
        }
    }

    suspend fun subscribeToGroupExpenses(groupId: String) {
        try {
            Log.d("RealtimeRepository", "Subscribing to expenses for group: $groupId")

            expenseChannel = supabase.realtime.channel("group_expenses_$groupId")
            expenseChannel?.subscribe()

            Log.d("RealtimeRepository", "Successfully subscribed to group expenses")

        } catch (e: Exception) {
            Log.e("RealtimeRepository", "Error subscribing to expenses: ${e.message}")
        }
    }

    suspend fun subscribeToGroupMembers(groupId: String) {
        try {
            Log.d("RealtimeRepository", "Subscribing to members for group: $groupId")

            memberChannel = supabase.realtime.channel("group_members_$groupId")
            memberChannel?.subscribe()

            Log.d("RealtimeRepository", "Successfully subscribed to group members")

        } catch (e: Exception) {
            Log.e("RealtimeRepository", "Error subscribing to members: ${e.message}")
        }
    }

    // Method to manually trigger message updates
    // Call this after successfully inserting a message to the database
    fun notifyNewMessage(message: GroupMessage) {
        Log.d("RealtimeRepository", "Notifying new message: ${message.id}")
        _messageUpdates.tryEmit(message)
    }

    // Method to manually trigger expense updates
    // Call this after successfully inserting an expense to the database
    fun notifyNewExpense(expense: GroupExpense) {
        Log.d("RealtimeRepository", "Notifying new expense: ${expense.id}")
        _expenseUpdates.tryEmit(expense)
    }

    // Method to manually trigger member updates
    // Call this after successfully adding a member to the database
    fun notifyNewMember(member: GroupMember) {
        Log.d("RealtimeRepository", "Notifying new member: ${member.id}")
        _memberUpdates.tryEmit(member)
    }

    suspend fun unsubscribeFromGroup() {
        try {
            messageChannel?.unsubscribe()
            expenseChannel?.unsubscribe()
            memberChannel?.unsubscribe()

            messageChannel = null
            expenseChannel = null
            memberChannel = null

            Log.d("RealtimeRepository", "Unsubscribed from all group channels")
        } catch (e: Exception) {
            Log.e("RealtimeRepository", "Error unsubscribing: ${e.message}")
        }
    }

    fun cleanup() {
        scope.launch {
            unsubscribeFromGroup()
        }
    }
}