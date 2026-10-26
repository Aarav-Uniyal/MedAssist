package com.example.medassist.data.local

import android.content.Context
import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.model.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saved chats. Off by default: nothing is stored unless the user switches "Save chat history" on. */
interface ChatHistoryStore {
    fun isEnabled(uid: String): Boolean
    fun setEnabled(uid: String, enabled: Boolean)
    suspend fun load(uid: String): List<ChatMessage>
    suspend fun save(uid: String, messages: List<ChatMessage>)
    suspend fun clear(uid: String)

    /** Removes every saved chat on this phone (used on sign-out and account deletion). */
    fun clearAll()
}

/**
 * Keeps chats only on this phone, in the app's private storage (no other app can read it, and it is not
 * uploaded anywhere, so it uses no Firebase quota).
 */
class FileChatHistoryStore(context: Context) : ChatHistoryStore {
    private val prefs = context.applicationContext.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private val dir = File(context.applicationContext.filesDir, "chat")

    private fun file(uid: String) = File(dir, uid.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    override fun isEnabled(uid: String) = prefs.getBoolean("enabled_$uid", false)

    override fun setEnabled(uid: String, enabled: Boolean) {
        prefs.edit().putBoolean("enabled_$uid", enabled).apply()
    }

    override suspend fun load(uid: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val f = file(uid)
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            val array = JSONArray(f.readText())
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                ChatMessage(
                    id = o.getString("id"),
                    role = ChatRole.valueOf(o.getString("role")),
                    text = o.getString("text"),
                    timeMillis = o.getLong("time"),
                    failed = o.optBoolean("failed", false),
                )
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun save(uid: String, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        messages.forEach { m ->
            array.put(
                JSONObject().put("id", m.id).put("role", m.role.name).put("text", m.text)
                    .put("time", m.timeMillis).put("failed", m.failed),
            )
        }
        dir.mkdirs()
        val tmp = File(dir, file(uid).name + ".tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file(uid))) {
            file(uid).writeText(array.toString())
            tmp.delete()
        }
        Unit
    }

    override suspend fun clear(uid: String) = withContext(Dispatchers.IO) {
        file(uid).delete()
        Unit
    }

    override fun clearAll() {
        dir.deleteRecursively()
        prefs.edit().clear().apply()
    }
}
