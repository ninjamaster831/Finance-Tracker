package com.Aman.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Aman.myapplication.data.Transaction
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    transactions: List<Transaction>,
    navController: NavController,
    currentRoute: String
) {
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    // Use index-based bar entries (0f -> Income, 1f -> Expense)
    val entryModel = entryModelOf(
        0f to totalIncome.toFloat(),
        1f to totalExpense.toFloat()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentRoute != "main") navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard("Total Balance", "₹%.2f".format(balance), Color(0xFF5B3CC4), Color(0xFFF0F0F0))
            StatCard("Income", "+₹%.2f".format(totalIncome), Color(0xFF00796B), Color(0xFFE0F2F1))
            StatCard("Expenses", "-₹%.2f".format(totalExpense), Color(0xFFC62828), Color(0xFFFFEBEE))

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Overview Chart",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Chart(
                    chart = columnChart(),
                    model = entryModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            when (value) {
                                0f -> "Income"
                                1f -> "Expense"
                                else -> ""
                            }
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, valueColor: Color, bgColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, color = Color.Gray)
            Text(value, fontSize = 22.sp, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}
