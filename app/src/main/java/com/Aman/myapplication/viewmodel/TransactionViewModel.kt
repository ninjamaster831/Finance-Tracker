package com.Aman.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.Aman.myapplication.data.Transaction
import kotlinx.coroutines.launch
import com.Aman.myapplication.data.TransactionRepository

class TransactionViewModel : ViewModel() {
    private val repository = TransactionRepository()

    private val _transactions = MutableLiveData<List<Transaction>>(emptyList())
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("TransactionVM", "Loading transactions...")
                val transactionList = repository.getAllTransactions()
                Log.d("TransactionVM", "Loaded ${transactionList.size} transactions")
                _transactions.value = transactionList
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error loading transactions: ${e.message}", e)
                _transactions.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun addTransactionToDatabase(transaction: Transaction) {
        viewModelScope.launch {
            try {
                Log.d("TransactionVM", "Adding transaction to database: $transaction")
                val success = repository.insertTransaction(transaction)
                Log.d("TransactionVM", "Transaction insert success: $success")

                if (success) {
                    Log.d("TransactionVM", "Transaction saved successfully, reloading list...")
                    // Reload transactions to update the UI
                    loadTransactions()
                } else {
                    Log.e("TransactionVM", "Failed to insert transaction")
                }
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error adding transaction: ${e.message}", e)
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        Log.d("TransactionVM", "Adding transaction locally: $transaction")
        val currentList = _transactions.value ?: emptyList()
        _transactions.value = currentList + transaction
    }

    fun filterTransactionsByDate(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            try {
                Log.d("TransactionVM", "Filtering transactions from $startDate to $endDate")
                val filteredTransactions = repository.getTransactionsByDateRange(startDate, endDate)
                Log.d("TransactionVM", "Found ${filteredTransactions.size} transactions in date range")
                _transactions.value = filteredTransactions
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error filtering transactions: ${e.message}", e)
            }
        }
    }

    fun searchTransactionsByTitle(query: String) {
        viewModelScope.launch {
            try {
                Log.d("TransactionVM", "Searching transactions with query: $query")
                val searchResults = repository.searchTransactionsByTitle(query)
                Log.d("TransactionVM", "Found ${searchResults.size} transactions matching query")
                _transactions.value = searchResults
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error searching transactions: ${e.message}", e)
            }
        }
    }
}