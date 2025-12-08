package com.example.scholartrack

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

// --- Hardcoded Locations ---
val scholarshipLocations = mapOf(
    "1" to LatLng(16.046, 120.332), // STEM Future Leader - Dagupan
    "2" to LatLng(15.931, 120.575)  // Community Grant - Urdaneta
)

// --- Constants for Pangasinan ---
val PANGASINAN_CENTER = LatLng(15.92, 120.35)
val PANGASINAN_BOUNDS = LatLngBounds(
    LatLng(15.40, 119.70), // SW
    LatLng(16.50, 121.10)  // NE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTrackerScreen(scholarships: List<ScholarshipApp>, onAppClick: (ScholarshipApp) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // State
    var searchQuery by remember { mutableStateOf("") }

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
            onMapClick = { /* Disabled */ }
        ) {
            // Render Scholarship Pins
            scholarships.forEach { app ->
                scholarshipLocations[app.id]?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = app.name,
                        snippet = app.provider,
                        onClick = {
                            onAppClick(app)
                            true
                        }
                    )
                }
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
            val effectiveQuery = "$query, Pangasinan, Philippines"
            
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
