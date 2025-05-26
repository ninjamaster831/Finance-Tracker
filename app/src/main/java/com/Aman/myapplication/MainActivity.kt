package com.Aman.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.Aman.myapplication.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: TransactionViewModel = viewModel()
            val navController = rememberNavController()
            val transactions by viewModel.transactions.observeAsState(emptyList())
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavHost(navController, startDestination = "main") {
                composable("main") {
                    FinanceTrackerScreen(
                        viewModel = viewModel,
                        onSearchNavigate = { navController.navigate("search") },
                        navController = navController,
                        currentRoute = currentRoute ?: "main"
                    )
                }
                composable("search") {
                    SearchScreen(
                        transactions = transactions,
                        onBack = { navController.popBackStack() },
                        navController = navController,
                        currentRoute = currentRoute ?: "search"
                    )
                }
            }

        }
    }
}
