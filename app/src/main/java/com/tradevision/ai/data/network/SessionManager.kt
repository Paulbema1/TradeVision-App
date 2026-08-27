package com.tradevision.ai.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val securePrefsName = "tradevision_secure_prefs"
    private val normalPrefsName = "tradevision_prefs"

    // Encrypted prefs for sensitive data (JWT)
    private val securePrefs: SharedPreferences

    // Normal prefs for non-sensitive settings
    private val prefs: SharedPreferences = context.getSharedPreferences(normalPrefsName, Context.MODE_PRIVATE)

    init {
        // Initialize encrypted prefs; fail fast if not available to avoid insecure fallback for JWT
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            securePrefs = EncryptedSharedPreferences.create(
                context,
                securePrefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Optional: migrate legacy token if present (legacy storage name kept intentionally short)
            migrateLegacyTokenIfPresent(context)
        } catch (e: Exception) {
            // Do NOT fallback to plain SharedPreferences for JWT — fail fast so environment can be fixed
            throw IllegalStateException("EncryptedSharedPreferences unavailable: JWT storage requires AndroidX Security. Details: ${e.message}")
        }
    }

    private fun migrateLegacyTokenIfPresent(context: Context) {
        try {
            val legacy = context.getSharedPreferences("tradevision_session", Context.MODE_PRIVATE)
            val legacyToken = legacy.getString("jwt_token", null)
            if (!legacyToken.isNullOrEmpty() && securePrefs.getString("jwt_token", null).isNullOrEmpty()) {
                securePrefs.edit().putString("jwt_token", legacyToken).apply()
                legacy.edit().remove("jwt_token").apply()
            }
        } catch (_: Exception) {
            // ignore migration errors
        }
    }

    // Sensitive token storage
    fun saveToken(token: String) {
        securePrefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return securePrefs.getString("jwt_token", null)
    }

    fun clearToken() {
        securePrefs.edit().remove("jwt_token").apply()
    }

    // Non-sensitive data stored in regular prefs
    fun saveRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getRole(): String {
        return prefs.getString("user_role", "USER") ?: "USER"
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getUsername(): String {
        return prefs.getString("username", "") ?: ""
    }

    fun saveMainTf(tf: String) {
        prefs.edit().putString("main_tf", tf).apply()
    }

    fun getMainTf(): String {
        return prefs.getString("main_tf", "1h") ?: "1h"
    }

    fun saveConfirmTf(tf: String) {
        prefs.edit().putString("confirm_tf", tf).apply()
    }

    fun getConfirmTf(): String {
        return prefs.getString("confirm_tf", "4h") ?: "4h"
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun clear() {
        // clear secure token and normal prefs
        try { securePrefs.edit().clear().apply() } catch (_: Exception) {}
        prefs.edit().clear().apply()
    }

    // Deduplication: persist seen signal IDs (non-sensitive)
    fun hasSeenSignal(signalId: String?): Boolean {
        if (signalId.isNullOrEmpty()) return false
        return prefs.getBoolean("seen_signal_" + signalId, false)
    }

    fun markSignalSeen(signalId: String?) {
        if (signalId.isNullOrEmpty()) return
        prefs.edit().putBoolean("seen_signal_" + signalId, true).apply()
    }
}
