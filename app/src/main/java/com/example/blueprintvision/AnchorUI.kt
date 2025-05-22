package com.example.blueprintvision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.xr.arcore.Pose
import androidx.xr.arcore.TrackingState
import java.util.UUID

/**
 * Main UI component for anchor management
 */
@Composable
fun AnchorManagementUI(
    arCoreManager: ARCoreManager,
    modifier: Modifier = Modifier
) {
    val uiState by arCoreManager.uiState.collectAsState()
    var showPersistedAnchors by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title and status
            Text(
                text = "Blueprint Vision",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Session status indicator
            SessionStatusIndicator(
                isInitialized = uiState.sessionInitialized,
                errorMessage = uiState.errorMessage
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tabs for switching between active and persisted anchors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { showPersistedAnchors = false },
                    modifier = Modifier.weight(1f),
                    enabled = !showPersistedAnchors
                ) {
                    Text("Active Anchors")
                }
                
                Button(
                    onClick = { 
                        showPersistedAnchors = true
                        arCoreManager.getPersistedAnchors() // Refresh the list
                    },
                    modifier = Modifier.weight(1f),
                    enabled = showPersistedAnchors
                ) {
                    Text("Persisted Anchors")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display different content based on the selected tab
            if (showPersistedAnchors) {
                PersistedAnchorsContent(
                    persistedAnchors = uiState.availablePersistedAnchors,
                    onLoadAnchor = { arCoreManager.loadAnchor(it) },
                    onDeleteAnchor = { arCoreManager.unpersistAnchor(it) }
                )
            } else {
                ActiveAnchorsContent(
                    anchors = uiState.anchors,
                    onPersistAnchor = { arCoreManager.persistAnchor(it) }
                )
            }
            
            // Add new anchor button (only available in active anchors tab)
            if (!showPersistedAnchors) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (uiState.sessionInitialized) {
                                val pose = arCoreManager.createDefaultPose()
                                arCoreManager.createAnchor(pose)
                            }
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Anchor"
                        )
                    }
                }
            }
            
            // Error message if any
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Displays the session status
 */
@Composable
fun SessionStatusIndicator(
    isInitialized: Boolean,
    errorMessage: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (isInitialized) Color.Green else if (errorMessage != null) Color.Red else Color.Yellow
                )
        )
        
        Text(
            text = when {
                isInitialized -> "ARCore Session Active"
                errorMessage != null -> "Session Error"
                else -> "Session Initializing..."
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Content for displaying active anchors
 */
@Composable
fun ActiveAnchorsContent(
    anchors: Map<UUID, AnchorInfo>,
    onPersistAnchor: (UUID) -> UUID?
) {
    if (anchors.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active anchors.\nClick + to create a new anchor.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn {
            items(anchors.entries.toList()) { (uuid, anchorInfo) ->
                AnchorItem(
                    anchorInfo = anchorInfo,
                    onPersistClick = {
                        if (!anchorInfo.isPersisted) {
                            onPersistAnchor(uuid)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Content for displaying persisted anchors
 */
@Composable
fun PersistedAnchorsContent(
    persistedAnchors: List<UUID>,
    onLoadAnchor: (UUID) -> UUID?,
    onDeleteAnchor: (UUID) -> Boolean
) {
    if (persistedAnchors.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No persisted anchors found.\nPersist an anchor from the Active Anchors tab.",
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn {
            items(persistedAnchors) { persistentId ->
                PersistedAnchorItem(
                    persistentId = persistentId,
                    onLoadClick = { onLoadAnchor(persistentId) },
                    onDeleteClick = { onDeleteAnchor(persistentId) }
                )
            }
        }
    }
}

/**
 * UI item for a single active anchor
 */
@Composable
fun AnchorItem(
    anchorInfo: AnchorInfo,
    onPersistClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anchor ${anchorInfo.id.toString().take(8)}...",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (anchorInfo.trackingState) {
                                TrackingState.Tracking -> Color.Green
                                TrackingState.Paused -> Color.Yellow
                                else -> Color.Red
                            }
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Status: ${anchorInfo.trackingState.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = if (anchorInfo.isPersisted) "Persisted: Yes" else "Persisted: No",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (anchorInfo.persistentId != null) {
                Text(
                    text = "Persistent ID: ${anchorInfo.persistentId.toString().take(8)}...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!anchorInfo.isPersisted) {
                    IconButton(onClick = onPersistClick) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Persist Anchor"
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Already Persisted",
                            tint = Color.Green
                        )
                    }
                }
            }
        }
    }
}

/**
 * UI item for a single persisted anchor
 */
@Composable
fun PersistedAnchorItem(
    persistentId: UUID,
    onLoadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Persisted Anchor",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "ID: ${persistentId.toString().take(8)}...",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onLoadClick
                ) {
                    Text("Load")
                }
                
                Button(
                    onClick = onDeleteClick
                ) {
                    Text("Remove")
                }
            }
        }
    }
}
