package com.tishukoff.feature.mcp.impl.data

import android.content.Context

/**
 * Stores MCP server URLs in SharedPreferences.
 */
class McpPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun saveUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    /**
     * Returns saved orchestration servers as list of "label|url" pairs.
     */
    fun getOrchestrationServers(): List<Pair<String, String>> {
        val raw = prefs.getStringSet(KEY_ORCHESTRATION_SERVERS, null) ?: return emptyList()
        return raw.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }

    fun saveOrchestrationServers(servers: List<Pair<String, String>>) {
        val set = servers.map { "${it.first}|${it.second}" }.toSet()
        prefs.edit().putStringSet(KEY_ORCHESTRATION_SERVERS, set).apply()
    }

    fun getSavedHost(): String? = prefs.getString(KEY_HOST_ADDRESS, null)

    fun saveHost(host: String) {
        prefs.edit().putString(KEY_HOST_ADDRESS, host).apply()
    }

    private companion object {
        const val PREFS_NAME = "mcp_prefs"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_ORCHESTRATION_SERVERS = "orchestration_servers"
        const val KEY_HOST_ADDRESS = "host_address"
    }
}
