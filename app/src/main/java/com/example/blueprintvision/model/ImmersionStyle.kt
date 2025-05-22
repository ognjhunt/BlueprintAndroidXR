package com.example.blueprintvision.model

/**
 * Represents different immersion styles for the XR experience.
 * This is the Kotlin equivalent of the Swift enum for immersion styles.
 */
enum class ImmersionStyle {
    MIXED,   // Mixed reality mode that blends virtual content with the real world
    FULL,    // Fully immersive VR mode
    PROGRESSIVE // Progressive immersion that adapts based on context
}
