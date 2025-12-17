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

// --- 1. Data Models (Equivalent to types.ts) ---

enum class AppStatus(val label: String, val color: Color) {
    SUBMITTED("Submitted", Color(0xFF2563EB)), // Blue
    PENDING("Pending", Color(0xFFD97706)),     // Amber
    APPROVED("Approved", Color(0xFF059669)),   // Green
    DECLINED("Declined", Color(0xFFDC2626))    // Red
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

// 1. Rename 'User' to 'Applicant'
data class Applicant( 
    val id: String,
    val username: String,
    val email: String,
    val role: String = "applicant", // Default role is now 'applicant'
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
fun ScholarTrackApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    // 2. Update state variable
    var currentApplicant by remember { mutableStateOf<Applicant?>(null) } 

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            WelcomeScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { response -> 
                    // 3. Create the Applicant object
                    currentApplicant = Applicant(
                        id = response.userId ?: "0",
                        username = "${response.firstName} ${response.lastName}",
                        email = response.email ?: "applicant@example.com",
                        // Force the role to be 'applicant' if the backend sends 'student'
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
                MainAppScreen(applicant = currentApplicant!!, onLogout = {
                    authViewModel.resetState()
                    currentApplicant = null // Clear user
                    navController.navigate("login") { popUpTo("main_app") { inclusive = true } }
                })
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
    applicant: Applicant, // Update parameter name
    onLogout: () -> Unit, 
    scholarshipViewModel: ScholarshipViewModel = viewModel()
) {
    var selectedApp by remember { mutableStateOf<ScholarshipApp?>(null) }
    
    val scholarships by scholarshipViewModel.scholarships.collectAsState()
    val errorMessage by scholarshipViewModel.errorMessage.collectAsState()

    // Update fetch call
    LaunchedEffect(applicant.id) {
        scholarshipViewModel.fetchScholarships(applicant.id)
    }

    val innerNavController = rememberNavController()

    val cameraState = rememberCameraState()
    var isMapInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isMapInitialized) {
            cameraState.geoPoint = GeoPoint(15.92, 120.35) // Pangasinan Center
            cameraState.zoom = 10.0
            isMapInitialized = true
        }
    }

    val onAppClick = remember<(ScholarshipApp) -> Unit> { { app -> selectedApp = app } }
    val onDialogDismiss = remember { { selectedApp = null } }
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentRoute == "dashboard",
                    onClick = { innerNavController.navigate("dashboard") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    label = { Text("Tracker") },
                    selected = currentRoute == "tracker",
                    onClick = { innerNavController.navigate("tracker") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Map") },
                    selected = currentRoute == "map",
                    onClick = { innerNavController.navigate("map") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = { innerNavController.navigate("profile") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding).background(Color(0xFFF8FAFC))
        ) {
            composable("dashboard") { 
                DashboardScreen(
                    applicant = applicant, 
                    apps = scholarships, 
                    onAppClick = onAppClick,
                    onRefresh = { scholarshipViewModel.fetchScholarships(applicant.id) } 
                )
            }
            composable("tracker") { TrackerScreen(scholarships.toMutableList(), onAppClick) }
            composable("map") { MapTrackerScreen(scholarships, cameraState, onAppClick) }
            composable("profile") { ProfileScreen(applicant = applicant, onLogout = onLogout) }
        }

        errorMessage?.let {
            Snackbar(action = { Button(onClick = { scholarshipViewModel.fetchScholarships(applicant.id) }) { Text("Retry") } }) {
                Text(it)
            }
        }
    }

    if (selectedApp != null) {
        ScholarshipDetailDialog(app = selectedApp!!, onDismiss = onDialogDismiss)
    }
}

// --- 3. Screens ---

@Composable
fun DashboardScreen(applicant: Applicant, apps: List<ScholarshipApp>, onAppClick: (ScholarshipApp) -> Unit, onRefresh: () -> Unit) {
    val pendingCount = apps.count { it.status == AppStatus.PENDING || it.status == AppStatus.SUBMITTED }
    val approvedCount = apps.count { it.status == AppStatus.APPROVED }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(applicant.username.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hi, ${applicant.username}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Your progress", color = Color.Gray)
            }
            
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
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

@Composable
fun WelcomeScreen(
    authViewModel: AuthViewModel, 
    onLoginSuccess: (AuthResponse) -> Unit, // Pass the full response
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val loginResponse by authViewModel.loginResponse.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "BrightPath Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "BrightPath",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (authState == AuthState.LOADING) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { authViewModel.login(email, password) }) {
                Text(text = "Login")
            }
            errorMessage?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
        }

        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Sign up")
        }
    }

    LaunchedEffect(authState) {
        if (authState == AuthState.AUTHENTICATED) {
            loginResponse?.let { onLoginSuccess(it) }
        }
    }
}