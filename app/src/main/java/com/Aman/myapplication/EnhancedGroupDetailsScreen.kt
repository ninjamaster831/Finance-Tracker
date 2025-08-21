package com.Aman.myapplication


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Aman.myapplication.viewmodel.GroupViewModel
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedGroupDetailsScreen(
    navController: NavController,
    groupViewModel: GroupViewModel,
    groupId: String,
    currentUserId: String
) {
    val groupMembers by groupViewModel.groupMembers.collectAsState()
    val groupExpenses by groupViewModel.groupExpenses.collectAsState()
    val userBalances by groupViewModel.userBalances.collectAsState()
    val groupMessages by groupViewModel.groupMessages.collectAsState()
    val isLoading by groupViewModel.isLoading.collectAsState()
    val isLoadingExpenses by groupViewModel.isLoadingExpenses.collectAsState()
    val isLoadingMessages by groupViewModel.isLoadingMessages.collectAsState()
    val error by groupViewModel.error.collectAsState()
    val successMessage by groupViewModel.successMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("Group Details") }

    // Load data when screen loads
    LaunchedEffect(groupId) {
        groupViewModel.loadGroupMembers(groupId)
        groupViewModel.loadGroupExpenses(groupId)
        groupViewModel.calculateGroupBalances(groupId)
        groupViewModel.loadGroupMessages(groupId)
    }

    // Handle success messages
    successMessage?.let { message ->
        LaunchedEffect(message) {
            groupViewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate("add_group_members/$groupId")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Members"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) { // Expenses tab
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Members") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Expenses") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Chat") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> MembersTab(
                    members = groupMembers,
                    userBalances = userBalances,
                    currentUserId = currentUserId,
                    isLoading = isLoading
                )
                1 -> ExpensesTab(
                    expenses = groupExpenses,
                    userBalances = userBalances,
                    isLoading = isLoadingExpenses,
                    onSettleSplit = { splitId ->
                        groupViewModel.settleSplit(splitId, groupId)
                    }
                )
                2 -> ChatTab(
                    messages = groupMessages,
                    currentUserId = currentUserId,
                    isLoading = isLoadingMessages,
                    onSendMessage = { message ->
                        groupViewModel.sendMessage(groupId, currentUserId, message)
                    }
                )
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            groupMembers = groupMembers,
            currentUserId = currentUserId,
            onDismiss = { showAddExpenseDialog = false },
            onAddExpense = { title, description, amount, category, splitWith ->
                groupViewModel.createExpense(
                    groupId = groupId,
                    title = title,
                    description = description,
                    amount = amount,
                    category = category,
                    paidBy = currentUserId,
                    splitWith = splitWith
                )
                showAddExpenseDialog = false
            }
        )
    }

    // Error handling
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            groupViewModel.clearError()
        }
    }
}

@Composable
fun MembersTab(
    members: List<GroupMember>,
    userBalances: List<UserBalance>,
    currentUserId: String,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Members",
                            value = members.size.toString()
                        )
                        StatItem(
                            label = "Total Expenses",
                            value = "₹${userBalances.sumOf { kotlin.math.abs(it.balance) / 2 }}"
                        )
                        StatItem(
                            label = "Unsettled",
                            value = userBalances.count { it.balance != 0.0 }.toString()
                        )
                    }
                }
            }

            // Balances Section
            if (userBalances.isNotEmpty()) {
                item {
                    Text(
                        text = "Balances",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(userBalances) { balance ->
                    BalanceCard(
                        userBalance = balance,
                        isCurrentUser = balance.user.id == currentUserId
                    )
                }
            }

            // Members Section
            item {
                Text(
                    text = "Members (${members.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(members) { member ->
                MemberCard(
                    member = member,
                    isCurrentUser = member.user_id == currentUserId
                )
            }
        }
    }
}

@Composable
fun ExpensesTab(
    expenses: List<GroupExpense>,
    userBalances: List<UserBalance>,
    isLoading: Boolean,
    onSettleSplit: (String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Expense Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Expenses: ₹${expenses.sumOf { it.amount }}")
                            Text("Count: ${expenses.size}")
                        }
                    }
                }
            }

            if (expenses.isEmpty()) {
                item {
                    EmptyExpensesState()
                }
            } else {
                items(expenses) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onSettleSplit = onSettleSplit
                    )
                }
            }
        }
    }
}

@Composable
fun ChatTab(
    messages: List<GroupMessage>,
    currentUserId: String,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (messages.isEmpty()) {
                item {
                    EmptyMessagesState()
                }
            } else {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.sender_id == currentUserId
                    )
                }
            }
        }

        // Message Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(
    userBalance: UserBalance,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                userBalance.balance > 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                userBalance.balance < 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userBalance.user.full_name?.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = userBalance.user.full_name ?: "Unknown User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(You)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                when {
                    userBalance.balance > 0 -> {
                        Text(
                            text = "Owes ₹${String.format("%.2f", userBalance.balance)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    userBalance.balance < 0 -> {
                        Text(
                            text = "Is owed ₹${String.format("%.2f", -userBalance.balance)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Text(
                            text = "Settled up",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Balance Amount
            Text(
                text = when {
                    userBalance.balance > 0 -> "-₹${String.format("%.2f", userBalance.balance)}"
                    userBalance.balance < 0 -> "+₹${String.format("%.2f", -userBalance.balance)}"
                    else -> "₹0.00"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    userBalance.balance > 0 -> MaterialTheme.colorScheme.error
                    userBalance.balance < 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}