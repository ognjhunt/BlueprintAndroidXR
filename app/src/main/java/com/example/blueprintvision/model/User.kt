package com.example.blueprintvision.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Class representing a User stored in Firestore
 */
class User(userFireDoc: Map<String, Any>) {
    var uid: String = ""
    var tempID: String = ""
    var name: String = ""
    var email: String = ""
    var username: String = ""
    var planType: String = ""
    var deviceToken: String = ""
    var referralCode: String = ""
    var currentConnectedNetworkID: String = ""
    var numSessions: Int = 0
    var uploadedContentCount: Int = 0
    var collectedContentCount: Int = 0
    var connectedBlueprintIDs: List<String> = emptyList()
    var collectedObjectIDs: List<String> = emptyList()
    var collectedPortalIDs: List<String> = emptyList()
    var uploadedFileIDs: List<String> = emptyList()
    var createdPhotoIDs: List<String> = emptyList()
    var createdNoteIDs: List<String> = emptyList()
    var createdReportIDs: List<String> = emptyList()
    var createdSuggestionIDs: List<String> = emptyList()
    var createdContentIDs: List<String> = emptyList()
    var credits: Int? = null
    var finishedOnboarding: Boolean? = null
    var hasEnteredNotes: Boolean? = null
    var hasEnteredInventory: Boolean? = null
    var hasEnteredCameraRoll: Boolean? = null
    var amountEarned: Double? = null
    var blockedUserIDs: List<String> = emptyList()
    var followers: List<String> = emptyList()
    var following: List<String> = emptyList()
    var createdDate: Date = Date()
    var lastSessionDate: Date = Date()
    var latitude: Double? = null
    var longitude: Double? = null
    var altitude: Double? = null
    var historyConnectedBlueprintIDs: Map<String, Timestamp> = emptyMap()
    var createdBlueprintIDs: List<String> = emptyList()
    var likedAnchorIDs: List<String> = emptyList()
    var subscriptions: List<String> = emptyList()
    var subscribers: List<String> = emptyList()
    var profileImageUrl: String? = null
    var assetUids: List<String> = emptyList()
    var ownedAssets: List<String> = emptyList()
    var usageTime: Double = 0.0
    
    // New properties
    var modelInteractions: Map<String, Int> = emptyMap()
    var blueprintInteractions: Map<String, Int> = emptyMap()
    var portalInteractions: Map<String, Int> = emptyMap()
    var categoryPreferences: Map<String, Int> = emptyMap()
    var averageSessionDuration: Double = 0.0
    var peakUsageHours: List<Int> = emptyList()
    var lastLoginDate: Date = Date()
    var featureUsageCount: Map<String, Int> = emptyMap()
    var mostUsedFeatures: List<String> = emptyList()
    var creationFrequency: Map<String, Double> = emptyMap()
    var contentEditFrequency: Map<String, Double> = emptyMap()
    var collaborationScore: Int = 0
    var sharedContentCount: Int = 0
    var preferredModelScales: List<Double> = emptyList()
    var preferredRoomTypes: List<String> = emptyList()
    var preferredColors: List<String> = emptyList()
    var dailyActiveStreak: Int = 0
    var weeklyEngagementScore: Double = 0.0
    var completedTutorials: List<String> = emptyList()
    var skillLevels: Map<String, Int> = emptyMap()
    var mostFrequentLocation: String = ""
    var deviceTypes: List<String> = emptyList()
    var providedRatings: Map<String, Double> = emptyMap()
    var feedbackSentiment: Map<String, String> = emptyMap()
    var customizationPreferences: Map<String, Any> = emptyMap()
    var notificationPreferences: Map<String, Boolean> = emptyMap()
    var creativityScore: Double = 0.0
    var explorationIndex: Double = 0.0
    var connectedPlatforms: List<String> = emptyList()
    var importedContentSources: List<String> = emptyList()
    
