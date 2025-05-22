package com.example.blueprintvision.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blueprintvision.ARCoreManager
import com.example.blueprintvision.firebase.FirestoreManager
import com.example.blueprintvision.manager.BlueprintManager
import com.example.blueprintvision.model.Anchor
import com.example.blueprintvision.model.Blueprint
import com.example.blueprintvision.model.Quaternion
import com.example.blueprintvision.model.Vector3
import com.example.blueprintvision.ui.theme.BlueprintVisionTheme
import com.example.blueprintvision.viewmodel.AnchorPlacementViewModel
import java.util.*

/**
 * The main AR experience that uses ARCore for anchor placement and persistence
 * This is the Android equivalent of the visionOS ObjectPlacementRealityView
 */
class AnchorPlacementActivity : ComponentActivity() {
    private lateinit var blueprintManager: BlueprintManager
    private val viewModel: AnchorPlacementViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        blueprintManager = BlueprintManager.getInstance(this)
        
        setContent {
            BlueprintVisionTheme {
                AnchorPlacementScreen(
                    blueprintManager = blueprintManager,
                    viewModel = viewModel,
                    onNavigateBack = { 
                        // Return to welcome screen
                        val intent = Intent(this, WelcomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeARSession()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseARSession()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorPlacementScreen(
    blueprintManager: BlueprintManager,
    viewModel: AnchorPlacementViewModel,
    onNavigateBack: () -> Unit
) {
    val blueprint by blueprintManager.currentBlueprint.collectAsState()
    val anchors by blueprintManager.anchors.collectAsState()
    val arSessionState by viewModel.arSessionState.collectAsState()
    val isPlacementMode by viewModel.isPlacementMode.collectAsState()
    val isDownloadingAnchors by viewModel.isDownloadingAnchors.collectAsState()
    val currentAnchorStatus by viewModel.currentAnchorStatus.collectAsState()
    
    // Initialize AR view when the screen loads
    LaunchedEffect(Unit) {
        viewModel.initARCore()
        
        // Start downloading anchors from Firebase
        blueprint?.let { viewModel.downloadAnchorsFromFirebase(it.id, it.anchorIDs) }
    }
    
    // AR view takes up full screen with UI elements overlaid
    Box(modifier = Modifier.fillMaxSize()) {
        // AR View - This would be replaced with a SurfaceView in a real implementation
        // The ARCore implementation would render the camera feed and AR content here
        ARCoreView(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel
        )
        
        // Top toolbar
        TopAppBar(
            title = {
                blueprint?.let {
                    Text(
                        text = it.name,
                        color = Color.White
                    )
                } ?: Text("AR View", color = Color.White)
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                if (isPlacementMode) {
                    // Cancel placement mode
                    IconButton(onClick = { viewModel.togglePlacementMode() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Placement",
                            tint = Color.White
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        )
        
        // Status indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (arSessionState) {
                                AnchorPlacementViewModel.ARSessionState.INITIALIZING -> Color.Yellow
                                AnchorPlacementViewModel.ARSessionState.TRACKING -> Color.Green
                                AnchorPlacementViewModel.ARSessionState.NOT_TRACKING -> Color.Red
                                AnchorPlacementViewModel.ARSessionState.ERROR -> Color.Red
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp) // Below the top app bar
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(width = 8.dp)
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Show loading indicator when downloading anchors
        if (isDownloadingAnchors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Downloading Anchors...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        // Floating action buttons for various actions
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Voice Assistant Button
            FloatingActionButton(
                onClick = { showVoiceAssistant = !showVoiceAssistant },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Mic, "Voice Assistant")
            }
            
            // Info Button
            FloatingActionButton(
                onClick = { showInfoPanel = !showInfoPanel },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.Info, "Blueprint Info")
            }
        }
        
        // Voice assistant panel
        if (showVoiceAssistant) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .width(300.dp)
                    .shadow(8.dp, MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Voice Assistant",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showVoiceAssistant = false }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                    
                    Spacer(height = 16.dp)
                    
                    Text("Tap the microphone to start speaking")
                    
                    Spacer(height = 16.dp)
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                // This would activate voice recognition in a real implementation
                                viewModel.setAnchorStatus("Voice assistant activated")
                            },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Mic, "Start Voice")
                        }
                    }
                }
            }
        }
        
        // Blueprint info panel
        if (showInfoPanel && blueprint != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .width(300.dp)
                    .shadow(8.dp, MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Blueprint Info",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showInfoPanel = false }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    blueprint?.let { bp ->
                        Text(
                            "Name: ${bp.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        if (bp.description.isNotEmpty()) {
                            Text(
                                "Description: ${bp.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        Text(
                            "Anchors: ${bp.anchorIDs.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        Text(
                            "Marked Areas: ${bp.markedAreas.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Bottom UI panel with anchor controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status message
            if (currentAnchorStatus.isNotEmpty()) {
                Text(
                    text = currentAnchorStatus,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Create new anchor button
                if (!isPlacementMode) {
                    Button(
                        onClick = { viewModel.togglePlacementMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Anchor"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Anchor")
                    }
                } else {
                    // Place anchor button
                    Button(
                        onClick = {
                            viewModel.placeAnchor { anchorId, anchorData ->
                                if (anchorId != null) {
                                    // Create Firebase anchor
                                    val newAnchor = Anchor(
                                        id = anchorId,
                                        blueprintId = blueprint?.id ?: "",
                                        name = "Anchor ${Date().time}",
                                        anchorData = anchorData,
                                        createdAt = Date(),
                                        createdBy = FirestoreManager.getCurrentUserUid2(),
                                        position = Vector3(0.0, 0.0, 0.0), // Will be updated by ARCore
                                        rotation = Quaternion(0.0, 0.0, 0.0, 1.0)
                                    )
                                    
                                    // Save to Firebase
                                    blueprintManager.saveAnchor(newAnchor) { success ->
                                        if (success) {
                                            viewModel.setAnchorStatus("Anchor saved successfully")
                                        } else {
                                            viewModel.setAnchorStatus("Failed to save anchor")
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Place Anchor"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Place Anchor")
                    }
                    
                    // Cancel placement button
                    Button(
                        onClick = { viewModel.togglePlacementMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Placement"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ARCoreView(
    modifier: Modifier = Modifier,
    viewModel: AnchorPlacementViewModel
) {
    val arSessionState by viewModel.arSessionState.collectAsState()
    val anchors by viewModel.uiState.collectAsState()
    
    // AndroidView would be used here to integrate with the actual ARCore GLSurfaceView
    // For this implementation, we're using a placeholder that simulates the AR view
    Box(
        modifier = modifier.background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        when (arSessionState) {
            AnchorPlacementViewModel.ARSessionState.INITIALIZING -> {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = "Initializing AR...",
                    color = Color.White,
                    modifier = Modifier.padding(top = 50.dp)
                )
            }
            AnchorPlacementViewModel.ARSessionState.ERROR -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AR Session Error",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Please restart the app or check device compatibility",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                // In a real implementation, this would be the AR camera feed with anchors
                // For now, just show a message
                Text(
                    text = "AR View Active - ${anchors.anchors.size} anchors visible",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        
        // Display anchor visualization indicators
        // In a real implementation, these would be 3D objects in the AR scene
        if (arSessionState == AnchorPlacementViewModel.ARSessionState.READY && anchors.anchors.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                // This is just a visual indicator for demonstration
                // Real anchors would be rendered in the AR scene by ARCore
                anchors.anchors.forEach { (_, anchorInfo) ->
                    if (anchorInfo.trackingState == androidx.xr.arcore.TrackingState.TRACKING) {
                        // Place a dot at a random position to simulate anchor visualization
                        // In real implementation, these would be at the actual anchor positions
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (Math.random() * 300).dp,
                                    y = (Math.random() * 500).dp
                                )
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                    }
                }
            }
        }
    }
}
