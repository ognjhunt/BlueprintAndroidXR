package com.example.blueprintvision.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.blueprintvision.model.LocationCoordinate
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * StorageManager: A utility class for managing uploads and downloads with Firebase Storage.
 */
class StorageManager {
    companion object {
        private const val TAG = "StorageManager"
        
        // Custom error types for storage operations
        sealed class BlueprintError(message: String) : Exception(message) {
            object CurrentUserNotFound : BlueprintError("Current user not found")
            object UploadFailed : BlueprintError("Upload failed")
            object DownloadFailed : BlueprintError("Download failed")
        }

        // Storage reference initialized with Firebase storage
        private val fs = Firebase.storage
        
        // Static variables to hold various unique identifiers for storage operations
        var photoAnchorRandomID = ""
        var videoAnchorRandomID = ""
        var thumbnailRandomID = ""
        var blueprintID = ""
        
        var storageURL = ""

        /**
         * Authorizes a Backblaze B2 account and returns authentication tokens.
         * @param completion Callback with success status, auth token, and API URL
         */
        fun authorizeBackblazeAccount(completion: (Boolean, String?, String?) -> Unit) {
            val keyID = "00550218f7653950000000001" // Your keyID from the dashboard
            val applicationKey = "K00521En3D6GNXS9VFKTLXFVctlPP3Y" // Your applicationKey from the dashboard

            // Encode credentials for basic auth
            val credentials = okhttp3.Credentials.basic(keyID, applicationKey)

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.backblazeb2.com/b2api/v1/b2_authorize_account")
                .header("Authorization", credentials)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Authorization failed: ${e.message}")
                    completion(false, null, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Authorization failed with response: ${response.code}")
                        completion(false, null, null)
                        return
                    }

                    response.body?.string()?.let { responseBody ->
                        try {
                            val json = JSONObject(responseBody)
                            val authToken = json.getString("authorizationToken")
                            val uploadUrl = json.getString("apiUrl")
                            val completeUploadUrl = "$uploadUrl/b2api/v1"
                            Log.d(TAG, "$completeUploadUrl is completeUploadUrl")
                            completion(true, authToken, completeUploadUrl)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing response: ${e.message}")
                            completion(false, null, null)
                        }
                    } ?: completion(false, null, null)
                }
            })
        }

        /**
         * Gets an upload URL from Backblaze B2 using the authorization token.
         * @param authorizationToken The token from authorizeBackblazeAccount
         * @param apiUrl The API URL from authorizeBackblazeAccount
         * @param bucketId The target bucket ID
         * @param completion Callback with success status, upload URL, and upload auth token
         */
        fun getUploadUrl(authorizationToken: String, apiUrl: String, bucketId: String, completion: (Boolean, String?, String?) -> Unit) {
            val client = OkHttpClient()
            
            val jsonBody = JSONObject().apply {
                put("bucketId", bucketId)
            }.toString()
            
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$apiUrl/b2_get_upload_url")
                .header("Authorization", authorizationToken)
                .post(requestBody)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to get upload URL: ${e.message}")
                    completion(false, null, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to get upload URL, response: ${response.code}")
                        completion(false, null, null)
                        return
                    }

                    response.body?.string()?.let { responseBody ->
                        try {
                            val json = JSONObject(responseBody)
                            val uploadUrl = json.getString("uploadUrl")
                            val uploadAuthToken = json.getString("authorizationToken")
                            completion(true, uploadUrl, uploadAuthToken)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing response: ${e.message}")
                            completion(false, null, null)
                        }
                    } ?: completion(false, null, null)
                }
            })
        }

        /**
         * Updates the user's profile picture by uploading to Backblaze B2.
         * @param data The image data to upload
         * @param completion Callback with the file ID if successful, or null if failed
         */
        fun updateProfilePicture(data: ByteArray, completion: (String?) -> Unit) {
            val currentUserUid = FirebaseAuthHelper.getCurrentUserUid() ?: run {
                completion(null)
                return
            }
            
            // Determine bucket based on build variant
            val bucketString = "054022e1986fd72685d30915" // Default to DEBUG
            // In a real app, you'd use BuildConfig.DEBUG, etc.
            
            // Step 1: Get authorization token and API URL
            authorizeBackblazeAccount { success, authToken, apiUrl ->
                if (!success || authToken == null || apiUrl == null) {
                    Log.e(TAG, "Authorization failed")
                    completion(null)
                    return@authorizeBackblazeAccount
                }

                // Step 2: Get upload URL
                getUploadUrl(authToken, apiUrl, bucketString) { success, uploadUrl, uploadAuthToken ->
                    if (!success || uploadUrl == null || uploadAuthToken == null) {
                        Log.e(TAG, "Failed to get upload URL")
                        completion(null)
                        return@getUploadUrl
                    }

                    // Step 3: Upload file
                    val client = OkHttpClient()
                    
                    val requestBody = data.toRequestBody(
                        "image/jpeg".toMediaTypeOrNull(),
                        0,
                        data.size
                    )
                    
                    val request = Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", uploadAuthToken)
                        .header("X-Bz-File-Name", "$currentUserUid.jpg")
                        .header("Content-Type", "image/jpeg")
                        .header("X-Bz-Content-Sha1", data.sha1())
                        .post(requestBody)
                        .build()
                        
                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e(TAG, "Error uploading to Backblaze B2: ${e.message}")
                            completion(null)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                Log.e(TAG, "File upload failed, response: ${response.code}")
                                completion(null)
                                return
                            }

                            response.body?.string()?.let { responseBody ->
                                try {
                                    val json = JSONObject(responseBody)
                                    val fileID = json.getString("fileId")
                                    Log.d(TAG, "Upload successful, file ID: $fileID")
                                    completion(fileID)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing response: ${e.message}")
                                    completion(null)
                                }
                            } ?: completion(null)
                        }
                    })
                }
            }
        }

        /**
         * Generates a random alphanumeric string of specified length.
         * @param length The length of the string to generate
         * @return A random string
         */
        fun randomStorageString(length: Int): String {
            val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..length)
                .map { letters.random() }
                .joinToString("")
        }

        /**
         * Uploads a blueprint model file to Firebase Storage with metadata.
         * @param data The file data to upload
         * @param name The name of the blueprint
         * @param completion Callback with the file name if successful, or null if failed
         */
        fun uploadBlueprint(data: ByteArray, name: String, completion: (String?) -> Unit) {
            val ref = fs.reference.child("blueprints")
            val randID = UUID.randomUUID().toString()
            val blueprintID = "$randID-$name.usdz"
            val networkRef = ref.child(blueprintID)

            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("model/vnd.usdz+zip")
                .build()

            networkRef.putBytes(data, metadata)
                .addOnSuccessListener {
                    completion(blueprintID)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error uploading blueprint: ${e.message}")
                    completion(null)
                }
        }

        /**
         * Uploads a thumbnail image to Firebase Storage.
         * @param data The image data to upload
         * @param completion Callback with the file ID if successful, or null if failed
         */
        fun uploadThumbnail(data: ByteArray, completion: (String?) -> Unit) {
            val currentUserUid = FirebaseAuthHelper.getCurrentUserUid() ?: run {
                completion(null)
                return
            }

            val ref = fs.reference.child("thumbnails")
            val randID = UUID.randomUUID().toString()
            thumbnailRandomID = randID
            val thumbnailRef = ref.child(randID)

            thumbnailRef.putBytes(data)
                .addOnSuccessListener {
                    completion(randID)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error uploading thumbnail: ${e.message}")
                    completion(null)
                }
        }

        /**
         * Extension function to compute SHA1 hash of ByteArray.
         */
        private fun ByteArray.sha1(): String {
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(this)
            return digest.joinToString("") { "%02x".format(it) }
        }
        
        // Thumbnail type enumeration
        enum class ThumbnailType(val value: String) {
            MODEL("model"),
            PORTAL("portal"),
            BLUEPRINT("blueprint"),
            WIDGET("widget")
        }
        
        // Cache for profile pictures
        private val profilePicCache = mutableMapOf<String, Bitmap>()
        
        // Cache for photos
        private val photoPicCache = mutableMapOf<String, Bitmap>()
        
        // Cache for videos
        private val portalVideoCache = mutableMapOf<String, ByteArray>()
        
        // Cache for images
        private val imageCache = mutableMapOf<String, Bitmap>()
        
        // OkHttpClient for network requests
        private val client = OkHttpClient()
        
        /**
         * Uploads a video file to Firebase Storage.
         * @param data The video data to upload
         * @param completion Callback with the file ID if successful, or null if failed
         */
        fun uploadVideo(data: ByteArray, completion: (String?) -> Unit) {
            val currentUserUid = FirebaseAuthHelper.getCurrentUserUid() ?: run {
                completion(null)
                return
            }

            val ref = fs.reference.child("videos")
            val randID = UUID.randomUUID().toString()
            videoAnchorRandomID = randID
            val videoRef = ref.child(randID)

            videoRef.putBytes(data)
                .addOnSuccessListener {
                    completion(randID)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error uploading video: ${e.message}")
                    completion(null)
                }
        }
        
        /**
         * Deletes all files in a user's directory in Firebase Storage.
         * @param userUid The user's UID
         * @param completion Callback with success status
         */
        fun deleteUserDirectory(userUid: String, completion: (Boolean) -> Unit) {
            val fileRef = fs.reference.child(userUid)
            
            fileRef.listAll()
                .addOnSuccessListener { listResult ->
                    if (listResult.items.isEmpty()) {
                        completion(true)
                        return@addOnSuccessListener
                    }
                    
                    var count = 0
                    for (item in listResult.items) {
                        Log.d(TAG, "Deleting ${item.path}")
                        item.delete()
                            .addOnCompleteListener {
                                count++
                                if (count == listResult.items.size) {
                                    completion(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "ERROR - deleteUserDirectory: ${e.message}")
                                completion(false)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ERROR - deleteUserDirectory: ${e.message}")
                    completion(false)
                }
        }
        
        /**
         * Retrieves a user's profile picture from Backblaze B2.
         * @param userId The user's ID
         * @param completion Callback with the bitmap image
         */
        fun getProPic(userId: String, completion: (Bitmap) -> Unit) {
            // Check cache first
            profilePicCache[userId]?.let {
                completion(it)
                return
            }
            
            // Determine bucket based on build variant
            val urlString = "uploadedProfileImages-dev" // Default to DEBUG
            // In a real app, you'd use BuildConfig.DEBUG, etc.
            
            val backblazeURL = "https://f005.backblazeb2.com/file/$urlString/$userId.jpg"
            
            val request = Request.Builder()
                .url(backblazeURL)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Error downloading image: ${e.message}")
                    // Return a default image
                    val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    completion(defaultBitmap)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to download image, response: ${response.code}")
                        val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                        completion(defaultBitmap)
                        return
                    }

                    response.body?.bytes()?.let { data ->
                        try {
                            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            if (bitmap != null) {
                                profilePicCache[userId] = bitmap
                                completion(bitmap)
                            } else {
                                val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                                completion(defaultBitmap)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding image: ${e.message}")
                            val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            completion(defaultBitmap)
                        }
                    } ?: run {
                        val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                        completion(defaultBitmap)
                    }
                }
            })
        }
        
        /**
         * Retrieves a photo by its ID from Backblaze B2.
         * @param photoId The photo ID
         * @param completion Callback with the bitmap image or null if not found
         */
        fun getPhotoPic(photoId: String, completion: (Bitmap?) -> Unit) {
            // Check cache first
            photoPicCache[photoId]?.let {
                completion(it)
                return
            }
            
            // Determine bucket based on build variant
            val urlString = "uploadedPhotos-dev" // Default to DEBUG
            // In a real app, you'd use BuildConfig.DEBUG, etc.
            
            val backblazeURL = "https://f005.backblazeb2.com/file/$urlString/$photoId.jpg"
            
            val request = Request.Builder()
                .url(backblazeURL)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Error downloading image: ${e.message}")
                    completion(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to download image, response: ${response.code}")
                        completion(null)
                        return
                    }

                    response.body?.bytes()?.let { data ->
                        try {
                            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            if (bitmap != null) {
                                photoPicCache[photoId] = bitmap
                                completion(bitmap)
                            } else {
                                completion(null)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding image: ${e.message}")
                            completion(null)
                        }
                    } ?: completion(null)
                }
            })
        }
        
        /**
         * Downloads a file from Backblaze B2 and saves it to the local filesystem.
         * @param fileID The ID of the file
         * @param fileName The name of the file
         * @param fileType The type/extension of the file
         * @param completion Callback with the local file URI or null if download failed
         */
        fun downloadFileContent(fileID: String, fileName: String, fileType: String, completion: (Uri?) -> Unit) {
            // Determine bucket based on build variant
            val bucketString = "uploadedFiles-dev" // Default to DEBUG
            // In a real app, you'd use BuildConfig.DEBUG, etc.
            
            val fileExtension = if (fileType.startsWith(".")) "" else "."
            val urlString = "https://f005.backblazeb2.com/file/$bucketString/${fileID}_$fileName"
            
            Log.d(TAG, "$urlString is downloadURLString")

            // Determine local file URL
            val filesDir = File(android.os.Environment.getExternalStorageDirectory(), "Documents")
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            
            val localFile = File(filesDir, "$fileID.$fileType")
            Log.d(TAG, "$localFile is localFileURL")
            
            // Check if the file already exists locally
            if (localFile.exists()) {
                completion(Uri.fromFile(localFile))
                return
            }
            
            // Create parent directories if needed
            localFile.parentFile?.mkdirs()
            
            val request = Request.Builder()
                .url(urlString)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Error downloading file: ${e.message}")
                    completion(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to download file, response: ${response.code}")
                        completion(null)
                        return
                    }

                    try {
                        response.body?.bytes()?.let { data ->
                            localFile.writeBytes(data)
                            completion(Uri.fromFile(localFile))
                        } ?: completion(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving file: ${e.message}")
                        completion(null)
                    }
                }
            })
        }
        
        /**
         * Gets an authorization token to download files from Backblaze B2.
         * @param bucketId The bucket ID
         * @param fileNamePrefix The file name prefix to authorize
         * @param completion Callback with success status and authorization token
         */
        fun getDownloadAuthorization(bucketId: String, fileNamePrefix: String, completion: (Boolean, String?) -> Unit) {
            authorizeBackblazeAccount { success, authToken, apiUrl ->
                if (!success || authToken == null || apiUrl == null) {
                    Log.e(TAG, "Authorization failed")
                    completion(false, null)
                    return@authorizeBackblazeAccount
                }
                
                val jsonBody = JSONObject().apply {
                    put("bucketId", bucketId)
                    put("fileNamePrefix", fileNamePrefix)
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url("$apiUrl/b2_get_download_authorization")
                    .header("Authorization", authToken)
                    .post(requestBody)
                    .build()
                    
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to get download authorization: ${e.message}")
                        completion(false, null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Failed to get download authorization, response: ${response.code}")
                            completion(false, null)
                            return
                        }

                        response.body?.string()?.let { responseBody ->
                            try {
                                val json = JSONObject(responseBody)
                                val downloadAuthToken = json.getString("authorizationToken")
                                completion(true, downloadAuthToken)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing response: ${e.message}")
                                completion(false, null)
                            }
                        } ?: completion(false, null)
                    }
                })
            }
        }
        
        /**
         * Retrieves a video from the cache or downloads it if not available.
         * @param storagePath The storage path of the video
         * @param completion Callback with the video data or null if not found
         */
        fun getPortalVideo(storagePath: String, completion: (ByteArray?) -> Unit) {
            // Check cache first
            portalVideoCache[storagePath]?.let {
                completion(it)
                return
            }
            
            val ref = fs.reference.child("portalVideos").child(storagePath)
            ref.getBytes(400 * 1024 * 1024) // 400MB max size
                .addOnSuccessListener { data ->
                    portalVideoCache[storagePath] = data
                    completion(data)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ERROR - getPortalVideo: ${e.message}")
                    completion(null)
                }
        }
        
        /**
         * Fetches a thumbnail image from Backblaze B2.
         * @param type The type of thumbnail
         * @param thumbnailName The name of the thumbnail file
         * @param completion Callback with the bitmap image
         */
        fun getThumbnail(ofType type: ThumbnailType, named thumbnailName: String, completion: (Bitmap) -> Unit) {
            // Check cache first
            imageCache[thumbnailName]?.let {
                completion(it)
                return
            }
            
            // Check if the thumbnail name has a .jpeg extension and change it to .jpg
            var updatedThumbnailName = thumbnailName
            if (thumbnailName.endsWith(".jpeg")) {
                updatedThumbnailName = thumbnailName.dropLast(5) + ".jpg"
            }
            
            // Determine bucket based on build variant
            val urlString = "contentThumbnails-dev" // Default to DEBUG
            // In a real app, you'd use BuildConfig.DEBUG, etc.
            
            val downloadURL = "https://f005.backblazeb2.com/file/$urlString/$updatedThumbnailName"
            
            val request = Request.Builder()
                .url(downloadURL)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "ERROR - getThumbnail: ${e.message}")
                    val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    completion(defaultBitmap)
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "Response: $response")
                    
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to download thumbnail, response: ${response.code}")
                        val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                        completion(defaultBitmap)
                        return
                    }

                    response.body?.bytes()?.let { data ->
                        Log.d(TAG, "Data size: ${data.size} bytes")
                        
                        try {
                            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            if (bitmap != null) {
                                imageCache[updatedThumbnailName] = bitmap
                                completion(bitmap)
                            } else {
                                Log.e(TAG, "Could not convert data to Bitmap")
                                val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                                completion(defaultBitmap)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding image: ${e.message}")
                            val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            completion(defaultBitmap)
                        }
                    } ?: run {
                        val defaultBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                        completion(defaultBitmap)
                    }
                }
            })
        }
        
        /**
         * Deletes a user's profile picture from Backblaze B2.
         * @param userUid The user's UID
         * @param completion Callback with success status
         */
        fun deleteProPic(userUid: String, completion: (Boolean) -> Unit) {
            // Step 1: Get authorization token and API URL
            authorizeBackblazeAccount { success, authToken, apiUrl ->
                if (!success || authToken == null || apiUrl == null) {
                    Log.e(TAG, "Authorization failed")
                    completion(false)
                    return@authorizeBackblazeAccount
                }

                // Step 2: Prepare the delete request
                val fileName = "$userUid.jpg"
                
                // Determine bucket based on build variant
                val bucketId = "054022e1986fd72685d30915" // Default to DEBUG
                // In a real app, you'd use BuildConfig.DEBUG, etc.
                
                val jsonBody = JSONObject().apply {
                    put("fileName", fileName)
                    put("bucketId", bucketId)
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url("$apiUrl/b2api/v1/b2_delete_file_version")
                    .header("Authorization", authToken)
                    .post(requestBody)
                    .build()
                    
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Error during file deletion: ${e.message}")
                        completion(false)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "File deletion successful for file: $fileName")
                            // Remove from cache
                            profilePicCache.remove(userUid)
                            completion(true)
                        } else {
                            Log.e(TAG, "File deletion failed, response: ${response.code}")
                            completion(false)
                        }
                    }
                })
            }
        }
        
        /**
         * Uploads a blueprint model file using a file URL to Firebase Storage.
         * @param fileUri The file URI to upload
         * @param completion Callback with the file ID if successful, or null if failed
         */
        fun uploadBlueprint(fileUri: Uri, completion: (String?) -> Unit) {
            val currentUserUid = FirebaseAuthHelper.getCurrentUserUid() ?: run {
                completion(null)
                return
            }

            val ref = fs.reference.child("blueprints")
            val randID = UUID.randomUUID().toString()
            blueprintID = randID
            val networkRef = ref.child(randID)
            
            try {
                val inputStream = fileUri.toFile().inputStream()
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                networkRef.putBytes(bytes)
                    .addOnSuccessListener {
                        completion(randID)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error uploading blueprint: ${e.message}")
                        completion(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading file: ${e.message}")
                completion(null)
            }
        }
        
        /**
         * Extension function to convert a Uri to a File.
         */
        private fun Uri.toFile(): File {
            val path = this.path ?: throw IllegalArgumentException("URI path is null")
            return File(path)
        }
    }
}
