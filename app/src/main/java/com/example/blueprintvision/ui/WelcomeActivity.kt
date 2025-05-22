package com.example.blueprintvision.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blueprintvision.manager.BlueprintManager
import com.example.blueprintvision.model.Blueprint
import com.example.blueprintvision.ui.theme.BlueprintVisionTheme

/**
 * Welcome screen that shows available blueprints and allows selection
 */
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val blueprintManager = BlueprintManager.getInstance(this)
        
        setContent {
            BlueprintVisionTheme {
                WelcomeScreen(
                    blueprintManager = blueprintManager,
                    onBlueprintSelected = { blueprintId ->
                        // Save selected blueprint ID
                        blueprintManager.loadBlueprint(blueprintId)
                        
                        // Navigate to loading screen
                        val intent = Intent(this, LoadingBlueprintActivity::class.java)
                        intent.putExtra("BLUEPRINT_ID", blueprintId)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    blueprintManager: BlueprintManager,
    onBlueprintSelected: (String) -> Unit
) {
    var blueprints by remember { mutableStateOf<List<Blueprint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load blueprints on first composition
    LaunchedEffect(Unit) {
        blueprintManager.getAllBlueprints { loadedBlueprints ->
            blueprints = loadedBlueprints
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blueprint Vision") }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (blueprints.isEmpty()) {
                    // No blueprints available
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "No Blueprints Available",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Create a new blueprint to get started",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Display blueprint list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Select a Blueprint",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        items(blueprints) { blueprint ->
                            BlueprintItem(
                                blueprint = blueprint,
                                onClick = { onBlueprintSelected(blueprint.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintItem(
    blueprint: Blueprint,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = blueprint.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (blueprint.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = blueprint.description,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                blueprint.createdAt?.let { date ->
                    Text(
                        text = "Created: ${date.toString().substring(0, 10)}",
                        fontSize = 12.sp
                    )
                }
                
                if (blueprint.isPrivate) {
                    Text(
                        text = "Private",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
