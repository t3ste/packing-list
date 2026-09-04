package com.t3ste.packinglist

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// Persists a link to a single user-chosen document across app restarts, via
// Android's Storage Access Framework (SAF). This is the native-app equivalent
// of the File System Access API used on desktop browsers by the web version —
// that browser API doesn't exist in an Android WebView, so this plugin fills
// the same role: "remember one file, reuse it until the user picks another."
@CapacitorPlugin(name = "FileLink")
class FileLinkPlugin : Plugin() {
    private val prefsName = "file_link_prefs"
    private val keyUri = "linked_uri"
    private val keyName = "linked_name"

    private fun prefs(): SharedPreferences =
        context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)

    private fun displayNameFor(uri: Uri): String {
        var name: String? = null
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        } catch (e: Exception) {
            // Fall through to the path-based fallback below.
        } finally {
            cursor?.close()
        }
        return name ?: (uri.lastPathSegment ?: "file")
    }

    @PluginMethod
    fun hasLinkedFile(call: PluginCall) {
        val uriString = prefs().getString(keyUri, null)
        val result = JSObject()
        if (uriString == null) {
            result.put("linked", false)
        } else {
            result.put("linked", true)
            result.put("name", prefs().getString(keyName, "file"))
        }
        call.resolve(result)
    }

    @PluginMethod
    fun getLinkedFileName(call: PluginCall) {
        val result = JSObject()
        result.put("name", prefs().getString(keyName, null))
        call.resolve(result)
    }

    @PluginMethod
    fun createFile(call: PluginCall) {
        val suggestedName = call.getString("suggestedName", "packing_list.json") ?: "packing_list.json"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        saveCall(call)
        startActivityForResult(call, intent, "handleCreateResult")
    }

    @ActivityCallback
    private fun handleCreateResult(call: PluginCall?, result: androidx.activity.result.ActivityResult) {
        if (call == null) return
        if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) {
            call.reject("User cancelled")
            return
        }
        val uri = result.data!!.data!!
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            call.reject("Could not obtain persistent access to the chosen file: " + e.message)
            return
        }
        val name = displayNameFor(uri)
        prefs().edit().putString(keyUri, uri.toString()).putString(keyName, name).apply()
        val ret = JSObject()
        ret.put("name", name)
        call.resolve(ret)
    }

    @PluginMethod
    fun openFile(call: PluginCall) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
        }
        saveCall(call)
        startActivityForResult(call, intent, "handleOpenResult")
    }

    @ActivityCallback
    private fun handleOpenResult(call: PluginCall?, result: androidx.activity.result.ActivityResult) {
        if (call == null) return
        if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) {
            call.reject("User cancelled")
            return
        }
        val uri = result.data!!.data!!
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            call.reject("Could not obtain persistent access to the chosen file: " + e.message)
            return
        }
        val name = displayNameFor(uri)
        val content = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            } ?: ""
        } catch (e: Exception) {
            call.reject("Could not read the chosen file: " + e.message)
            return
        }
        prefs().edit().putString(keyUri, uri.toString()).putString(keyName, name).apply()
        val ret = JSObject()
        ret.put("name", name)
        ret.put("content", content)
        call.resolve(ret)
    }

    @PluginMethod
    fun readLinkedFile(call: PluginCall) {
        val uriString = prefs().getString(keyUri, null)
        if (uriString == null) {
            call.reject("No file linked")
            return
        }
        val uri = Uri.parse(uriString)
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            } ?: ""
            val ret = JSObject()
            ret.put("content", content)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("Could not read the linked file: " + e.message)
        }
    }

    @PluginMethod
    fun writeLinkedFile(call: PluginCall) {
        val uriString = prefs().getString(keyUri, null)
        if (uriString == null) {
            call.reject("No file linked")
            return
        }
        val content = call.getString("content", "") ?: ""
        val uri = Uri.parse(uriString)
        try {
            // "wt" truncates before writing, so the file always ends up exactly matching
            // the new content (no leftover bytes from a previous, longer save).
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(content) }
            } ?: throw Exception("Could not open output stream")
            call.resolve()
        } catch (e: Exception) {
            call.reject("Could not write the linked file: " + e.message)
        }
    }

    @PluginMethod
    fun clearLinkedFile(call: PluginCall) {
        prefs().edit().remove(keyUri).remove(keyName).apply()
        call.resolve()
    }
}
