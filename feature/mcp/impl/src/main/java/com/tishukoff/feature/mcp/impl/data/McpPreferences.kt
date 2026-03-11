package com.tishukoff.feature.mcp.impl.data

import android.content.Context

/**
 * Stores the last used MCP server URL in SharedPreferences.
 */
class McpPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun saveUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    private companion object {
        const val PREFS_NAME = "mcp_prefs"
        const val KEY_SERVER_URL = "server_url"
    }
}
