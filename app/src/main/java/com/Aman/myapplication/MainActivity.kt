// Updated MainActivity.kt
package com.Aman.myapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Aman.myapplication.viewmodel.AuthState
import com.Aman.myapplication.viewmodel.SupabaseAuthViewModel
import com.Aman.myapplication.viewmodel.TransactionViewModel
import com.Aman.myapplication.viewmodel.GroupViewModel
import io.github.jan.supabase.SupabaseClient

class MainActivity : ComponentActivity() {
    private val authViewModel: SupabaseAuthViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()

    // Create GroupViewModel with both repositories
    private val groupViewModel: GroupViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return GroupViewModel(
                        GroupRepository(SupabaseConfig.client),
                        ExpenseRepository(SupabaseConfig.client)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    transactionViewModel = transactionViewModel,
                    groupViewModel = groupViewModel
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
@Composable
fun AppNavigation(
    authViewModel: SupabaseAuthViewModel,
    transactionViewModel: TransactionViewModel,
    groupViewModel: GroupViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val navController = rememberNavController()

    when (authState) {
        is AuthState.Authenticated -> {
            // Get current user ID using the new method
            val currentUserId = authViewModel.getCurrentUserId()

            Log.d("MainActivity", "Current user ID: '$currentUserId', auth state: $authState")

            // Don't show the main app if we don't have a valid user ID
            if (currentUserId.isBlank()) {
                Log.w("MainActivity", "No valid user ID, showing loading screen")
                LoadingScreen()
            } else {
                // User is logged in, show main app
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        FinanceTrackerScreen(
                            viewModel = transactionViewModel,
                            authViewModel = authViewModel,
                            onSearchNavigate = { navController.navigate("search") },
                            navController = navController,
                            currentRoute = "main"
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            viewModel = transactionViewModel,
                            navController = navController,
                            currentRoute = "search"
                        )
                    }
                    composable("stats") {
                        StatsScreen(
                            viewModel = transactionViewModel,
                            navController = navController,
                            currentRoute = "stats"
                        )
                    }
                    composable("settings") {
                        ModernSettingsScreen(
                            authViewModel = authViewModel,
                            navController = navController,
                            currentRoute = "settings"
                        )
                    }

                    // Profile Management
                    composable("edit_profile") {
                        EditProfileScreen(
                            navController = navController,
                            authViewModel = authViewModel
                        )
                    }

                    // Group Management Routes
                    composable("create_group") {
                        CreateGroupScreen(
                            navController = navController,
                            groupViewModel = groupViewModel,
                            currentUserId = currentUserId
                        )
                    }

                    composable("manage_groups") {
                        ManageGroupsScreen(
                            navController = navController,
                            groupViewModel = groupViewModel,
                            currentUserId = currentUserId
                        )
                    }

                    // Updated group details route to use enhanced screen
                    composable("group_details/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        EnhancedGroupDetailsScreen(
                            navController = navController,
                            groupViewModel = groupViewModel,
                            groupId = groupId,
                            currentUserId = currentUserId
                        )
                    }

                    composable("add_group_members/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        AddGroupMembersScreen(
                            navController = navController,
                            groupViewModel = groupViewModel,
                            groupId = groupId,
                            currentUserId = currentUserId
                        )
                    }

                    // Legal Pages
                    composable("privacy_policy") {
                        PrivacyPolicyScreen(navController = navController)
                    }
                    composable("terms_conditions") {
                        TermsConditionsScreen(navController = navController)
                    }
                }
            }
        }
        is AuthState.Loading -> {
            LoadingScreen()
        }
        else -> {
            SupabaseLoginScreen(
                onLoginSuccess = {
                    // Navigation will be handled automatically by state change
                },
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// Keep your existing placeholder screens for search and stats
@Composable
fun SearchScreen(
    viewModel: TransactionViewModel,
    navController: NavController,
    currentRoute: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Search Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        // Add search functionality here

        // Bottom navigation
        Spacer(modifier = Modifier.weight(1f))
        BottomNavigationBar(navController = navController, currentRoute = currentRoute)
    }
}

@Composable
fun StatsScreen(
    viewModel: TransactionViewModel,
    navController: NavController,
    currentRoute: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Statistics Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        // Add charts and statistics here

        // Bottom navigation
        Spacer(modifier = Modifier.weight(1f))
        BottomNavigationBar(navController = navController, currentRoute = currentRoute)
    }
}

// Keep the existing AddGroupMembersScreen and AddMemberItem components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupMembersScreen(
    navController: NavController,
    groupViewModel: GroupViewModel,
    groupId: String,
    currentUserId: String
) {
    var searchQuery by remember { mutableStateOf("") }
    val friends by groupViewModel.friends.collectAsState()
    val searchResults by groupViewModel.searchResults.collectAsState()
    val isLoading by groupViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        groupViewModel.loadUserFriends(currentUserId)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            groupViewModel.searchUsers(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search users by email or name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (friends.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Friends",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(friends.size) { index ->
                    val friend = friends[index]
                    AddMemberItem(
                        user = friend.friend,
                        onAddClick = {
                            friend.friend?.let { user ->
                                groupViewModel.addMemberToGroup(groupId, user.id)
                            }
                        }
                    )
                }
            }

            if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(searchResults.size) { index ->
                    val user = searchResults[index]
                    if (user.id != currentUserId) {
                        AddMemberItem(
                            user = user,
                            onAddClick = {
                                groupViewModel.addMemberToGroup(groupId, user.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberItem(
    user: User?,
    onAddClick: () -> Unit
) {
    user?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.full_name?.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // User Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.full_name ?: "Unknown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add Button
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Add", fontSize = 12.sp)
                }
            }
        }
    }
}