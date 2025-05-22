package com.example.blueprintvision

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.Config
import androidx.xr.arcore.Earth
import androidx.xr.arcore.Plane
import androidx.xr.arcore.Pose
import androidx.xr.arcore.Trackable
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.ConfigCloudAnchorMode
import androidx.xr.runtime.PermissionsNotGranted
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureConfigurationNotSupported
import androidx.xr.runtime.SessionConfigurePermissionsNotGranted
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateFeatureNotSupported
import androidx.xr.runtime.SessionCreatePermissionsNotGranted
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionProvider
import androidx.xr.runtime.createSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages ARCore functionality including session creation, anchor creation,
 * and anchor persistence.
 */
class ARCoreManager(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ARCoreUiState())
    val uiState: StateFlow<ARCoreUiState> = _uiState.asStateFlow()
    
    private var session: Session? = null
    private val anchors = mutableMapOf<UUID, Anchor>()
    private var frameUpdateListener: ((com.google.ar.core.Frame) -> Unit)? = null

    /**
     * Initializes the ARCore session
     */
    fun initSession() {
        viewModelScope.launch {
            if (session != null) return@launch
            
            when (val result = context.createSession()) {
                is SessionCreateSuccess -> {
                    session = result.session
                    configureSession()
                    _uiState.update { it.copy(sessionInitialized = true) }
                    Log.d(TAG, "Session created successfully")
                }
                is SessionCreatePermissionsNotGranted -> {
                    _uiState.update { 
                        it.copy(
                            sessionInitialized = false,
                            errorMessage = "Missing permissions: ${result.permissions.joinToString()}"
                        )
                    }
                    Log.e(TAG, "Missing permissions: ${result.permissions.joinToString()}")
                }
                is SessionCreateFeatureNotSupported -> {
                    _uiState.update { 
                        it.copy(
                            sessionInitialized = false,
                            errorMessage = "Feature not supported: ${result.feature}"
                        )
                    }
                    Log.e(TAG, "Feature not supported: ${result.feature}")
                }
            }
        }
    }
    
    /**
     * Sets up the ARCore session with a callback for completion
     */
    fun setupSession(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (session != null) {
                callback(true)
                return@launch
            }
            
            when (val result = context.createSession()) {
                is SessionCreateSuccess -> {
                    session = result.session
                    configureSession()
                    _uiState.update { it.copy(sessionInitialized = true) }
                    Log.d(TAG, "Session created successfully")
                    callback(true)
                }
                is SessionCreatePermissionsNotGranted -> {
                    _uiState.update { 
                        it.copy(
                            sessionInitialized = false,
                            errorMessage = "Missing permissions: ${result.permissions.joinToString()}"
                        )
                    }
                    Log.e(TAG, "Missing permissions: ${result.permissions.joinToString()}")
                    callback(false)
                }
                is SessionCreateFeatureNotSupported -> {
                    _uiState.update { 
                        it.copy(
                            sessionInitialized = false,
                            errorMessage = "Feature not supported: ${result.feature}"
                        )
                    }
                    Log.e(TAG, "Feature not supported: ${result.feature}")
                    callback(false)
                }
            }
        }
    }
    
    /**
     * Configures the session with anchor persistence enabled
     */
    private fun configureSession() {
        val currentSession = session ?: return
        
        val newConfig = currentSession.config.copy(
            anchorPersistence = true
        )
        
        when (val result = currentSession.configure(newConfig)) {
            is SessionConfigureSuccess -> {
                Log.d(TAG, "Session configured successfully with anchor persistence")
            }
            is SessionConfigureConfigurationNotSupported -> {
                _uiState.update { 
                    it.copy(errorMessage = "Configuration not supported") 
                }
                Log.e(TAG, "Configuration not supported")
            }
            is SessionConfigurePermissionsNotGranted -> {
                _uiState.update { 
                    it.copy(errorMessage = "Missing permissions: ${result.permissions.joinToString()}") 
                }
                Log.e(TAG, "Missing permissions: ${result.permissions.joinToString()}")
            }
        }
    }
    
    /**
     * Creates an anchor at the specified pose
     * @param pose The pose at which to create the anchor
     * @param trackable Optional trackable to which the anchor will be attached
     * @return The UUID of the anchor if successful, null otherwise
     */
    fun createAnchor(pose: Pose, trackable: Trackable? = null): UUID? {
        val currentSession = session ?: return null
        
        val result = if (trackable != null) {
            Anchor.create(currentSession, pose, trackable)
        } else {
            Anchor.create(currentSession, pose)
        }
        
        return when (result) {
            is Anchor.CreateSuccess -> {
                val anchor = result.anchor
                val uuid = UUID.randomUUID()
                anchors[uuid] = anchor
                _uiState.update { 
                    it.copy(
                        anchors = it.anchors + Pair(uuid, AnchorInfo(uuid, anchor.trackingState, false))
                    )
                }
                Log.d(TAG, "Anchor created successfully with ID: $uuid")
                uuid
            }
            else -> {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to create anchor") 
                }
                Log.e(TAG, "Failed to create anchor")
                null
            }
        }
    }
    
    /**
     * Persists an anchor so it can be retrieved in future sessions
     * @param uuid The UUID of the anchor to persist
     * @return The persistent ID of the anchor if successful, null otherwise
     */
    fun persistAnchor(uuid: UUID): UUID? {
        val anchor = anchors[uuid] ?: return null
        
        try {
            val persistentId = anchor.persist()
            _uiState.update { state ->
                val updatedAnchors = state.anchors.toMutableMap()
                updatedAnchors[uuid] = AnchorInfo(uuid, anchor.trackingState, true, persistentId)
                state.copy(
                    anchors = updatedAnchors,
                    persistedAnchors = state.persistedAnchors + Pair(persistentId, uuid)
                )
            }
            Log.d(TAG, "Anchor persisted successfully with ID: $persistentId")
            return persistentId
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(errorMessage = "Failed to persist anchor: ${e.message}") 
            }
            Log.e(TAG, "Failed to persist anchor", e)
            return null
        }
    }
    
    /**
     * Loads a persisted anchor from storage
     * @param persistentId The persistent ID of the anchor to load
     * @return The UUID of the loaded anchor if successful, null otherwise
     */
    fun loadAnchor(persistentId: UUID): UUID? {
        val currentSession = session ?: return null
        
        when (val result = Anchor.load(currentSession, persistentId)) {
            is Anchor.CreateSuccess -> {
                val anchor = result.anchor
                val uuid = UUID.randomUUID()
                anchors[uuid] = anchor
                _uiState.update { state ->
                    state.copy(
                        anchors = state.anchors + Pair(uuid, AnchorInfo(uuid, anchor.trackingState, true, persistentId)),
                        persistedAnchors = state.persistedAnchors + Pair(persistentId, uuid)
                    )
                }
                Log.d(TAG, "Anchor loaded successfully with ID: $uuid")
                return uuid
            }
            else -> {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to load persisted anchor") 
                }
                Log.e(TAG, "Failed to load persisted anchor")
                return null
            }
        }
    }
    
    /**
     * Gets all persisted anchor UUIDs from storage
     * @return List of persisted anchor UUIDs
     */
    fun getPersistedAnchors(): List<UUID> {
        val currentSession = session ?: return emptyList()
        return try {
            val persistedUuids = Anchor.getPersistedAnchorUuids(currentSession)
            _uiState.update { 
                it.copy(availablePersistedAnchors = persistedUuids) 
            }
            persistedUuids
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(errorMessage = "Failed to get persisted anchors: ${e.message}") 
            }
            Log.e(TAG, "Failed to get persisted anchors", e)
            emptyList()
        }
    }
    
    /**
     * Removes a persisted anchor from storage
     * @param persistentId The persistent ID of the anchor to remove
     * @return True if successful, false otherwise
     */
    fun unpersistAnchor(persistentId: UUID): Boolean {
        val currentSession = session ?: return false
        
        return try {
            Anchor.unpersist(currentSession, persistentId)
            _uiState.update { state ->
                val associatedUuid = state.persistedAnchors[persistentId]
                val updatedPersistedAnchors = state.persistedAnchors.toMutableMap()
                updatedPersistedAnchors.remove(persistentId)
                
                val updatedAnchors = if (associatedUuid != null) {
                    val updatedMap = state.anchors.toMutableMap()
                    val anchor = updatedMap[associatedUuid]
                    if (anchor != null) {
                        updatedMap[associatedUuid] = anchor.copy(isPersisted = false, persistentId = null)
                    }
                    updatedMap
                } else {
                    state.anchors
                }
                
                state.copy(
                    anchors = updatedAnchors,
                    persistedAnchors = updatedPersistedAnchors,
                    availablePersistedAnchors = state.availablePersistedAnchors - persistentId
                )
            }
            Log.d(TAG, "Anchor unpersisted successfully with ID: $persistentId")
            true
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(errorMessage = "Failed to unpersist anchor: ${e.message}") 
            }
            Log.e(TAG, "Failed to unpersist anchor", e)
            false
        }
    }
    
    /**
     * Updates the tracking state of all anchors
     */
    fun updateAnchorTrackingStates() {
        for ((uuid, anchor) in anchors) {
            _uiState.update { state ->
                val updatedAnchors = state.anchors.toMutableMap()
                val anchorInfo = updatedAnchors[uuid]
                if (anchorInfo != null) {
                    updatedAnchors[uuid] = anchorInfo.copy(trackingState = anchor.trackingState)
                }
                state.copy(anchors = updatedAnchors)
            }
        }
    }
    
    /**
     * Creates a default pose 1 meter in front of the user
     * @return A pose 1 meter in front of the user
     */
    fun createDefaultPose(): Pose {
        // Create a pose that's 1 meter in front of the user
        return Pose.identity().translate(0f, 0f, -1f)
    }
    
    /**
     * Gets the Anchor object associated with the given UUID
     * @param uuid The UUID of the anchor to retrieve
     * @return The Anchor object if found, null otherwise
     */
    fun getAnchor(uuid: UUID): Anchor? {
        return anchors[uuid]
    }
    
    /**
     * Resume ARCore session
     */
    fun resumeSession() {
        session?.resume()
        Log.d(TAG, "ARCore session resumed")
    }
    
    /**
     * Pause ARCore session
     */
    fun pauseSession() {
        session?.pause()
        Log.d(TAG, "ARCore session paused")
    }
    
    /**
     * Creates an anchor at the current pose and returns its ID and data for persistence
     */
    fun createAnchor(callback: (String?, String) -> Unit) {
        val currentSession = session ?: run {
            Log.e(TAG, "Cannot create anchor: session is null")
            callback(null, "")
            return
        }
        
        try {
            // Get the camera pose (for placing in front of the user if no hit test)
            val cameraPose = currentSession.update().camera.pose
            
            // Create a pose 1 meter in front of the camera
            val anchorPose = cameraPose.compose(
                Pose.translation(0f, 0f, -1f)
            )
            
            // Create the anchor
            val anchor = currentSession.createAnchor(anchorPose)
            val anchorId = UUID.randomUUID().toString()
            anchors[UUID.fromString(anchorId)] = anchor
            
            // Get the cloud anchor ID
            val cloudAnchorData = anchor.cloudAnchorId ?: ""
            
            // Update UI state
            _uiState.update { state ->
                val updatedAnchors = state.anchors.toMutableMap()
                updatedAnchors[UUID.fromString(anchorId)] = AnchorInfo(
                    id = UUID.fromString(anchorId),
                    trackingState = anchor.trackingState,
                    isPersisted = false
                )
                state.copy(anchors = updatedAnchors)
            }
            
            Log.d(TAG, "Anchor created with ID: $anchorId")
            callback(anchorId, cloudAnchorData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create anchor: ${e.message}", e)
            callback(null, "")
        }
    }
    
    /**
     * Resolves an anchor from cloud anchor data
     */
    fun resolveAnchor(anchorId: String, cloudAnchorData: String) {
        val currentSession = session ?: run {
            Log.e(TAG, "Cannot resolve anchor: session is null")
            return
        }
        
        try {
            // Create the anchor from the cloud data
            // In alpha versions, this might need to be done differently
            // For now, let's create a regular anchor instead of a cloud anchor
            val cameraPose = currentSession.update().camera.pose
            val anchorPose = cameraPose.compose(Pose.translation(0f, 0f, -1f))
            val anchor = currentSession.createAnchor(anchorPose)
            if (anchor != null) {
                val uuid = UUID.fromString(anchorId)
                anchors[uuid] = anchor
                
                // Update UI state
                _uiState.update { state ->
                    val updatedAnchors = state.anchors.toMutableMap()
                    updatedAnchors[uuid] = AnchorInfo(
                        id = uuid,
                        trackingState = anchor.trackingState,
                        isPersisted = true,
                        persistentId = UUID.fromString(cloudAnchorData)
                    )
                    state.copy(anchors = updatedAnchors)
                }
                
                Log.d(TAG, "Anchor resolved with ID: $anchorId")
            } else {
                Log.e(TAG, "Failed to resolve anchor: null result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve anchor: ${e.message}", e)
        }
    }
    
    /**
     * Set a listener for frame updates
     */
    fun setFrameUpdateListener(listener: (com.google.ar.core.Frame) -> Unit) {
        frameUpdateListener = listener
    }
    
    /**
     * Close the ARCore session and release resources
     */
    fun close() {
        frameUpdateListener = null
        session?.close()
        session = null
    }
    
    /**
     * Cleanup resources when no longer needed
     */
    override fun onCleared() {
        super.onCleared()
        close()
    }
    
    companion object {
        private const val TAG = "ARCoreManager"
    }
}

/**
 * Represents the UI state for ARCore functionality
 */
data class ARCoreUiState(
    val sessionInitialized: Boolean = false,
    val anchors: Map<UUID, AnchorInfo> = emptyMap(),
    val persistedAnchors: Map<UUID, UUID> = emptyMap(), // Map of persistentId to runtime UUID
    val availablePersistedAnchors: List<UUID> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Information about an anchor
 */
data class AnchorInfo(
    val id: UUID,
    val trackingState: TrackingState,
    val isPersisted: Boolean,
    val persistentId: UUID? = null
)

/**
 * Composable that provides a SessionProvider for ARCore
 */
@Composable
fun SessionProviderEffect(
    arCoreManager: ARCoreManager,
    content: @Composable () -> Unit
) {
    LaunchedEffect(Unit) {
        arCoreManager.initSession()
    }
    
    content()
}
