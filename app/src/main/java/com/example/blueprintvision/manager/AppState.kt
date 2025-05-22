package com.example.blueprintvision.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blueprintvision.model.ImmersionStyle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mixpanel.android.mpmetrics.MixpanelAPI
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

/**
 * AppState manages the active state of the application and tracks user session time.
 */
class AppState private constructor() {
    companion object {
        private const val TAG = "AppState"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: AppState? = null
        
        fun getInstance(): AppState {
            return INSTANCE ?: synchronized(this) {
                val instance = AppState()
                INSTANCE = instance
                instance
            }
        }
    }
    
    var currentStyle: ImmersionStyle = ImmersionStyle.FULL
    
    // Flag to indicate whether the app is currently active
    private val _appIsActive = MutableLiveData<Boolean>(true)
    val appIsActive: LiveData<Boolean> = _appIsActive
    
    // Setter for app active state
    var isActive: Boolean
        get() = _appIsActive.value ?: true
        set(value) {
            _appIsActive.postValue(value)
        }
    
    // Stores the start time of the user's session
    private val _sessionStartTime = MutableLiveData<Date?>()
    val sessionStartTime: LiveData<Date?> = _sessionStartTime
    
    // Separate timers for different purposes
    private var updateTimer: Timer? = null
    private var subscriptionCheckTimer: Timer? = null
    
    val timerTracker = ConnectedTimerTracker.getInstance()
    
    private val _isSessionEnded = MutableLiveData<Boolean>(false)
    val isSessionEnded: LiveData<Boolean> = _isSessionEnded
    
    // Property to store the last update time
    private var lastUpdateTime: Date? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Starts a new user session by initializing variables and setting up Firestore documents and timers.
     */
    fun startSession() {
        _isSessionEnded.postValue(false)
        val currentDate = Date()
        _sessionStartTime.postValue(currentDate)
        lastUpdateTime = currentDate
        
        AudioSessionManager.getInstance().configureAudioSession(null)
        startPeriodicUpdateTimer()
        startPeriodicSubscriptionCheck()
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: 
                     getSharedPrefsString("temporaryUserID")
        
        userId?.let {
            // Identify the user with the same ID used in RevenueCat
            val mixpanel = MixpanelAPI.getInstance(null, null) // Context is not needed here as instance is already created
            com.revenuecat.purchases.Purchases.sharedInstance.attribution.setMixpanelDistinctID(mixpanel.distinctId)
            
            createSessionDocument(it, currentDate)
            
            // Update user document in Firestore
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(it)
            
            userDocRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null && task.result!!.exists()) {
                    userDocRef.update(
                        mapOf(
                            "sessionDates" to FieldValue.arrayUnion(currentDate),
                            "lastSessionDate" to currentDate,
                            "numSessions" to FieldValue.increment(1)
                        )
                    ).addOnSuccessListener {
                        Log.d(TAG, "User data updated successfully!")
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error updating user data: ${e.message}")
                        trackErrorInMixpanel("error_updating_user_in_firestore_from_start_session", userId)
                    }
                }
            }
            
