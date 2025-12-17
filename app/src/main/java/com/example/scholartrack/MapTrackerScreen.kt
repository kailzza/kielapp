package com.example.scholartrack

import android.content.Context
import android.graphics.PorterDuff
import android.location.Geocoder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.utsman.osmandcompose.CameraState
import com.utsman.osmandcompose.Marker
import com.utsman.osmandcompose.OpenStreetMap
import com.utsman.osmandcompose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTrackerScreen(
    scholarships: List<ScholarshipApp>,
    cameraState: CameraState,
    onAppClick: (ScholarshipApp) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }

    val redMarkerIcon = remember {
        val resourceId = context.resources.getIdentifier("marker_default", "drawable", context.packageName)
        val drawable = ContextCompat.getDrawable(context, resourceId)
        drawable?.mutate()?.apply {
            setColorFilter(Color.Red.toArgb(), PorterDuff.Mode.SRC_IN)
        }
        drawable
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        OpenStreetMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState
        ) {
            // --- UPDATED LOGIC: Loop through API data directly ---
            scholarships.forEach { app ->
                // Check if coordinates are valid (not 0.0, 0.0)
                if (app.latitude != 0.0 && app.longitude != 0.0) {
                    val location = GeoPoint(app.latitude, app.longitude)
                    
                    Marker(
                        state = rememberMarkerState(geoPoint = location),
                        title = app.name,
                        snippet = app.provider,
                        icon = redMarkerIcon,
                        onClick = { 
                            cameraState.geoPoint = location
                            cameraState.zoom = 15.0 // Slightly closer zoom
                            onAppClick(app)
                            true 
                        }
                    )
                }
            }
        }

        // Search Bar (No changes needed here, keeping your existing code)
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
    }
}

// ... performSearch function remains the same ...

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
            val effectiveQuery = "$query, Pangasinan, Philippines"
            
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