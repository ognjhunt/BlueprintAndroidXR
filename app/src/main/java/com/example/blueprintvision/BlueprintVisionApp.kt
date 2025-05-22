package com.example.blueprintvision

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.blueprintvision.manager.AppState
import com.example.blueprintvision.manager.AudioSessionManager
import com.example.blueprintvision.manager.UserSettingsManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.revenuecat.purchases.Purchases
import java.util.*

/**
 * Main application class for BlueprintVision.
 * This serves as the primary point of coordination for the application's lifecycle.
 */
class BlueprintVisionApp : Application(), LifecycleObserver {
    
    // Access the shared instance of AppState
    val appState: AppState by lazy { AppState.getInstance() }
    
    companion object {
        private const val TAG = "BlueprintVisionApp"
        
        // Singleton instance
        private lateinit var instance: BlueprintVisionApp
        
        fun getInstance(): BlueprintVisionApp {
            return instance
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Register this class as a lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Initialize RevenueCat
        Purchases.configure(this, "appl_JIPJDfuETnHOEYHBBumfFrmuHzO")
        
        // Initialize Mixpanel
        val mixpanelToken = when (BuildConfig.BUILD_TYPE) {
            "debug" -> "d18e75c739f4259d492a95146b4fc891"
            "test" -> "667e7095f0d6f9fd62eea8f839a6fbe7"
            "release" -> "4db2c04d8075ba3b65a377d3bb52a43e"
            else -> "d18e75c739f4259d492a95146b4fc891"
        }
        MixpanelAPI.getInstance(this, mixpanelToken)
        
        // Configure Firebase
        val firebaseConfigFilename = when (BuildConfig.BUILD_TYPE) {
            "debug" -> "google-services-dev.json"
            "test" -> "google-services-test.json"
            "release" -> "google-services-prod.json"
            else -> "google-services-dev.json"
        }
        
        Log.d(TAG, "Firebase config: $firebaseConfigFilename")
        
        try {
            // Firebase should be automatically initialized through google-services.json
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e(TAG, "Could not initialize Firebase: ${e.message}")
        }
        
        // Assign a sign-up variant if not already assigned
        val sharedPrefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("sign_up_variant")) {
            val variants = arrayOf("variant_a", "variant_b")
            val randomVariant = variants.random()
            sharedPrefs.edit().putString("sign_up_variant", randomVariant).apply()
        }
        
        Log.d(TAG, "Sign up variant: ${sharedPrefs.getString("sign_up_variant", "")}")
        
        // Initialize audio session
        AudioSessionManager.getInstance().configureAudioSession(this)
        
        // Clean up UserDefaults equivalent (SharedPreferences)
        cleanupPreferences()
        
        // Start the session when the app launches
        appState.startSession()
    }
    
    /**
     * Cleans up preferences at app startup
     */
    private fun cleanupPreferences() {
        val sharedPrefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        editor.remove("SelectedBlueprintID")
        editor.remove("BlueprintSettingsID")
        editor.putInt("storedSelectedIndex", -1)
        editor.remove("SelectedObjectID")
        editor.remove("SelectedPortalID")
        editor.remove("SelectedBlueprintName")
        editor.remove("SessionID")
        editor.remove("BlueprintElapsedTime")
        editor.remove("SpaceLeftPercentage")
        editor.remove("inSharedSpace")
        editor.remove("isEditing")
        editor.remove("hasDownloadedObjectInSession")
        editor.putInt("selectedPhotoWidth", 200)
        editor.putInt("selectedPhotoHeight", 100)
        editor.putFloat("volumetricObjectWidth", 1f)
        editor.putFloat("volumetricObjectHeight", 1f)
        editor.putFloat("volumetricObjectDepth", 1f)
        editor.putString("objectURL", "https://www.google.com")
        editor.remove("previouslyOpened")
        editor.putInt("fileContentWidth", 1)
        editor.putInt("fileContentHeight", 1)
        
        editor.apply()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "App foregrounded")
        AudioSessionManager.getInstance().configureAudioSession(this)
        appState.appIsActive = true
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Log.d(TAG, "App backgrounded")
        appState.appIsActive = false
        appState.endSession()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppTerminated() {
        Log.d(TAG, "App terminated")
        val sharedPrefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("AppCrashedPreviously", false).apply()
        appState.endSession()
    }
}
