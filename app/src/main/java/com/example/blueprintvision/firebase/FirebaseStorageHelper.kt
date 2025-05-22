package com.example.blueprintvision.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A utility class for interacting with Firebase Cloud Storage, providing methods to perform download tasks.
 */
class FirebaseStorageHelper {
    companion object {
        private val TAG = "FirebaseStorageHelper"
        private val cloudStorage = Firebase.storage
        
        /**
         * Asynchronously downloads a file from Firebase Storage and saves it to the local file system.
         * @param relativePath The path within Firebase Storage where the file is located.
         * @param handler A callback that is invoked with the local file URL on successful download, or null if an error occurs.
         */
        fun asyncDownloadToFilesystem(relativePath: String, handler: (Uri?) -> Unit) {
            // Create local filesystem URL
            val localFile = try {
                val filesDir = File(android.os.Environment.getExternalStorageDirectory(), "Documents")
                if (!filesDir.exists()) {
                    filesDir.mkdirs()
                }
                File(filesDir, relativePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error: Could not get document directory URL", e)
                handler(null)
                return
            }
            
            // Check if the file already exists
            if (localFile.exists()) {
                handler(Uri.fromFile(localFile))
                return
            }
            
            // Make sure parent directories exist
            localFile.parentFile?.mkdirs()
            
            // Reference to cloud storage
            val storageRef = cloudStorage.reference.child(relativePath)
            
            // Download and write to file
            storageRef.getFile(localFile)
                .addOnSuccessListener {
                    handler(Uri.fromFile(localFile))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firebase storage: Error downloading file with relativePath: $relativePath", e)
                    handler(null)
                }
        }
        
        /**
         * Suspends coroutine until the file is downloaded from Firebase Storage to the local file system.
         * @param relativePath The path within Firebase Storage where the file is located.
         * @return The local file Uri on successful download, or null if an error occurs.
         */
        suspend fun downloadToFilesystem(relativePath: String): Uri? = suspendCoroutine { continuation ->
            asyncDownloadToFilesystem(relativePath) { fileUri ->
                continuation.resume(fileUri)
            }
        }
    }
}
