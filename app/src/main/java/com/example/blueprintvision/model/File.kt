package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a File stored in Firestore
 */
class File(userFireDoc: Map<String, Any>) {
    var id: String = ""
    var host: String = ""
    var name: String = ""
    var creatorName: String = ""
    var description: String = ""
    var fileType: String = ""
    var fileURL: String = ""
    var category: String = ""
    var scale: Double = 1.0
    var size: Double = 10.0
    var height: Double = 1.0
    var width: Double = 1.0
    var thumbnail: String? = null
    var modelName: String = ""
    var isPublic: Boolean? = null
    var url: String = ""
    var accessibility: String = ""
    var storagePath: String = ""
    var date: Date = Date()
    var blueprintId: String = ""
    var downloads: Int = 0
    
    companion object {
        const val CREATOR = "creatorId"
        const val DATE = "date"
        const val NAME = "name"
        const val MODELNAME = "modelName"
        
        /**
         * Creates a File from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): File {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return File(dataWithId)
        }
    }
    
    init {
        // id
        (userFireDoc["id"] as? String)?.let {
            this.id = it
        }
        
        // name
        (userFireDoc["name"] as? String)?.let {
            this.name = it
        }
        
        (userFireDoc["creatorName"] as? String)?.let {
            this.creatorName = it
        }
        
        (userFireDoc["host"] as? String)?.let {
            this.host = it
        }
        
        (userFireDoc["fileURL"] as? String)?.let {
            this.fileURL = it
        }
        
        (userFireDoc["url"] as? String)?.let {
            this.url = it
        }
        
        (userFireDoc["storagePath"] as? String)?.let {
            this.storagePath = it
        }
        
        (userFireDoc["blueprintId"] as? String)?.let {
            this.blueprintId = it
        }
        
        (userFireDoc["accessibility"] as? String)?.let {
            this.accessibility = it
        }
        
        (userFireDoc["date"] as? Date)?.let {
            this.date = it
        }
        
        (userFireDoc["description"] as? String)?.let {
            this.description = it
        }
        
        (userFireDoc["category"] as? String)?.let {
            this.category = it
        }
        
        (userFireDoc["fileType"] as? String)?.let {
            this.fileType = it
        }
        
        (userFireDoc["scale"] as? Double)?.let {
            this.scale = it
        }
        
        (userFireDoc["size"] as? Double)?.let {
            this.size = it
        }
        
        (userFireDoc["height"] as? Double)?.let {
            this.height = it
        }
        
        (userFireDoc["width"] as? Double)?.let {
            this.width = it
        }
        
        (userFireDoc["downloads"] as? Int)?.let {
            this.downloads = it
        }
        
        (userFireDoc["thumbnail"] as? String)?.let {
            this.thumbnail = it
        }
        
        (userFireDoc["modelName"] as? String)?.let {
            this.modelName = it
        }
        
        (userFireDoc["isPublic"] as? Boolean)?.let {
            this.isPublic = it
        }
    }
    
    /**
     * Converts the File to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["id"] = id
        result["host"] = host
        result["name"] = name
        result["creatorName"] = creatorName
        result["description"] = description
        result["fileType"] = fileType
        result["fileURL"] = fileURL
        result["category"] = category
        result["scale"] = scale
        result["size"] = size
        result["height"] = height
        result["width"] = width
        result["modelName"] = modelName
        result["storagePath"] = storagePath
        result["blueprintId"] = blueprintId
        result["downloads"] = downloads
        result["date"] = date
        
        if (url.isNotEmpty()) {
            result["url"] = url
        }
        
        if (accessibility.isNotEmpty()) {
            result["accessibility"] = accessibility
        }
        
        thumbnail?.let { result["thumbnail"] = it }
        isPublic?.let { result["isPublic"] = it }
        
        return result
    }
}
