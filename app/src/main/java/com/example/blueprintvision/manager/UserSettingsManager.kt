package com.example.blueprintvision.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blueprintvision.BlueprintVisionApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import java.util.*

/**
 * UserSettingsManager handles the user's settings, including their subscription plan type.
 */
class UserSettingsManager {
    private val TAG = "UserSettingsManager"
    
    // The current plan type of the user, defaulting to "free" if not set.
    private val _planType = MutableLiveData<String>("free")
    val planType: LiveData<String> = _planType
    
    /**
     * Fetches the current user's plan type from the Firestore database and updates the planType property.
     */
    fun fetchPlanType() {
        // Check if the user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(currentUser.uid)
            
            userRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        val data = document.data
                        val fetchedPlanType = data?.get("planType") as? String
                        
                        if (fetchedPlanType != null) {
                            _planType.postValue(fetchedPlanType)
                            trackMixpanelEvent("fetch_plan_type_user_does_exist")
                        }
                    } else {
                        Log.d(TAG, "User document does not exist.")
                        trackMixpanelEvent("fetch_plan_type_user_does_not_exist")
                    }
                } else {
                    Log.e(TAG, "Error fetching user document: ${task.exception?.message}")
                    trackMixpanelEvent("fetch_plan_type_error_fetching_user_doc")
                }
            }
        } else {
            // For temporary users, fetch from SharedPreferences
            val appContext = BlueprintVisionApp.getInstance()
            val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val tempUserJson = sharedPrefs.getString("temporaryUser", null)
            
            if (tempUserJson != null) {
                try {
                    val tempUserData = org.json.JSONObject(tempUserJson)
                    val fetchedPlanType = tempUserData.optString("planType", "free")
                    _planType.postValue(fetchedPlanType)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing temporary user data: ${e.message}")
                    trackMixpanelEvent("fetch_plan_type_temp_user_not_found")
                }
            } else {
                Log.e(TAG, "Temporary user data not found in SharedPreferences")
                trackMixpanelEvent("fetch_plan_type_temp_user_not_found")
            }
        }
    }
    
    /**
     * Helper method to track events in Mixpanel
     */
    private fun trackMixpanelEvent(eventName: String) {
        val mixpanel = MixpanelAPI.getInstance(null, null)
        val appContext = BlueprintVisionApp.getInstance()
        val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        
        val properties = JSONObject().apply {
            put("blueprint_id", sharedPrefs.getString("SelectedBlueprintID", ""))
            put("session_id", sharedPrefs.getString("SessionID", ""))
            put("sign_up_variant", sharedPrefs.getString("sign_up_variant", ""))
            put("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: 
                           sharedPrefs.getString("temporaryUserID", ""))
            put("date", Date().time)
        }
        
        mixpanel.track(eventName, properties)
    }
    
    // Temporary user ID generation and management
    val temporaryUserID: String
        get() {
            val appContext = BlueprintVisionApp.getInstance()
            val sharedPrefs = appContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            var storedId = sharedPrefs.getString("temporaryUserID", null)
            
            if (storedId == null) {
                storedId = UUID.randomUUID().toString()
                sharedPrefs.edit().putString("temporaryUserID", storedId).apply()
            }
            
            return storedId
        }
}
