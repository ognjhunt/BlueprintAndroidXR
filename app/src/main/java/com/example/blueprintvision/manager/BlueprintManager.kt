package com.example.blueprintvision.manager

import android.content.Context
import android.util.Log
import com.example.blueprintvision.firebase.FirestoreManager
import com.example.blueprintvision.model.Anchor
import com.example.blueprintvision.model.Blueprint
import com.example.blueprintvision.model.Model
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Manages blueprint data and operations for the application
 */
class BlueprintManager(private val context: Context) {
    companion object {
        private const val TAG = "BlueprintManager"
        private const val SELECTED_BLUEPRINT_KEY = "selected_blueprint_id"
        
        // Singleton instance
        @Volatile
        private var instance: BlueprintManager? = null
        
        fun getInstance(context: Context): BlueprintManager {
            return instance ?: synchronized(this) {
                instance ?: BlueprintManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // StateFlows for the currently selected blueprint
    private val _currentBlueprint = MutableStateFlow<Blueprint?>(null)
    val currentBlueprint = _currentBlueprint.asStateFlow()
    
    // StateFlow for the anchors in the current blueprint
    private val _anchors = MutableStateFlow<List<Anchor>>(emptyList())
    val anchors = _anchors.asStateFlow()
    
    // StateFlow for loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    // StateFlow for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    // StateFlow for the current location in the blueprint
    private val _currentLocation = MutableStateFlow<String?>(null)
    val currentLocation = _currentLocation.asStateFlow()
    
    // Store the currently selected blueprint ID
    private var _currentlySelectedBlueprintID: String? = null
    val currentlySelectedBlueprintID: String?
        get() = _currentlySelectedBlueprintID
    
    init {
        // Try to restore the previously selected blueprint ID
        context.getSharedPreferences("BlueprintVision", Context.MODE_PRIVATE).let { prefs ->
            val savedBlueprintId = prefs.getString(SELECTED_BLUEPRINT_KEY, null)
            if (savedBlueprintId != null) {
                _currentlySelectedBlueprintID = savedBlueprintId
                loadBlueprint(savedBlueprintId)
            }
        }
    }
    
    /**
     * Loads a blueprint by ID and its associated anchors
     */
    fun loadBlueprint(blueprintId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load blueprint from Firestore
                val blueprint = FirestoreManager.getBlueprint(blueprintId)
                
                if (blueprint != null) {
                    // Save selected blueprint ID
                    _currentlySelectedBlueprintID = blueprintId
                    context.getSharedPreferences("BlueprintVision", Context.MODE_PRIVATE).edit()
                        .putString(SELECTED_BLUEPRINT_KEY, blueprintId)
                        .apply()
                    
                    // Update StateFlow
                    _currentBlueprint.value = blueprint
                    
                    // Load anchors associated with this blueprint
                    loadAnchors(blueprintId)
                } else {
                    _errorMessage.value = "Blueprint not found"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading blueprint: ${e.message}", e)
                _errorMessage.value = "Error loading blueprint: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Loads anchors for a blueprint
     */
    suspend fun loadAnchors(blueprintId: String) {
        try {
            val anchors = FirestoreManager.getBlueprintAnchors(blueprintId)
            _anchors.value = anchors
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error loading anchors: ${e.message}", e)
            _errorMessage.value = "Error loading anchors: ${e.message}"
            _isLoading.value = false
        }
    }
    
    /**
     * Downloads anchors from Firebase for a blueprint
     */
    fun downloadAnchorsFromFirebase(blueprintId: String, anchorIds: List<String> = emptyList()) {
        _isLoading.value = true
        
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
                
                withContext(Dispatchers.Main) {
                    _anchors.value = anchors
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading anchors: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error downloading anchors: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Updates the current location in the blueprint
     */
    fun updateCurrentLocation(locationName: String?) {
        _currentLocation.value = locationName
    }
    
    /**
     * Saves an anchor to Firestore
     */
    fun saveAnchor(anchor: Anchor, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = FirestoreManager.saveAnchor(anchor)
                
                if (success) {
                    // If saved successfully, reload the anchors
                    _currentlySelectedBlueprintID?.let { loadAnchors(it) }
                    
                    // Also update the blueprint's anchorIDs if not already included
                    val blueprint = _currentBlueprint.value
                    if (blueprint != null && !blueprint.anchorIDs.contains(anchor.id)) {
                        val updatedAnchorIDs = blueprint.anchorIDs + anchor.id
                        val updateData = mapOf("anchorIDs" to updatedAnchorIDs)
                        FirestoreManager.updateBlueprint(blueprint.id, updateData)
                        
                        // Update the local blueprint object
                        _currentBlueprint.value = blueprint.copy(anchorIDs = updatedAnchorIDs)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    callback(success)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving anchor: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
    
    /**
     * Loads all available blueprints
     */
    fun getAllBlueprints(callback: (List<Blueprint>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val blueprints = FirestoreManager.getAllBlueprints()
                withContext(Dispatchers.Main) {
                    callback(blueprints)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading blueprints: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(emptyList())
                }
            }
        }
    }
    
    /**
     * Gets a model by ID
     */
    suspend fun getModel(modelId: String): Model? {
        return try {
            FirestoreManager.getModel(modelId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting model: ${e.message}", e)
            null
        }
    }
    
    /**
     * Creates a new blueprint
     */
    suspend fun createBlueprint(name: String, description: String, isPrivate: Boolean = false): String {
        val blueprint = Blueprint(
            name = name,
            description = description,
            creator = FirestoreManager.getCurrentUserUid2(),
            createdAt = Date(),
            updatedAt = Date(),
            isPrivate = isPrivate
        )
        
        return FirestoreManager.createBlueprint(blueprint)
    }
    
    /**
     * Clears the current blueprint selection
     */
    fun clearCurrentBlueprint() {
        _currentlySelectedBlueprintID = null
        _currentBlueprint.value = null
        _anchors.value = emptyList()
        _currentLocation.value = null
        
        context.getSharedPreferences("BlueprintVision", Context.MODE_PRIVATE).edit()
            .remove(SELECTED_BLUEPRINT_KEY)
            .apply()
    }
}
