// ModernChatUI.kt
package com.Aman.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernChatTab(
    messages: List<GroupMessage>,
    currentUserId: String,
    isLoading: Boolean,
    typingUsers: Set<String>,
    onSendMessage: (String) -> Unit,
    onStartTyping: () -> Unit,
    onStopTyping: () -> Unit,
    groupViewModel: EnhancedGroupViewModel
) {
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Handle typing indicators
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty() && !isTyping) {
            isTyping = true
            onStartTyping()
        } else if (messageText.isEmpty() && isTyping) {
            isTyping = false
            onStopTyping()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (isLoading) {
                item {
                    LoadingMessagesIndicator()
                }
            } else if (messages.isEmpty()) {
                item {
                    EmptyMessagesState()
                }
            } else {
                items(messages) { message ->
                    ModernMessageItem(
                        message = message,
                        isCurrentUser = message.sender_id == currentUserId,
                        showAvatar = shouldShowAvatar(message, messages)
                    )
                }
            }

            // Typing indicator
            if (typingUsers.isNotEmpty()) {
                item {
                    TypingIndicator(
                        typingUsers = typingUsers,
                        currentUserId = currentUserId
                    )
                }
            }
        }

        // Message Input
        ModernMessageInput(
            messageText = messageText,
            onMessageTextChange = { messageText = it },
            onSendMessage = {
                if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                    isTyping = false
                    onStopTyping()
                }
            },
            isEnabled = !isLoading
        )
    }
}

@Composable
fun ModernMessageItem(
    message: GroupMessage,
    isCurrentUser: Boolean,
    showAvatar: Boolean
) {
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            // Sender avatar
            if (showAvatar) {
                MessageAvatar(
                    name = message.sender?.full_name ?: "Unknown",
                    isOnline = true // You can implement online status
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Sender name (only for others and when showing avatar)
            if (!isCurrentUser && showAvatar) {
                Text(
                    text = message.sender?.full_name ?: "Unknown",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // Message bubble
            ModernMessageBubble(
                message = message,
                isCurrentUser = isCurrentUser
            )
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // Current user avatar
            if (showAvatar) {
                MessageAvatar(
                    name = "You",
                    isOnline = true,
                    isCurrentUser = true
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }
}

@Composable
fun ModernMessageBubble(
    message: GroupMessage,
    isCurrentUser: Boolean
) {
    val bubbleColor = if (isCurrentUser) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
        )
    }

    Card(
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isCurrentUser) 20.dp else 6.dp,
            bottomEnd = if (isCurrentUser) 6.dp else 20.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.background(bubbleColor)
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            ) {
                // Message content based on type
                when (message.message_type) {
                    "expense" -> {
                        ExpenseMessageContent(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                    "system" -> {
                        SystemMessageContent(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                    else -> {
                        RegularMessageContent(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                }

                // Timestamp and status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.created_at),
                        fontSize = 10.sp,
                        color = if (isCurrentUser)
                            Color.White.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(
                            isDelivered = true, // You can implement delivery status
                            isRead = false // You can implement read status
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseMessageContent(
    message: GroupMessage,
    isCurrentUser: Boolean
) {
    val textColor = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AttachMoney,
            contentDescription = "Expense",
            modifier = Modifier.size(18.dp),
            tint = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message.message,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }

    // Show expense details if available
    message.expense?.let { expense ->
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser)
                    Color.White.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = expense.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "₹${String.format("%.2f", expense.amount)}",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SystemMessageContent(
    message: GroupMessage,
    isCurrentUser: Boolean
) {
    Text(
        text = message.message,
        fontSize = 13.sp,
        color = if (isCurrentUser)
            Color.White.copy(alpha = 0.9f)
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
}

@Composable
fun RegularMessageContent(
    message: GroupMessage,
    isCurrentUser: Boolean
) {
    Text(
        text = message.message,
        fontSize = 14.sp,
        color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 20.sp
    )
}

@Composable
fun MessageAvatar(
    name: String,
    isOnline: Boolean = false,
    isCurrentUser: Boolean = false
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrentUser) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Online indicator
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
fun MessageStatusIcon(
    isDelivered: Boolean,
    isRead: Boolean
) {
    when {
        isRead -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF4CAF50)
            )
        }
        isDelivered -> {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Delivered",
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TypingIndicator(
    typingUsers: Set<String>,
    currentUserId: String
) {
    val filteredUsers = typingUsers.filter { it != currentUserId }

    if (filteredUsers.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            MessageAvatar(name = "?", isOnline = true)
            Spacer(modifier = Modifier.width(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypingDotsAnimation()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (filteredUsers.size) {
                            1 -> "Someone is typing..."
                            else -> "${filteredUsers.size} people are typing..."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun TypingDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_animation")

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val animationDelay = index * 200
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = animationDelay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isEnabled: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment button (for future features)
            IconButton(
                onClick = { /* Handle attachments */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Text input
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                placeholder = {
                    Text(
                        "Type a message...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                enabled = isEnabled,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Send button
            AnimatedVisibility(
                visible = messageText.isNotBlank(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onSendMessage,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Voice message button (when no text)
            AnimatedVisibility(
                visible = messageText.isBlank(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick = { /* Handle voice message */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice message",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingMessagesIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Loading messages...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



// Helper function to format message time
private fun formatMessageTime(timestamp: String): String {
    return try {
        val messageTime = timestamp.toLongOrNull() ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val diff = now - messageTime

        when {
            diff < 60000 -> "now" // less than 1 minute
            diff < 3600000 -> "${diff / 60000}m" // less than 1 hour
            diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(messageTime)) // same day
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(messageTime)) // different day
        }
    } catch (e: Exception) {
        "now"
    }
}

// Helper function to determine if avatar should be shown
private fun shouldShowAvatar(message: GroupMessage, messages: List<GroupMessage>): Boolean {
    val messageIndex = messages.indexOf(message)
    if (messageIndex == -1) return true

    // Show avatar if it's the first message or if the previous message is from a different sender
    return messageIndex == 0 ||
            messages.getOrNull(messageIndex - 1)?.sender_id != message.sender_id ||
            messageIndex == messages.lastIndex
}