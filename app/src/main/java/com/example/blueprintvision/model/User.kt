package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Data class representing a User stored in Firestore
 */
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePicUrl: String = "",
    val createdBlueprintIDs: List<String> = emptyList(),
    val collectedContentIDs: List<String> = emptyList(),
    val fcmToken: String = ""
) {
    companion object {
        /**
         * Creates a User from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): User {
            val data = document.data ?: return User(id = document.id)
            
            return User(
                id = document.id,
                username = data["username"] as? String ?: "",
                email = data["email"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                profilePicUrl = data["profilePicUrl"] as? String ?: "",
                createdBlueprintIDs = (data["createdBlueprintIDs"] as? List<String>) ?: emptyList(),
                collectedContentIDs = (data["collectedContentIDs"] as? List<String>) ?: emptyList(),
                fcmToken = data["fcmToken"] as? String ?: ""
            )
        }
    }
    
    /**
     * Converts the User to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["username"] = username
        result["email"] = email
        result["displayName"] = displayName
        
        if (profilePicUrl.isNotEmpty()) {
            result["profilePicUrl"] = profilePicUrl
        }
        
        if (createdBlueprintIDs.isNotEmpty()) {
            result["createdBlueprintIDs"] = createdBlueprintIDs
        }
        
        if (collectedContentIDs.isNotEmpty()) {
            result["collectedContentIDs"] = collectedContentIDs
        }
        
        if (fcmToken.isNotEmpty()) {
            result["fcmToken"] = fcmToken
        }
        
        return result
    }
}
