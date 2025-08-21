package com.Aman.myapplication

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Aman.myapplication.viewmodel.GroupViewModel
import com.Aman.myapplication.viewmodel.SupabaseAuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    authViewModel: SupabaseAuthViewModel
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    var fullName by remember { mutableStateOf(userProfile?.full_name ?: "") }
    var phone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Form Fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Profile Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = userProfile?.email ?: "",
                        onValueChange = { },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    isLoading = true
                    // Call authViewModel.updateProfile(fullName, phone)
                    // You'll need to implement this in your SupabaseAuthViewModel
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
// Add this to your EditProfileScreen.kt - replace the existing CreateGroupScreen

@RequiresApi(Build.VERSION_CODES.O_MR1)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavController,
    groupViewModel: GroupViewModel,
    currentUserId: String
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFriends by remember { mutableStateOf(setOf<String>()) }

    val friends by groupViewModel.friends.collectAsState()
    val searchResults by groupViewModel.searchResults.collectAsState()
    val isLoading by groupViewModel.isLoading.collectAsState()
    val error by groupViewModel.error.collectAsState()
    val successMessage by groupViewModel.successMessage.collectAsState()

    // Add user ID validation with logging
    LaunchedEffect(currentUserId) {
        Log.d("CreateGroupScreen", "Current user ID received: '$currentUserId'")
        if (currentUserId.isBlank()) {
            Log.e("CreateGroupScreen", "No valid user ID provided, navigating back")
            // Don't navigate back immediately, let the user see the error
            return@LaunchedEffect
        }
        // Only load friends if we have a valid user ID
        Log.d("CreateGroupScreen", "Loading friends for user: $currentUserId")
        groupViewModel.loadUserFriends(currentUserId)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && currentUserId.isNotBlank()) {
            groupViewModel.searchUsers(searchQuery)
        } else {
            groupViewModel.clearSearchResults()
        }
    }

    // Handle success navigation
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            Log.d("CreateGroupScreen", "Success message received: $successMessage")
            groupViewModel.clearSuccessMessage()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (groupName.isNotBlank() && !isLoading && currentUserId.isNotBlank()) {
                                Log.d("CreateGroupScreen", "Creating group: name='$groupName', userId='$currentUserId', friends=${selectedFriends.size}")
                                groupViewModel.createGroup(
                                    name = groupName,
                                    description = groupDescription.ifBlank { null },
                                    userId = currentUserId,
                                    selectedFriends = selectedFriends.toList()
                                )
                            } else {
                                Log.w("CreateGroupScreen", "Cannot create group: name='$groupName', userId='$currentUserId', loading=$isLoading")
                            }
                        },
                        enabled = groupName.isNotBlank() && !isLoading && currentUserId.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create")
                        }
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
            // User ID validation error
            if (currentUserId.isBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Authentication Error",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Unable to get current user ID. Please sign out and sign back in.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Debug info (you can remove this later)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Debug: User ID = '$currentUserId' (${currentUserId.length} chars)",
                        modifier = Modifier.padding(8.dp),
                        fontSize = 12.sp
                    )
                }
            }

            // Error message
            error?.let { errorMessage ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Group Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Group Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = groupName.isEmpty(),
                            enabled = currentUserId.isNotBlank()
                        )

                        OutlinedTextField(
                            value = groupDescription,
                            onValueChange = { groupDescription = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            enabled = currentUserId.isNotBlank()
                        )
                    }
                }
            }

            // Only show friends section if user ID is valid
            if (currentUserId.isNotBlank()) {
                // Add Friends Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Add Friends",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search by email or name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Selected friends count
                            if (selectedFriends.isNotEmpty()) {
                                Text(
                                    text = "${selectedFriends.size} friend(s) selected",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Friends List
                if (friends.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Friends (${friends.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    items(friends.size) { index ->
                        val friend = friends[index]
                        FriendSelectionItem(
                            friend = friend,
                            isSelected = selectedFriends.contains(friend.friend_id),
                            onToggle = { isSelected ->
                                selectedFriends = if (isSelected) {
                                    selectedFriends + friend.friend_id
                                } else {
                                    selectedFriends - friend.friend_id
                                }
                            }
                        )
                    }
                }

                // Search Results
                if (searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "Search Results (${searchResults.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    items(searchResults.size) { index ->
                        val user = searchResults[index]
                        if (user.id != currentUserId) {
                            UserSearchItem(
                                user = user,
                                onAddFriend = {
                                    groupViewModel.sendFriendRequest(currentUserId, user.email)
                                }
                            )
                        }
                    }
                }

                // Empty state for friends
                if (friends.isEmpty() && searchResults.isEmpty() && searchQuery.isBlank() && !isLoading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Friends Yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Search for users to add as friends and include them in your group",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Loading indicator for friends
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Loading friends...")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun FriendSelectionItem(
    friend: Friend,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.friend?.full_name?.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.friend?.full_name ?: "Unknown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = friend.friend?.email ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun UserSearchItem(
    user: User,
    onAddFriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.full_name?.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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

            // Add Friend Button
            OutlinedButton(
                onClick = onAddFriend,
                modifier = Modifier.height(36.dp)
            ) {
                Text("Add Friend", fontSize = 12.sp)
            }
        }
    }
}