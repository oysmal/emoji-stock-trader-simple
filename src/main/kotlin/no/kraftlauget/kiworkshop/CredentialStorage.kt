package no.kraftlauget.kiworkshop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Handles persistent storage of team credentials to avoid re-registering on every restart
 */
object CredentialStorage {
    
    private val storageFile = File("team-credentials.json")
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    @Serializable
    data class StoredCredentials(
        val teamId: String,
        val apiKey: String,
        val initialCash: Double,
        val registeredAt: String
    )
    
    /**
     * Save team credentials to persistent storage
     */
    fun saveCredentials(teamId: String, apiKey: String, initialCash: Double) {
        try {
            val credentials = StoredCredentials(
                teamId = teamId,
                apiKey = apiKey,
                initialCash = initialCash,
                registeredAt = java.time.LocalDateTime.now().toString()
            )
            
            val jsonString = json.encodeToString(StoredCredentials.serializer(), credentials)
            storageFile.writeText(jsonString)
            
            println("Credentials saved to ${storageFile.absolutePath}")
            
        } catch (e: Exception) {
            println("Failed to save credentials: ${e.message}")
        }
    }
    
    /**
     * Load team credentials from persistent storage
     * @return StoredCredentials if found, null if not found or invalid
     */
    fun loadCredentials(): StoredCredentials? {
        return try {
            if (!storageFile.exists()) {
                println("No stored credentials found at ${storageFile.absolutePath}")
                return null
            }
            
            val jsonString = storageFile.readText()
            val credentials = json.decodeFromString(StoredCredentials.serializer(), jsonString)
            
            println("Loaded existing credentials for team: ${credentials.teamId}")
            println("Originally registered at: ${credentials.registeredAt}")
            
            credentials
            
        } catch (e: Exception) {
            println("Failed to load credentials: ${e.message}")
            println("Will register new team...")
            null
        }
    }
    
    /**
     * Clear stored credentials (useful for testing or switching teams)
     */
    fun clearCredentials() {
        try {
            if (storageFile.exists()) {
                storageFile.delete()
                println("Stored credentials cleared")
            }
        } catch (e: Exception) {
            println("Failed to clear credentials: ${e.message}")
        }
    }
    
    /**
     * Check if credentials file exists
     */
    fun hasStoredCredentials(): Boolean = storageFile.exists()
}