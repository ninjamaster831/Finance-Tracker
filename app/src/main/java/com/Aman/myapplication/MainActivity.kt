package com.Aman.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
            var isDarkTheme by rememberSaveable { mutableStateOf(false) } // 🔁 Dynamic theme state

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val viewModel: TransactionViewModel = viewModel()
                val navController = rememberNavController()
                val transactions by viewModel.transactions.observeAsState(emptyList())
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavHost(navController = navController, startDestination = "main") {
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
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            navController = navController,
                            currentRoute = currentRoute ?: "search"
                        )
                    }
                    composable("stats") {
                        StatsScreen(
                            transactions = transactions,
                            navController = navController,
                            currentRoute = currentRoute ?: "stats"
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            navController = navController,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = it }, // 🔁 Toggle dark mode
                            onLogout = { navController.navigate("login") }
                        )
                    }
                    composable("privacy_policy") { PrivacyPolicyScreen(navController) }
                    composable("terms_conditions") { TermsConditionsScreen(navController) }
                    composable("account") { ManageAccountScreen() }
                    composable("notifications") { NotificationsScreen() }
                }
            }
        }
    }
}
