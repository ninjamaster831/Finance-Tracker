package com.Aman.myapplication

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.navigation.NavHostController


@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        SettingsGroupHeader("Appearance")
        SwitchRow(
            title = "Dark Mode",
            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            isChecked = isDarkTheme
        ) {
            onToggleTheme(it)
        }


        SettingsGroupHeader("Privacy")
        SettingsRow("Privacy Policy", Icons.Default.Policy) {
            navController.navigate("privacy_policy")
        }
        SettingsRow("Terms & Conditions", Icons.Default.Description) {
            navController.navigate("terms_conditions")
        }

        SettingsGroupHeader("App Info")
        SettingsRow("Rate the App", Icons.Default.Star) {
            // Add store intent if needed
        }
        SettingsRow("Version 1.0.0", Icons.Default.Info, onClick = null)

        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Made with ❤️ by Aman",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(vertical = 12.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SettingsRow(title: String, icon: ImageVector, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onBackground // 👈 fix icon color
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground // 👈 fix text color
        )
    }
}

@Composable
fun SwitchRow(title: String, icon: ImageVector, isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onBackground // 👈 fix icon color
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground, // 👈 fix text color
            modifier = Modifier.weight(1f)
        )
        Switch(checked = isChecked, onCheckedChange = onToggle)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PrivacyPolicyScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("We respect your privacy. All data is stored locally using Room DB. No data is shared externally.", fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TermsConditionsScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("By using this app, you agree that it's for personal financial tracking. We are not liable for data loss or misuse.", fontSize = 16.sp)
        }
    }
}

@Composable
fun ManageAccountScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Manage Account Screen", fontSize = 16.sp)
    }
}

@Composable
fun NotificationsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Notifications Settings Screen", fontSize = 16.sp)
    }
}
