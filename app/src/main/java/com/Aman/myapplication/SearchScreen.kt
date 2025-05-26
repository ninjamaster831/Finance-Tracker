package com.Aman.myapplication

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Aman.myapplication.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    transactions: List<Transaction>,
    onBack: () -> Unit,
    navController: NavController,
    currentRoute: String
) {
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var filteredList by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search by Date") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        fromDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (fromDate.isEmpty()) "Pick From Date" else "From: $fromDate")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        toDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (toDate.isEmpty()) "Pick To Date" else "To: $toDate")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    format.isLenient = false
                    if (fromDate.isNotBlank() && toDate.isNotBlank()) {
                        try {
                            val startDate = format.parse(fromDate)?.apply {
                                hours = 0; minutes = 0; seconds = 0
                            }
                            val endDate = format.parse(toDate)?.apply {
                                hours = 23; minutes = 59; seconds = 59
                            }
                            val start = startDate?.time ?: 0L
                            val end = endDate?.time ?: System.currentTimeMillis()
                            filteredList = transactions.filter { it.date in start..end }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B3CC4))
            ) {
                Text("Search", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Text("No transactions found.", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredList) { txn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(txn.title, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = {
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(txn.date)),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                },
                                trailingContent = {
                                    Text(
                                        if (txn.isIncome) "+₹${txn.amount}" else "-₹${txn.amount}",
                                        color = if (txn.isIncome) Color(0xFF009688) else Color(0xFFD32F2F),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
