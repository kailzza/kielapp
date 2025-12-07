package com.example.scholartrack

import android.content.Context
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.compose.*

// --- Data Model ---
data class MapPin(
    val id: String,
    val position: GeoPoint,
    var title: String,
    var description: String,
    var imageUri: String? = null // Storing URI string for native images
)

// --- Constants for Pangasinan ---
val PANGASINAN_CENTER = GeoPoint(15.92, 120.35)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTrackerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // State
    var pins by remember { mutableStateOf(listOf<MapPin>()) }
    var isAddingMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Modal/Dialog State
    var activePin by remember { mutableStateOf<MapPin?>(null) } // Pin being viewed/edited
    var tempGeoPoint by remember { mutableStateOf<GeoPoint?>(null) } // Location picked for new pin
    var showEditDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }

    // Map Camera State
    val cameraState = rememberCameraState {
        geoPoint = PANGASINAN_CENTER
        zoom = 10.0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. THE OPENSTREETMAP
        OpenStreetMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            onMapClick = { geoPoint ->
                if (isAddingMode) {
                    tempGeoPoint = geoPoint
                    activePin = MapPin(System.currentTimeMillis().toString(), geoPoint, "", "")
                    showEditDialog = true
                    isAddingMode = false
                }
            }
        ){
            // Render Pins
            pins.forEach { pin ->
                Marker(
                    state = rememberMarkerState(geoPoint = pin.position),
                    title = pin.title,
                    snippet = pin.description.take(20) + "...",
                    onClick = {
                        activePin = pin
                        showViewDialog = true
                        true
                    }
                )
            }
        }

        // 2. SEARCH BAR (Top)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Pangasinan...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        performSearch(context, searchQuery, scope) { loc ->
                            cameraState.geoPoint = loc
                            cameraState.zoom = 14.0
                        }
                    })
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            }
        }

        // 3. BOTTOM CONTROLS (Add Button)
        if (!isAddingMode) {
            ExtendedFloatingActionButton(
                onClick = { isAddingMode = true },
                icon = { Icon(Icons.Default.AddLocation, null) },
                text = { Text("Drop New Pin") },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        } else {
            // Cancel Adding Mode Banner
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap map to place pin", color = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.Gray))
                    TextButton(onClick = { isAddingMode = false }) {
                        Text("Cancel", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. EDIT DIALOG (For New or Editing Pins)
    if (showEditDialog && activePin != null) {
        var title by remember { mutableStateOf(activePin!!.title) }
        var desc by remember { mutableStateOf(activePin!!.description) }

        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                // If canceling a new pin, reset temp
                if (tempGeoPoint != null) activePin = null 
            },
            title = { Text(if (tempGeoPoint != null) "New Location" else "Edit Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedPin = activePin!!.copy(title = title, description = desc)
                    
                    if (tempGeoPoint != null) {
                        // Creating new
                        pins = pins + updatedPin
                        tempGeoPoint = null
                    } else {
                        // Editing existing
                        pins = pins.map { if (it.id == updatedPin.id) updatedPin else it }
                    }
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditDialog = false 
                    if (tempGeoPoint != null) activePin = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. VIEW DIALOG (Read Only Mode)
    if (showViewDialog && activePin != null) {
        ModalBottomSheet(
            onDismissRequest = { showViewDialog = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activePin!!.title.ifEmpty { "Untitled" }, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { 
                        showViewDialog = false
                        showEditDialog = true
                        // Ensure we aren't in "Create" mode
                        tempGeoPoint = null 
                    }) {
                        Icon(Icons.Default.Edit, null, tint = Color.Blue)
                    }
                    IconButton(onClick = {
                        pins = pins.filter { it.id != activePin!!.id }
                        showViewDialog = false
                    }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Placeholder for Image functionality
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                   Column(horizontalAlignment = Alignment.CenterHorizontally) {
                       Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                       Text("Image Attachment", color = Color.Gray)
                   }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    activePin!!.description.ifEmpty { "No description provided." }, 
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- Helper Logic for Search ---
fun performSearch(
    context: Context, 
    query: String, 
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (GeoPoint) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context)
            // Restrict search to roughly PH area box to prioritize local results
            // Note: Android Geocoder doesn't support strict bounds like Nominatim easily, 
            // but we can filter or just search "Query, Pangasinan"
            val effectiveQuery = "$query, Pangasinan, Philippines"
            
            // getFromLocationName is deprecated in API 33 but simple for examples
            @Suppress("DEPRECATION") 
            val addresses = geocoder.getFromLocationName(effectiveQuery, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                withContext(Dispatchers.Main) {
                    onResult(GeoPoint(location.latitude, location.longitude))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}