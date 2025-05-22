package com.example.blueprintvision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.Pose
import androidx.xr.runtime.Session
import androidx.xr.scenecore.EntityAttachment
import androidx.xr.scenecore.EntityComponentBuilders
import androidx.xr.scenecore.MaterialColor
import androidx.xr.scenecore.MaterialPropertyType
import androidx.xr.scenecore.TransformProperty
import androidx.xr.scenecore.components.Material
import androidx.xr.scenecore.components.Mesh
import androidx.xr.scenecore.mesh.MeshPrimitiveType
import androidx.xr.scenecore.mesh.PrimitiveCubeBuilder
import androidx.xr.scenecore.mesh.PrimitiveSphereBuilder
import androidx.xr.scenecore.properties.TransformExtensions.translation
import androidx.xr.scenecore.properties.TransformExtensions.worldTranslation
import java.util.UUID

/**
 * Enum defining different types of anchor visualizations
 */
enum class AnchorVisualType {
    CUBE,
    SPHERE
}

/**
 * A composable that creates a visual representation of an anchor in 3D space
 */
@Composable
fun AnchorVisual(
    anchor: Anchor,
    type: AnchorVisualType = AnchorVisualType.CUBE,
    color: Color = Color.Blue,
    size: Float = 0.1f, // 10cm
    onDelete: () -> Unit = {}
) {
    // Remember the entity attachment for the anchor visual
    var entityAttachment by remember { mutableStateOf<EntityAttachment?>(null) }
    
    // Create the entity when the composable enters the composition
    LaunchedEffect(anchor, type, color, size) {
        // Create the appropriate primitive mesh based on the type
        val mesh = when (type) {
            AnchorVisualType.CUBE -> {
                val cubeBuilder = PrimitiveCubeBuilder()
                cubeBuilder.setWidth(size)
                cubeBuilder.setHeight(size)
                cubeBuilder.setDepth(size)
                cubeBuilder.build()
            }
            AnchorVisualType.SPHERE -> {
                val sphereBuilder = PrimitiveSphereBuilder()
                sphereBuilder.setRadius(size / 2)
                sphereBuilder.build()
            }
        }
        
        // Create material for the mesh
        val material = EntityComponentBuilders.material {
            setMaterialType(Material.MaterialType.UNLIT)
            setProperty(
                MaterialPropertyType.COLOR,
                MaterialColor(color.red, color.green, color.blue, color.alpha)
            )
        }.build()
        
        // Create the entity for the anchor visual
        val entity = EntityComponentBuilders.entity {
            // Add mesh component
            addComponent(
                EntityComponentBuilders.mesh {
                    setPrimitiveType(MeshPrimitiveType.TRIANGLES)
                    setMesh(mesh)
                    setMaterial(material)
                }.build()
            )
            
            // Set the transform to identity (will be updated by anchor)
            setProperty(TransformProperty.TRANSFORM, Pose.identity())
        }.build()
        
        // Attach the entity to the anchor
        entityAttachment = EntityAttachment.attachToAnchor(entity, anchor)
    }
    
    // Clean up the entity when the composable leaves the composition
    DisposableEffect(anchor) {
        onDispose {
            entityAttachment?.destroy()
            entityAttachment = null
        }
    }
}

/**
 * Creates a composable that represents an anchor visual with tracking state visualization
 */
@Composable
fun TrackingAwareAnchorVisual(
    anchor: Anchor,
    id: UUID,
    isPersisted: Boolean = false
) {
    // Determine color based on tracking state and persistence
    val color = when (anchor.trackingState) {
        androidx.xr.arcore.TrackingState.Tracking -> {
            if (isPersisted) Color.Green else Color.Blue
        }
        androidx.xr.arcore.TrackingState.Paused -> Color.Yellow
        else -> Color.Red
    }
    
    // Create the visual representation
    AnchorVisual(
        anchor = anchor,
        type = if (isPersisted) AnchorVisualType.SPHERE else AnchorVisualType.CUBE,
        color = color,
        size = 0.1f
    )
}
