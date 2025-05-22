package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Data class representing an Anchor stored in Firestore
 */
data class Anchor(
    val id: String = "",
    val blueprintId: String = "",
    val name: String = "",
    val position: Vector3 = Vector3(),
    val rotation: Quaternion = Quaternion(),
    val scale: Vector3 = Vector3(1.0, 1.0, 1.0),
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val createdBy: String = "",
    val anchorData: String = "", // ARCore specific anchor data for persistence
    val modelId: String = "",    // Optional reference to a 3D model
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * Creates an Anchor from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Anchor {
            val data = document.data ?: return Anchor(id = document.id)
            
            val positionMap = data["position"] as? Map<String, Any>
            val position = if (positionMap != null) {
                Vector3(
                    x = (positionMap["x"] as? Number)?.toDouble() ?: 0.0,
                    y = (positionMap["y"] as? Number)?.toDouble() ?: 0.0,
                    z = (positionMap["z"] as? Number)?.toDouble() ?: 0.0
                )
            } else {
                Vector3(0.0, 0.0, 0.0)
            }
            
            val rotationMap = data["rotation"] as? Map<String, Any>
            val rotation = if (rotationMap != null) {
                Quaternion(
                    x = (rotationMap["x"] as? Number)?.toDouble() ?: 0.0,
                    y = (rotationMap["y"] as? Number)?.toDouble() ?: 0.0,
                    z = (rotationMap["z"] as? Number)?.toDouble() ?: 0.0,
                    w = (rotationMap["w"] as? Number)?.toDouble() ?: 1.0
                )
            } else {
                Quaternion(0.0, 0.0, 0.0, 1.0)
            }
            
            val scaleMap = data["scale"] as? Map<String, Any>
            val scale = if (scaleMap != null) {
                Vector3(
                    x = (scaleMap["x"] as? Number)?.toDouble() ?: 1.0,
                    y = (scaleMap["y"] as? Number)?.toDouble() ?: 1.0,
                    z = (scaleMap["z"] as? Number)?.toDouble() ?: 1.0
                )
            } else {
                Vector3(1.0, 1.0, 1.0)
            }
            
            @Suppress("UNCHECKED_CAST")
            val metadata = data["metadata"] as? Map<String, Any> ?: emptyMap()
            
            return Anchor(
                id = document.id,
                blueprintId = data["blueprintId"] as? String ?: "",
                name = data["name"] as? String ?: "",
                position = position,
                rotation = rotation,
                scale = scale,
                createdAt = data["createdAt"] as? Date,
                updatedAt = data["updatedAt"] as? Date,
                createdBy = data["createdBy"] as? String ?: "",
                anchorData = data["anchorData"] as? String ?: "",
                modelId = data["modelId"] as? String ?: "",
                metadata = metadata
            )
        }
    }
    
    /**
     * Converts the Anchor to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["blueprintId"] = blueprintId
        result["name"] = name
        result["position"] = mapOf(
            "x" to position.x,
            "y" to position.y,
            "z" to position.z
        )
        result["rotation"] = mapOf(
            "x" to rotation.x,
            "y" to rotation.y,
            "z" to rotation.z,
            "w" to rotation.w
        )
        result["scale"] = mapOf(
            "x" to scale.x,
            "y" to scale.y,
            "z" to scale.z
        )
        
        createdAt?.let { result["createdAt"] = it }
        updatedAt?.let { result["updatedAt"] = it }
        
        result["createdBy"] = createdBy
        result["anchorData"] = anchorData
        
        if (modelId.isNotEmpty()) {
            result["modelId"] = modelId
        }
        
        if (metadata.isNotEmpty()) {
            result["metadata"] = metadata
        }
        
        return result
    }
}

/**
 * Data class representing a quaternion for 3D rotation
 */
data class Quaternion(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val w: Double = 1.0
)
