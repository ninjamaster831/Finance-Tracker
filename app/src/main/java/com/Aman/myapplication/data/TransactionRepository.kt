package com.Aman.myapplication.data

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import com.Aman.myapplication.SupabaseConfig
import com.Aman.myapplication.SupabaseAuthRepository
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class Transaction(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val user_id: String? = null,
    @SerialName("title")
    val title: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("isincome")
    val isIncome: Boolean,
    @SerialName("date")
    val date: Long,
    @SerialName("created_at")
    val created_at: String? = null
)

class TransactionRepository {
    private val client = SupabaseConfig.client
    private val authRepository = SupabaseAuthRepository()

    // Maximum allowed value for DECIMAL(12,2)
    private val MAX_AMOUNT = 9999999999.99

    private fun validateAmount(amount: Double): Double {
        return when {
            amount.isNaN() || amount.isInfinite() -> {
                Log.w("TransactionRepo", "Invalid amount detected: $amount, setting to 0.0")
                0.0
            }
            amount > MAX_AMOUNT -> {
                Log.w("TransactionRepo", "Amount too large: $amount, capping to $MAX_AMOUNT")
                MAX_AMOUNT
            }
            amount < 0 -> {
                Log.w("TransactionRepo", "Negative amount: $amount, using absolute value")
                kotlin.math.abs(amount)
            }
            else -> {
                // Round to 2 decimal places to match database precision
                BigDecimal.valueOf(amount)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
            }
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Boolean {
        return try {
            Log.d("TransactionRepo", "Starting transaction insert")
            Log.d("TransactionRepo", "Original amount: ${transaction.amount}")

            val currentUser = authRepository.getCurrentUser()
            Log.d("TransactionRepo", "Current user: ${currentUser?.id}")

            if (currentUser != null) {
                // Validate and sanitize the amount
                val validatedAmount = validateAmount(transaction.amount)
                Log.d("TransactionRepo", "Validated amount: $validatedAmount")

                val transactionWithUser = transaction.copy(
                    user_id = currentUser.id,
                    amount = validatedAmount
                )

                Log.d("TransactionRepo", "Transaction to insert: $transactionWithUser")

                val result = client.from("transactions").insert(transactionWithUser)
                Log.d("TransactionRepo", "Insert result: $result")

                true
            } else {
                Log.e("TransactionRepo", "No current user found")
                false
            }
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error inserting transaction: ${e.message}", e)
            false
        }
    }

    suspend fun getAllTransactions(): List<Transaction> {
        return try {
            Log.d("TransactionRepo", "Getting all transactions")

            val currentUser = authRepository.getCurrentUser()
            Log.d("TransactionRepo", "Current user for fetch: ${currentUser?.id}")

            if (currentUser != null) {
                val transactions = client.from("transactions")
                    .select() {
                        filter {
                            eq("user_id", currentUser.id)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Transaction>()

                Log.d("TransactionRepo", "Found ${transactions.size} transactions")
                transactions
            } else {
                Log.e("TransactionRepo", "No current user for fetch")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error fetching transactions: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<Transaction> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                client.from("transactions")
                    .select() {
                        filter {
                            eq("user_id", currentUser.id)
                            gte("date", startDate)
                            lte("date", endDate)
                        }
                        order("date", Order.DESCENDING)
                    }
                    .decodeList<Transaction>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error fetching transactions by date: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun searchTransactionsByTitle(query: String): List<Transaction> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                client.from("transactions")
                    .select() {
                        filter {
                            eq("user_id", currentUser.id)
                            ilike("title", "%$query%")
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Transaction>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error searching transactions: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun deleteTransaction(transactionId: String): Boolean {
        return try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                client.from("transactions")
                    .delete() {
                        filter {
                            eq("id", transactionId)
                            eq("user_id", currentUser.id)
                        }
                    }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error deleting transaction: ${e.message}", e)
            false
        }
    }
}