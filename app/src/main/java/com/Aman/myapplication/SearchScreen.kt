package com.Aman.myapplication

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Aman.myapplication.data.Transaction
import com.Aman.myapplication.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    navController: NavController,
    currentRoute: String
) {
    val transactions by viewModel.transactions.observeAsState(emptyList())

    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var filteredList by remember { mutableStateOf<List<Transaction>>(emptyList()) }

    fun filterTransactionsIfReady() {
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
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatePickerItem(
                    label = "Start Date",
                    date = fromDate,
                    onDateSelected = {
                        fromDate = it
                        filterTransactionsIfReady()
                    },
                    modifier = Modifier.weight(1f)
                )
                DatePickerItem(
                    label = "End Date",
                    date = toDate,
                    onDateSelected = {
                        toDate = it
                        filterTransactionsIfReady()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (filteredList.isEmpty()) {
                Text(
                    "No transactions found.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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

@Composable
fun DatePickerItem(
    label: String,
    date: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val selectedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
                    onDateSelected(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = modifier
            .height(56.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (date.isEmpty()) label else "$label: $date",
                fontSize = 13.sp,
                color = Color(0xFF5B3CC4),
                maxLines = 1
            )
        }
    }
}
