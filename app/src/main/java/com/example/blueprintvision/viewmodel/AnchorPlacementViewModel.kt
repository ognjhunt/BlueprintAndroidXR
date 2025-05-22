package com.example.blueprintvision.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.xr.arcore.TrackingState
import com.example.blueprintvision.ARCoreManager
import com.example.blueprintvision.firebase.FirestoreManager
import com.example.blueprintvision.model.Anchor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing AR anchor placement and interaction
 */
class AnchorPlacementViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AnchorPlacementVM"
    
    // AR Core manager
    private val arCoreManager = ARCoreManager(application)
    
    // AR session state
    enum class ARSessionState {
        INITIALIZING, // AR session is being set up
        READY,        // AR session is ready for placement
        ERROR         // AR session encountered an error
    }
    
    // State flows
    private val _arSessionState = MutableStateFlow(ARSessionState.INITIALIZING)
    val arSessionState: StateFlow<ARSessionState> = _arSessionState.asStateFlow()
    
    private val _isPlacementMode = MutableStateFlow(false)
    val isPlacementMode: StateFlow<Boolean> = _isPlacementMode.asStateFlow()
    
    private val _isDownloadingAnchors = MutableStateFlow(false)
    val isDownloadingAnchors: StateFlow<Boolean> = _isDownloadingAnchors.asStateFlow()
    
    private val _currentAnchorStatus = MutableStateFlow("")
    val currentAnchorStatus: StateFlow<String> = _currentAnchorStatus.asStateFlow()
    
    private val _userRelativeWorldPosition = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val userRelativeWorldPosition: StateFlow<FloatArray> = _userRelativeWorldPosition.asStateFlow()
    
    /**
     * Initialize ARCore
     */
    fun initARCore() {
        _arSessionState.value = ARSessionState.INITIALIZING
        
        try {
            arCoreManager.setupSession { success ->
                if (success) {
                    _arSessionState.value = ARSessionState.READY
                } else {
                    _arSessionState.value = ARSessionState.ERROR
                    setAnchorStatus("Failed to initialize AR session")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ARCore: ${e.message}", e)
            _arSessionState.value = ARSessionState.ERROR
            setAnchorStatus("Error: ${e.message}")
        }
    }
    
    /**
     * Resume AR session when the app resumes
     */
    fun resumeARSession() {
        arCoreManager.resumeSession()
        
        // Start tracking user position
        arCoreManager.setFrameUpdateListener { frame ->
            val pose = frame.camera.pose
            val position = pose.translation
            _userRelativeWorldPosition.value = position
        }
    }
    
    /**
     * Pause AR session when the app pauses
     */
    fun pauseARSession() {
        arCoreManager.pauseSession()
    }
    
    /**
     * Toggle placement mode for creating new anchors
     */
    fun togglePlacementMode() {
        _isPlacementMode.value = !_isPlacementMode.value
        setAnchorStatus(if (_isPlacementMode.value) "Tap to place an anchor" else "")
    }
    
    /**
     * Place an anchor at the current position
     */
    fun placeAnchor(callback: (String?, String) -> Unit) {
        if (_arSessionState.value != ARSessionState.READY) {
            setAnchorStatus("AR session not ready")
            callback(null, "")
            return
        }
        
        arCoreManager.createAnchor { anchorId, anchorData ->
            if (anchorId != null) {
                setAnchorStatus("Anchor created")
                _isPlacementMode.value = false
                callback(anchorId, anchorData)
            } else {
                setAnchorStatus("Failed to create anchor")
                callback(null, "")
            }
        }
    }
    
    /**
     * Set the current anchor status message
     */
    fun setAnchorStatus(status: String) {
        _currentAnchorStatus.value = status
    }
    
    /**
     * Download anchors from Firebase
     */
    fun downloadAnchorsFromFirebase(blueprintId: String, anchorIds: List<String>) {
        _isDownloadingAnchors.value = true
        setAnchorStatus("Downloading anchors...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val anchors = if (anchorIds.isEmpty()) {
                    // If no anchor IDs provided, get all anchors for the blueprint
                    FirestoreManager.getBlueprintAnchors(blueprintId)
                } else {
                    // Get specific anchors by ID
                    val result = mutableListOf<Anchor>()
                    for (anchorId in anchorIds) {
                        val anchor = FirestoreManager.getAnchor(anchorId)
                        if (anchor != null) {
                            result.add(anchor)
                        }
                    }
                    result
                }
                
                // Resolve anchors in ARCore
                for (anchor in anchors) {
                    if (anchor.anchorData.isNotEmpty()) {
                        arCoreManager.resolveAnchor(anchor.id, anchor.anchorData)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    setAnchorStatus("Downloaded ${anchors.size} anchors")
                    _isDownloadingAnchors.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading anchors: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    setAnchorStatus("Error: ${e.message}")
                    _isDownloadingAnchors.value = false
                }
            }
        }
    }
    
    /**
     * Check if user is in a marked area
     */
    fun checkUserInMarkedArea(min: FloatArray, max: FloatArray): Boolean {
        val position = _userRelativeWorldPosition.value
        
        return position[0] >= min[0] && position[0] <= max[0] &&
               position[1] >= min[1] && position[1] <= max[1] &&
               position[2] >= min[2] && position[2] <= max[2]
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        arCoreManager.close()
    }
}
