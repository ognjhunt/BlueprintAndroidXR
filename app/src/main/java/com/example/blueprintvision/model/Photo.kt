package com.example.blueprintvision.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a Photo stored in Firestore
 */
class Photo(userFireDoc: Map<String, Any>) {
    var id: String = ""
    var host: String = ""
    var height: Double = 0.0
    var width: Double = 0.0
    var size: Double = 0.0
    var storagePath: String = ""
    var date: Date = Date()
    var blueprintId: String = ""
    
    companion object {
        const val CREATOR = "host"
        const val DATE = "date"
        
        /**
         * Creates a Photo from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Photo {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return Photo(dataWithId)
        }
    }
    
    init {
        // id
        (userFireDoc["id"] as? String)?.let {
            this.id = it
        }
        
        // host
        (userFireDoc["host"] as? String)?.let {
            this.host = it
        }
        
        (userFireDoc["storagePath"] as? String)?.let {
            this.storagePath = it
        }
        
        (userFireDoc["blueprintId"] as? String)?.let {
            this.blueprintId = it
        }
        
        (userFireDoc["height"] as? Double)?.let {
            this.height = it
        }
        
        // width
        (userFireDoc["width"] as? Double)?.let {
            this.width = it
        }
        
        (userFireDoc["size"] as? Double)?.let {
            this.size = it
        }
        
        (userFireDoc["date"] as? Timestamp)?.let {
            this.date = it.toDate()
        }
    }
    
    /**
     * Converts the Photo to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["id"] = id
        result["host"] = host
        result["height"] = height
        result["width"] = width
        result["size"] = size
        result["storagePath"] = storagePath
        result["blueprintId"] = blueprintId
        result["date"] = Timestamp(date)
        
        return result
    }
}
