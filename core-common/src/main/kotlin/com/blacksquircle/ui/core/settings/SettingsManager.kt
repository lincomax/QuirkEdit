/*
 * Copyright Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blacksquircle.ui.core.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

typealias OnChangedListener = () -> Unit

class SettingsManager(private val context: Context) {

    companion object {

        private const val EXPORT_FORMAT = "quirkedit-settings"
        private val NOT_EXPORTED by lazy {
            setOf(KEY_SELECTED_DOCUMENT_ID, KEY_WORKSPACE, KEY_GIT_CREDENTIALS_PASSWORD)
        }

        // Look And Feel
        const val KEY_EDITOR_THEME = "editor_theme_internal"
        const val KEY_FULLSCREEN_MODE = "fullscreen_mode"

        // Other
        const val KEY_CONFIRM_EXIT = "confirm_exit"

        // Font
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_FONT_TYPE = "font_type_internal"

        // Editor
        const val KEY_WORD_WRAP = "word_wrap"
        const val KEY_STICKY_SCROLL = "sticky_scroll"
        const val KEY_CODE_COMPLETION = "code_completion"
        const val KEY_PINCH_ZOOM = "pinch_zoom"
        const val KEY_LINE_NUMBERS = "line_numbers"
        const val KEY_HIGHLIGHT_CURRENT_LINE = "highlight_current_line"
        const val KEY_HIGHLIGHT_MATCHING_DELIMITERS = "highlight_matching_delimiters"
        const val KEY_HIGHLIGHT_CODE_BLOCKS = "highlight_code_blocks"
        const val KEY_SHOW_INVISIBLE_CHARS = "show_invisible_chars"
        const val KEY_READ_ONLY = "read_only"

        // Tabs
        const val KEY_SELECTED_DOCUMENT_ID = "selected_document_id"
        const val KEY_AUTO_SAVE_FILES = "auto_save_files"

        // Keyboard
        const val KEY_USE_EXTENDED_KEYBOARD = "use_extended_keyboard"
        const val KEY_MINI_TOOLBAR = "mini_toolbar"
        const val KEY_MINI_TOOLBAR_ITEMS = "mini_toolbar_items"
        const val KEY_MAGNIFIER = "magnifier"
        const val KEY_CURSOR_HAPTICS = "cursor_haptics"
        const val KEY_HIGHLIGHT_COLOR = "highlight_color"
        const val KEY_KEYBOARD_PRESET = "keyboard_preset"
        const val KEY_SOFT_KEYBOARD = "soft_keyboard"

        // Code Style
        const val KEY_AUTO_INDENTATION = "auto_indentation"
        const val KEY_AUTO_CLOSE_PAIRS = "auto_close_pairs"

        // Tab Options
        const val KEY_USE_SPACES_NOT_TABS = "use_spaces_not_tabs"
        const val KEY_TAB_WIDTH = "tab_width"

        // Encoding
        const val KEY_ENCODING_AUTO_DETECT = "encoding_auto_detect"
        const val KEY_ENCODING_FOR_OPENING = "encoding_for_opening"
        const val KEY_ENCODING_FOR_SAVING = "encoding_for_saving"

        // Linebreaks
        const val KEY_LINEBREAK_FOR_SAVING = "linebreak_for_saving"

        // File Explorer
        const val KEY_WORKSPACE = "workspace"
        const val KEY_SHOW_HIDDEN_FILES = "show_hidden_files"
        const val KEY_COMPACT_PACKAGES = "compact_packages"
        const val KEY_SEARCH_ALL_STORAGE = "search_all_storage"
        const val KEY_FOLDERS_ON_TOP = "folders_on_top"
        const val KEY_SORT_MODE = "sort_mode"

        // Terminal
        const val KEY_TERMINAL_RUNTIME = "terminal_runtime"
        const val KEY_TERMINAL_CURSOR_BLINKING = "terminal_cursor_blinking"
        const val KEY_TERMINAL_KEEP_SCREEN_ON = "terminal_keep_screen_on"

        // Git
        const val KEY_GIT_CREDENTIALS_USERNAME = "git_credentials_username"
        const val KEY_GIT_CREDENTIALS_PASSWORD = "git_credentials_password"
        const val KEY_GIT_USER_EMAIL = "git_user_email"
        const val KEY_GIT_USER_NAME = "git_user_name"
        const val KEY_GIT_SUBMODULES = "git_submodules"
        const val KEY_GIT_RECURSIVE_SUBMODULES = "git_recursive_submodules"
    }

    private val fileName: String
        get() = context.packageName + "_preferences"

    private var _sharedPreferences: SharedPreferences? = null
    private val sharedPreferences: SharedPreferences
        get() = _sharedPreferences ?: context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .also { _sharedPreferences = it }

    /** Writes every setting except session state and the git password as JSON */
    fun exportSettings(uri: Uri) {
        val values = JSONObject()
        for ((key, value) in sharedPreferences.all) {
            if (key in NOT_EXPORTED || value == null) continue
            val entry = JSONObject()
            when (value) {
                is Boolean -> entry.put("type", "boolean").put("value", value)
                is Int -> entry.put("type", "int").put("value", value)
                is Long -> entry.put("type", "long").put("value", value)
                is Float -> entry.put("type", "float").put("value", value.toDouble())
                is String -> entry.put("type", "string").put("value", value)
                is Set<*> -> entry.put("type", "set").put("value", JSONArray(value.map { it.toString() }))
                else -> continue
            }
            values.put(key, entry)
        }
        val json = JSONObject()
            .put("format", EXPORT_FORMAT)
            .put("version", 1)
            .put("settings", values)
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(json.toString(2))
        } ?: throw IllegalStateException("Can't open $uri")
    }

    /** Applies settings from a file made by [exportSettings], returns how many were applied */
    fun importSettings(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Can't open $uri")
        val json = JSONObject(text)
        require(json.optString("format") == EXPORT_FORMAT) { "Not a settings file" }
        val values = json.getJSONObject("settings")
        val editor = sharedPreferences.edit()
        var count = 0
        for (key in values.keys()) {
            if (key in NOT_EXPORTED) continue
            val entry = values.getJSONObject(key)
            when (entry.getString("type")) {
                "boolean" -> editor.putBoolean(key, entry.getBoolean("value"))
                "int" -> editor.putInt(key, entry.getInt("value"))
                "long" -> editor.putLong(key, entry.getLong("value"))
                "float" -> editor.putFloat(key, entry.getDouble("value").toFloat())
                "string" -> editor.putString(key, entry.getString("value"))
                "set" -> {
                    val array = entry.getJSONArray("value")
                    editor.putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                }
                else -> continue
            }
            count++
        }
        editor.commit()
        return count
    }

    var searchAllStorage: Boolean
        get() = sharedPreferences.getBoolean(KEY_SEARCH_ALL_STORAGE, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SEARCH_ALL_STORAGE, value).apply()

    var editorTheme: String
        get() = sharedPreferences.getString(KEY_EDITOR_THEME, "squick_dark") ?: "squick_dark"
        set(value) = sharedPreferences.edit().putString(KEY_EDITOR_THEME, value).apply()
    var fullScreenMode: Boolean
        get() = sharedPreferences.getBoolean(KEY_FULLSCREEN_MODE, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FULLSCREEN_MODE, value).apply()

    var confirmExit: Boolean
        get() = sharedPreferences.getBoolean(KEY_CONFIRM_EXIT, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_CONFIRM_EXIT, value).apply()

    var fontSize: Int
        get() = sharedPreferences.getInt(KEY_FONT_SIZE, 16)
        set(value) = sharedPreferences.edit().putInt(KEY_FONT_SIZE, value).apply()
    var fontType: String
        get() = sharedPreferences.getString(KEY_FONT_TYPE, "system_default") ?: "system_default"
        set(value) = sharedPreferences.edit().putString(KEY_FONT_TYPE, value).apply()

    var wordWrap: Boolean
        get() = sharedPreferences.getBoolean(KEY_WORD_WRAP, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_WORD_WRAP, value).apply()
    var stickyScroll: Boolean
        get() = sharedPreferences.getBoolean(KEY_STICKY_SCROLL, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_STICKY_SCROLL, value).apply()
    var codeCompletion: Boolean
        get() = sharedPreferences.getBoolean(KEY_CODE_COMPLETION, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_CODE_COMPLETION, value).apply()
    var pinchZoom: Boolean
        get() = sharedPreferences.getBoolean(KEY_PINCH_ZOOM, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_PINCH_ZOOM, value).apply()
    var lineNumbers: Boolean
        get() = sharedPreferences.getBoolean(KEY_LINE_NUMBERS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_LINE_NUMBERS, value).apply()
    var highlightCurrentLine: Boolean
        get() = sharedPreferences.getBoolean(KEY_HIGHLIGHT_CURRENT_LINE, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_HIGHLIGHT_CURRENT_LINE, value).apply()
    var highlightMatchingDelimiters: Boolean
        get() = sharedPreferences.getBoolean(KEY_HIGHLIGHT_MATCHING_DELIMITERS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_HIGHLIGHT_MATCHING_DELIMITERS, value).apply()
    var highlightCodeBlocks: Boolean
        get() = sharedPreferences.getBoolean(KEY_HIGHLIGHT_CODE_BLOCKS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_HIGHLIGHT_CODE_BLOCKS, value).apply()
    var showInvisibleChars: Boolean
        get() = sharedPreferences.getBoolean(KEY_SHOW_INVISIBLE_CHARS, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SHOW_INVISIBLE_CHARS, value).apply()
    var readOnly: Boolean
        get() = sharedPreferences.getBoolean(KEY_READ_ONLY, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_READ_ONLY, value).apply()

    var selectedUuid: String
        get() = sharedPreferences.getString(KEY_SELECTED_DOCUMENT_ID, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_SELECTED_DOCUMENT_ID, value).apply()
    var autoSaveFiles: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_SAVE_FILES, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_SAVE_FILES, value).apply()

    var extendedKeyboard: Boolean
        get() = sharedPreferences.getBoolean(KEY_USE_EXTENDED_KEYBOARD, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_USE_EXTENDED_KEYBOARD, value).apply()
    var cursorHaptics: Boolean
        get() = sharedPreferences.getBoolean(KEY_CURSOR_HAPTICS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_CURSOR_HAPTICS, value).apply()

    var magnifier: Boolean
        get() = sharedPreferences.getBoolean(KEY_MAGNIFIER, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_MAGNIFIER, value).apply()

    /** 0 means "use the theme's highlight color" */
    var highlightColor: Int
        get() = sharedPreferences.getInt(KEY_HIGHLIGHT_COLOR, 0)
        set(value) = sharedPreferences.edit().putInt(KEY_HIGHLIGHT_COLOR, value).apply()
    var miniToolbar: Boolean
        get() = sharedPreferences.getBoolean(KEY_MINI_TOOLBAR, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_MINI_TOOLBAR, value).apply()
    /** Bottom toolbar order and visibility, empty means default */
    var miniToolbarItems: String
        get() = sharedPreferences.getString(KEY_MINI_TOOLBAR_ITEMS, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_MINI_TOOLBAR_ITEMS, value).apply()
    var keyboardPreset: String
        get() = sharedPreferences.getString(KEY_KEYBOARD_PRESET, "{}();,.=|&![]<>+-/*?:_") ?: "{}();,.=|&![]<>+-/*?:_"
        set(value) = sharedPreferences.edit().putString(KEY_KEYBOARD_PRESET, value).apply()
    var softKeyboard: Boolean
        get() = sharedPreferences.getBoolean(KEY_SOFT_KEYBOARD, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SOFT_KEYBOARD, value).apply()

    var autoIndentation: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_INDENTATION, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_INDENTATION, value).apply()
    var autoClosePairs: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_CLOSE_PAIRS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_CLOSE_PAIRS, value).apply()

    var useSpacesInsteadOfTabs: Boolean
        get() = sharedPreferences.getBoolean(KEY_USE_SPACES_NOT_TABS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_USE_SPACES_NOT_TABS, value).apply()
    var tabWidth: Int
        get() = sharedPreferences.getInt(KEY_TAB_WIDTH, 4)
        set(value) = sharedPreferences.edit().putInt(KEY_TAB_WIDTH, value).apply()

    var encodingAutoDetect: Boolean
        get() = sharedPreferences.getBoolean(KEY_ENCODING_AUTO_DETECT, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_ENCODING_AUTO_DETECT, value).apply()
    var encodingForOpening: String
        get() = sharedPreferences.getString(KEY_ENCODING_FOR_OPENING, "UTF-8") ?: "UTF-8"
        set(value) = sharedPreferences.edit().putString(KEY_ENCODING_FOR_OPENING, value).apply()
    var encodingForSaving: String
        get() = sharedPreferences.getString(KEY_ENCODING_FOR_SAVING, "UTF-8") ?: "UTF-8"
        set(value) = sharedPreferences.edit().putString(KEY_ENCODING_FOR_SAVING, value).apply()

    var lineBreakForSaving: String
        get() = sharedPreferences.getString(KEY_LINEBREAK_FOR_SAVING, "lf") ?: "lf"
        set(value) = sharedPreferences.edit().putString(KEY_LINEBREAK_FOR_SAVING, value).apply()

    var showHidden: Boolean
        get() = sharedPreferences.getBoolean(KEY_SHOW_HIDDEN_FILES, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SHOW_HIDDEN_FILES, value).apply()
    var compactPackages: Boolean
        get() = sharedPreferences.getBoolean(KEY_COMPACT_PACKAGES, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_COMPACT_PACKAGES, value).apply()
    var foldersOnTop: Boolean
        get() = sharedPreferences.getBoolean(KEY_FOLDERS_ON_TOP, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FOLDERS_ON_TOP, value).apply()
    var sortMode: String
        get() = sharedPreferences.getString(KEY_SORT_MODE, "sort_by_name") ?: "sort_by_name"
        set(value) = sharedPreferences.edit().putString(KEY_SORT_MODE, value).apply()
    var workspace: String
        get() = sharedPreferences.getString(KEY_WORKSPACE, "local") ?: "local"
        set(value) = sharedPreferences.edit().putString(KEY_WORKSPACE, value).apply()

    var terminalRuntime: String
        get() = sharedPreferences.getString(KEY_TERMINAL_RUNTIME, "android") ?: "android"
        set(value) = sharedPreferences.edit().putString(KEY_TERMINAL_RUNTIME, value).apply()
    var cursorBlinking: Boolean
        get() = sharedPreferences.getBoolean(KEY_TERMINAL_CURSOR_BLINKING, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_TERMINAL_CURSOR_BLINKING, value).apply()
    var keepScreenOn: Boolean
        get() = sharedPreferences.getBoolean(KEY_TERMINAL_KEEP_SCREEN_ON, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_TERMINAL_KEEP_SCREEN_ON, value).apply()

    var gitCredentialsUsername: String
        get() = sharedPreferences.getString(KEY_GIT_CREDENTIALS_USERNAME, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_GIT_CREDENTIALS_USERNAME, value).apply()
    var gitCredentialsPassword: String
        get() = sharedPreferences.getString(KEY_GIT_CREDENTIALS_PASSWORD, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_GIT_CREDENTIALS_PASSWORD, value).apply()
    var gitUserEmail: String
        get() = sharedPreferences.getString(KEY_GIT_USER_EMAIL, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_GIT_USER_EMAIL, value).apply()
    var gitUserName: String
        get() = sharedPreferences.getString(KEY_GIT_USER_NAME, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_GIT_USER_NAME, value).apply()
    var gitSubmodules: Boolean
        get() = sharedPreferences.getBoolean(KEY_GIT_SUBMODULES, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_GIT_SUBMODULES, value).apply()
    var gitRecursiveSubmodules: Boolean
        get() = sharedPreferences.getBoolean(KEY_GIT_RECURSIVE_SUBMODULES, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_GIT_RECURSIVE_SUBMODULES, value).apply()

    private val listeners = HashMap<String, OnChangedListener>()
    private val callback = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in listeners.keys) {
            listeners[key]?.invoke()
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(callback)
    }

    fun load(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun update(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun registerListener(key: String, onValueChanged: OnChangedListener) {
        listeners[key] = onValueChanged
    }

    fun unregisterListener(key: String) {
        listeners.remove(key)
    }
}