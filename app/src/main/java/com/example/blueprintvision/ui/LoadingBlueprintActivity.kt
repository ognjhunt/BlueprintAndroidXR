package com.example.blueprintvision.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blueprintvision.ARCoreManager
import com.example.blueprintvision.manager.BlueprintManager
import com.example.blueprintvision.ui.theme.BlueprintVisionTheme
import kotlinx.coroutines.delay

/**
 * Loading screen that shows the progress of loading a blueprint and its anchors
 */
class LoadingBlueprintActivity : ComponentActivity() {
    private lateinit var blueprintManager: BlueprintManager
    private lateinit var arCoreManager: ARCoreManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        blueprintManager = BlueprintManager.getInstance(this)
        arCoreManager = ARCoreManager(this)
        
        val blueprintId = intent.getStringExtra("BLUEPRINT_ID") ?: return
        
        setContent {
            BlueprintVisionTheme {
                LoadingScreen(
                    blueprintManager = blueprintManager,
                    arCoreManager = arCoreManager,
                    blueprintId = blueprintId,
                    onLoadingComplete = {
                        // Navigate to the Anchor Placement screen
                        val intent = Intent(this, AnchorPlacementActivity::class.java)
                        startActivity(intent)
                        finish() // Finish this activity so user can't go back to loading screen
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(
    blueprintManager: BlueprintManager,
    arCoreManager: ARCoreManager,
    blueprintId: String,
    onLoadingComplete: () -> Unit
) {
    val isLoading by blueprintManager.isLoading.collectAsState()
    val errorMessage by blueprintManager.errorMessage.collectAsState()
    val blueprint by blueprintManager.currentBlueprint.collectAsState()
    val anchors by blueprintManager.anchors.collectAsState()
    
    // Track loading steps
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4
    
    var arSessionInitialized by remember { mutableStateOf(false) }
    var anchorsDownloaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(blueprintId) {
        // Start loading the blueprint
        blueprintManager.loadBlueprint(blueprintId)
    }
    
    // Initialize ARCore session
    LaunchedEffect(Unit) {
        arCoreManager.setupSession { success ->
            arSessionInitialized = success
            if (success) {
                currentStep = 1
            } else {
                // Handle ARCore initialization failure
                // This could still allow the app to continue but with limited functionality
                Log.e("LoadingScreen", "Failed to initialize ARCore session")
            }
        }
    }
    
    // Process blueprint loading and anchor download
    LaunchedEffect(blueprint, arSessionInitialized) {
        if (blueprint != null) {
            currentStep = if (arSessionInitialized) 2 else 1
            delay(500) // Process blueprint
            
            // Download anchors
            if (blueprint.anchorIDs.isNotEmpty()) {
                currentStep = 3
                blueprintManager.downloadAnchorsFromFirebase(blueprintId, blueprint.anchorIDs)
            } else {
                anchorsDownloaded = true
                currentStep = 3
            }
        }
    }
    
    // Check for completion
    LaunchedEffect(isLoading, anchors, anchorsDownloaded) {
        if (blueprint != null && (!isLoading || anchorsDownloaded || anchors.isNotEmpty())) {
            // Anchors are loaded or there are no anchors to load
            currentStep = 4
            delay(1000) // Final preparation
            
            if (errorMessage == null) {
                onLoadingComplete()
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Loading Blueprint",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps },
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = when (currentStep) {
                        0 -> "Initializing..."
                        1 -> "Initializing AR Session..."
                        2 -> "Loading blueprint data..."
                        3 -> "Loading anchors..."
                        else -> "Preparing XR environment..."
                    },
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                
                // Show blueprint name if available
                blueprint?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it.name,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Show anchor count if available
                if (anchors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Found ${anchors.size} anchors",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Show AR session status
                if (arSessionInitialized) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AR Session Ready",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Show error if any
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onBackPressedDispatcher.onBackPressed() }
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
