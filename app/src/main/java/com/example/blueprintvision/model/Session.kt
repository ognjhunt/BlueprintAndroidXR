package com.example.blueprintvision.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a Session stored in Firestore
 */
class Session(userFireDoc: Map<String, Any>) {
    var id: String = ""
    var userId: String = ""
    var startTime: Date = Date()
    var endTime: Date = Date()
    var duration: Long = 0
    
    // New properties for enhanced context awareness
    var connectedBlueprintId: String? = null
    var roomsVisited: Map<String, Double> = emptyMap() // Room ID : Time spent
    var startLocation: Pair<Double, Double>? = null
    var lastKnownLocation: Pair<Double, Double>? = null
    var deviceType: String? = null
    var appVersion: String? = null
    
    // User behavior tracking
    var actionsPerformed: Map<String, Int> = emptyMap() // Action type : Count
    var contentInteractions: Map<String, Int> = emptyMap() // Content ID : Interaction count
    
    // Time-based data
    var timeOfDay: Int? = null // Hour of the day (0-23)
    var dayOfWeek: Int? = null // Day of the week (1-7)
    
    // Environmental data
    var ambientLightLevel: Float? = null
    var noiseLevel: Float? = null
    
    // User state
    var stepCount: Int? = null
    var lastMealTime: Date? = null
    var sleepDuration: Double? = null
    
    // External data integration
    var weatherCondition: String? = null
    var calendarEvents: List<String>? = null // Event IDs for the day
    
    companion object {
        /**
         * Creates a Session from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): Session {
            val data = document.data ?: mapOf("id" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("id")) put("id", document.id) 
            }
            return Session(dataWithId)
        }
    }
    
    init {
        // Existing initializations
        (userFireDoc["id"] as? String)?.let {
            this.id = it
        }
        
        (userFireDoc["userId"] as? String)?.let {
            this.userId = it
        }
        
        (userFireDoc["startTime"] as? Timestamp)?.let {
            this.startTime = it.toDate()
        }
        
        (userFireDoc["endTime"] as? Timestamp)?.let {
            this.endTime = it.toDate()
        }
        
        (userFireDoc["duration"] as? Long)?.let {
            this.duration = it
        }
        
        // New initializations
        (userFireDoc["connectedBlueprintId"] as? String)?.let {
            this.connectedBlueprintId = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["roomsVisited"] as? Map<String, Double>)?.let {
            this.roomsVisited = it
        }
        
        val startLocationMap = userFireDoc["startLocation"] as? Map<String, Double>
        if (startLocationMap != null) {
            val latitude = startLocationMap["latitude"]
            val longitude = startLocationMap["longitude"]
            if (latitude != null && longitude != null) {
                this.startLocation = Pair(latitude, longitude)
            }
        }
        
        val locationMap = userFireDoc["lastKnownLocation"] as? Map<String, Double>
        if (locationMap != null) {
            val latitude = locationMap["latitude"]
            val longitude = locationMap["longitude"]
            if (latitude != null && longitude != null) {
                this.lastKnownLocation = Pair(latitude, longitude)
            }
        }
        
        (userFireDoc["deviceType"] as? String)?.let {
            this.deviceType = it
        }
        
        (userFireDoc["appVersion"] as? String)?.let {
            this.appVersion = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["actionsPerformed"] as? Map<String, Int>)?.let {
            this.actionsPerformed = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["contentInteractions"] as? Map<String, Int>)?.let {
            this.contentInteractions = it
        }
        
        (userFireDoc["timeOfDay"] as? Int)?.let {
            this.timeOfDay = it
        }
        
        (userFireDoc["dayOfWeek"] as? Int)?.let {
            this.dayOfWeek = it
        }
        
        (userFireDoc["ambientLightLevel"] as? Float)?.let {
            this.ambientLightLevel = it
        }
        
        (userFireDoc["noiseLevel"] as? Float)?.let {
            this.noiseLevel = it
        }
        
        (userFireDoc["stepCount"] as? Int)?.let {
            this.stepCount = it
        }
        
        (userFireDoc["lastMealTime"] as? Timestamp)?.let {
            this.lastMealTime = it.toDate()
        }
        
        (userFireDoc["sleepDuration"] as? Double)?.let {
            this.sleepDuration = it
        }
        
        (userFireDoc["weatherCondition"] as? String)?.let {
            this.weatherCondition = it
        }
        
        (userFireDoc["calendarEvents"] as? List<String>)?.let {
            this.calendarEvents = it
        }
    }
    
    /**
     * Converts the Session to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["id"] = id
        result["userId"] = userId
        result["startTime"] = Timestamp(startTime)
        result["endTime"] = Timestamp(endTime)
        result["duration"] = duration
        
        // New properties
        connectedBlueprintId?.let { result["connectedBlueprintId"] = it }
        
        if (roomsVisited.isNotEmpty()) {
            result["roomsVisited"] = roomsVisited
        }
        
        startLocation?.let { (latitude, longitude) ->
            result["startLocation"] = mapOf(
                "latitude" to latitude,
                "longitude" to longitude
            )
        }
        
        lastKnownLocation?.let { (latitude, longitude) ->
            result["lastKnownLocation"] = mapOf(
                "latitude" to latitude,
                "longitude" to longitude
            )
        }
        
        deviceType?.let { result["deviceType"] = it }
        appVersion?.let { result["appVersion"] = it }
        
        if (actionsPerformed.isNotEmpty()) {
            result["actionsPerformed"] = actionsPerformed
        }
        
        if (contentInteractions.isNotEmpty()) {
            result["contentInteractions"] = contentInteractions
        }
        
        timeOfDay?.let { result["timeOfDay"] = it }
        dayOfWeek?.let { result["dayOfWeek"] = it }
        
        ambientLightLevel?.let { result["ambientLightLevel"] = it }
        noiseLevel?.let { result["noiseLevel"] = it }
        
        stepCount?.let { result["stepCount"] = it }
        lastMealTime?.let { result["lastMealTime"] = Timestamp(it) }
        sleepDuration?.let { result["sleepDuration"] = it }
        
        weatherCondition?.let { result["weatherCondition"] = it }
        calendarEvents?.let { result["calendarEvents"] = it }
        
        return result
    }
}
