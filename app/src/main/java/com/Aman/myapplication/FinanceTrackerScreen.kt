package com.Aman.myapplication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.Aman.myapplication.data.Transaction
import com.Aman.myapplication.viewmodel.SupabaseAuthViewModel
import com.Aman.myapplication.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTrackerScreen(
    viewModel: TransactionViewModel,
    authViewModel: SupabaseAuthViewModel,
    onSearchNavigate: () -> Unit,
    navController: NavController,
    currentRoute: String
) {
    val transactions by viewModel.transactions.observeAsState(emptyList())
    val userProfile by authViewModel.userProfile.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isDialogIncome by remember { mutableStateOf(true) }
    var showImagePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Load user profile when screen loads
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile()
        viewModel.loadTransactions() // Load transactions from database
    }

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
                scope.launch {
                    val transaction = Transaction(
                        title = title,
                        amount = amount,
                        isIncome = isDialogIncome,
                        date = System.currentTimeMillis()
                    )
                    viewModel.addTransactionToDatabase(transaction) // Save to database
                    showAddDialog = false
                }
            }
        )
    }

    if (showImagePicker) {
        ImagePickerDialog(
            onDismiss = { showImagePicker = false },
            onImageSelected = { imageUri ->
                scope.launch {
                    authViewModel.uploadProfileImage(imageUri.toString()) // Convert Uri to String
                    showImagePicker = false
                }
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
            Header(
                userName = userProfile?.full_name ?: "User",
                avatarUrl = userProfile?.avatar_url,
                onProfileClick = { showImagePicker = true },
                isLoading = isLoading
            )
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
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit  // Changed parameter type to Uri
) {
    // Launcher for picking image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
        onDismiss()
    }

    // Launcher for taking photo with camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // You'll need to handle the camera URI separately
            // For now, we'll focus on gallery picker
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Profile Picture") },
        text = {
            Column {
                Text("Choose an option:")
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B3CC4))
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Gallery")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { /* Camera functionality - can be added later */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun Header(
    userName: String,
    avatarUrl: String?,
    onProfileClick: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Hello,", fontSize = 14.sp, color = Color.Gray)
            Text(
                text = userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f))
                .border(2.dp, Color(0xFF5B3CC4), CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF5B3CC4)
                )
            } else if (avatarUrl != null && avatarUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .build()
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    tint = Color(0xFF5B3CC4),
                    modifier = Modifier.size(30.dp)
                )
            }
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
                Spacer(modifier = Modifier.height(8.dp))
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
                    // Handle invalid input
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
                Spacer(modifier = Modifier.height(8.dp))
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
                        imageVector = if (txn.isIncome) Icons.Default.Add else Icons.Default.ShoppingCart,
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
            HorizontalDivider()
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
            icon = { Icon(Icons.Default.Info, contentDescription = "Stats") }
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