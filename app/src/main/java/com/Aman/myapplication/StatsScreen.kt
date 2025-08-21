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
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.Aman.myapplication.data.Transaction
import kotlin.math.max

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
                SimpleBarChart(
                    incomeAmount = totalIncome.toFloat(),
                    expenseAmount = totalExpense.toFloat(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
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

@Composable
fun SimpleBarChart(
    incomeAmount: Float,
    expenseAmount: Float,
    modifier: Modifier = Modifier
) {
    val maxAmount = max(incomeAmount, expenseAmount)

    Column(modifier = modifier) {
        // Chart area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Income bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "₹%.0f".format(incomeAmount),
                    fontSize = 12.sp,
                    color = Color(0xFF00796B),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height((incomeAmount / maxAmount * 120).dp)
                        .background(
                            Color(0xFF00796B),
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                )
            }

            // Expense bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "₹%.0f".format(expenseAmount),
                    fontSize = 12.sp,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height((expenseAmount / maxAmount * 120).dp)
                        .background(
                            Color(0xFFC62828),
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Income",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF00796B),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Expense",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}