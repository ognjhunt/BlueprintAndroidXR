package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Data class representing a Blueprint stored in Firestore
 */
data class Blueprint(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creator: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val isPrivate: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val anchorIDs: List<String> = emptyList(),
    val markedAreas: List<MarkedArea> = emptyList()
) {
    companion object {
        const val CREATOR = "creator"
        
        /**
         * Creates a Blueprint from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Blueprint {
            val data = document.data ?: return Blueprint(id = document.id)
            
            val markedAreasList = mutableListOf<MarkedArea>()
            (data["markedAreas"] as? List<Map<String, Any>>)?.let { areas ->
                for (areaMap in areas) {
                    val areaId = areaMap["id"] as? String ?: ""
                    val areaName = areaMap["name"] as? String ?: ""
                    
                    val minMap = areaMap["min"] as? Map<String, Any>
                    val maxMap = areaMap["max"] as? Map<String, Any>
                    
                    val min = if (minMap != null) {
                        Vector3(
                            x = (minMap["x"] as? Number)?.toDouble() ?: 0.0,
                            y = (minMap["y"] as? Number)?.toDouble() ?: 0.0,
                            z = (minMap["z"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else {
                        Vector3(0.0, 0.0, 0.0)
                    }
                    
                    val max = if (maxMap != null) {
                        Vector3(
                            x = (maxMap["x"] as? Number)?.toDouble() ?: 0.0,
                            y = (maxMap["y"] as? Number)?.toDouble() ?: 0.0,
                            z = (maxMap["z"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else {
                        Vector3(0.0, 0.0, 0.0)
                    }
                    
                    markedAreasList.add(MarkedArea(id = areaId, name = areaName, min = min, max = max))
                }
            }
            
            return Blueprint(
                id = document.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                creator = data["creator"] as? String ?: "",
                createdAt = data["createdAt"] as? Date,
                updatedAt = data["updatedAt"] as? Date,
                isPrivate = data["isPrivate"] as? Boolean ?: false,
                latitude = (data["latitude"] as? Number)?.toDouble(),
                longitude = (data["longitude"] as? Number)?.toDouble(),
                altitude = (data["altitude"] as? Number)?.toDouble(),
                anchorIDs = (data["anchorIDs"] as? List<String>) ?: emptyList(),
                markedAreas = markedAreasList
            )
        }
    }
    
    /**
     * Converts the Blueprint to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["name"] = name
        result["description"] = description
        result["creator"] = creator
        result["isPrivate"] = isPrivate
        result["anchorIDs"] = anchorIDs
        
        createdAt?.let { result["createdAt"] = it }
        updatedAt?.let { result["updatedAt"] = it }
        latitude?.let { result["latitude"] = it }
        longitude?.let { result["longitude"] = it }
        altitude?.let { result["altitude"] = it }
        
        // Convert markedAreas to maps
        val markedAreasList = markedAreas.map { area ->
            mapOf(
                "id" to area.id,
                "name" to area.name,
                "min" to mapOf(
                    "x" to area.min.x,
                    "y" to area.min.y,
                    "z" to area.min.z
                ),
                "max" to mapOf(
                    "x" to area.max.x,
                    "y" to area.max.y,
                    "z" to area.max.z
                )
            )
        }
        
        if (markedAreasList.isNotEmpty()) {
            result["markedAreas"] = markedAreasList
        }
        
        return result
    }
}

/**
 * Data class representing a 3D vector
 */
data class Vector3(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
)

/**
 * Data class representing a marked area in a blueprint
 */
data class MarkedArea(
    val id: String = "",
    val name: String = "",
    val min: Vector3 = Vector3(),
    val max: Vector3 = Vector3()
)
