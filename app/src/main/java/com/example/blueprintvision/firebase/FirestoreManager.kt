package com.example.blueprintvision.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.blueprintvision.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Structure to hold blueprint IDs and their last update time
 */
data class BlueprintCache(
    var blueprintIDs: List<String> = emptyList(),
    var lastUpdated: Date = Date()
)

/**
 * `FirestoreManager` is a utility class responsible for handling all interactions
 * with the Firestore database. It includes caching mechanisms and provides methods
 * to fetch various objects from the database such as User, Blueprint, Anchor, etc.
 */
class FirestoreManager {
    companion object {
        private val TAG = "FirestoreManager"
        private val db = Firebase.firestore
        private val auth = Firebase.auth
        private val storage = Firebase.storage
        
        // Cache for blueprints
        private val blueprintCache = mutableMapOf<String, Blueprint>()
        
        // Cache for anchors
        private val anchorCache = mutableMapOf<String, Anchor>()
        
        // Cache for users
        private val userCache = mutableMapOf<String, User>()
        
        // Cache for models
        private val modelCache = mutableMapOf<String, Model>()
        
        /**
         * Retrieves a `User` object for a given UID. It first attempts to retrieve the user from
         * a cache. If the user is not found in the cache, it fetches from Firestore and then stores
         * it in the cache before returning it via the completion handler.
         */
        suspend fun getUser(userUid: String): User? = suspendCoroutine { continuation ->
            // Check cache first
            if (userCache.containsKey(userUid)) {
                continuation.resume(userCache[userUid])
                return@suspendCoroutine
            }
            
            // Check if the user is authenticated
            if (auth.currentUser != null) {
                // User is authenticated, fetch from Firestore
                val docRef = db.collection("users").document(userUid)
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val user = User.fromFirestore(document)
                            userCache[userUid] = user
                            continuation.resume(user)
                        } else {
                            Log.d(TAG, "ERROR - getUser: Could not get user $userUid")
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "ERROR - getUser: ${e.message}")
                        continuation.resume(null)
                    }
            } else {
                // User is not authenticated, check for temporary user in SharedPreferences
                // This would need to be implemented with context, so returning null for now
                continuation.resume(null)
            }
        }
        
        /**
         * Retrieves a `Blueprint` object by its ID from Firestore. If the object is found, it's returned via
         * the completion handler, otherwise `null` is returned.
         */
        suspend fun getBlueprint(id: String): Blueprint? = suspendCoroutine { continuation ->
            // Check cache first
            if (blueprintCache.containsKey(id)) {
                continuation.resume(blueprintCache[id])
                return@suspendCoroutine
            }
            
            val docRef = db.collection("blueprints").document(id)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val blueprint = Blueprint.fromFirestore(document)
                        blueprintCache[id] = blueprint
                        continuation.resume(blueprint)
                    } else {
                        Log.d(TAG, "ERROR - getBlueprint: Could not get blueprint $id")
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - getBlueprint: ${e.message}")
                    continuation.resume(null)
                }
        }
        
        /**
         * Retrieves a `Model` object by its ID from Firestore. If the object is found, it's returned via
         * the completion handler, otherwise `null` is returned.
         */
        suspend fun getModel(id: String): Model? = suspendCoroutine { continuation ->
            // Check cache first
            if (modelCache.containsKey(id)) {
                continuation.resume(modelCache[id])
                return@suspendCoroutine
            }
            
            val docRef = db.collection("models").document(id)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val model = Model.fromFirestore(document)
                        modelCache[id] = model
                        continuation.resume(model)
                    } else {
                        Log.d(TAG, "ERROR - getModel: Could not get model $id")
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - getModel: ${e.message}")
                    continuation.resume(null)
                }
        }
        
        /**
         * Retrieves an `Anchor` object for a given ID, utilizing a cache to improve performance.
         */
        suspend fun getAnchor(id: String): Anchor? = suspendCoroutine { continuation ->
            // Check cache first
            if (anchorCache.containsKey(id)) {
                continuation.resume(anchorCache[id])
                return@suspendCoroutine
            }
            
            val docRef = db.collection("anchors").document(id)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val anchor = Anchor.fromFirestore(document)
                        anchorCache[id] = anchor
                        continuation.resume(anchor)
                    } else {
                        Log.d(TAG, "ERROR - getAnchor: Could not get anchor $id")
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - getAnchor: ${e.message}")
                    continuation.resume(null)
                }
        }
        
        /**
         * Gets all anchors for a specific blueprint
         */
        suspend fun getBlueprintAnchors(blueprintId: String): List<Anchor> = suspendCoroutine { continuation ->
            db.collection("anchors")
                .whereEqualTo("blueprintId", blueprintId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val documents = querySnapshot.documents
                    val anchors = mutableListOf<Anchor>()
                    for (document in documents) {
                        val anchor = Anchor.fromFirestore(document)
                        anchorCache[anchor.id] = anchor
                        anchors.add(anchor)
                    }
                    continuation.resume(anchors)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - getBlueprintAnchors: ${e.message}")
                    continuation.resume(emptyList())
                }
        }
        
        /**
         * Creates or updates an anchor in Firestore
         */
        suspend fun saveAnchor(anchor: Anchor): Boolean = suspendCoroutine { continuation ->
            val anchorData = anchor.toFirestore()
            
            db.collection("anchors").document(anchor.id)
                .set(anchorData)
                .addOnSuccessListener {
                    // Update the cache
                    anchorCache[anchor.id] = anchor
                    
                    // Update the blueprint's anchorIDs if not already included
                    val blueprintId = anchor.blueprintId
                    if (blueprintId.isNotEmpty()) {
                        getBlueprint(blueprintId) { blueprint ->
                            if (blueprint != null && !blueprint.anchorIDs.contains(anchor.id)) {
                                val updatedAnchorIDs = blueprint.anchorIDs + anchor.id
                                updateBlueprint(blueprintId, mapOf("anchorIDs" to updatedAnchorIDs)) { success ->
                                    if (!success) {
                                        Log.d(TAG, "Failed to update blueprint's anchorIDs")
                                    }
                                }
                            }
                        }
                    }
                    
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - saveAnchor: ${e.message}")
                    continuation.resume(false)
                }
        }
        
        /**
         * Gets all blueprints
         */
        suspend fun getAllBlueprints(): List<Blueprint> = suspendCoroutine { continuation ->
            db.collection("blueprints")
                .get()
                .addOnSuccessListener { documents ->
                    val blueprints = mutableListOf<Blueprint>()
                    for (document in documents) {
                        val blueprint = Blueprint.fromFirestore(document)
                        blueprintCache[blueprint.id] = blueprint
                        blueprints.add(blueprint)
                    }
                    continuation.resume(blueprints)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - getAllBlueprints: ${e.message}")
                    continuation.resume(emptyList())
                }
        }
        
        /**
         * Gets all blueprints created by a specific user
         */
        suspend fun getCreatedBlueprints(userUid: String): List<Blueprint> = suspendCoroutine { continuation ->
            db.collection("blueprints")
                .whereEqualTo("creator", userUid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val blueprints = mutableListOf<Blueprint>()
                    for (document in querySnapshot.documents) {
                        val blueprint = Blueprint.fromFirestore(document)
                        blueprintCache[blueprint.id] = blueprint
                        blueprints.add(blueprint)
                    }
                    continuation.resume(blueprints)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - getCreatedBlueprints: ${e.message}")
                    continuation.resume(emptyList())
                }
        }
        
        /**
         * Fetches blueprints for a user profile
         */
        suspend fun getProfileCreatedBlueprints(userUid: String): List<Blueprint> = suspendCoroutine { continuation ->
            val currentUserUid = auth.currentUser?.uid
            
            if (currentUserUid == userUid) {
                // Get user's blueprint IDs and fetch the blueprints
                getUserBlueprintIDs(userUid) { blueprintIDs ->
                    fetchBlueprints(blueprintIDs) { blueprints ->
                        continuation.resume(blueprints)
                    }
                }
            } else {
                // Fetch blueprints for a different user
                getCreatedBlueprints(userUid) { blueprints ->
                    continuation.resume(blueprints)
                }
            }
        }
        
        /**
         * Helper method to fetch user's blueprint IDs
         */
        private fun getUserBlueprintIDs(userID: String, completion: (List<String>) -> Unit) {
            if (auth.currentUser != null) {
                // Fetch from Firestore
                getUser(userID) { user ->
                    completion(user?.createdBlueprintIDs ?: emptyList())
                }
            } else {
                // Could fetch from SharedPreferences for a temporary user
                completion(emptyList())
            }
        }
        
        /**
         * Helper method to fetch blueprints from their IDs
         */
        private fun fetchBlueprints(blueprintIDs: List<String>, completion: (List<Blueprint>) -> Unit) {
            val fetchedBlueprints = mutableListOf<Blueprint>()
            var remainingFetches = blueprintIDs.size
            
            if (blueprintIDs.isEmpty()) {
                completion(emptyList())
                return
            }
            
            for (blueprintID in blueprintIDs) {
                if (blueprintID.isNotEmpty()) {
                    getBlueprint(blueprintID) { blueprint ->
                        if (blueprint != null) {
                            fetchedBlueprints.add(blueprint)
                        }
                        remainingFetches--
                        if (remainingFetches == 0) {
                            completion(fetchedBlueprints)
                        }
                    }
                } else {
                    remainingFetches--
                    if (remainingFetches == 0) {
                        completion(fetchedBlueprints)
                    }
                }
            }
        }
        
        /**
         * Fetches a blueprint name by its ID
         */
        suspend fun fetchBlueprintName(blueprintID: String): String? = suspendCoroutine { continuation ->
            val blueprintRef = db.collection("blueprints").document(blueprintID)
            
            blueprintRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val blueprintName = document.getString("name")
                        continuation.resume(blueprintName)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Error fetching blueprint: ${e.message}")
                    continuation.resume(null)
                }
        }
        
        /**
         * Searches for blueprints by name
         */
        suspend fun searchBlueprints(query: String, limit: Int = 12): List<Blueprint> = suspendCoroutine { continuation ->
            val lowercasedQuery = query.lowercase()
            
            db.collection("blueprints")
                .whereEqualTo("isPrivate", false)
                .orderBy("name")
                .limit(limit.toLong())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val blueprints = querySnapshot.documents
                        .mapNotNull { Blueprint.fromFirestore(it) }
                        .filter { it.name.lowercase().contains(lowercasedQuery) }
                    
                    continuation.resume(blueprints)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - searchBlueprints: ${e.message}")
                    continuation.resume(emptyList())
                }
        }
        
        /**
         * Retrieves the current user's UID if authenticated
         */
        fun getCurrentUserUid(): String? {
            return auth.currentUser?.uid
        }
        
        /**
         * Gets a string representation of the current user's UID, or empty string if not authenticated
         */
        fun getCurrentUserUid2(): String {
            return auth.currentUser?.uid ?: ""
        }
        
        /**
         * Updates a blueprint document with new data
         */
        suspend fun updateBlueprint(blueprintId: String, updateData: Map<String, Any>): Boolean = suspendCoroutine { continuation ->
            db.collection("blueprints").document(blueprintId)
                .update(updateData)
                .addOnSuccessListener {
                    // Remove from cache so next fetch gets updated data
                    blueprintCache.remove(blueprintId)
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - updateBlueprint: ${e.message}")
                    continuation.resume(false)
                }
        }
        
        /**
         * Updates a blueprint document with new data with a callback
         */
        fun updateBlueprint(blueprintId: String, updateData: Map<String, Any>, completion: (Boolean) -> Unit) {
            db.collection("blueprints").document(blueprintId)
                .update(updateData)
                .addOnSuccessListener {
                    // Remove from cache so next fetch gets updated data
                    blueprintCache.remove(blueprintId)
                    completion(true)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - updateBlueprint: ${e.message}")
                    completion(false)
                }
        }
        
        /**
         * Creates a new blueprint in Firestore
         */
        suspend fun createBlueprint(blueprint: Blueprint): String = suspendCoroutine { continuation ->
            val newBlueprintRef = if (blueprint.id.isEmpty()) {
                db.collection("blueprints").document()
            } else {
                db.collection("blueprints").document(blueprint.id)
            }
            
            val blueprintData = blueprint.toFirestore()
            newBlueprintRef.set(blueprintData)
                .addOnSuccessListener {
                    // Add blueprint ID to user's createdBlueprintIDs
                    val currentUserUid = getCurrentUserUid()
                    if (currentUserUid != null) {
                        db.collection("users").document(currentUserUid)
                            .update("createdBlueprintIDs", FieldValue.arrayUnion(newBlueprintRef.id))
                            .addOnFailureListener { e ->
                                Log.d(TAG, "ERROR - failed to update user's createdBlueprintIDs: ${e.message}")
                            }
                    }
                    
                    continuation.resume(newBlueprintRef.id)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - createBlueprint: ${e.message}")
                    continuation.resume("")
                }
        }
        
        /**
         * Authenticates a user with email and password
         */
        suspend fun loginUser(email: String, password: String): Boolean = suspendCoroutine { continuation ->
            if (email.isEmpty() || password.isEmpty()) {
                continuation.resume(false)
                return@suspendCoroutine
            }
            
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - loginUser: ${e.message}")
                    continuation.resume(false)
                }
        }
        
        /**
         * Checks if a username is unique
         */
        suspend fun usernameUnique(username: String): Boolean = suspendCoroutine { continuation ->
            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    continuation.resume(querySnapshot.documents.isEmpty())
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "ERROR - usernameUnique: ${e.message}")
                    continuation.resume(false)
                }
        }
    }
}
