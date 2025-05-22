package com.example.blueprintvision.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blueprintvision.BlueprintVisionApp
import com.example.blueprintvision.manager.BlueprintManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import org.json.JSONObject
import java.util.*

/**
 * ViewModel for managing store operations and subscription status.
 */
class StoreVM : ViewModel() {
    private val TAG = "StoreVM"
    
    // LiveData for subscription status
    private val _isSubscribed = MutableLiveData<Boolean>(false)
    val isSubscribed: LiveData<Boolean> = _isSubscribed
    
    // LiveData for current plan
    private val _currentPlan = MutableLiveData<String>("free")
    val currentPlan: LiveData<String> = _currentPlan
    
    // LiveData for available products
    private val _availableProducts = MutableLiveData<List<String>>(emptyList())
    val availableProducts: LiveData<List<String>> = _availableProducts
    
    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        fetchAndUpdateReceipt()
    }
    
    /**
     * Fetches and updates the subscription receipt from RevenueCat.
     */
    fun fetchAndUpdateReceipt() {
        _isLoading.postValue(true)
        
        Purchases.sharedInstance.getCustomerInfo { customerInfo, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching customer info: ${error.message}")
                _isLoading.postValue(false)
                return@getCustomerInfo
            }
            
            processCustomerInfo(customerInfo)
            _isLoading.postValue(false)
        }
    }
    
    /**
     * Processes the customer info from RevenueCat.
     */
    private fun processCustomerInfo(customerInfo: CustomerInfo?) {
        if (customerInfo == null) {
            Log.e(TAG, "Customer info is null")
            return
        }
        
        // Check if user has active subscription
        val hasActiveSubscription = customerInfo.entitlements.active.isNotEmpty()
        _isSubscribed.postValue(hasActiveSubscription)
        
        // Update plan type based on entitlements
        if (hasActiveSubscription) {
            // Check for specific entitlements to determine plan
            when {
                customerInfo.entitlements.active.containsKey("premium") -> {
                    _currentPlan.postValue("premium")
                    updateFirestorePlanType("premium")
                    updateBlueprintLimits("premium")
                }
                customerInfo.entitlements.active.containsKey("pro") -> {
                    _currentPlan.postValue("pro")
                    updateFirestorePlanType("pro")
                    updateBlueprintLimits("pro")
                }
                customerInfo.entitlements.active.containsKey("basic") -> {
                    _currentPlan.postValue("basic")
                    updateFirestorePlanType("basic")
                    updateBlueprintLimits("basic")
                }
                else -> {
                    _currentPlan.postValue("free")
                    updateFirestorePlanType("free")
                    updateBlueprintLimits("free")
                }
            }
        } else {
            _currentPlan.postValue("free")
            updateFirestorePlanType("free")
            updateBlueprintLimits("free")
        }
        
        // Track subscription status in Mixpanel
        trackSubscriptionStatusInMixpanel(hasActiveSubscription, _currentPlan.value ?: "free")
    }
    
    /**
     * Updates the plan type in Firestore.
     */
    private fun updateFirestorePlanType(planType: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        
        userRef.update("planType", planType)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore plan type updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating Firestore plan type: ${e.message}")
            }
    }
    
    /**
     * Updates the blueprint limits based on plan type.
     */
    private fun updateBlueprintLimits(planType: String) {
        val blueprintManager = BlueprintManager.getInstance()
        
        when (planType) {
            "premium" -> {
                blueprintManager.blueprintPortalLimit = 10
                blueprintManager.blueprintObjectLimit = 50
                blueprintManager.blueprintPhotoLimit = 50
                blueprintManager.blueprintNoteLimit = 50
            }
            "pro" -> {
                blueprintManager.blueprintPortalLimit = 5
                blueprintManager.blueprintObjectLimit = 25
                blueprintManager.blueprintPhotoLimit = 25
                blueprintManager.blueprintNoteLimit = 25
            }
            "basic" -> {
                blueprintManager.blueprintPortalLimit = 3
                blueprintManager.blueprintObjectLimit = 10
                blueprintManager.blueprintPhotoLimit = 15
                blueprintManager.blueprintNoteLimit = 15
            }
            else -> {
                blueprintManager.blueprintPortalLimit = 1
                blueprintManager.blueprintObjectLimit = 6
                blueprintManager.blueprintPhotoLimit = 8
                blueprintManager.blueprintNoteLimit = 8
            }
        }
    }
    
    /**
     * Tracks subscription status in Mixpanel.
     */
    private fun trackSubscriptionStatusInMixpanel(isSubscribed: Boolean, planType: String) {
        val mixpanel = MixpanelAPI.getInstance(null, null)
        val properties = JSONObject().apply {
            put("is_subscribed", isSubscribed)
            put("plan_type", planType)
            put("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: "")
            put("date", Date().time)
            put("session_id", getSharedPrefsString("SessionID") ?: "")
            put("blueprint_id", getSharedPrefsString("SelectedBlueprintID") ?: "")
        }
        
        mixpanel.track("subscription_status_checked", properties)
    }
    
    /**
     * Helper method to get string from SharedPreferences
     */
    private fun getSharedPrefsString(key: String): String? {
        val appContext = BlueprintVisionApp.getInstance()
        val sharedPrefs = appContext.getSharedPreferences("app_preferences", android.content.Context.MODE_PRIVATE)
        return sharedPrefs.getString(key, null)
    }
    
    /**
     * Purchases a product and handles the purchase flow.
     */
    fun purchaseProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.postValue(true)
        
        // Implement purchase flow using RevenueCat
        // This is a placeholder for the actual implementation
        
        _isLoading.postValue(false)
    }
    
    /**
     * Restores purchases and updates the user's subscription status.
     */
    fun restorePurchases(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.postValue(true)
        
        Purchases.sharedInstance.restorePurchases { customerInfo, error ->
            if (error != null) {
                Log.e(TAG, "Error restoring purchases: ${error.message}")
                onError(error.message ?: "Unknown error")
                _isLoading.postValue(false)
                return@restorePurchases
            }
            
            processCustomerInfo(customerInfo)
            onSuccess()
            _isLoading.postValue(false)
        }
    }
}
