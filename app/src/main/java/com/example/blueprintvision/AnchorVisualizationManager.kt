package com.example.blueprintvision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.xr.arcore.Anchor
import androidx.xr.compose.spatial.Subspace
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * A composable that manages the visualization of anchors in 3D space
 * This component connects the anchor data from ARCoreManager with visual representations
 */
@Composable
fun AnchorVisualizationManager(
    arCoreManager: ARCoreManager
) {
    val uiState by arCoreManager.uiState.collectAsState()
    
    // Periodically update the tracking states of anchors
    LaunchedEffect(Unit) {
        while (true) {
            arCoreManager.updateAnchorTrackingStates()
            delay(500) // Update every 500ms
        }
    }
    
    // For each anchor in the manager, create a visual representation
    uiState.anchors.forEach { (uuid, anchorInfo) ->
        // Lookup the actual Anchor object
        val anchor = arCoreManager.getAnchor(uuid)
        
        // If we have a valid anchor, visualize it
        if (anchor != null) {
            TrackingAwareAnchorVisual(
                anchor = anchor,
                id = uuid,
                isPersisted = anchorInfo.isPersisted
            )
        }
    }
}
