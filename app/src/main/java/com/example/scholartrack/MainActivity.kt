package com.example.scholartrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.annotations.SerializedName
import com.utsman.osmandcompose.rememberCameraState
import org.osmdroid.util.GeoPoint

// --- 1. Data Models ---

enum class AppStatus(val label: String, val color: Color) {
    SUBMITTED("Submitted", Color(0xFF2563EB)),
    PENDING("Pending", Color(0xFFD97706)),
    APPROVED("Approved", Color(0xFF059669)),
    DECLINED("Declined", Color(0xFFDC2626))
}

data class ScholarshipApp(
    @SerializedName("scholarship_id") val id: String,
    @SerializedName("scholarship_name") val name: String,
    @SerializedName("provider_name") val provider: String,
    @SerializedName("deadline_date") val deadline: String,
    @SerializedName("application_status") var status: AppStatus,
    @SerializedName("scholarship_notes") val notes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Applicant( 
    val id: String,
    val username: String,
    val email: String,
    val role: String = "applicant",
    val themeColor: Color = BluePrimary,
    val avatarUri: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScholarTrackTheme {
                ScholarTrackApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarTrackApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    var currentApplicant by remember { mutableStateOf<Applicant?>(null) } 

    NavHost(
        navController = navController, 
        startDestination = "login",
        enterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) },
        popExitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) }
    ) {
        composable("login") {
            WelcomeScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { response -> 
                    currentApplicant = Applicant(
                        id = response.userId ?: "0",
                        username = "${response.firstName} ${response.lastName}",
                        email = response.email ?: "applicant@example.com",
                        role = if (response.role == "student") "applicant" else (response.role ?: "applicant")
                    )
                    navController.navigate("main_app") { popUpTo("login") { inclusive = true } } 
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }
        composable("signup") {
             SignUpScreen(
                authViewModel = authViewModel,
                onSignUpSuccess = { navController.navigate("main_app") { popUpTo("login") { inclusive = true } } },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
        composable("main_app") {
            if (currentApplicant != null) {
                MainAppScreen(
                    applicant = currentApplicant!!, 
                    onLogout = {
                        authViewModel.resetState()
                        currentApplicant = null
                        navController.navigate("login") { popUpTo("main_app") { inclusive = true } }
                    },
                    onUpdateApplicant = { updated -> currentApplicant = updated }
                )
            } else {
                CircularProgressIndicator()
                LaunchedEffect(Unit) {
                     navController.navigate("login") { popUpTo("main_app") { inclusive = true } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    applicant: Applicant,
    onLogout: () -> Unit, 
    onUpdateApplicant: (Applicant) -> Unit,
    scholarshipViewModel: ScholarshipViewModel = viewModel()
) {
    var selectedApp by remember { mutableStateOf<ScholarshipApp?>(null) }
    val scholarships by scholarshipViewModel.scholarships.collectAsState()
    val errorMessage by scholarshipViewModel.errorMessage.collectAsState()

    LaunchedEffect(applicant.id) {
        scholarshipViewModel.fetchScholarships(applicant.id)
    }

    val innerNavController = rememberNavController()
    
    // Initialize the camera state with a GeoPoint and Zoom level.
    val cameraState = rememberCameraState {
        geoPoint = GeoPoint(15.92, 120.35)
        zoom = 10.0
    }

    val onAppClick = remember<(ScholarshipApp) -> Unit> { { app -> selectedApp = app } }
    
    // Updated dismiss handler to also reset the map view
    val onDialogDismiss = remember { 
        { 
            selectedApp = null 
            cameraState.geoPoint = GeoPoint(15.92, 120.35)
            cameraState.zoom = 10.0
        } 
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    Triple("dashboard", Icons.Default.Dashboard, "Home"),
                    Triple("tracker", Icons.Default.School, "Tracker"),
                    Triple("map", Icons.Default.Map, "Map"),
                    Triple("profile", Icons.Default.Person, "Profile")
                )

                items.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = { 
                            if (currentRoute != route) {
                                innerNavController.navigate(route) {
                                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding).background(Color.White),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable("dashboard") { DashboardScreen(applicant, scholarships, onAppClick, onRefresh = { scholarshipViewModel.fetchScholarships(applicant.id) }) }
            composable("tracker") { TrackerScreen(scholarships.toMutableList(), onAppClick) }
            composable("map") { MapTrackerScreen(scholarships, cameraState, onAppClick) }
            composable("profile") { 
                ProfileScreen(
                    applicant = applicant, 
                    onLogout = onLogout,
                    onUpdateAvatar = { newUri -> 
                        onUpdateApplicant(applicant.copy(avatarUri = newUri)) 
                    }
                ) 
            }
        }

        errorMessage?.let {
            Snackbar(action = { Button(onClick = { scholarshipViewModel.fetchScholarships(applicant.id) }) { Text("Retry") } }) {
                Text(it)
            }
        }
    }

    AnimatedVisibility(
        visible = selectedApp != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        selectedApp?.let { ScholarshipDetailDialog(app = it, onDismiss = onDialogDismiss) }
    }
}

@Composable
fun DashboardScreen(applicant: Applicant, apps: List<ScholarshipApp>, onAppClick: (ScholarshipApp) -> Unit, onRefresh: () -> Unit) {
    val pendingCount = apps.count { it.status == AppStatus.PENDING || it.status == AppStatus.SUBMITTED }
    val approvedCount = apps.count { it.status == AppStatus.APPROVED }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    applicant.username.take(1), 
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hi, ${applicant.username}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                Text("Your progress", color = Color.Gray)
            }
            
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "Pending",
                count = pendingCount.toString(),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Approved",
                count = approvedCount.toString(),
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Recent Updates", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkSlate)
        Spacer(modifier = Modifier.height(16.dp))
        
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

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        Text("My Applications", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            item {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(AppStatus.values()) { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { selectedStatus = status },
                    label = { Text(status.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
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

@Composable
fun StatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, fontSize = 14.sp)
            Text(count, color = color, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
    }
}

@Composable
fun AppListItem(app: ScholarshipApp, onAppClick: (ScholarshipApp) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
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
                Text(app.name, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                Text(app.provider, fontSize = 14.sp, color = Color.Gray)
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ScholarshipDetailDialog(app: ScholarshipApp, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(app.provider, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deadline: ${app.deadline}")
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Text(app.notes, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
