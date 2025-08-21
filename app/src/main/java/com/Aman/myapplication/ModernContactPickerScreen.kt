// ModernContactPickerScreen.kt
package com.Aman.myapplication

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernContactPickerScreen(
    navController: NavController,
    groupViewModel: EnhancedGroupViewModel,
    groupId: String,
    groupName: String,
    currentUserName: String
) {
    val context = LocalContext.current
    val contacts by groupViewModel.contacts.collectAsState()
    val isLoadingContacts by groupViewModel.isLoadingContacts.collectAsState()
    val hasContactsPermission by groupViewModel.hasContactsPermission.collectAsState()
    val error by groupViewModel.error.collectAsState()
    val successMessage by groupViewModel.successMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedContacts by remember { mutableStateOf(setOf<String>()) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            groupViewModel.checkContactsPermission()
            groupViewModel.loadContacts()
        } else {
            showPermissionDialog = true
        }
    }

    // Filter contacts based on search
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                        contact.phoneNumber.contains(searchQuery) ||
                        contact.email?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    // Categorize contacts
    val appUsers = remember(filteredContacts) {
        filteredContacts.filter { it.isAppUser }
    }
    val nonAppUsers = remember(filteredContacts) {
        filteredContacts.filter { !it.isAppUser }
    }

    LaunchedEffect(Unit) {
        if (hasContactsPermission) {
            groupViewModel.loadContacts()
        }
    }

    // Handle success messages
    successMessage?.let { message ->
        LaunchedEffect(message) {
            delay(3000)
            groupViewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Invite to Group")
                        Text(
                            text = groupName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedContacts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedContacts.forEach { contactId ->
                                    contacts.find { it.id == contactId }?.let { contact ->
                                        groupViewModel.inviteContactToGroup(
                                            contact = contact,
                                            groupName = groupName,
                                            inviterName = currentUserName
                                        )
                                    }
                                }
                                selectedContacts = emptySet()
                            }
                        ) {
                            Text("Invite ${selectedContacts.size}")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasContactsPermission -> {
                    PermissionRequestContent(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    )
                }
                isLoadingContacts -> {
                    LoadingContactsContent()
                }
                contacts.isEmpty() -> {
                    EmptyContactsContent()
                }
                else -> {
                    ContactListContent(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        appUsers = appUsers,
                        nonAppUsers = nonAppUsers,
                        selectedContacts = selectedContacts,
                        onContactSelectionChange = { contactId, isSelected ->
                            selectedContacts = if (isSelected) {
                                selectedContacts + contactId
                            } else {
                                selectedContacts - contactId
                            }
                        },
                        onInviteContact = { contact ->
                            groupViewModel.inviteContactToGroup(
                                contact = contact,
                                groupName = groupName,
                                inviterName = currentUserName
                            )
                        }
                    )
                }
            }

            // Success message snackbar
            successMessage?.let { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Contacts Permission") },
            text = { Text("To invite friends from your contacts, we need access to your contacts. This helps you quickly find and invite people you know.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        val infiniteTransition = rememberInfiniteTransition(label = "permission_animation")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale_animation"
        )

        Icon(
            imageVector = Icons.Default.Contacts,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Access Your Contacts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To invite friends from your contact list, we need permission to access your contacts.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Grant Permission",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your contacts are only used to help you invite friends and are never shared.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun LoadingContactsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your contacts...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyContactsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ContactPhone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Contacts Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Make sure you have contacts saved on your device",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ContactListContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    appUsers: List<Contact>,
    nonAppUsers: List<Contact>,
    selectedContacts: Set<String>,
    onContactSelectionChange: (String, Boolean) -> Unit,
    onInviteContact: (Contact) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        item {
            SearchBarItem(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        // Quick stats
        item {
            ContactStatsCard(
                totalContacts = appUsers.size + nonAppUsers.size,
                appUsers = appUsers.size,
                selectedCount = selectedContacts.size
            )
        }

        // App Users Section
        if (appUsers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Friends on ExpenseTracker",
                    subtitle = "${appUsers.size} friends",
                    icon = Icons.Default.Smartphone
                )
            }

            items(appUsers) { contact ->
                ModernContactItem(
                    contact = contact,
                    isSelected = selectedContacts.contains(contact.id),
                    onSelectionChange = { isSelected ->
                        onContactSelectionChange(contact.id, isSelected)
                    },
                    onInvite = { onInviteContact(contact) }
                )
            }
        }

        // Non-App Users Section
        if (nonAppUsers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Invite to ExpenseTracker",
                    subtitle = "${nonAppUsers.size} contacts",
                    icon = Icons.Default.PersonAdd
                )
            }

            items(nonAppUsers) { contact ->
                ModernContactItem(
                    contact = contact,
                    isSelected = selectedContacts.contains(contact.id),
                    onSelectionChange = { isSelected ->
                        onContactSelectionChange(contact.id, isSelected)
                    },
                    onInvite = { onInviteContact(contact) }
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}