    init {
        // uid
        (userFireDoc["uid"] as? String)?.let {
            this.uid = it
        }
        
        (userFireDoc["tempID"] as? String)?.let {
            this.tempID = it
        }
        
        (userFireDoc["finishedOnboarding"] as? Boolean)?.let {
            this.finishedOnboarding = it
        }
        
        (userFireDoc["hasEnteredNotes"] as? Boolean)?.let {
            this.hasEnteredNotes = it
        }
        
        (userFireDoc["hasEnteredCameraRoll"] as? Boolean)?.let {
            this.hasEnteredCameraRoll = it
        }
        
        (userFireDoc["hasEnteredInventory"] as? Boolean)?.let {
            this.hasEnteredInventory = it
        }
        
        // name
        (userFireDoc["name"] as? String)?.let {
            this.name = it
        }
        
        // email
        (userFireDoc["email"] as? String)?.let {
            this.email = it
        }
        
        // username
        (userFireDoc["username"] as? String)?.let {
            this.username = it
        }
        
        (userFireDoc["planType"] as? String)?.let {
            this.planType = it
        }
        
        (userFireDoc["deviceToken"] as? String)?.let {
            this.deviceToken = it
        }
        
        (userFireDoc["referralCode"] as? String)?.let {
            this.referralCode = it
        }
        
        (userFireDoc["credits"] as? Int)?.let {
            this.credits = it
        }
        
        (userFireDoc["usageTime"] as? Double)?.let {
            this.usageTime = it
        }
        
        (userFireDoc["createdDate"] as? Timestamp)?.let {
            this.createdDate = it.toDate()
        }
        
        (userFireDoc["lastSessionDate"] as? Timestamp)?.let {
            this.lastSessionDate = it.toDate()
        }
        
        (userFireDoc["amountEarned"] as? Double)?.let {
            this.amountEarned = it
        }
        
        (userFireDoc["numSessions"] as? Int)?.let {
            this.numSessions = it
        }
        
        (userFireDoc["uploadedContentCount"] as? Int)?.let {
            this.uploadedContentCount = it
        }
        
        (userFireDoc["collectedContentCount"] as? Int)?.let {
            this.collectedContentCount = it
        }
        
        (userFireDoc["blockedUserIDs"] as? List<String>)?.let {
            this.blockedUserIDs = it
        }
        
        (userFireDoc["connectedBlueprintIDs"] as? List<String>)?.let {
            this.connectedBlueprintIDs = it
        }
        
        (userFireDoc["collectedObjectIDs"] as? List<String>)?.let {
            this.collectedObjectIDs = it
        }
        
        (userFireDoc["collectedPortalIDs"] as? List<String>)?.let {
            this.collectedPortalIDs = it
        }
        
        (userFireDoc["uploadedFileIDs"] as? List<String>)?.let {
            this.uploadedFileIDs = it
        }
        
        (userFireDoc["createdNoteIDs"] as? List<String>)?.let {
            this.createdNoteIDs = it
        }
        
        (userFireDoc["createdPhotoIDs"] as? List<String>)?.let {
            this.createdPhotoIDs = it
        }
        
        (userFireDoc["createdReportIDs"] as? List<String>)?.let {
            this.createdReportIDs = it
        }
        
        (userFireDoc["createdSuggestionIDs"] as? List<String>)?.let {
            this.createdSuggestionIDs = it
        }
        
        (userFireDoc["createdContentIDs"] as? List<String>)?.let {
            this.createdContentIDs = it
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
        
        // followers
        (userFireDoc["followers"] as? List<String>)?.let {
            this.followers = it
        }
        
        // following
        (userFireDoc["following"] as? List<String>)?.let {
            this.following = it
        }
        
        (userFireDoc["likedAnchorIDs"] as? List<String>)?.let {
            this.likedAnchorIDs = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["historyConnectedBlueprintIDs"] as? Map<String, Timestamp>)?.let {
            this.historyConnectedBlueprintIDs = it
        }
        
        (userFireDoc["currentConnectedNetworkID"] as? String)?.let {
            this.currentConnectedNetworkID = it
        }
        
        // created blueprints
        (userFireDoc["createdBlueprintIDs"] as? List<String>)?.let {
            this.createdBlueprintIDs = it
        }
        
        // profile image
        (userFireDoc["profileImageUrl"] as? String)?.let {
            this.profileImageUrl = it
        }
        
        // Assets
        (userFireDoc["assetUids"] as? List<String>)?.let {
            this.assetUids = it
        }
        
        (userFireDoc["ownedAssets"] as? List<String>)?.let {
            this.ownedAssets = it
        }
        
        // subscriptions
        (userFireDoc["subscriptions"] as? List<String>)?.let {
            this.subscriptions = it
        }
        
        (userFireDoc["subscribers"] as? List<String>)?.let {
            this.subscribers = it
        }
        
        // New property initializations
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["modelInteractions"] as? Map<String, Int>)?.let {
            this.modelInteractions = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["blueprintInteractions"] as? Map<String, Int>)?.let {
            this.blueprintInteractions = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["portalInteractions"] as? Map<String, Int>)?.let {
            this.portalInteractions = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["categoryPreferences"] as? Map<String, Int>)?.let {
            this.categoryPreferences = it
        }
        
        (userFireDoc["averageSessionDuration"] as? Double)?.let {
            this.averageSessionDuration = it
        }
        
        (userFireDoc["peakUsageHours"] as? List<Int>)?.let {
            this.peakUsageHours = it
        }
        
        (userFireDoc["lastLoginDate"] as? Timestamp)?.let {
            this.lastLoginDate = it.toDate()
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["featureUsageCount"] as? Map<String, Int>)?.let {
            this.featureUsageCount = it
        }
        
        (userFireDoc["mostUsedFeatures"] as? List<String>)?.let {
            this.mostUsedFeatures = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["creationFrequency"] as? Map<String, Double>)?.let {
            this.creationFrequency = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["contentEditFrequency"] as? Map<String, Double>)?.let {
            this.contentEditFrequency = it
        }
        
        (userFireDoc["collaborationScore"] as? Int)?.let {
            this.collaborationScore = it
        }
        
        (userFireDoc["sharedContentCount"] as? Int)?.let {
            this.sharedContentCount = it
        }
        
        (userFireDoc["preferredModelScales"] as? List<Double>)?.let {
            this.preferredModelScales = it
        }
        
        (userFireDoc["preferredRoomTypes"] as? List<String>)?.let {
            this.preferredRoomTypes = it
        }
        
        (userFireDoc["preferredColors"] as? List<String>)?.let {
            this.preferredColors = it
        }
        
        (userFireDoc["dailyActiveStreak"] as? Int)?.let {
            this.dailyActiveStreak = it
        }
        
        (userFireDoc["weeklyEngagementScore"] as? Double)?.let {
            this.weeklyEngagementScore = it
        }
        
        (userFireDoc["completedTutorials"] as? List<String>)?.let {
            this.completedTutorials = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["skillLevels"] as? Map<String, Int>)?.let {
            this.skillLevels = it
        }
        
        (userFireDoc["mostFrequentLocation"] as? String)?.let {
            this.mostFrequentLocation = it
        }
        
        (userFireDoc["deviceTypes"] as? List<String>)?.let {
            this.deviceTypes = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["providedRatings"] as? Map<String, Double>)?.let {
            this.providedRatings = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["feedbackSentiment"] as? Map<String, String>)?.let {
            this.feedbackSentiment = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["customizationPreferences"] as? Map<String, Any>)?.let {
            this.customizationPreferences = it
        }
        
        @Suppress("UNCHECKED_CAST")
        (userFireDoc["notificationPreferences"] as? Map<String, Boolean>)?.let {
            this.notificationPreferences = it
        }
        
        (userFireDoc["creativityScore"] as? Double)?.let {
            this.creativityScore = it
        }
        
        (userFireDoc["explorationIndex"] as? Double)?.let {
            this.explorationIndex = it
        }
        
        (userFireDoc["connectedPlatforms"] as? List<String>)?.let {
            this.connectedPlatforms = it
        }
        
        (userFireDoc["importedContentSources"] as? List<String>)?.let {
            this.importedContentSources = it
        }
    }
    
