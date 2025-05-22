package com.example.blueprintvision.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.os.Handler
import android.os.Looper

/**
 * AudioSessionManager handles audio session configuration and audio interruptions.
 */
class AudioSessionManager private constructor() {
    private val TAG = "AudioSessionManager"
    
    companion object {
        @Volatile
        private var INSTANCE: AudioSessionManager? = null
        
        fun getInstance(): AudioSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioSessionManager()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    
    /**
     * Configures the audio session for the application.
     */
    fun configureAudioSession(context: Context?) {
        val appContext = context ?: com.example.blueprintvision.BlueprintVisionApp.getInstance()
        
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Set up audio focus change listener
        if (audioFocusChangeListener == null) {
            audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "Audio focus lost")
                        // Handle audio focus loss - pause playback
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "Audio focus lost temporarily")
                        // Handle temporary loss - pause playback
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(TAG, "Audio focus gained")
                        // Resume playback if needed
                    }
                }
            }
        }
        
        // Request audio focus for playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener!!, Handler(Looper.getMainLooper()))
                .build()
            
            val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audio focus request granted")
            } else {
                Log.d(TAG, "Audio focus request failed")
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audio focus request granted")
            } else {
                Log.d(TAG, "Audio focus request failed")
            }
        }
    }
    
    /**
     * Starts listening for audio interruptions.
     */
    fun startListeningForInterruptions(context: Context) {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        
        context.registerReceiver(audioNoisyReceiver, filter)
    }
    
    /**
     * Handles interruptions to the audio session.
     */
    fun handleInterruptions(context: Context) {
        startListeningForInterruptions(context)
    }
    
    /**
     * Broadcast receiver for audio becoming noisy (e.g., headphones unplugged).
     */
    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Handle audio becoming noisy - pause playback
                Log.d(TAG, "Audio becoming noisy - pausing playback")
            }
        }
    }
    
    /**
     * Handles audio interruption based on the notification.
     */
    fun handleInterruption(what: Int, extras: Map<String, Any>?) {
        when (what) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Interruption began, take appropriate actions - pause playback
                Log.d(TAG, "Audio interruption began")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Interruption ended, resume playback if needed
                Log.d(TAG, "Audio interruption ended")
            }
        }
    }
    
    /**
     * Cleanup audio session resources.
     */
    fun cleanup(context: Context) {
        try {
            context.unregisterReceiver(audioNoisyReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered, ignore
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusChangeListener)
        }
    }
}
