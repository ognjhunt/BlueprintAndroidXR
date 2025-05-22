package com.example.blueprintvision.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a Note stored in Firestore
 */
class Note(userFireDoc: Map<String, Any>) {
    var id: String = ""
    var host: String = ""
    var message: String = ""
    var date: Date = Date()
    var blueprintId: String = ""
    
    companion object {
        const val CREATOR = "host"
        const val DATE = "date"
        
        /**
         * Creates a Note from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Note {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return Note(dataWithId)
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
        
        (userFireDoc["date"] as? Timestamp)?.let {
            this.date = it.toDate()
        }
        
        // message
        (userFireDoc["message"] as? String)?.let {
            this.message = it
        }
        
        (userFireDoc["blueprintId"] as? String)?.let {
            this.blueprintId = it
        }
    }
    
    /**
     * Converts the Note to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["id"] = id
        result["host"] = host
        result["message"] = message
        result["blueprintId"] = blueprintId
        result["date"] = Timestamp(date)
        
        return result
    }
}
