package com.example.blueprintvision.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Data class representing a 3D coordinate
 */
data class Coordinate3D(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0
) {
    constructor(dictionary: Map<String, Double>?) : this(
        x = dictionary?.get("x") ?: 0.0,
        y = dictionary?.get("y") ?: 0.0,
        z = dictionary?.get("z") ?: 0.0
    )
}

/**
 * Data class representing a marked area in a blueprint
 */
data class MarkedArea(
    var id: String = "",
    var name: String = "",
    var color: String = "",
    var min: Coordinate3D = Coordinate3D(),
    var max: Coordinate3D = Coordinate3D()
) {
    constructor(dictionary: Map<String, Any>?) : this(
        id = dictionary?.get("id") as? String ?: "",
        name = dictionary?.get("name") as? String ?: "",
        color = dictionary?.get("color") as? String ?: "",
        min = Coordinate3D(dictionary?.get("min") as? Map<String, Double>),
        max = Coordinate3D(dictionary?.get("max") as? Map<String, Double>)
    )
}

/**
 * Class representing a Blueprint stored in Firestore
 */
class Blueprint(userFireDoc: Map<String, Any>) {
    companion object {
        const val GEOHASH = "geohash"
        const val CREATOR = "host"
    }

    var latitude: Double? = null
    var longitude: Double? = null
    var altitude: Double? = null
    var id: String = ""
    var host: String = ""
    var name: String = ""
    var size: Double = 0.0
    var numSessions: Int = 0
    var password: String = ""
    var category: String = ""
    var storage: Double = 0.0
    var thumbnail: String? = null
    var isPrivate: Boolean? = null
    var createdDate: Date = Date()
    var connectedTime: Double = 0.0
    var photoLimit: Double = 0.0
    var noteLimit: Double = 0.0
    var objectLimit: Double = 0.0
    var widgetLimit: Double = 0.0
    var portalLimit: Double = 0.0
    var websiteLimit: Double = 0.0
    var fileLimit: Double = 0.0
    var userCount: Int = 0
    var photoCount: Double = 0.0
    var noteCount: Double = 0.0
    var widgetCount: Double = 0.0
    var objectCount: Double = 0.0
    var portalCount: Double = 0.0
    var fileCount: Double = 0.0
    var websiteCount: Double = 0.0
    var lastSessionDate: Date = Date()
    
    var roomIDs: List<String> = emptyList()
    var anchorIDs: List<String> = emptyList()
    var websiteURLs: List<String> = emptyList()
    var objectIDs: List<String> = emptyList()
    var portalIDs: List<String> = emptyList()
    var noteIDs: List<String> = emptyList()
    var photoIDs: List<String> = emptyList()
    var fileIDs: List<String> = emptyList()
    var widgetIDs: List<String> = emptyList()
    var users: List<String> = emptyList()
    var markedAreas: List<MarkedArea>? = null
    
