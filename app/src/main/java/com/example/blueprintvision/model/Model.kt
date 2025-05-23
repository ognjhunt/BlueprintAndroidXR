package com.example.blueprintvision.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a 3D Model stored in Firestore
 */
class Model(userFireDoc: Map<String, Any>) {
    companion object {
        const val CREATOR = "creatorId"
        const val DATE = "date"
        const val NAME = "name"
        const val MODELNAME = "modelName"
        
        /**
         * Creates a Model from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Model {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return Model(dataWithId)
        }
    }
    
    var id: String = ""
    var creatorId: String = ""
    var name: String = ""
    var creatorName: String = ""
    var description: String = ""
    var category: String = ""
    var scale: Double = 1.0
    var size: Double = 10.0
    var height: Double = 1.0
    var width: Double = 1.0
    var depth: Double = 1.0
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0
    var rotation: Double = 0.0
    var rotationAxis: String = ""
    var thumbnail: String? = null
    var modelName: String = ""
    var isPublic: Boolean? = null
    var price: Int? = null
    var url: String = ""
    var prompt: String = ""
    var accessibility: String = ""
    var date: Date = Date()
    var downloads: Int = 0
    
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
        
        (userFireDoc["prompt"] as? String)?.let {
            this.prompt = it
        }
        
        (userFireDoc["url"] as? String)?.let {
            this.url = it
        }
        
        (userFireDoc["accessibility"] as? String)?.let {
            this.accessibility = it
        }
        
        (userFireDoc["date"] as? Date)?.let {
            this.date = it
        }
        
        (userFireDoc["creatorId"] as? String)?.let {
            this.creatorId = it
        }
        
        (userFireDoc["description"] as? String)?.let {
            this.description = it
        }
        
        (userFireDoc["category"] as? String)?.let {
            this.category = it
        }
        
        (userFireDoc["rotationAxis"] as? String)?.let {
            this.rotationAxis = it
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
        
        (userFireDoc["depth"] as? Double)?.let {
            this.depth = it
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
        
        (userFireDoc["price"] as? Int)?.let {
            this.price = it
        }
    }
    
    /**
     * Converts the Model to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["id"] = id
        result["name"] = name
        result["creatorName"] = creatorName
        result["creatorId"] = creatorId
        result["description"] = description
        result["category"] = category
        result["scale"] = scale
        result["size"] = size
        result["height"] = height
        result["width"] = width
        result["depth"] = depth
        result["x"] = x
        result["y"] = y
        result["z"] = z
        result["rotation"] = rotation
        result["rotationAxis"] = rotationAxis
        result["modelName"] = modelName
        result["downloads"] = downloads
        result["date"] = date
        
        if (prompt.isNotEmpty()) {
            result["prompt"] = prompt
        }
        
        if (url.isNotEmpty()) {
            result["url"] = url
        }
        
        if (accessibility.isNotEmpty()) {
            result["accessibility"] = accessibility
        }
        
        thumbnail?.let { result["thumbnail"] = it }
        isPublic?.let { result["isPublic"] = it }
        price?.let { result["price"] = it }
        
        return result
    }
}