    companion object {
        /**
         * Creates a User from a Firestore document
         */
        fun fromFirestore(document: DocumentSnapshot): User {
            val data = document.data ?: mapOf("uid" to document.id)
            val dataWithId = data.toMutableMap().apply { 
                if (!containsKey("uid")) put("uid", document.id) 
            }
            return User(dataWithId)
        }
    }
    
    /**
     * Converts the User to a Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        result["uid"] = uid
        result["tempID"] = tempID
        result["name"] = name
        result["email"] = email
        result["username"] = username
        result["planType"] = planType
        result["deviceToken"] = deviceToken
        result["referralCode"] = referralCode
        result["currentConnectedNetworkID"] = currentConnectedNetworkID
        result["numSessions"] = numSessions
        result["uploadedContentCount"] = uploadedContentCount
        result["collectedContentCount"] = collectedContentCount
        result["usageTime"] = usageTime
        
        // Lists
        result["connectedBlueprintIDs"] = connectedBlueprintIDs
        result["collectedObjectIDs"] = collectedObjectIDs
        result["collectedPortalIDs"] = collectedPortalIDs
        result["uploadedFileIDs"] = uploadedFileIDs
        result["createdPhotoIDs"] = createdPhotoIDs
        result["createdNoteIDs"] = createdNoteIDs
        result["createdReportIDs"] = createdReportIDs
        result["createdSuggestionIDs"] = createdSuggestionIDs
        result["createdContentIDs"] = createdContentIDs
        result["blockedUserIDs"] = blockedUserIDs
        result["followers"] = followers
        result["following"] = following
        result["createdBlueprintIDs"] = createdBlueprintIDs
        result["likedAnchorIDs"] = likedAnchorIDs
        result["subscriptions"] = subscriptions
        result["subscribers"] = subscribers
        result["assetUids"] = assetUids
        result["ownedAssets"] = ownedAssets
        
        // Dates
        result["createdDate"] = Timestamp(createdDate)
        result["lastSessionDate"] = Timestamp(lastSessionDate)
        
        // Optional values
        credits?.let { result["credits"] = it }
        finishedOnboarding?.let { result["finishedOnboarding"] = it }
        hasEnteredNotes?.let { result["hasEnteredNotes"] = it }
        hasEnteredInventory?.let { result["hasEnteredInventory"] = it }
        hasEnteredCameraRoll?.let { result["hasEnteredCameraRoll"] = it }
        amountEarned?.let { result["amountEarned"] = it }
        latitude?.let { result["latitude"] = it }
        longitude?.let { result["longitude"] = it }
        altitude?.let { result["altitude"] = it }
        profileImageUrl?.let { result["profileImageUrl"] = it }
        
        // Maps
        if (historyConnectedBlueprintIDs.isNotEmpty()) {
            result["historyConnectedBlueprintIDs"] = historyConnectedBlueprintIDs
        }
        
        // New properties
        if (modelInteractions.isNotEmpty()) {
            result["modelInteractions"] = modelInteractions
        }
        
        if (blueprintInteractions.isNotEmpty()) {
            result["blueprintInteractions"] = blueprintInteractions
        }
        
        if (portalInteractions.isNotEmpty()) {
            result["portalInteractions"] = portalInteractions
        }
        
        if (categoryPreferences.isNotEmpty()) {
            result["categoryPreferences"] = categoryPreferences
        }
        
        result["averageSessionDuration"] = averageSessionDuration
        
        if (peakUsageHours.isNotEmpty()) {
            result["peakUsageHours"] = peakUsageHours
        }
        
        result["lastLoginDate"] = Timestamp(lastLoginDate)
        
        if (featureUsageCount.isNotEmpty()) {
            result["featureUsageCount"] = featureUsageCount
        }
        
        if (mostUsedFeatures.isNotEmpty()) {
            result["mostUsedFeatures"] = mostUsedFeatures
        }
        
        if (creationFrequency.isNotEmpty()) {
            result["creationFrequency"] = creationFrequency
        }
        
        if (contentEditFrequency.isNotEmpty()) {
            result["contentEditFrequency"] = contentEditFrequency
        }
        
        result["collaborationScore"] = collaborationScore
        result["sharedContentCount"] = sharedContentCount
        
        if (preferredModelScales.isNotEmpty()) {
            result["preferredModelScales"] = preferredModelScales
        }
        
        if (preferredRoomTypes.isNotEmpty()) {
            result["preferredRoomTypes"] = preferredRoomTypes
        }
        
        if (preferredColors.isNotEmpty()) {
            result["preferredColors"] = preferredColors
        }
        
        result["dailyActiveStreak"] = dailyActiveStreak
        result["weeklyEngagementScore"] = weeklyEngagementScore
        
        if (completedTutorials.isNotEmpty()) {
            result["completedTutorials"] = completedTutorials
        }
        
        if (skillLevels.isNotEmpty()) {
            result["skillLevels"] = skillLevels
        }
        
        if (mostFrequentLocation.isNotEmpty()) {
            result["mostFrequentLocation"] = mostFrequentLocation
        }
        
        if (deviceTypes.isNotEmpty()) {
            result["deviceTypes"] = deviceTypes
        }
        
        if (providedRatings.isNotEmpty()) {
            result["providedRatings"] = providedRatings
        }
        
        if (feedbackSentiment.isNotEmpty()) {
            result["feedbackSentiment"] = feedbackSentiment
        }
        
        if (customizationPreferences.isNotEmpty()) {
            result["customizationPreferences"] = customizationPreferences
        }
        
        if (notificationPreferences.isNotEmpty()) {
            result["notificationPreferences"] = notificationPreferences
        }
        
        result["creativityScore"] = creativityScore
        result["explorationIndex"] = explorationIndex
        
        if (connectedPlatforms.isNotEmpty()) {
            result["connectedPlatforms"] = connectedPlatforms
        }
        
        if (importedContentSources.isNotEmpty()) {
            result["importedContentSources"] = importedContentSources
        }
        
        return result
    }
}