    init {
        // id
        (userFireDoc["id"] as? String)?.let {
            this.id = it
        }
        
        // name
        (userFireDoc["name"] as? String)?.let {
            this.name = it
        }
        
        (userFireDoc["password"] as? String)?.let {
            this.password = it
        }
        
        (userFireDoc["category"] as? String)?.let {
            this.category = it
        }
        
        // host
        (userFireDoc["host"] as? String)?.let {
            this.host = it
        }
        
        (userFireDoc["portalLimit"] as? Double)?.let {
            this.portalLimit = it
        }
        
        (userFireDoc["objectLimit"] as? Double)?.let {
            this.objectLimit = it
        }
        
        (userFireDoc["fileLimit"] as? Double)?.let {
            this.fileLimit = it
        }
        
        (userFireDoc["photoLimit"] as? Double)?.let {
            this.photoLimit = it
        }
        
        (userFireDoc["noteLimit"] as? Double)?.let {
            this.noteLimit = it
        }
        
        (userFireDoc["widgetLimit"] as? Double)?.let {
            this.widgetLimit = it
        }
        
        (userFireDoc["websiteLimit"] as? Double)?.let {
            this.websiteLimit = it
        }
        
        (userFireDoc["portalCount"] as? Double)?.let {
            this.portalCount = it
        }
        
        (userFireDoc["fileCount"] as? Double)?.let {
            this.fileCount = it
        }
        
        (userFireDoc["websiteCount"] as? Double)?.let {
            this.websiteCount = it
        }
        
        (userFireDoc["widgetCount"] as? Double)?.let {
            this.widgetCount = it
        }
        
        (userFireDoc["objectCount"] as? Double)?.let {
            this.objectCount = it
        }
        
        (userFireDoc["photoCount"] as? Double)?.let {
            this.photoCount = it
        }
        
        (userFireDoc["noteCount"] as? Double)?.let {
            this.noteCount = it
        }
        
        (userFireDoc["storage"] as? Double)?.let {
            this.storage = it
        }
        
        (userFireDoc["createdDate"] as? Timestamp)?.let {
            this.createdDate = it.toDate()
        }
        
        (userFireDoc["lastSessionDate"] as? Timestamp)?.let {
            this.lastSessionDate = it.toDate()
        }
        
        // size
        (userFireDoc["size"] as? Double)?.let {
            this.size = it
        }
        
        (userFireDoc["isPrivate"] as? Boolean)?.let {
            this.isPrivate = it
        }
        
        (userFireDoc["thumbnail"] as? String)?.let {
            this.thumbnail = it
        }
        
        (userFireDoc["latitude"] as? Double)?.let {
            this.latitude = it
        }
        
        (userFireDoc["longitude"] as? Double)?.let {
            this.longitude = it
        }
        
        (userFireDoc["altitude"] as? Double)?.let {
            this.altitude = it
        }
        
        (userFireDoc["connectedTime"] as? Double)?.let {
            this.connectedTime = it
        }
        
        (userFireDoc["numSessions"] as? Int)?.let {
            this.numSessions = it
        }
        
        (userFireDoc["userCount"] as? Int)?.let {
            this.userCount = it
        }
        
        (userFireDoc["users"] as? List<String>)?.let {
            this.users = it
        }
        
        (userFireDoc["anchorIDs"] as? List<String>)?.let {
            this.anchorIDs = it
        }
        
        (userFireDoc["roomIDs"] as? List<String>)?.let {
            this.roomIDs = it
        }
        
        (userFireDoc["objectIDs"] as? List<String>)?.let {
            this.objectIDs = it
        }
        
        (userFireDoc["portalIDs"] as? List<String>)?.let {
            this.portalIDs = it
        }
        
        (userFireDoc["photoIDs"] as? List<String>)?.let {
            this.photoIDs = it
        }
        
        (userFireDoc["noteIDs"] as? List<String>)?.let {
            this.noteIDs = it
        }
        
        (userFireDoc["fileIDs"] as? List<String>)?.let {
            this.fileIDs = it
        }
        
        (userFireDoc["widgetIDs"] as? List<String>)?.let {
            this.widgetIDs = it
        }
        
        (userFireDoc["websiteURLs"] as? List<String>)?.let {
            this.websiteURLs = it
        }
        
        // Marked areas
        (userFireDoc["markedAreas"] as? List<Map<String, Any>>)?.let { areasData ->
            this.markedAreas = areasData.mapNotNull { MarkedArea(it) }
        }
    }
    
    /**
     * Creates a Blueprint from a Firestore document
     */
    companion object {
        fun fromFirestore(document: DocumentSnapshot): Blueprint {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return Blueprint(dataWithId)
        }
    }
    
    /**
     * Converts the Blueprint to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["id"] = id
        result["name"] = name
        result["host"] = host
        result["size"] = size
        result["numSessions"] = numSessions
        result["password"] = password
        result["category"] = category
        result["storage"] = storage
        thumbnail?.let { result["thumbnail"] = it }
        isPrivate?.let { result["isPrivate"] = it }
        result["createdDate"] = Timestamp(createdDate)
        result["connectedTime"] = connectedTime
        result["photoLimit"] = photoLimit
        result["noteLimit"] = noteLimit
        result["objectLimit"] = objectLimit
        result["widgetLimit"] = widgetLimit
        result["portalLimit"] = portalLimit
        result["websiteLimit"] = websiteLimit
        result["fileLimit"] = fileLimit
        result["userCount"] = userCount
        result["photoCount"] = photoCount
        result["noteCount"] = noteCount
        result["widgetCount"] = widgetCount
        result["objectCount"] = objectCount
        result["portalCount"] = portalCount
        result["fileCount"] = fileCount
        result["websiteCount"] = websiteCount
        result["lastSessionDate"] = Timestamp(lastSessionDate)
        
        result["roomIDs"] = roomIDs
        result["anchorIDs"] = anchorIDs
        result["websiteURLs"] = websiteURLs
        result["objectIDs"] = objectIDs
        result["portalIDs"] = portalIDs
        result["noteIDs"] = noteIDs
        result["photoIDs"] = photoIDs
        result["fileIDs"] = fileIDs
        result["widgetIDs"] = widgetIDs
        result["users"] = users
        
        latitude?.let { result["latitude"] = it }
        longitude?.let { result["longitude"] = it }
        altitude?.let { result["altitude"] = it }
        
        markedAreas?.let { areas ->
            if (areas.isNotEmpty()) {
                val markedAreasList = areas.map { area ->
                    mapOf(
                        "id" to area.id,
                        "name" to area.name,
                        "color" to area.color,
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
                result["markedAreas"] = markedAreasList
            }
        }
        
        return result
    }
}
