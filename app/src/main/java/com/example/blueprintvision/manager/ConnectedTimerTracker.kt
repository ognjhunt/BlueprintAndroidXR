package com.example.blueprintvision.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

/**
 * Manages and tracks connected timers for the application.
 * This is a utility class to track time-related operations.
 */
class ConnectedTimerTracker private constructor() {
    // Stores active timers
    private val activeTimers = mutableMapOf<String, Timer>()
    
    // Track the active status of timers
    private val _activeTimerCount = MutableLiveData<Int>(0)
    val activeTimerCount: LiveData<Int> = _activeTimerCount
    
    companion object {
        @Volatile
        private var INSTANCE: ConnectedTimerTracker? = null
        
        fun getInstance(): ConnectedTimerTracker {
            return INSTANCE ?: synchronized(this) {
                val instance = ConnectedTimerTracker()
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Starts a new timer with the given ID.
     *
     * @param timerId Unique identifier for the timer
     * @param period Period in milliseconds between timer executions
     * @param action Action to perform when the timer fires
     */
    fun startTimer(timerId: String, period: Long, action: () -> Unit) {
        stopTimer(timerId) // Ensure any existing timer with this ID is stopped
        
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                action()
            }
        }, 0, period)
        
        activeTimers[timerId] = timer
        _activeTimerCount.postValue(activeTimers.size)
    }
    
    /**
     * Stops the timer with the given ID.
     *
     * @param timerId Unique identifier for the timer to stop
     */
    fun stopTimer(timerId: String) {
        activeTimers[timerId]?.let { timer ->
            timer.cancel()
            timer.purge()
            activeTimers.remove(timerId)
            _activeTimerCount.postValue(activeTimers.size)
        }
    }
    
    /**
     * Stops all active timers.
     */
    fun stopAllTimers() {
        activeTimers.forEach { (_, timer) ->
            timer.cancel()
            timer.purge()
        }
        activeTimers.clear()
        _activeTimerCount.postValue(0)
    }
    
    /**
     * Checks if a timer with the given ID is active.
     *
     * @param timerId Unique identifier for the timer to check
     * @return True if the timer is active, false otherwise
     */
    fun isTimerActive(timerId: String): Boolean {
        return activeTimers.containsKey(timerId)
    }
    
    /**
     * Returns the number of active timers.
     *
     * @return Count of active timers
     */
    fun getActiveTimerCount(): Int {
        return activeTimers.size
    }
}
