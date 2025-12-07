package com.example.scholartrack

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage // Requires coil-compose dependency for real images

// Mock Notification Data Class
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val type: String // "info", "success", "alert"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    onLogout: () -> Unit,
    onUpdateUser: (User) -> Unit
) {
    // State
    var showNotifications by remember { mutableStateOf(false) }
    var showCustomization by remember { mutableStateOf(false) }
    
    // Mock Notifications
    val notifications = remember { mutableStateListOf(
        AppNotification("1", "Welcome", "Thanks for joining ScholarTrack!", false, "info"),
        AppNotification("2", "Deadline Warning", "Grant deadline in 3 days.", false, "alert")
    )}

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        
        // --- Header Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
                // Avatar with Theme Gradient
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(user.themeColor.copy(alpha = 0.2f), user.themeColor.copy(alpha = 0.6f))
                            )
                        )
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatarUri != null) {
                        // Use Coil for image loading if you added the dependency
                        // AsyncImage(model = user.avatarUri, contentDescription = null, contentScale = ContentScale.Crop)
                        Text("IMG", color = Color.White) // Fallback
                    } else {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(user.username, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(user.email, fontSize = 14.sp, color = Color(0xFF64748B))
            }
        }

        // --- Menu List ---
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // Account Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ProfileMenuItem(icon = Icons.Default.Email, label = "Email", value = user.email)
                    Divider(color = Color(0xFFF1F5F9))
                    ProfileMenuItem(icon = Icons.Default.Person, label = "Username", value = user.username)
                }
            }

            // Preferences Card
            Text("PREFERENCES", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    // Notification Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNotifications = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Notifications", modifier = Modifier.weight(1f))
                        val unread = notifications.count { !it.isRead }
                        if (unread > 0) {
                            Surface(color = Color(0xFFEF4444), shape = CircleShape) {
                                Text(
                                    unread.toString(), 
                                    color = Color.White, 
                                    fontSize = 12.sp, 
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
                    }
                    
                    Divider(color = Color(0xFFF1F5F9))
                    
                    // Customization Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomization = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Profile Customization", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2)),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    // --- Customization Bottom Sheet / Dialog ---
    if (showCustomization) {
        var tempName by remember { mutableStateOf(user.username) }
        var tempColor by remember { mutableStateOf(user.themeColor) }
        
        // Image Picker Launcher
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
             // In a real app, you'd persist this URI permissions
             if (uri != null) {
                 onUpdateUser(user.copy(avatarUri = uri.toString()))
             }
        }

        AlertDialog(
            onDismissRequest = { showCustomization = false },
            title = { Text("Customize Profile") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar Edit
                    Box(modifier = Modifier.clickable { launcher.launch("image/*") }) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                        }
                    }
                    Text("Tap to change photo", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme Color", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val colors = listOf(
                            Color(0xFF2563EB), // Blue
                            Color(0xFF059669), // Emerald
                            Color(0xFFD97706), // Amber
                            Color(0xFF7C3AED), // Purple
                            Color(0xFFE11D48)  // Rose
                        )
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { tempColor = c }
                                    .border(if (tempColor == c) 3.dp else 0.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                            ) {
                                if (tempColor == c) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateUser(user.copy(username = tempName, themeColor = tempColor))
                    showCustomization = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showCustomization = false }) { Text("Cancel") } }
        )
    }

    // --- Notifications Dialog ---
    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notifications", modifier = Modifier.weight(1f))
                    TextButton(onClick = { notifications.clear() }) { Text("Clear", color = Color.Gray) }
                }
            },
            text = {
                if (notifications.isEmpty()) {
                    Text("No new notifications", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notifications) { notif ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if(notif.isRead) Color.White else Color(0xFFEFF6FF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(notif.message, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 14.sp, color = Color(0xFF0F172A))
        }
    }
}