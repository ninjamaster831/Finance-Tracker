package com.Aman.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Aman.myapplication.data.Transaction
import com.Aman.myapplication.data.TransactionDatabase
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TransactionDatabase.getDatabase(application).transactionDao()

    val transactions = dao.getAllTransactions().asLiveData()

    fun addTransaction(transaction: Transaction) = viewModelScope.launch {
        dao.insert(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        dao.delete(transaction)
    }
}
