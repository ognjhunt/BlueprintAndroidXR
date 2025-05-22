package com.example.blueprintvision.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.blueprintvision.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A helper class to manage Firebase authentication related tasks.
 */
class FirebaseAuthHelper {
    companion object {
        private val TAG = "FirebaseAuthHelper"
        private val auth = Firebase.auth

        /**
         * Retrieves the unique identifier for the currently logged-in user.
         * @return An optional string containing the UID of the current user, if signed in.
         */
        fun getCurrentUserUid(): String? {
            return auth.currentUser?.uid
        }

        /**
         * Retrieves the unique identifier for the current user or prints an error message and returns an empty string if no user is found.
         * @return A string containing the UID of the current user, or an empty string if no user is found.
         */
        fun getCurrentUserUid2(): String {
            return auth.currentUser?.uid ?: run {
                Log.e(TAG, "Could not get uid of current User - FATAL ERROR")
                ""
            }
        }

        /**
         * Fetches the full `User` object of the currently authenticated user.
         * @param completion A callback that gets called with the `User` object once retrieved.
         */
        fun getCurrentUser(completion: (User) -> Unit) {
            val currentUser = getCurrentUserUid2()
            FirestoreManager.getUser(currentUser) { user ->
                user?.let { completion(it) } ?: run {
                    Log.e(TAG, "Could not get current user")
                    // In Kotlin we can't use fatalError like in Swift, so we'll log an error instead
                }
            }
        }

        /**
         * Attempts to log in a user with an email and password.
         * @param email The user's email address as an optional string.
         * @param password The user's password as an optional string.
         * @param completion A callback that gets called with an optional error message if login fails.
         */
        fun loginUser(email: String?, password: String?, completion: (String?) -> Unit) {
            // Validate email / pwd
            if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                return completion("Fields cannot be empty")
            }

            // sign in user via firebase auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        // Upload fcm token
                        val sharedPrefs = getSharedPreferences()
                        val fcmToken = sharedPrefs?.getString("fcmToken", "") ?: ""
                        
                        FirestoreManager.updateFCMToken(user?.uid ?: "", fcmToken) { success ->
                            if (!success) {
                                Log.e(TAG, "Could not update FCMToken")
                                completion("Please try again")
                            } else {
                                completion(null)
                            }
                        }
                    } else {
                        completion(task.exception?.localizedMessage)
                    }
                }
        }

        /**
         * Signs out the currently logged-in user.
         * @param completion A callback that gets called once the user is signed out.
         */
        fun signOut(completion: () -> Unit) {
            try {
                auth.signOut()
                completion()
            } catch (signOutError: Exception) {
                Log.e(TAG, "Error signing out: ${signOutError.localizedMessage}")
            }
        }

        /**
         * Sends a password reset email to the specified email address.
         * @param email The email address to send the password reset to.
         * @param completion A callback that gets called with an optional error message if the password reset fails.
         */
        fun sendPasswordReset(email: String, completion: (String?) -> Unit) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        completion(null)
                    } else {
                        Log.e(TAG, "ERROR - resetPassword: ${task.exception?.localizedMessage}")
                        completion(task.exception?.localizedMessage)
                    }
                }
        }

        /**
         * Deletes the currently authenticated user from Firebase Authentication.
         * @param completion A callback that gets called with a boolean indicating the success of the operation.
         */
        fun deleteUser(completion: (Boolean) -> Unit) {
            val firUser = auth.currentUser
            if (firUser == null) {
                Log.e(TAG, "ERROR - deleteUser: No current user to delete")
                return completion(false)
            }

            firUser.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        completion(true)
                    } else {
                        Log.e(TAG, "ERROR - deleteUser: ${task.exception?.localizedMessage}")
                        completion(false)
                    }
                }
        }

        /**
         * Registers a new user with an email and password to Firebase Authentication.
         * @param email The email address to register with.
         * @param password The password for the new account.
         * @param completion A callback that gets called with an optional error message if the registration fails.
         */
        fun registerAuthUser(email: String, password: String, completion: (String?) -> Unit) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        completion(null)
                    } else {
                        Log.e(TAG, "ERROR - registerAuthUser: ${task.exception?.localizedMessage}")
                        completion(task.exception?.localizedMessage)
                    }
                }
        }

        // Helper function to get SharedPreferences
        private fun getSharedPreferences(): SharedPreferences? {
            // Since this is a companion object, we don't have a context
            // In a real implementation, you'd either pass a context to the methods
            // that need it, or use a dependency injection framework
            // For now, I'll return null and handle it in the calling methods
            return null
        }
    }
}
