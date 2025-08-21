// ExpenseRepository.kt
package com.Aman.myapplication

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class ExpenseRepository(private val supabase: SupabaseClient) {

    // Create a group expense
    suspend fun createExpense(request: CreateExpenseRequest): Result<GroupExpense> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ExpenseRepository", "Creating expense: ${request.title}")

                @Serializable
                data class ExpenseData(
                    val group_id: String,
                    val paid_by: String,
                    val title: String,
                    val description: String?,
                    val amount: Double,
                    val category: String,
                    val date: Long
                )

                val expenseData = ExpenseData(
                    group_id = request.group_id,
                    paid_by = request.paid_by,
                    title = request.title,
                    description = request.description,
                    amount = request.amount,
                    category = request.category,
                    date = request.date
                )

                // Create the expense
                val expense = supabase.from("group_expenses").insert(expenseData) {
                    select()
                }.decodeSingle<GroupExpense>()

                Log.d("ExpenseRepository", "Expense created: ${expense.id}")

                // Create splits
                val splitAmount = request.amount / request.split_with.size
                request.split_with.forEach { userId ->
                    try {
                        val splitData = CreateSplitRequest(
                            expense_id = expense.id,
                            user_id = userId,
                            amount = splitAmount
                        )
                        supabase.from("expense_splits").insert(splitData)
                        Log.d("ExpenseRepository", "Split created for user: $userId")
                    } catch (e: Exception) {
                        Log.e("ExpenseRepository", "Failed to create split for user $userId: ${e.message}")
                    }
                }

                // Create a system message about the expense
                try {
                    val messageData = CreateMessageRequest(
                        group_id = request.group_id,
                        sender_id = request.paid_by,
                        message = "${getUserName(request.paid_by)} added expense: ${request.title} (₹${request.amount})",
                        message_type = "expense",
                        expense_id = expense.id
                    )
                    supabase.from("group_messages").insert(messageData)
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "Failed to create expense message: ${e.message}")
                }

                Result.success(expense)
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to create expense: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Get all expenses for a group
    suspend fun getGroupExpenses(groupId: String): Result<List<GroupExpense>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ExpenseRepository", "Getting expenses for group: $groupId")

                val expenses = supabase.from("group_expenses")
                    .select() {
                        filter {
                            eq("group_id", groupId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<GroupExpense>()

                // Get user details and splits for each expense
                val expensesWithDetails = expenses.map { expense ->
                    try {
                        // Get paid by user
                        val paidByUser = supabase.from("users")
                            .select() {
                                filter {
                                    eq("id", expense.paid_by)
                                }
                            }
                            .decodeSingleOrNull<User>()

                        // Get splits
                        val splits = supabase.from("expense_splits")
                            .select() {
                                filter {
                                    eq("expense_id", expense.id)
                                }
                            }
                            .decodeList<ExpenseSplit>()

                        // Get user details for splits
                        val splitsWithUsers = splits.map { split ->
                            val user = supabase.from("users")
                                .select() {
                                    filter {
                                        eq("id", split.user_id)
                                    }
                                }
                                .decodeSingleOrNull<User>()
                            split.copy(user = user)
                        }

                        expense.copy(paid_by_user = paidByUser, splits = splitsWithUsers)
                    } catch (e: Exception) {
                        Log.w("ExpenseRepository", "Failed to load details for expense ${expense.id}: ${e.message}")
                        expense
                    }
                }

                Log.d("ExpenseRepository", "Found ${expensesWithDetails.size} expenses")
                Result.success(expensesWithDetails)
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to get expenses: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Calculate balances for group members
    suspend fun calculateGroupBalances(groupId: String): Result<List<UserBalance>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ExpenseRepository", "Calculating balances for group: $groupId")

                // Get all group members
                val members = supabase.from("group_members")
                    .select() {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                    .decodeList<GroupMember>()

                val userBalances = mutableListOf<UserBalance>()

                members.forEach { member ->
                    try {
                        // Get user details
                        val user = supabase.from("users")
                            .select() {
                                filter {
                                    eq("id", member.user_id)
                                }
                            }
                            .decodeSingle<User>()

                        // Calculate how much they paid
                        val paidExpenses = supabase.from("group_expenses")
                            .select() {
                                filter {
                                    eq("group_id", groupId)
                                    eq("paid_by", member.user_id)
                                }
                            }
                            .decodeList<GroupExpense>()

                        val totalPaid = paidExpenses.sumOf { it.amount }

                        // Calculate how much they owe
                        val splits = supabase.from("expense_splits")
                            .select() {
                                filter {
                                    eq("user_id", member.user_id)
                                    isIn("expense_id", paidExpenses.map { it.id } + getOtherExpenseIds(groupId, member.user_id))
                                }
                            }
                            .decodeList<ExpenseSplit>()

                        val totalOwes = splits.sumOf { it.amount }
                        val balance = totalOwes - totalPaid // positive means they owe, negative means they are owed

                        userBalances.add(
                            UserBalance(
                                user = user,
                                owes = if (balance > 0) balance else 0.0,
                                owed = if (balance < 0) -balance else 0.0,
                                balance = balance
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("ExpenseRepository", "Failed to calculate balance for user ${member.user_id}: ${e.message}")
                    }
                }

                Log.d("ExpenseRepository", "Calculated balances for ${userBalances.size} users")
                Result.success(userBalances)
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to calculate balances: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Send a chat message
    suspend fun sendMessage(request: CreateMessageRequest): Result<GroupMessage> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ExpenseRepository", "Sending message to group: ${request.group_id}")

                val message = supabase.from("group_messages").insert(request) {
                    select()
                }.decodeSingle<GroupMessage>()

                // Get sender details
                val sender = supabase.from("users")
                    .select() {
                        filter {
                            eq("id", request.sender_id)
                        }
                    }
                    .decodeSingleOrNull<User>()

                Result.success(message.copy(sender = sender))
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to send message: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Get chat messages for a group
    suspend fun getGroupMessages(groupId: String): Result<List<GroupMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ExpenseRepository", "Getting messages for group: $groupId")

                val messages = supabase.from("group_messages")
                    .select() {
                        filter {
                            eq("group_id", groupId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<GroupMessage>()

                // Get sender details for each message
                val messagesWithSenders = messages.map { message ->
                    try {
                        val sender = supabase.from("users")
                            .select() {
                                filter {
                                    eq("id", message.sender_id)
                                }
                            }
                            .decodeSingleOrNull<User>()

                        // If it's an expense message, get expense details
                        val expense = if (message.expense_id != null) {
                            supabase.from("group_expenses")
                                .select() {
                                    filter {
                                        eq("id", message.expense_id)
                                    }
                                }
                                .decodeSingleOrNull<GroupExpense>()
                        } else null

                        message.copy(sender = sender, expense = expense)
                    } catch (e: Exception) {
                        Log.w("ExpenseRepository", "Failed to load details for message ${message.id}: ${e.message}")
                        message
                    }
                }

                Log.d("ExpenseRepository", "Found ${messagesWithSenders.size} messages")
                Result.success(messagesWithSenders)
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to get messages: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Settle an expense split
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun settleSplit(splitId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                @Serializable
                data class SettleData(
                    val is_settled: Boolean,
                    val settled_at: String
                )

                val settleData = SettleData(
                    is_settled = true,
                    settled_at = java.time.Instant.now().toString()
                )

                supabase.from("expense_splits")
                    .update(settleData) {
                        filter {
                            eq("id", splitId)
                        }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Failed to settle split: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            supabase.from("users")
                .select() {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting user by email: ${e.message}")
            null
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            supabase.from("users")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting user by ID: ${e.message}")
            null
        }
    }

    suspend fun getExpenseById(expenseId: String): Result<GroupExpense> {
        return try {
            val expense = supabase.from("group_expenses")
                .select() {
                    filter {
                        eq("id", expenseId)
                    }
                }
                .decodeSingle<GroupExpense>()
            Result.success(expense)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting expense by ID: ${e.message}")
            Result.failure(e)
        }
    }

    // Helper function to get user name
    private suspend fun getUserName(userId: String): String {
        return try {
            val user = supabase.from("users")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
            user?.full_name ?: "Unknown User"
        } catch (e: Exception) {
            "Unknown User"
        }
    }

    // Helper function to get other expense IDs for balance calculation
    private suspend fun getOtherExpenseIds(groupId: String, userId: String): List<String> {
        return try {
            val expenses = supabase.from("group_expenses")
                .select() {
                    filter {
                        eq("group_id", groupId)
                        neq("paid_by", userId)
                    }
                }
                .decodeList<GroupExpense>()
            expenses.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }
}