            // Update blueprint document if selected
            val selectedBlueprintID = getSharedPrefsString("SelectedBlueprintID")
            if (!selectedBlueprintID.isNullOrEmpty()) {
                val blueprintDocRef = db.collection("blueprints").document(selectedBlueprintID)
                blueprintDocRef.get().addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null && task.result!!.exists()) {
                        blueprintDocRef.update(
                            mapOf(
                                "lastSessionDate" to currentDate,
                                "numSessions" to FieldValue.increment(1)
                            )
                        ).addOnSuccessListener {
                            Log.d(TAG, "Blueprint data updated successfully!")
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Error updating BP data: ${e.message}")
                            trackErrorInMixpanel("error_updating_blueprint_last_date_from_start_session", userId)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Ends the current session, updating Firestore and Mixpanel with elapsed time, and stops all timers.
     */
    fun endSession(completion: (() -> Unit)? = null) {
        // Check if the session is already ended
        if (_isSessionEnded.value == true) {
            completion?.invoke()
            return
        }
        
        // Mark the session as ended
        _isSessionEnded.postValue(true)
        
        val elapsedTime = calculateElapsedTimeSinceLastUpdate()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: 
                     getSharedPrefsString("temporaryUserID")
        
        userId?.let {
            updateFirestoreWithElapsedTime(elapsedTime, it)
            updateMixpanelSessionDurationWithElapsedTime(elapsedTime, it)
            
            val sessionStartTime = _sessionStartTime.value
            if (sessionStartTime != null) {
                val sessionDuration = (Date().time - sessionStartTime.time) / 1000.0
                updateMixpanelFirstSessionDuration(sessionDuration, it)
                
                val mixpanel = MixpanelAPI.getInstance(null, null)
                val properties = JSONObject().apply {
                    put("session_id", getSharedPrefsString("SessionID") ?: "")
                    put("sign_up_variant", getSharedPrefsString("sign_up_variant") ?: "")
                    put("session_start_time", sessionStartTime.time)
                    put("blueprint_id", getSharedPrefsString("SelectedBlueprintID") ?: "")
                    put("user_id", userId)
                    put("date", Date().time)
                }
                mixpanel.track("end_session", properties)
                
                lastUpdateTime = Date()
                Log.d(TAG, "Tracked session duration: $sessionDuration seconds")
            }
        }
        
        stopAllTimers()
        
        // Track and end session in Mixpanel
        trackAndEndSessionInMixpanel {
            completion?.invoke()
        }
    }
    
    /**
     * Ends the session with a given session ID and handles the Firestore updates.
     */
    fun endAppSession(sessionId: String, completion: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("sessions").document(sessionId)
        
        // Set the end time to the current date
        val currentDate = Date()
        
        // Fetch the session document
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null && task.result!!.exists()) {
                val sessionData = task.result!!.data
                val startTimeTimestamp = sessionData?.get("startTime") as? com.google.firebase.Timestamp
                
                if (startTimeTimestamp != null) {
                    val startTime = startTimeTimestamp.toDate()
                    
                    // Calculate the duration in seconds
                    val duration = (currentDate.time - startTime.time) / 1000.0
                    val roundedDuration = Math.round(duration).toDouble()
                    
                    // Debugging statement to check calculated values
                    Log.d(TAG, "Debug Info: StartTime: $startTime, EndTime: $currentDate, Duration: $roundedDuration")
                    
                    // Create a batch to update multiple fields at once
                    val batch = db.batch()
                    
                    // Update endTime and duration
                    batch.update(docRef, mapOf(
                        "endTime" to currentDate,
                        "duration" to roundedDuration
                    ))
                    
                    // Commit the batch
                    batch.commit().addOnCompleteListener { batchTask ->
                        if (batchTask.isSuccessful) {
                            Log.d(TAG, "Session updated successfully with duration: $roundedDuration")
                            
                            // Remove SessionID from SharedPreferences
                            val appContext = com.example.blueprintvision.BlueprintVisionApp.getInstance()
                            val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                            sharedPrefs.edit().remove("SessionID").apply()
                            
                            completion(true)
                        } else {
                            Log.e(TAG, "Error updating session: ${batchTask.exception?.message}")
                            completion(false)
                        }
                    }
                } else {
                    Log.e(TAG, "Invalid or missing data for startTime")
                    completion(false)
                }
            } else {
                Log.e(TAG, "Error fetching session document: ${task.exception?.message}")
                completion(false)
            }
        }
    }
    
    /**
     * Updates Mixpanel with the last seen date for a given user ID.
     */
    private fun updateMixpanelLastSeen(forUserID: String) {
        val mixpanel = MixpanelAPI.getInstance(null, null)
        mixpanel.people.set("\$last_seen", Date())
        mixpanel.flush()
        
        Log.d(TAG, "Updated last seen for user: $forUserID")
    }
    
