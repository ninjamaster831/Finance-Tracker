package com.Aman.myapplication


import kotlinx.serialization.Serializable

@Serializable
data class GroupExpense(
    val id: String,
    val group_id: String,
    val paid_by: String,
    val title: String,
    val description: String? = null,
    val amount: Double,
    val category: String = "general",
    val date: Long,
    val created_at: String,
    val updated_at: String? = null,
    val paid_by_user: User? = null,
    val splits: List<ExpenseSplit> = emptyList()
)

@Serializable
data class ExpenseSplit(
    val id: String,
    val expense_id: String,
    val user_id: String,
    val amount: Double,
    val is_settled: Boolean = false,
    val settled_at: String? = null,
    val created_at: String,
    val user: User? = null
)

@Serializable
data class GroupMessage(
    val id: String,
    val group_id: String,
    val sender_id: String,
    val message: String,
    val message_type: String = "text", // text, expense, system
    val expense_id: String? = null,
    val created_at: String,
    val updated_at: String? = null,
    val sender: User? = null,
    val expense: GroupExpense? = null
)

@Serializable
data class CreateExpenseRequest(
    val group_id: String,
    val paid_by: String,
    val title: String,
    val description: String? = null,
    val amount: Double,
    val category: String = "general",
    val date: Long,
    val split_with: List<String> // List of user IDs to split with
)

@Serializable
data class CreateSplitRequest(
    val expense_id: String,
    val user_id: String,
    val amount: Double
)

@Serializable
data class CreateMessageRequest(
    val group_id: String,
    val sender_id: String,
    val message: String,
    val message_type: String = "text",
    val expense_id: String? = null
)

data class UserBalance(
    val user: User,
    val owes: Double,
    val owed: Double,
    val balance: Double // positive means they owe money, negative means they are owed money
)