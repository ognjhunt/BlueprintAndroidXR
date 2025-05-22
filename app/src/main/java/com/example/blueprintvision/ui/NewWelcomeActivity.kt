package com.example.blueprintvision.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.blueprintvision.manager.BlueprintManager
import com.example.blueprintvision.manager.UserManager
import com.example.blueprintvision.model.UserType
import com.example.blueprintvision.ui.theme.BlueprintVisionTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mixpanel.android.mpmetrics.MixpanelAPI
import java.util.*
import kotlin.collections.HashMap

/**
 * The entry point of the application, presenting the welcome message and handling user interaction
 * for creating a new blueprint and temporary user.
 */
class NewWelcomeActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermission: LocationPermission
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var blueprintManager: BlueprintManager
    private lateinit var db: FirebaseFirestore
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            locationPermission.startUpdatingLocation()
        } else {
            showLocationAlert()
        }
    }
    
    private var temporaryUserID: String = UUID.randomUUID().toString()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize components
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationPermission = LocationPermission(this)
        sharedPreferences = getSharedPreferences("blueprintPrefs", MODE_PRIVATE)
        blueprintManager = BlueprintManager.getInstance(this)
        db = FirebaseFirestore.getInstance()
        
        // Get or create temporary user ID
        if (sharedPreferences.getString("temporaryUserID", null) == null) {
            sharedPreferences.edit().putString("temporaryUserID", temporaryUserID).apply()
        } else {
            temporaryUserID = sharedPreferences.getString("temporaryUserID", temporaryUserID) ?: temporaryUserID
        }
        
        // Request location permission
        requestLocationAuthorization()
        
        setContent {
            BlueprintVisionTheme {
                WelcomeScreen(
                    locationPermission = locationPermission,
                    onCreateTeam = { userType ->
                        val intent = Intent(this, CreateTeamActivity::class.java)
                        intent.putExtra("USER_TYPE", userType.name)
                        startActivity(intent)
                    },
                    onLogin = {
                        val intent = Intent(this, LogInActivity::class.java)
                        startActivity(intent)
                    },
                    onTerms = {
                        val intent = Intent(this, TermsActivity::class.java)
                        startActivity(intent)
                    },
                    onBlueprintConnect = { blueprintId ->
                        connectBlueprint(blueprintId)
                    },
                    onPersonalization = {
                        val intent = Intent(this, PersonalizationTestActivity::class.java)
                        startActivity(intent)
                    },
                    createTempUserAndBlueprint = { completion ->
                        createTempUserAndBlueprint {
                            completion()
                        }
                    }
                )
            }
        }
        
        // Check if returning user and open appropriate window
        if (FirebaseAuth.getInstance().currentUser != null || sharedPreferences.getString("temporaryUserID", null) != null) {
            sharedPreferences.edit().putBoolean("previouslyOpened", true).apply()
            openWindowForReturningUser()
        }
        
        // Track app open event
        if (!sharedPreferences.getBoolean("finishedOnboarding", false)) {
            sharedPreferences.edit().putBoolean("isFirstSession", true).apply()
            val properties = HashMap<String, Any>()
            properties["date"] = Date()
            properties["session_id"] = sharedPreferences.getString("SessionID", "") ?: ""
            MixpanelAPI.getInstance(this, "MIXPANEL_TOKEN").track("initial_app_open", properties)
        }
    }
    
    private fun requestLocationAuthorization() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 
                    PackageManager.PERMISSION_GRANTED -> {
                locationPermission.startUpdatingLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    
    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage("Blueprint is a location-based app. To continue, you'll need to allow Location Access.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showLocationAlert()
            }
            .create()
            .show()
    }
    
    private fun showLocationAlert() {
        AlertDialog.Builder(this)
            .setTitle("Oops!")
            .setMessage("Blueprint is a location-based app! To continue, you'll need to allow Location Access in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                goToSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    
    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
    
    private fun connectBlueprint(blueprintId: String) {
        if (blueprintId.isEmpty()) return
        
        val docRef = db.collection("blueprints").document(blueprintId)
        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                // Set this as the currently selected blueprint
                BlueprintManager.getInstance(this).loadBlueprint(blueprintId)
                sharedPreferences.edit().putString("SelectedBlueprintID", blueprintId).apply()
                
                // Mark the blueprint as connected for the user
                val connected = sharedPreferences.getStringSet("connectedBlueprintIDs", mutableSetOf()) ?: mutableSetOf()
                if (!connected.contains(blueprintId)) {
                    val newSet = connected.toMutableSet()
                    newSet.add(blueprintId)
                    sharedPreferences.edit().putStringSet("connectedBlueprintIDs", newSet).apply()
                }
                
                // Set flags to bypass recommendation system
                sharedPreferences.edit()
                    .putBoolean("isRecommendationProcessed", true)
                    .putBoolean("showNearbyBlueprints", false)
                    .apply()
                
                // Open loading blueprint activity
                val intent = Intent(this, LoadingBlueprintActivity::class.java)
                intent.putExtra("BLUEPRINT_ID", blueprintId)
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun createTempUserAndBlueprint(completion: () -> Unit) {
        // Create temporary user
        createTempUser {
            // Create blueprint after user is created
            createBlueprint {
                // Clean up user data
                removeEmptyStringsFromArrays(temporaryUserID)
                completion()
            }
        }
    }
    
    private fun createTempUser(completion: () -> Unit) {
        val userLocation = locationPermission.userLocation
        val lat = userLocation?.latitude ?: 0.0
        val long = userLocation?.longitude ?: 0.0
        val altitude = userLocation?.altitude ?: 0.0
        
        val userData = HashMap<String, Any>()
        userData["tempID"] = temporaryUserID
        userData["name"] = ""
        userData["tempUser"] = true
        userData["hasEnteredNotes"] = false
        userData["hasEnteredCameraRoll"] = false
        userData["planType"] = "free"
        userData["usageTime"] = 0
        userData["connectedBlueprintIDs"] = ArrayList<String>()
        userData["historyConnectedBlueprintIDs"] = ArrayList<String>()
        userData["username"] = ""
        userData["profileImageUrl"] = ""
        userData["sessionsDates"] = listOf(Date())
        userData["lastSessionDate"] = Date()
        userData["numSessions"] = 1
        userData["createdDate"] = Date()
        userData["latitude"] = lat
        userData["longitude"] = long
        userData["altitude"] = altitude
        userData["geohash"] = ""
        userData["uploadedFileIDs"] = ArrayList<String>()
        userData["createdNoteIDs"] = ArrayList<String>()
        userData["createdPhotoIDs"] = ArrayList<String>()
        userData["createdReportIDs"] = ArrayList<String>()
        userData["createdSuggestionIDs"] = ArrayList<String>()
        userData["createdBlueprintIDs"] = ArrayList<String>()
        userData["collectedObjectIDs"] = ArrayList<String>()
        userData["collectedPortalIDs"] = ArrayList<String>()
        
        // Save user data to SharedPreferences
        val userDataJson = com.google.gson.Gson().toJson(userData)
        sharedPreferences.edit().putString("temporaryUser", userDataJson).apply()
        
        completion()
    }
    
    private fun createBlueprint(completion: () -> Unit) {
        val userLocation = locationPermission.userLocation
        val lat = userLocation?.latitude ?: 0.0
        val long = userLocation?.longitude ?: 0.0
        val altitude = userLocation?.altitude ?: 0.0
        
        // Generate geohash
        val hash = com.firebase.geofire.GeoFireUtils.getGeoHashForLocation(
            com.firebase.geofire.GeoLocation(lat, long)
        )
        
        val blueprintData = HashMap<String, Any>()
        blueprintData["name"] = "Blueprint 1"
        blueprintData["createdDate"] = Date()
        blueprintData["latitude"] = lat
        blueprintData["longitude"] = long
        blueprintData["altitude"] = altitude
        blueprintData["geohash"] = hash
        blueprintData["password"] = ""
        blueprintData["connectedTime"] = 0
        blueprintData["numLikes"] = 0
        blueprintData["numDislikes"] = 0
        blueprintData["isPrivate"] = "true"
        blueprintData["host"] = temporaryUserID
        blueprintData["storage"] = 500
        blueprintData["size"] = 0
        blueprintData["userCount"] = 1
        blueprintData["portalCount"] = 0
        blueprintData["objectCount"] = 0
        blueprintData["fileCount"] = 0
        blueprintData["websiteCount"] = 0
        blueprintData["noteCount"] = 0
        blueprintData["photoCount"] = 0
        blueprintData["widgetCount"] = 0
        blueprintData["portalLimit"] = 1
        blueprintData["fileLimit"] = 4
        blueprintData["websiteLimit"] = 4
        blueprintData["objectLimit"] = 5
        blueprintData["widgetLimit"] = 4
        blueprintData["noteLimit"] = 6
        blueprintData["photoLimit"] = 6
        blueprintData["anchors"] = ArrayList<String>()
        blueprintData["objectIDs"] = ArrayList<String>()
        blueprintData["portalIDs"] = ArrayList<String>()
        blueprintData["noteIDs"] = ArrayList<String>()
        blueprintData["fileIDs"] = ArrayList<String>()
        blueprintData["websiteURLs"] = ArrayList<String>()
        blueprintData["widgetIDs"] = ArrayList<String>()
        blueprintData["photoIDs"] = ArrayList<String>()
        blueprintData["numSessions"] = 0
        blueprintData["users"] = listOf(temporaryUserID)
        blueprintData["thumbnail"] = "1"
        
        // Add document to Firestore
        db.collection("blueprints").add(blueprintData)
            .addOnSuccessListener { docRef ->
                val blueprintId = docRef.id
                
                // Update the document with its ID
                blueprintData["id"] = blueprintId
                docRef.update(blueprintData)
                
                // Update user's createdBlueprintIDs
                updateCreatedBlueprintIDs(temporaryUserID, blueprintId) {
                    completion()
                }
            }
            .addOnFailureListener {
                // Handle failure
                completion()
            }
    }
    
    private fun updateCreatedBlueprintIDs(userID: String, blueprintID: String, completion: () -> Unit) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            openWindowForReturningUser()
            sharedPreferences.edit().putBoolean("finishedOnboarding", true).apply()
            completion()
        } else {
            // For temporary users, update SharedPreferences
            val userDataJson = sharedPreferences.getString("temporaryUser", null)
            if (userDataJson != null) {
                val userData = com.google.gson.Gson().fromJson(userDataJson, HashMap::class.java)
                val createdBlueprintIDs = userData["createdBlueprintIDs"] as? ArrayList<String> ?: ArrayList()
                
                // Filter out empty strings
                val filteredIds = createdBlueprintIDs.filter { it.isNotEmpty() }.toMutableList()
                
                // Add the new blueprint ID
                filteredIds.add(blueprintID)
                userData["createdBlueprintIDs"] = filteredIds
                
                // Save updated user data
                val updatedUserDataJson = com.google.gson.Gson().toJson(userData)
                sharedPreferences.edit().putString("temporaryUser", updatedUserDataJson).apply()
                
                sharedPreferences.edit().putBoolean("previouslyOpened", true).apply()
                completion()
            } else {
                completion()
            }
        }
    }
    
    private fun removeEmptyStringsFromArrays(userID: String) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            // For authenticated users, would update Firestore - stubbed for now
        } else {
            // For temporary users, update SharedPreferences
            val userDataJson = sharedPreferences.getString("temporaryUser", null)
            if (userDataJson != null) {
                val userData = com.google.gson.Gson().fromJson(userDataJson, HashMap::class.java)
                
                // List of keys that contain string arrays
                val keys = listOf(
                    "createdNoteIDs", "createdPhotoIDs", "createdReportIDs", "uploadedFileIDs",
                    "createdSuggestionIDs", "createdBlueprintIDs", "collectedObjectIDs",
                    "collectedPortalIDs"
                )
                
                // Filter out empty strings for each key
                for (key in keys) {
                    val array = userData[key] as? ArrayList<String>
                    if (array != null) {
                        userData[key] = array.filter { it.isNotEmpty() }
                    }
                }
                
                // Save updated user data
                val updatedUserDataJson = com.google.gson.Gson().toJson(userData)
                sharedPreferences.edit().putString("temporaryUser", updatedUserDataJson).apply()
            }
        }
    }
    
    private fun openWindowForReturningUser() {
        val intent = Intent(this, LoadingBlueprintActivity::class.java)
        startActivity(intent)
    }
}