    /**
     * Starts a timer that periodically updates Firestore and Mixpanel with the session duration.
     */
    private fun startPeriodicUpdateTimer() {
        updateTimer = Timer().apply {
            scheduleAtFixedRate(0, 300000) { // 300000 ms = 5 minutes
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: 
                             getSharedPrefsString("temporaryUserID")
                
                userId?.let {
                    updateFirestoreWithElapsedTime(300.0, it)
                    updateMixpanelSessionDurationWithElapsedTime(300.0, it)
                }
            }
        }
    }
    
    /**
     * Updates session duration in Mixpanel for a specific user ID with the elapsed time.
     */
    private fun updateMixpanelSessionDurationWithElapsedTime(elapsedTime: Double, forUserID: String) {
        val mixpanel = MixpanelAPI.getInstance(null, null)
        val properties = JSONObject().apply {
            put("duration", elapsedTime)
            put("session_id", getSharedPrefsString("SessionID") ?: "")
            put("sign_up_variant", getSharedPrefsString("sign_up_variant") ?: "")
            put("session_start_time", _sessionStartTime.value?.time)
            put("blueprint_id", getSharedPrefsString("SelectedBlueprintID") ?: "")
            put("user_id", forUserID)
            put("date", Date().time)
        }
        
        mixpanel.track("session_duration", properties)
        mixpanel.flush()
        Log.d(TAG, "Tracked updated session duration: $elapsedTime seconds")
    }
    
    /**
     * Updates Mixpanel with first session duration
     */
    private fun updateMixpanelFirstSessionDuration(elapsedTime: Double, forUserID: String) {
        val appContext = com.example.blueprintvision.BlueprintVisionApp.getInstance()
        val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        
        if (sharedPrefs.getBoolean("isFirstSession", false)) {
            val mixpanel = MixpanelAPI.getInstance(null, null)
            val selectedBlueprintID = getSharedPrefsString("SelectedBlueprintID")
            
            val properties = JSONObject().apply {
                put("total_session_duration", elapsedTime)
                put("blueprint_id", selectedBlueprintID ?: "NO BLUEPRINT ID")
                put("session_id", getSharedPrefsString("SessionID") ?: "")
                put("sign_up_variant", getSharedPrefsString("sign_up_variant") ?: "")
                put("user_id", forUserID)
                put("date", Date().time)
            }
            
            mixpanel.track("initial_session_end", properties)
            mixpanel.flush()
            sharedPrefs.edit().putBoolean("isFirstSession", false).apply()
            
            Log.d(TAG, "Tracked initial session end with duration: $elapsedTime")
        }
    }
    
