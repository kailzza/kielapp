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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Data Model ---
data class MapPin(
    val id: String,
    val position: LatLng,
    var title: String,
    var description: String,
    var imageUri: String? = null // Storing URI string for native images
)

// --- Constants for Pangasinan ---
val PANGASINAN_CENTER = LatLng(15.92, 120.35)
val PANGASINAN_BOUNDS = LatLngBounds(
    LatLng(15.40, 119.70), // SW
    LatLng(16.50, 121.10)  // NE
)

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
    var isSearching by remember { mutableStateOf(false) }
    
    // Modal/Dialog State
    var activePin by remember { mutableStateOf<MapPin?>(null) } // Pin being viewed/edited
    var tempLatLng by remember { mutableStateOf<LatLng?>(null) } // Location picked for new pin
    var showEditDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }

    // Map Camera State
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(PANGASINAN_CENTER, 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. THE GOOGLE MAP
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false, // Set true if you handle permissions
                latLngBoundsForCameraTarget = PANGASINAN_BOUNDS,
                minZoomPreference = 9f
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            onMapClick = { latLng ->
                if (isAddingMode) {
                    tempLatLng = latLng
                    activePin = MapPin(System.currentTimeMillis().toString(), latLng, "", "")
                    showEditDialog = true
                    isAddingMode = false
                }
            }
        ) {
            // Render Pins
            pins.forEach { pin ->
                Marker(
                    state = MarkerState(position = pin.position),
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
                            scope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 14f))
                            }
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
                if (tempLatLng != null) activePin = null 
            },
            title = { Text(if (tempLatLng != null) "New Location" else "Edit Location") },
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
                    
                    if (tempLatLng != null) {
                        // Creating new
                        pins = pins + updatedPin
                        tempLatLng = null
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
                    if (tempLatLng != null) activePin = null
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
                        tempLatLng = null 
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
    onResult: (LatLng) -> Unit
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
                    onResult(LatLng(location.latitude, location.longitude))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
