package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Data class representing a 3D Model stored in Firestore
 */
data class Model(
    val id: String = "",
    val name: String = "",
    val modelName: String = "",
    val description: String = "",
    val creator: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val modelUrl: String = "",
    val thumbnail: String = "",
    val category: String = "",
    val tags: List<String> = emptyList()
) {
    companion object {
        const val CREATOR = "creator"
        
        /**
         * Creates a Model from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Model {
            val data = document.data ?: return Model(id = document.id)
            
            return Model(
                id = document.id,
                name = data["name"] as? String ?: "",
                modelName = data["modelName"] as? String ?: "",
                description = data["description"] as? String ?: "",
                creator = data["creator"] as? String ?: "",
                createdAt = data["createdAt"] as? Date,
                updatedAt = data["updatedAt"] as? Date,
                modelUrl = data["modelUrl"] as? String ?: "",
                thumbnail = data["thumbnail"] as? String ?: "",
                category = data["category"] as? String ?: "",
                tags = (data["tags"] as? List<String>) ?: emptyList()
            )
        }
    }
    
    /**
     * Converts the Model to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["name"] = name
        result["modelName"] = modelName
        result["description"] = description
        result["creator"] = creator
        
        createdAt?.let { result["createdAt"] = it }
        updatedAt?.let { result["updatedAt"] = it }
        
        if (modelUrl.isNotEmpty()) {
            result["modelUrl"] = modelUrl
        }
        
        if (thumbnail.isNotEmpty()) {
            result["thumbnail"] = thumbnail
        }
        
        if (category.isNotEmpty()) {
            result["category"] = category
        }
        
        if (tags.isNotEmpty()) {
            result["tags"] = tags
        }
        
        return result
    }
}
