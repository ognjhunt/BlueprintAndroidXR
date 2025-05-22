package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a Portal stored in Firestore
 */
class Portal(userFireDoc: Map<String, Any>) {
    var id: String = ""
    var creatorId: String = ""
    var name: String = ""
    var creatorName: String = ""
    var description: String = ""
    var type: String = ""
    var category: String = ""
    var url: String = ""
    var size: Double = 10.0
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0
    var rotation: Double = 0.0
    var backgroundRotation: Double = 0.0
    var scale: Double = 0.0
    var thumbnail: String? = null
    var backgroundScale: Double = 0.0
    var backgroundThumbnail: String? = null
    var modelStoragePath: String? = null
    var videoStoragePath: String? = null
    var proVideoStoragePath: String? = null
    var apiVideoID: String? = null
    var apiProVideoID: String? = null
    var audioStoragePath: String? = null
    var downloads: Int = 0
    var price: Double? = null
    var date: Date = Date()
    
    companion object {
        const val CREATOR = "creatorId"
        const val DATE = "date"
        const val NAME = "name"
        const val MODELNAME = "modelName"
        
        /**
         * Creates a Portal from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Portal {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return Portal(dataWithId)
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
        
        (userFireDoc["thumbnail"] as? String)?.let {
            this.thumbnail = it
        }
        
        (userFireDoc["backgroundThumbnail"] as? String)?.let {
            this.backgroundThumbnail = it
        }
        
        (userFireDoc["modelStoragePath"] as? String)?.let {
            this.modelStoragePath = it
        }
        
        (userFireDoc["videoStoragePath"] as? String)?.let {
            this.videoStoragePath = it
        }
        
        (userFireDoc["proVideoStoragePath"] as? String)?.let {
            this.proVideoStoragePath = it
        }
        
        (userFireDoc["apiVideoID"] as? String)?.let {
            this.apiVideoID = it
        }
        
        (userFireDoc["apiProVideoID"] as? String)?.let {
            this.apiProVideoID = it
        }
        
        (userFireDoc["audioStoragePath"] as? String)?.let {
            this.audioStoragePath = it
        }
        
        (userFireDoc["date"] as? Date)?.let {
            this.date = it
        }
        
        (userFireDoc["url"] as? String)?.let {
            this.url = it
        }
        
        // creatorId
        (userFireDoc["creatorId"] as? String)?.let {
            this.creatorId = it
        }
        
        (userFireDoc["description"] as? String)?.let {
            this.description = it
        }
        
        (userFireDoc["category"] as? String)?.let {
            this.category = it
        }
        
        (userFireDoc["size"] as? Double)?.let {
            this.size = it
        }
        
        (userFireDoc["x"] as? Double)?.let {
            this.x = it
        }
        
        (userFireDoc["y"] as? Double)?.let {
            this.y = it
        }
        
        (userFireDoc["z"] as? Double)?.let {
            this.z = it
        }
        
        (userFireDoc["rotation"] as? Double)?.let {
            this.rotation = it
        }
        
        (userFireDoc["backgroundRotation"] as? Double)?.let {
            this.backgroundRotation = it
        }
        
        (userFireDoc["scale"] as? Double)?.let {
            this.scale = it
        }
        
        (userFireDoc["backgroundScale"] as? Double)?.let {
            this.backgroundScale = it
        }
        
        (userFireDoc["type"] as? String)?.let {
            this.type = it
        }
        
        (userFireDoc["price"] as? Double)?.let {
            this.price = it
        }
        
        (userFireDoc["downloads"] as? Int)?.let {
            this.downloads = it
        }
    }
    
    /**
     * Converts the Portal to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["id"] = id
        result["creatorId"] = creatorId
        result["name"] = name
        result["creatorName"] = creatorName
        result["description"] = description
        result["type"] = type
        result["category"] = category
        result["url"] = url
        result["size"] = size
        result["x"] = x
        result["y"] = y
        result["z"] = z
        result["rotation"] = rotation
        result["backgroundRotation"] = backgroundRotation
        result["scale"] = scale
        result["backgroundScale"] = backgroundScale
        result["downloads"] = downloads
        result["date"] = date
        
        thumbnail?.let { result["thumbnail"] = it }
        backgroundThumbnail?.let { result["backgroundThumbnail"] = it }
        modelStoragePath?.let { result["modelStoragePath"] = it }
        videoStoragePath?.let { result["videoStoragePath"] = it }
        proVideoStoragePath?.let { result["proVideoStoragePath"] = it }
        apiVideoID?.let { result["apiVideoID"] = it }
        apiProVideoID?.let { result["apiProVideoID"] = it }
        audioStoragePath?.let { result["audioStoragePath"] = it }
        price?.let { result["price"] = it }
        
        return result
    }
}