/**
 * A class responsible for managing location permissions and tracking the user's current location.
 */
class LocationPermission(private val activity: ComponentActivity) {
    var userLocation: Location? = null
        private set
    
    var authorizationStatus: Int = PackageManager.PERMISSION_DENIED
        private set
        
    var authorizationStatusChecked = false
        private set
    
    var isLocationAvailable = false
        private set
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(activity)
    
    /**
     * Starts the process of updating the user's location.
     */
    fun startUpdatingLocation() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    isLocationAvailable = true
                    println("User's current location: ${location.latitude}, ${location.longitude}, ${location.altitude}")
                }
            }
        }
    }
    
    /**
     * Stops the process of updating the user's location.
     */
    fun stopUpdatingLocation() {
        // Would implement location updates cancellation here
    }
    
    /**
     * Requests the user's permission for location updates.
     */
    fun requestLocationPermission() {
        activity.requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

/**
 * The main welcome screen composable that provides the user interface for the welcome screen.
 */
@Composable
fun WelcomeScreen(
    locationPermission: LocationPermission,
    onCreateTeam: (UserType) -> Unit,
    onLogin: () -> Unit,
    onTerms: () -> Unit,
    onBlueprintConnect: (String) -> Unit,
    onPersonalization: () -> Unit,
    createTempUserAndBlueprint: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    
    var selectedUserType by remember { mutableStateOf<UserType?>(null) }
    var showingBlueprintIDDialog by remember { mutableStateOf(false) }
    var blueprintIDInput by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "What will you be using Blueprint for?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp, start = 14.dp)
            )
            
            Text(
                text = "We'll use this to recommend content and templates for you.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 30.dp, start = 14.dp)
            )
            
            Text(
                text = "There are no Blueprints to connect to here.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 40.dp, start = 14.dp)
                    .width(390.dp)
            )
            
            // Purpose selection buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BlueprintPurposeButton(
                    icon = "building.2",
                    mainText = "Large company",
                    subText = "You're here to scale your brand's experience",
                    onClick = {
                        selectedUserType = UserType.ENTERPRISE
                        UserManager.getInstance(context).setUserType(UserType.ENTERPRISE)
                        onCreateTeam(UserType.ENTERPRISE)
                    }
                )
                
                BlueprintPurposeButton(
                    icon = "paintpalette",
                    mainText = "Personal",
                    subText = "You're here to make anything and everything",
                    onClick = {
                        selectedUserType = UserType.PERSONAL
                        UserManager.getInstance(context).setUserType(UserType.PERSONAL)
                        val intent = Intent(context, EnterpriseOnboardingActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                
                BlueprintPurposeButton(
                    icon = "house",
                    mainText = "Small Business",
                    subText = "You're here to design your location's experience",
                    onClick = {
                        selectedUserType = UserType.SMALL_BUSINESS
                        UserManager.getInstance(context).setUserType(UserType.SMALL_BUSINESS)
                        onCreateTeam(UserType.SMALL_BUSINESS)
                    }
                )
            }
            
            // Login link
            TextButton(
                onClick = onLogin,
                modifier = Modifier.padding(bottom = 20.dp, start = 14.dp)
            ) {
                Text(
                    text = "Already have an account? Log in",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Terms and conditions text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val annotatedString = buildAnnotatedString {
                    append("By continuing, you agree to the ")
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = Color.White))
                    append("Terms of Service")
                    pop()
                    append(".")
                }
                
                Text(
                    text = annotatedString,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onTerms() }
                )
            }
            
            // Blueprint ID connection
            TextButton(
                onClick = { showingBlueprintIDDialog = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Connect to a Blueprint by ID",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Personalization button
            Button(
                onClick = onPersonalization,
                modifier = Modifier
                    .width(280.dp)
                    .padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Personalize Your Experience",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Blueprint ID Dialog
    if (showingBlueprintIDDialog) {
        AlertDialog(
            onDismissRequest = { showingBlueprintIDDialog = false },
            title = { Text("Enter Blueprint ID") },
            text = {
                TextField(
                    value = blueprintIDInput,
                    onValueChange = { blueprintIDInput = it },
                    label = { Text("Blueprint ID") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingBlueprintIDDialog = false
                        onBlueprintConnect(blueprintIDInput.trim())
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showingBlueprintIDDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BlueprintPurposeButton(
    icon: String,
    mainText: String,
    subText: String,
    onClick: () -> Unit
) {
    val iconMap = mapOf(
        "building.2" to Icons.Default.QuestionMark,
        "paintpalette" to Icons.Default.QuestionMark,
        "house" to Icons.Default.QuestionMark
    )
    
    // We would use actual icons here, but for simplicity using placeholders
    val displayIcon = iconMap[icon] ?: Icons.Default.QuestionMark
    
    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = displayIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF9C27B0)
        )
        
        Text(
            text = mainText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = subText,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
