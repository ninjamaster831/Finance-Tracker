package com.Aman.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun FinanceTrackerScreen(
    viewModel: TransactionViewModel,
    onSearchNavigate: () -> Unit,
    navController: NavController,
    currentRoute: String
) {
    val transactions by viewModel.transactions.observeAsState(emptyList())
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isDialogIncome by remember { mutableStateOf(true) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            SheetContent(
                onClose = { showSheet = false },
                onAddIncome = {
                    isDialogIncome = true
                    showAddDialog = true
                    showSheet = false
                },
                onAddExpense = {
                    isDialogIncome = false
                    showAddDialog = true
                    showSheet = false
                }
            )
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            isIncome = isDialogIncome,
            onDismiss = { showAddDialog = false },
            onSave = { title, amount ->
                viewModel.addTransaction(
                    Transaction(
                        title = title,
                        amount = amount,
                        isIncome = isDialogIncome,
                        date = System.currentTimeMillis()
                    )
                )
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = Color(0xFF5B3CC4),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(60.dp)
                    .offset(y = (-20).dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(30.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)

        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Header()
            Spacer(modifier = Modifier.height(12.dp))
            BalanceCard(balance)
            ChartCard(totalIncome.toFloat(), totalExpense.toFloat())
            Text(
                "Transactions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            TransactionList(transactions)
        }
    }
}

@Composable
fun SearchByDateDialog(
    onDismiss: () -> Unit,
    onFilter: (Long, Long) -> Unit
) {
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Date Range") },
        text = {
            Column {
                OutlinedTextField(
                    value = fromDate,
                    onValueChange = { fromDate = it },
                    label = { Text("From (dd-MM-yyyy)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = toDate,
                    onValueChange = { toDate = it },
                    label = { Text("To (dd-MM-yyyy)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val start = format.parse(fromDate)?.time ?: 0L
                    val end = format.parse(toDate)?.time ?: System.currentTimeMillis()
                    onFilter(start, end)
                    onDismiss()
                } catch (e: Exception) {
                    // Optional: handle invalid input
                }
            }) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}


@Composable
fun SheetContent(
    onClose: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("Choose Action", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onAddIncome()
                onClose()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
        ) {
            Text("Add Income")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                onAddExpense()
                onClose()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text("Add Expense")
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
@Composable
fun AddTransactionDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        onSave(title, amt)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(if (isIncome) "Add Income" else "Add Expense")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Finance Tracker", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun BalanceCard(balance: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFB76EFF), Color(0xFF5B3CC4))
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Text("Total Balance", color = Color.White.copy(alpha = 0.8f))
            Text(
                text = "₹%,.2f".format(balance),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ChartCard(income: Float, expense: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F1F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Apr", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Income", color = Color(0xFF009688))
                    Text("₹%,.0f".format(income), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Expenses", color = Color(0xFFD32F2F))
                    Text("₹%,.0f".format(expense), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionList(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions) { txn ->
            ListItem(
                headlineContent = { Text(txn.title, fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Text(
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(txn.date)),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = if (txn.isIncome) Color(0xFF009688) else Color(0xFFD32F2F)
                    )
                },
                trailingContent = {
                    Text(
                        text = if (txn.isIncome)
                            "+₹%,.1f".format(txn.amount)
                        else
                            "-₹%,.1f".format(txn.amount),
                        color = if (txn.isIncome) Color(0xFF009688) else Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            )
            Divider()
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = currentRoute == "main",
            onClick = {
                if (currentRoute != "main") navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "search",
            onClick = {
                if (currentRoute != "search") navController.navigate("search")
            },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") }
        )
        Spacer(modifier = Modifier.weight(1f)) // Space for center FAB
        NavigationBarItem(
            selected = currentRoute == "stats",
            onClick = {
                if (currentRoute != "stats") navController.navigate("stats")
            },
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Stats") }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = {
                if (currentRoute != "settings") navController.navigate("settings")
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        )
    }
}