    /**
     * Updates Firestore with the elapsed time for a specific user ID.
     */
    private fun updateFirestoreWithElapsedTime(elapsedTime: Double, forUserID: String) {
        lastUpdateTime = Date()
        
        if (FirebaseAuth.getInstance().currentUser != null) {
            // For authenticated users, update Firestore
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(forUserID)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val oldTime = snapshot.getDouble("usageTime") ?: 0.0
                    transaction.update(docRef, "usageTime", oldTime + elapsedTime)
                }
                null
            }.addOnSuccessListener {
                Log.d(TAG, "Transaction successfully committed!")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Transaction failed: ${e.message}")
            }
        } else {
            // For temporary users, update SharedPreferences
            val appContext = com.example.blueprintvision.BlueprintVisionApp.getInstance()
            val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            
            val tempUserJson = sharedPrefs.getString("temporaryUser", null)
            if (tempUserJson != null) {
                try {
                    val tempUserData = org.json.JSONObject(tempUserJson)
                    val oldTime = tempUserData.optDouble("usageTime", 0.0)
                    tempUserData.put("usageTime", oldTime + elapsedTime)
                    
                    sharedPrefs.edit().putString("temporaryUser", tempUserData.toString()).apply()
                    Log.d(TAG, "Usage time updated successfully in SharedPreferences for temporary user.")
                    lastUpdateTime = Date()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing temporary user data: ${e.message}")
                }
            } else {
                Log.e(TAG, "Temporary user data not found in SharedPreferences")
            }
        }
    }
    
    /**
     * Tracks and ends the session in Mixpanel with a completion handler.
     */
    fun trackAndEndSessionInMixpanel(completion: (() -> Unit)? = null) {
        val sessionID = getSharedPrefsString("SessionID")
        if (sessionID != null) {
            endAppSession(sessionID) { success ->
                Log.d(TAG, if (success) "Session ended successfully" else "Failed to end the session")
                completion?.invoke()
            }
        } else {
            completion?.invoke()
        }
    }
    
    /**
     * Calculates and returns the elapsed time since the last update.
     */
    fun calculateElapsedTimeSinceLastUpdate(): Double {
        val lastUpdate = lastUpdateTime ?: return 0.0
        return (Date().time - lastUpdate.time) / 1000.0
    }
    
    /**
     * Starts a timer to periodically check the subscription status.
     */
    fun startPeriodicSubscriptionCheck() {
        val storeVM = StoreVM()
        
        subscriptionCheckTimer = Timer().apply {
            scheduleAtFixedRate(0, 7200000) { // 7200000 ms = 2 hours
                mainHandler.post {
                    storeVM.fetchAndUpdateReceipt()
                }
            }
        }
    }
    
    /**
     * Creates a session document in Firestore with user and location details.
     */
    fun createSessionDocument(userID: String, currentDate: Date) {
        val locationPermission = LocationPermission.getInstance()
        val lat = locationPermission.currentLocation?.latitude ?: 0.0
        val long = locationPermission.currentLocation?.longitude ?: 0.0
        val alt = locationPermission.currentLocation?.altitude ?: 0.0
        
        val sessionData = mutableMapOf<String, Any>(
            "userId" to userID,
            "startTime" to currentDate,
            "endTime" to currentDate, // You can update this when the session ends
            "latitude" to lat,
            "longitude" to long,
            "altitude" to alt,
            "duration" to 0 // You can update this when the session ends
        )
        
        // Reference to the "sessions" collection
        val sessionsCollectionRef = FirebaseFirestore.getInstance().collection("sessions")
        
        sessionsCollectionRef.add(sessionData).addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val docRef = task.result
                Log.d(TAG, "Session document created successfully!")
                
                // Add the "id" field to the document with Firebase's generated document ID
                sessionData["id"] = docRef.id
                
                // Update the document with the "id" field
                docRef.update(sessionData)
                
                val appContext = com.example.blueprintvision.BlueprintVisionApp.getInstance()
                val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("SessionID", docRef.id).apply()
                
                val mixpanel = MixpanelAPI.getInstance(null, null)
                val properties = JSONObject().apply {
                    put("session_id", docRef.id)
                    put("user_id", userID)
                    put("date", Date().time)
                }
                mixpanel.track("app_started", properties)
            } else {
                Log.e(TAG, "Error creating session document: ${task.exception?.message}")
            }
        }
    }
    
    /**
     * Stops all active timers related to the session.
     */
    private fun stopAllTimers() {
        updateTimer?.cancel()
        updateTimer = null
        
        subscriptionCheckTimer?.cancel()
        subscriptionCheckTimer = null
    }
    
    /**
     * Calculates and returns the elapsed time of the current session.
     */
    fun calculateElapsedTime(): Double {
        val sessionStartTime = _sessionStartTime.value ?: return 0.0
        return (Date().time - sessionStartTime.time) / 1000.0
    }
    
    /**
     * Helper method to track errors in Mixpanel
     */
    private fun trackErrorInMixpanel(eventName: String, userId: String) {
        val mixpanel = MixpanelAPI.getInstance(null, null)
        val properties = JSONObject().apply {
            put("session_id", getSharedPrefsString("SessionID") ?: "")
            put("sign_up_variant", getSharedPrefsString("sign_up_variant") ?: "")
            put("user_id", userId)
            put("blueprint_id", getSharedPrefsString("SelectedBlueprintID") ?: "")
            put("date", Date().time)
        }
        mixpanel.track(eventName, properties)
        mixpanel.flush()
    }
    
    /**
     * Helper method to get string from SharedPreferences
     */
    private fun getSharedPrefsString(key: String): String? {
        val appContext = com.example.blueprintvision.BlueprintVisionApp.getInstance()
        val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPrefs.getString(key, null)
    }
}
