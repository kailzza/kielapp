package com.example.scholartrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

// --- 1. Data Models (Equivalent to types.ts) ---

enum class AppStatus(val label: String, val color: Color) {
    SUBMITTED("Submitted", Color(0xFF2563EB)), // Blue
    PENDING("Pending", Color(0xFFD97706)),     // Amber
    APPROVED("Approved", Color(0xFF059669)),   // Green
    DECLINED("Declined", Color(0xFFDC2626))    // Red
}

data class ScholarshipApp(
    val id: String,
    val name: String,
    val provider: String,
    val deadline: String,
    var status: AppStatus,
    val notes: String = ""
)

data class User(
    val username: String,
    val email: String,
    val themeColor: Color = Color(0xFF2563EB),
    val avatarUri: String? = null
)

// --- 2. Main Activity & Navigation ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScholarTrackApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarTrackApp() {
    var isAuthenticated by remember { mutableStateOf(false) }

    if (isAuthenticated) {
        MainAppScreen(onLogout = { isAuthenticated = false })
    } else {
        WelcomeScreen(onLoginClicked = { isAuthenticated = true })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    // Global State
    var user by remember { mutableStateOf(User("Student", "student@example.com")) }
    var selectedApp by remember { mutableStateOf<ScholarshipApp?>(null) }
    
    // Mock Data
    val scholarships = remember { mutableStateListOf(
        ScholarshipApp("1", "STEM Future Leader", "Tech Foundation", "2024-05-15", AppStatus.SUBMITTED, notes = "This is a note for the STEM scholarship."),
        ScholarshipApp("2", "Community Grant", "City Council", "2024-06-01", AppStatus.PENDING, notes = "Notes for the community grant.")
    )}

    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentRoute == "dashboard",
                    onClick = { navController.navigate("dashboard") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    label = { Text("Tracker") },
                    selected = currentRoute == "tracker",
                    onClick = { navController.navigate("tracker") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Map") },
                    selected = currentRoute == "map",
                    onClick = { navController.navigate("map") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = { navController.navigate("profile") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding).background(Color(0xFFF8FAFC))
        ) {
            composable("dashboard") { DashboardScreen(user, scholarships) { app -> selectedApp = app } }
            composable("tracker") { TrackerScreen(scholarships) { app -> selectedApp = app } }
            composable("map") { MapTrackerScreen(scholarships) { app -> selectedApp = app } }
            composable("profile") { ProfileScreen(user = user, onLogout = onLogout, onUpdateUser = { user = it }) }
        }
    }

    if (selectedApp != null) {
        ScholarshipDetailDialog(app = selectedApp!!, onDismiss = { selectedApp = null })
    }
}

// --- 3. Screens ---

@Composable
fun DashboardScreen(user: User, apps: List<ScholarshipApp>, onAppClick: (ScholarshipApp) -> Unit) {
    val pendingCount = apps.count { it.status == AppStatus.PENDING || it.status == AppStatus.SUBMITTED }
    val approvedCount = apps.count { it.status == AppStatus.APPROVED }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.username.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Hi, ${user.username}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Your progress", color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "Pending",
                count = pendingCount.toString(),
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Approved",
                count = approvedCount.toString(),
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Recent Updates", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps) { app ->
                AppListItem(app, onAppClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(apps: MutableList<ScholarshipApp>, onAppClick: (ScholarshipApp) -> Unit) {
    var selectedStatus by remember { mutableStateOf<AppStatus?>(null) }

    val filteredApps = if (selectedStatus == null) {
        apps
    } else {
        apps.filter { it.status == selectedStatus }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Applications", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            item {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("All") }
                )
            }
            items(AppStatus.values()) { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { selectedStatus = status },
                    label = { Text(status.label) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredApps) { app ->
                AppListItem(app, onAppClick)
            }
        }
    }
}



// --- 4. Helper Components ---

@Composable
fun StatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
    }
}

@Composable
fun AppListItem(app: ScholarshipApp, onAppClick: (ScholarshipApp) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAppClick(app) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                Text(app.provider, fontSize = 14.sp, color = Color(0xFF64748B))
            }
            Surface(
                color = app.status.color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = app.status.label,
                    color = app.status.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ScholarshipDetailDialog(app: ScholarshipApp, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Provider: ${app.provider}", fontWeight = FontWeight.SemiBold)
                Text("Deadline: ${app.deadline}")
                Text("Status: ${app.status.label}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(app.notes)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color.White
    )
}