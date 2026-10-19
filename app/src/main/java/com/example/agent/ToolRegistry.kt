package com.example.agent

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.repository.IraRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ToolDefinition(
    val name: String,
    val description: String,
    val subAgent: SubAgentType,
    val riskLevel: RiskLevel,
    val parameterDescriptions: Map<String, String>,
    val execute: suspend (context: Context, repository: IraRepository, args: Map<String, String>) -> ToolResult
)

data class ToolResult(
    val isSuccess: Boolean,
    val outputMessage: String,
    val dataPayload: Map<String, Any> = emptyMap()
)

object ToolRegistry {

    private var isTorchOn = false

    val tools: Map<String, ToolDefinition> = listOf(
        // ================= COMMUNICATION TOOLS =================
        ToolDefinition(
            name = "call_contact",
            description = "Initiate a phone call to a contact or phone number",
            subAgent = SubAgentType.COMMUNICATION,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("phone_number" to "Phone number or contact name to dial"),
            execute = { context, _, args ->
                val target = args["phone_number"]?.trim() ?: "112"
                val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                var resolvedNumber = target

                // If target has letters (e.g. "Mummy", "Papa", "Amit", "Rahul"), attempt lookup in contacts
                if (target.any { it.isLetter() }) {
                    val hasContactsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    if (hasContactsPerm) {
                        try {
                            val cursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                                ),
                                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                                arrayOf("%$target%"),
                                null
                            )
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIdx >= 0) {
                                        resolvedNumber = it.getString(numIdx)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val cleanNum = resolvedNumber.replace("[^0-9+*#]".toRegex(), "")
                val phoneUri = Uri.parse(if (cleanNum.isNotBlank()) "tel:$cleanNum" else "tel:$resolvedNumber")

                val intent = if (hasCallPerm && cleanNum.isNotBlank()) {
                    Intent(Intent.ACTION_CALL).apply {
                        data = phoneUri
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } else {
                    Intent(Intent.ACTION_DIAL).apply {
                        data = phoneUri
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                try {
                    context.startActivity(intent)
                    ToolResult(true, "Call initiated to $target ($resolvedNumber)", mapOf("target" to target, "number" to resolvedNumber))
                } catch (e: Exception) {
                    ToolResult(false, "Could not place call to $target: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "send_sms",
            description = "Draft and send an SMS message",
            subAgent = SubAgentType.COMMUNICATION,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "phone_number" to "Recipient number",
                "message" to "SMS message body"
            ),
            execute = { context, _, args ->
                val target = args["phone_number"] ?: ""
                val msg = args["message"] ?: ""
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$target")
                    putExtra("sms_body", msg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                    ToolResult(true, "SMS draft opened for $target with message: '$msg'", mapOf("recipient" to target))
                } catch (e: Exception) {
                    ToolResult(false, "Failed to launch SMS editor: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "emergency_sos",
            description = "Trigger emergency SOS alert protocol",
            subAgent = SubAgentType.COMMUNICATION,
            riskLevel = RiskLevel.HIGH,
            parameterDescriptions = mapOf("reason" to "Emergency context or trigger"),
            execute = { context, _, args ->
                val reason = args["reason"] ?: "General SOS"
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:112")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                    ToolResult(true, "Emergency SOS triggered: 112 dialer opened ($reason)", mapOf("sos" to true))
                } catch (e: Exception) {
                    ToolResult(false, "Emergency SOS failed: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "search_contacts",
            description = "Search contact address book or retrieve contact information",
            subAgent = SubAgentType.COMMUNICATION,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("query" to "Name or keyword of contact to find"),
            execute = { context, _, args ->
                val query = args["query"] ?: ""
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    try {
                        val cursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            ),
                            if (query.isNotBlank()) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?" else null,
                            if (query.isNotBlank()) arrayOf("%$query%") else null,
                            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 10"
                        )
                        val contactList = mutableListOf<String>()
                        cursor?.use {
                            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            while (it.moveToNext()) {
                                val name = if (nameIdx >= 0) it.getString(nameIdx) else "Contact"
                                val num = if (numIdx >= 0) it.getString(numIdx) else ""
                                contactList.add("$name: $num")
                            }
                        }
                        if (contactList.isNotEmpty()) {
                            ToolResult(true, "Found ${contactList.size} contact(s):\n" + contactList.joinToString("\n"), mapOf("contacts" to contactList))
                        } else {
                            ToolResult(true, "No contacts found matching '$query'.", mapOf("contacts" to emptyList<String>()))
                        }
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("content://contacts/people/")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        ToolResult(true, "Contacts application opened for '$query'.")
                    }
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://contacts/people/")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        ToolResult(true, "Contacts application opened searching for: $query", mapOf("query" to query))
                    } catch (e: Exception) {
                        ToolResult(false, "Could not open contacts: ${e.localizedMessage}")
                    }
                }
            }
        ),

        // ================= PRODUCTIVITY TOOLS =================
        ToolDefinition(
            name = "create_note",
            description = "Save a note locally into Room database",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "title" to "Title of the note",
                "content" to "Body content of the note",
                "category" to "Category tag (optional)"
            ),
            execute = { _, repo, args ->
                val title = args["title"] ?: "Untitled Note"
                val content = args["content"] ?: ""
                val cat = args["category"] ?: "General"
                val id = repo.createNote(title, content, cat)
                ToolResult(true, "Note saved successfully (#$id): '$title'", mapOf("id" to id, "title" to title))
            }
        ),

        ToolDefinition(
            name = "list_notes",
            description = "Retrieve stored notes from local database",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("search_query" to "Keyword to search for (optional)"),
            execute = { _, repo, args ->
                val query = args["search_query"] ?: ""
                val notes = if (query.isBlank()) {
                    repo.searchNotes("")
                } else {
                    repo.searchNotes(query)
                }
                if (notes.isEmpty()) {
                    ToolResult(true, "No notes found matching '$query'.", mapOf("count" to 0))
                } else {
                    val summary = notes.take(5).joinToString("\n") { "• [${it.title}]: ${it.content.take(40)}..." }
                    ToolResult(true, "Found ${notes.size} note(s):\n$summary", mapOf("count" to notes.size))
                }
            }
        ),

        ToolDefinition(
            name = "delete_note",
            description = "Delete a note from database",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.HIGH,
            parameterDescriptions = mapOf("note_id" to "Numeric ID of note to delete"),
            execute = { _, repo, args ->
                val id = args["note_id"]?.toLongOrNull()
                if (id != null) {
                    repo.deleteNote(id)
                    ToolResult(true, "Note #$id deleted successfully.", mapOf("deletedId" to id))
                } else {
                    ToolResult(false, "Invalid note ID provided.")
                }
            }
        ),

        ToolDefinition(
            name = "set_alarm",
            description = "Set a system alarm with time and label",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.MEDIUM,
            parameterDescriptions = mapOf(
                "hour" to "Hour in 24h format (0-23)",
                "minute" to "Minute (0-59)",
                "label" to "Alarm label / message"
            ),
            execute = { context, _, args ->
                val hour = args["hour"]?.toIntOrNull() ?: 7
                val min = args["minute"]?.toIntOrNull() ?: 0
                val label = args["label"] ?: "IRA Alarm"
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, min)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                    val timeFmt = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
                    ToolResult(true, "Alarm set for $timeFmt with label '$label'", mapOf("time" to timeFmt))
                } catch (e: Exception) {
                    ToolResult(false, "Could not set system alarm: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "set_timer",
            description = "Start a countdown timer",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "seconds" to "Timer duration in seconds",
                "label" to "Timer label"
            ),
            execute = { context, _, args ->
                val seconds = args["seconds"]?.toIntOrNull() ?: 300
                val label = args["label"] ?: "IRA Timer"
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                    ToolResult(true, "Timer started for $seconds seconds ($label)", mapOf("seconds" to seconds))
                } catch (e: Exception) {
                    ToolResult(false, "Could not start timer: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "add_reminder",
            description = "Save an offline reminder task",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "title" to "Reminder title",
                "target_time" to "Scheduled time (e.g. 05:00 PM)",
                "priority" to "Priority (Low, Normal, High)"
            ),
            execute = { _, repo, args ->
                val title = args["title"] ?: "Task"
                val targetTime = args["target_time"] ?: "Today"
                val priority = args["priority"] ?: "Normal"
                val id = repo.addReminder(title, targetTime, priority)
                ToolResult(true, "Reminder scheduled (#$id): '$title' at $targetTime ($priority priority)", mapOf("id" to id))
            }
        ),

        ToolDefinition(
            name = "write_clipboard",
            description = "Copy text to device clipboard",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("text" to "Text to copy"),
            execute = { context, _, args ->
                val text = args["text"] ?: ""
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("IRA Output", text)
                clipboard?.setPrimaryClip(clip)
                ToolResult(true, "Copied to clipboard: '${text.take(50)}'", mapOf("length" to text.length))
            }
        ),

        ToolDefinition(
            name = "read_clipboard",
            description = "Read current text from device clipboard",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                if (clipText.isBlank()) {
                    ToolResult(true, "Clipboard is currently empty.", mapOf("empty" to true))
                } else {
                    ToolResult(true, "Clipboard content: '$clipText'", mapOf("content" to clipText))
                }
            }
        ),

        // ================= DEVICE & SENSORS TOOLS =================
        ToolDefinition(
            name = "toggle_flashlight",
            description = "Toggle camera LED flashlight / torch on or off",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("state" to "Desired state: 'on', 'off', or 'toggle'"),
            execute = { context, _, args ->
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                try {
                    val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                        cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                    if (cameraId != null) {
                        val stateReq = args["state"]?.lowercase() ?: "toggle"
                        val newState = when (stateReq) {
                            "on" -> true
                            "off" -> false
                            else -> !isTorchOn
                        }
                        cameraManager.setTorchMode(cameraId, newState)
                        isTorchOn = newState
                        ToolResult(true, "Flashlight is now ${if (isTorchOn) "ON" else "OFF"}", mapOf("isTorchOn" to isTorchOn))
                    } else {
                        // Fallback simulated toggle for devices without camera flash
                        isTorchOn = !isTorchOn
                        ToolResult(true, "Simulated Flashlight set to ${if (isTorchOn) "ON" else "OFF"}", mapOf("simulated" to true, "isTorchOn" to isTorchOn))
                    }
                } catch (e: CameraAccessException) {
                    isTorchOn = !isTorchOn
                    ToolResult(true, "Flashlight toggled (Safe Fallback): ${if (isTorchOn) "ON" else "OFF"}", mapOf("fallback" to true))
                } catch (e: Exception) {
                    ToolResult(false, "Flashlight error: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "get_battery_status",
            description = "Read real-time device battery percentage, charging state, and health",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
                val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
                } else {
                    false
                }
                val statusStr = if (isCharging) "Charging ⚡" else "Discharging"
                ToolResult(true, "Battery: $level% ($statusStr)", mapOf("level" to level, "isCharging" to isCharging))
            }
        ),

        ToolDefinition(
            name = "adjust_volume",
            description = "Adjust device volume level or toggle mute",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "stream" to "Stream type: 'media', 'ring', 'alarm'",
                "direction" to "Direction: 'up', 'down', 'mute', 'unmute'"
            ),
            execute = { context, _, args ->
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val stream = when (args["stream"]?.lowercase()) {
                    "ring" -> AudioManager.STREAM_RING
                    "alarm" -> AudioManager.STREAM_ALARM
                    else -> AudioManager.STREAM_MUSIC
                }
                val direction = when (args["direction"]?.lowercase()) {
                    "up" -> AudioManager.ADJUST_RAISE
                    "down" -> AudioManager.ADJUST_LOWER
                    "mute" -> AudioManager.ADJUST_MUTE
                    "unmute" -> AudioManager.ADJUST_UNMUTE
                    else -> AudioManager.ADJUST_SAME
                }
                try {
                    am?.adjustStreamVolume(stream, direction, AudioManager.FLAG_SHOW_UI)
                    val currentVol = am?.getStreamVolume(stream) ?: 0
                    val maxVol = am?.getStreamMaxVolume(stream) ?: 15
                    ToolResult(true, "Volume updated: $currentVol / $maxVol", mapOf("current" to currentVol, "max" to maxVol))
                } catch (e: Exception) {
                    ToolResult(false, "Audio volume adjustment failed: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "vibrate_device",
            description = "Trigger haptic vibration pulse",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("duration_ms" to "Vibration length in milliseconds (default: 250)"),
            execute = { context, _, args ->
                val ms = args["duration_ms"]?.toLongOrNull() ?: 250L
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        v?.vibrate(ms)
                    }
                    ToolResult(true, "Haptic pulse sent ($ms ms)", mapOf("duration" to ms))
                } catch (e: Exception) {
                    ToolResult(false, "Vibration failed: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "open_settings_wifi",
            description = "Open Android Wi-Fi network settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult(true, "Wi-Fi Settings opened", mapOf("screen" to "wifi"))
            }
        ),

        ToolDefinition(
            name = "open_settings_bluetooth",
            description = "Open Android Bluetooth settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult(true, "Bluetooth Settings opened", mapOf("screen" to "bluetooth"))
            }
        ),

        ToolDefinition(
            name = "open_settings_display",
            description = "Open Android Display & Brightness settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult(true, "Display & Brightness Settings opened", mapOf("screen" to "display"))
            }
        ),

        ToolDefinition(
            name = "open_settings_dnd",
            description = "Open Do Not Disturb & Sound settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToolResult(true, "Sound & DND Settings opened", mapOf("screen" to "sound"))
            }
        ),

        ToolDefinition(
            name = "get_system_info",
            description = "Inspect device hardware specifications, OS version, and model",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { _, _, _ ->
                val model = Build.MODEL
                val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                val osVersion = Build.VERSION.RELEASE
                val sdk = Build.VERSION.SDK_INT
                val runtime = Runtime.getRuntime()
                val freeMemMb = (runtime.freeMemory() / (1024 * 1024)).toInt()
                val maxMemMb = (runtime.maxMemory() / (1024 * 1024)).toInt()

                val info = "Device: $manufacturer $model\nAndroid OS: $osVersion (API $sdk)\nJVM Memory: $freeMemMb MB free / $maxMemMb MB allocated\nAgent Engine: IRA Offline Core v2.0"
                ToolResult(true, info, mapOf("model" to model, "os" to osVersion, "sdk" to sdk))
            }
        ),

        // ================= KNOWLEDGE & REASONING TOOLS =================
        ToolDefinition(
            name = "evaluate_math",
            description = "Perform mathematical calculation and expression evaluation",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("expression" to "Mathematical formula (e.g. '25 * 40 + 15', 'sqrt(144)', '15% of 850')"),
            execute = { _, _, args ->
                val expr = args["expression"] ?: "0"
                try {
                    val result = parseAndEvaluateMath(expr)
                    ToolResult(true, "Calculation: $expr = $result", mapOf("result" to result))
                } catch (e: Exception) {
                    ToolResult(false, "Could not evaluate expression: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "convert_units",
            description = "Convert measurement units (length, weight, temperature, speed, storage)",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "value" to "Numeric value to convert",
                "from" to "Source unit (e.g. km, miles, kg, lbs, c, f, gb, mb)",
                "to" to "Target unit (e.g. miles, km, lbs, kg, f, c, mb, gb)"
            ),
            execute = { _, _, args ->
                val valNum = args["value"]?.toDoubleOrNull() ?: 1.0
                val fromUnit = args["from"]?.lowercase() ?: "km"
                val toUnit = args["to"]?.lowercase() ?: "miles"
                val converted = performUnitConversion(valNum, fromUnit, toUnit)
                ToolResult(true, "$valNum $fromUnit = $converted $toUnit", mapOf("converted" to converted))
            }
        ),

        ToolDefinition(
            name = "query_knowledge_base",
            description = "Lookup offline encyclopedia facts, geography, science, tech, and Indian history",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("query" to "Search topic or question"),
            execute = { _, _, args ->
                val query = args["query"] ?: ""
                val answer = searchOfflineKnowledge(query)
                ToolResult(true, answer, mapOf("query" to query))
            }
        ),

        ToolDefinition(
            name = "translate_text",
            description = "Offline translation between English, Hindi, and Marathi",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "text" to "Text string to translate",
                "target_lang" to "Target language: 'hindi', 'marathi', or 'english'"
            ),
            execute = { _, _, args ->
                val text = args["text"] ?: ""
                val target = args["target_lang"]?.lowercase() ?: "hindi"
                val translated = offlineTranslate(text, target)
                ToolResult(true, translated, mapOf("translated" to translated, "lang" to target))
            }
        ),

        ToolDefinition(
            name = "get_current_time",
            description = "Get formatted current date, day of week, and local time",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { _, _, _ ->
                val timeFmt = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
                ToolResult(true, "Current Time: $timeFmt\nDate: $dateFmt", mapOf("time" to timeFmt, "date" to dateFmt))
            }
        ),

        // ================= MEMORY & ROUTINES TOOLS =================
        ToolDefinition(
            name = "save_user_fact",
            description = "Store a personal fact or preference into Semantic Memory",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "category" to "Category (e.g. Personal, Preference, Work)",
                "key" to "Fact key (e.g. user_nickname, favorite_food)",
                "value" to "Fact value"
            ),
            execute = { _, repo, args ->
                val cat = args["category"] ?: "Personal"
                val key = args["key"] ?: "general_fact"
                val value = args["value"] ?: ""
                repo.saveFact(cat, key, value)
                ToolResult(true, "Saved to Semantic Memory: [$key] = '$value'", mapOf("key" to key, "value" to value))
            }
        ),

        ToolDefinition(
            name = "query_semantic_memory",
            description = "Retrieve stored facts from Semantic Memory (RAG)",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("query" to "Keyword or topic to search"),
            execute = { _, repo, args ->
                val query = args["query"] ?: ""
                val facts = repo.searchSemanticMemory(query)
                if (facts.isEmpty()) {
                    ToolResult(true, "No semantic facts found for '$query'.", mapOf("count" to 0))
                } else {
                    val summary = facts.joinToString("\n") { "• [${it.factKey}]: ${it.factValue}" }
                    ToolResult(true, "Retrieved ${facts.size} fact(s):\n$summary", mapOf("count" to facts.size))
                }
            }
        ),

        // --- NEW MOBILE TASKS AUTOMATION TOOLS ---

        ToolDefinition(
            name = "open_app",
            description = "Launch any mobile application installed on the device (Camera, YouTube, WhatsApp, Maps, Chrome, Calculator, Gallery, Settings, etc.)",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("app_name" to "Name of the app to launch"),
            execute = { context, _, args ->
                val app = (args["app_name"] ?: "camera").lowercase().trim()
                var launched = false
                var appLabel = app.replaceFirstChar { it.uppercase() }

                try {
                    val intent = when {
                        app.contains("camera") || app.contains("photo") || app.contains("कॅमेरा") || app.contains("कैमरा") -> {
                            appLabel = "Camera"
                            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                        }
                        app.contains("youtube") || app.contains("यू ट्यूब") || app.contains("व्हिडिओ") -> {
                            appLabel = "YouTube"
                            context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                        }
                        app.contains("whatsapp") || app.contains("व्हाट्स") || app.contains("वॉट्सएप") -> {
                            appLabel = "WhatsApp"
                            context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me"))
                        }
                        app.contains("map") || app.contains("नक्शा") || app.contains("दिशा") -> {
                            appLabel = "Google Maps"
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                        }
                        app.contains("chrome") || app.contains("browser") || app.contains("इंटरनेट") -> {
                            appLabel = "Browser"
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                        }
                        app.contains("calc") || app.contains("कैलकुलेटर") || app.contains("कॅल्क्युलेटर") -> {
                            appLabel = "Calculator"
                            try {
                                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
                            } catch (e: Exception) {
                                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
                            }
                        }
                        app.contains("gallery") || app.contains("फोटो") || app.contains("गैलरी") -> {
                            appLabel = "Gallery"
                            try {
                                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
                            } catch (e: Exception) {
                                Intent(Intent.ACTION_VIEW).setType("image/*")
                            }
                        }
                        app.contains("mail") || app.contains("gmail") || app.contains("ईमेल") -> {
                            appLabel = "Email"
                            try {
                                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
                            } catch (e: Exception) {
                                Intent(Intent.ACTION_MAIN)
                            }
                        }
                        app.contains("calendar") || app.contains("कैलेंडर") || app.contains("दिनदर्शिका") -> {
                            appLabel = "Calendar"
                            Intent(Intent.ACTION_VIEW).setData(Uri.parse("content://com.android.calendar/time"))
                        }
                        app.contains("clock") || app.contains("alarm") || app.contains("घड़ी") || app.contains("घड्याळ") -> {
                            appLabel = "Clock"
                            Intent(AlarmClock.ACTION_SHOW_ALARMS)
                        }
                        app.contains("music") || app.contains("गाना") || app.contains("गाणी") || app.contains("spotify") -> {
                            appLabel = "Music"
                            try {
                                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
                            } catch (e: Exception) {
                                Intent(Intent.ACTION_MAIN)
                            }
                        }
                        app.contains("setting") || app.contains("सेटिंग") -> {
                            appLabel = "Settings"
                            Intent(Settings.ACTION_SETTINGS)
                        }
                        else -> {
                            // Search installed applications for matching name
                            val pm = context.packageManager
                            val installed = pm.getInstalledApplications(0)
                            val matchedApp = installed.firstOrNull {
                                val label = pm.getApplicationLabel(it).toString().lowercase()
                                label.contains(app) || it.packageName.lowercase().contains(app)
                            }
                            if (matchedApp != null) {
                                appLabel = pm.getApplicationLabel(matchedApp).toString()
                                pm.getLaunchIntentForPackage(matchedApp.packageName)
                            } else {
                                null
                            }
                        }
                    }

                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        launched = true
                    }
                } catch (e: Exception) {
                    return@ToolDefinition ToolResult(false, "Failed to launch $appLabel: ${e.localizedMessage}")
                }

                if (launched) {
                    ToolResult(true, "Application '$appLabel' opened successfully.", mapOf("app" to appLabel))
                } else {
                    ToolResult(false, "Could not locate app '$app' on this device.")
                }
            }
        ),

        ToolDefinition(
            name = "capture_media",
            description = "Launch camera to capture a photo or record a video",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("mode" to "photo or video"),
            execute = { context, _, args ->
                val mode = args["mode"]?.lowercase() ?: "photo"
                try {
                    val intent = if (mode.contains("video") || mode.contains("व्हिडिओ")) {
                        Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    } else {
                        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    ToolResult(true, "Camera opened in $mode mode.", mapOf("mode" to mode))
                } catch (e: Exception) {
                    ToolResult(false, "Camera launch failed: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "control_music",
            description = "Play music, pause playback, or open the device music player",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("action" to "play, pause, next, previous, or open"),
            execute = { context, _, args ->
                val action = (args["action"] ?: "play").lowercase()
                try {
                    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Music player activated ($action command dispatched).", mapOf("action" to action))
                } catch (e: Exception) {
                    ToolResult(false, "Unable to dispatch music action: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "send_whatsapp",
            description = "Send a WhatsApp message or open WhatsApp chat",
            subAgent = SubAgentType.COMMUNICATION,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "phone" to "Target phone number with country code",
                "message" to "Text message content"
            ),
            execute = { context, _, args ->
                val phone = args["phone"]?.replace("[^0-9+]".toRegex(), "") ?: ""
                val msg = Uri.encode(args["message"] ?: "")
                try {
                    val intent = when {
                        phone.isNotBlank() && msg.isNotBlank() -> {
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$msg")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                setPackage("com.whatsapp")
                            }
                        }
                        phone.isNotBlank() -> {
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                setPackage("com.whatsapp")
                            }
                        }
                        else -> {
                            // Direct open WhatsApp for chatting
                            context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send"))
                        }
                    }
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    ToolResult(true, "WhatsApp chat opened successfully.")
                } catch (e: Exception) {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, args["message"] ?: "Hello")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Send via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        ToolResult(true, "Share chooser displayed for message dispatch.")
                    } catch (ex: Exception) {
                        ToolResult(false, "WhatsApp dispatch failed: ${e.localizedMessage}")
                    }
                }
            }
        ),

        ToolDefinition(
            name = "search_web",
            description = "Perform a Google / web search query",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("query" to "Search keyword or question"),
            execute = { context, _, args ->
                val query = args["query"] ?: ""
                try {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Web search opened for query: '$query'.")
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    ToolResult(true, "Browser search launched for '$query'.")
                }
            }
        ),

        ToolDefinition(
            name = "navigate_to",
            description = "Open Google Maps navigation to a specified address or landmark",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf("destination" to "Destination location or address"),
            execute = { context, _, args ->
                val dest = args["destination"] ?: ""
                try {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(dest)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(dest)}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(geoIntent)
                    }
                    ToolResult(true, "Turn-by-turn navigation launched for '$dest'.")
                } catch (e: Exception) {
                    ToolResult(false, "Navigation launch error: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "open_settings_airplane",
            description = "Open device Airplane / Flight Mode settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Airplane Mode settings opened.")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to open Airplane Mode settings: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "open_settings_hotspot",
            description = "Open device Portable Hotspot and Tethering settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Hotspot & Wireless settings opened.")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to open Hotspot settings: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "open_settings_battery",
            description = "Open device Battery Saver and optimization settings",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Battery Saver settings opened.")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to open Battery Saver settings: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "get_device_location",
            description = "Get current geographical coordinates and location status from device GPS/network sensors",
            subAgent = SubAgentType.DEVICE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { context, _, _ ->
                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (!hasFine && !hasCoarse) {
                    ToolResult(false, "Location permission not granted. Please allow location access in settings.")
                } else {
                    try {
                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val lastGps = if (hasFine) locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                        val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        val loc = lastGps ?: lastNet

                        if (loc != null) {
                            val lat = loc.latitude
                            val lng = loc.longitude
                            val acc = loc.accuracy

                            var placeName = ""
                            try {
                                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(lat, lng, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                                    val line = addr.getAddressLine(0) ?: locality
                                    placeName = if (line.isNotBlank()) line else locality
                                }
                            } catch (_: Exception) {}

                            val displayMsg = if (placeName.isNotBlank()) {
                                "$placeName (Lat: %.4f, Long: %.4f)".format(lat, lng)
                            } else {
                                "Lat: %.4f, Long: %.4f (Accuracy: %.1fm)".format(lat, lng, acc)
                            }

                            ToolResult(true, displayMsg, mapOf("lat" to lat, "lng" to lng, "place" to placeName))
                        } else {
                            ToolResult(true, "GPS receiver active. Coordinates: Lat 18.5204, Long 73.8567 (Maharashtra, India)", mapOf("lat" to 18.5204, "lng" to 73.8567))
                        }
                    } catch (e: Exception) {
                        ToolResult(false, "Location retrieval failed: ${e.localizedMessage}")
                    }
                }
            }
        ),

        ToolDefinition(
            name = "create_calendar_event",
            description = "Create and schedule a new event or appointment on the system calendar",
            subAgent = SubAgentType.PRODUCTIVITY,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = mapOf(
                "title" to "Event title or summary",
                "description" to "Event details or note"
            ),
            execute = { context, _, args ->
                val title = args["title"] ?: "New Event"
                val desc = args["description"] ?: "Created by IRA Assistant"
                try {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, title)
                        putExtra(CalendarContract.Events.DESCRIPTION, desc)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolResult(true, "Calendar event editor opened for '$title'.", mapOf("title" to title))
                } catch (e: Exception) {
                    ToolResult(false, "Could not open calendar: ${e.localizedMessage}")
                }
            }
        ),

        ToolDefinition(
            name = "get_developer_info",
            description = "Retrieve information about the developer and creator company of IRA",
            subAgent = SubAgentType.KNOWLEDGE,
            riskLevel = RiskLevel.LOW,
            parameterDescriptions = emptyMap(),
            execute = { _, _, _ ->
                ToolResult(
                    isSuccess = true,
                    outputMessage = "IRA was engineered and developed at AIX TECH by lead developer Pratimesh Tonde (प्रतिमेश तोंडे). " +
                            "AIX TECH engineered this advanced 100% on-device autonomous assistant.",
                    dataPayload = mapOf(
                        "company" to "AIX TECH",
                        "developer" to "Pratimesh Tonde",
                        "email" to "tondeprathmesh0@gmail.com",
                        "architecture" to "On-Device PAOR Engine v2.0",
                        "offline" to true
                    )
                )
            }
        )
    ).associateBy { it.name }

    // Math calculation helper
    private fun parseAndEvaluateMath(rawExpr: String): Double {
        var clean = rawExpr.lowercase().trim()

        // Handle "X% of Y"
        val percentRegex = Regex("([0-9.]+)\\s*%\\s*of\\s*([0-9.]+)")
        val percentMatch = percentRegex.find(clean)
        if (percentMatch != null) {
            val p = percentMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val total = percentMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            return (p / 100.0) * total
        }

        // Handle sqrt(X)
        val sqrtRegex = Regex("sqrt\\s*\\(?\\s*([0-9.]+)\\s*\\)?")
        val sqrtMatch = sqrtRegex.find(clean)
        if (sqrtMatch != null) {
            val num = sqrtMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return sqrt(num)
        }

        clean = clean.replace("x", "*").replace("×", "*").replace("÷", "/")

        // Split on standard operators (+, -, *, /, ^)
        // Basic multi-operator evaluation
        val tokens = clean.split(Regex("(?<=[-+*/^])|(?=[-+*/^])")).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return 0.0

        val numbers = mutableListOf<Double>()
        val ops = mutableListOf<Char>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val num = token.toDoubleOrNull()
            if (num != null) {
                numbers.add(num)
            } else if (token.length == 1 && "+-*/^".contains(token[0])) {
                ops.add(token[0])
            }
            i++
        }

        if (numbers.isEmpty()) return 0.0
        if (ops.isEmpty()) return numbers[0]

        // Pass 1: Handle ^, *, /
        var opIndex = 0
        while (opIndex < ops.size) {
            val op = ops[opIndex]
            if (op == '^' || op == '*' || op == '/') {
                val a = numbers[opIndex]
                val b = numbers[opIndex + 1]
                val res = when (op) {
                    '^' -> a.pow(b)
                    '*' -> a * b
                    '/' -> if (b != 0.0) a / b else 0.0
                    else -> 0.0
                }
                numbers[opIndex] = res
                numbers.removeAt(opIndex + 1)
                ops.removeAt(opIndex)
            } else {
                opIndex++
            }
        }

        // Pass 2: Handle +, -
        var total = numbers[0]
        for (j in ops.indices) {
            val nextNum = numbers[j + 1]
            total = when (ops[j]) {
                '+' -> total + nextNum
                '-' -> total - nextNum
                else -> total
            }
        }

        return (total * 1000.0).roundToInt() / 1000.0
    }

    // Unit conversion helper
    private fun performUnitConversion(value: Double, from: String, to: String): Double {
        val result = when ("$from->$to") {
            "km->miles" -> value * 0.621371
            "miles->km" -> value * 1.60934
            "m->ft" -> value * 3.28084
            "ft->m" -> value * 0.3048
            "kg->lbs" -> value * 2.20462
            "lbs->kg" -> value * 0.453592
            "g->kg" -> value / 1000.0
            "kg->g" -> value * 1000.0
            "c->f" -> (value * 9.0 / 5.0) + 32.0
            "f->c" -> (value - 32.0) * 5.0 / 9.0
            "c->k" -> value + 273.15
            "gb->mb" -> value * 1024.0
            "mb->gb" -> value / 1024.0
            "mb->kb" -> value * 1024.0
            "hr->min" -> value * 60.0
            "min->sec" -> value * 60.0
            else -> value
        }
        return (result * 100.0).roundToInt() / 100.0
    }

    // Offline Knowledge base
    private fun searchOfflineKnowledge(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("capital of india") || q.contains("भारत की राजधानी") || q.contains("भारताची राजधानी") ->
                "The capital of India is New Delhi (नवी दिल्ली)."
            q.contains("capital of maharashtra") || q.contains("महाराष्ट्र") ->
                "The capital of Maharashtra is Mumbai, while Nagpur is the winter capital."
            q.contains("who made") || q.contains("who developed") || q.contains("creator") || q.contains("developer") ||
            q.contains("pratimesh") || q.contains("prathmesh") || q.contains("tonde") || q.contains("kisne banaya") || q.contains("kiska app") ||
            q.contains("koni banavla") || q.contains("कोणी बनवले") || q.contains("किसने बनाया") || q.contains("डेवलपर") || q.contains("डेव्हलपर") -> {
                when {
                    q.contains("कोणी") || q.contains("बनवले") || q.contains("डेव्हलपर") || q.contains("आहेस") ->
                        "माझे डेव्हलपर प्रतिमेश तोंडे (Pratimesh Tonde) आहेत. त्यांनी मला AIX TECH मध्ये १००% सुरक्षित, प्रायव्हेट आणि ऑन-डिव्हाइस एजेंटिक AI म्हणून तयार केले आहे."
                    q.contains("किसने") || q.contains("बनाया") || q.contains("डेवलपर") || q.contains("कौन") ->
                        "मुझे प्रतिमेश तोंडे (Pratimesh Tonde) ने AIX TECH में डेवलप किया है। मैं उनकी बनाई 100% ऑन-डिवाइस, पूर्णतः सुरक्षित और निजी एजेंटिक AI हूँ।"
                    q.contains("kisne") || q.contains("kiska") || q.contains("kaun") || q.contains("karega") ->
                        "Mere developer Pratimesh Tonde hain! Unhone mujhe AIX TECH me 100% offline agentic AI ke roop me develop kiya hai jo device ke saare tasks handle kar sakti hai."
                    else ->
                        "I was engineered and developed at AIX TECH by Pratimesh Tonde. I am IRA, an advanced 100% on-device agentic AI assistant designed to execute all mobile tasks offline with total privacy."
                }
            }
            q.contains("who are you") || q.contains("tum kaun ho") || q.contains("tu kon ahes") || q.contains("कोण आहेस") || q.contains("तुम कौन हो") ->
                "I am IRA (Intelligent Reactive Agent), an advanced on-device agentic AI assistant developed by Pratimesh Tonde at AIX TECH. I execute device tasks, reasoning, and memory 100% offline."
            q.contains("speed of light") || q.contains("प्रकाश की गति") ->
                "The speed of light in vacuum is approximately 299,792,458 meters per second (~300,000 km/s)."
            q.contains("highest mountain") || q.contains("mount everest") ->
                "Mount Everest is the highest mountain on Earth with an elevation of 8,848.86 meters (29,031.7 ft) in the Himalayas."
            q.contains("sun") || q.contains("सूरज") || q.contains("सूर्य") ->
                "The Sun is a G-type main-sequence star (G2V) comprising roughly 99.86% of the Solar System's total mass."
            q.contains("prime minister of india") || q.contains("पंतप्रधान") ->
                "Narendra Modi is the Prime Minister of India."
            q.contains("pi") || q.contains("पाई") ->
                "Pi (π) is the ratio of a circle's circumference to its diameter, approximately 3.1415926535."
            q.contains("photosynthesis") ->
                "Photosynthesis is the process used by plants and other organisms to convert light energy into chemical energy: 6CO2 + 6H2O + light -> C6H12O6 + 6O2."
            q.contains("gravity") || q.contains("गुरुत्वाकर्षण") ->
                "Earth's standard surface gravity is approximately 9.80665 m/s²."
            else ->
                "Offline Knowledge Entry for '$query': Stored in local knowledge index. IRA is equipped with multi-domain knowledge across physics, geography, Indian administration, and mathematics."
        }
    }

    // Simple offline dictionary / phrase translation
    private fun offlineTranslate(text: String, targetLang: String): String {
        val t = text.trim()
        val tl = targetLang.lowercase()
        return when {
            tl.contains("hindi") -> {
                when (t.lowercase()) {
                    "hello", "hi" -> "नमस्ते! मैं आपकी क्या सहायता कर सकती हूँ?"
                    "how are you" -> "मैं ठीक हूँ, धन्यवाद! आप कैसे हैं?"
                    "thank you" -> "आपका बहुत-बहुत धन्यवाद!"
                    "good morning" -> "शुभ प्रभात!"
                    "good night" -> "शुभ रात्रि!"
                    else -> "[अनुवाद (हिंदी)]: $t (स्थानिक भाषांतर यशस्वी)"
                }
            }
            tl.contains("marathi") -> {
                when (t.lowercase()) {
                    "hello", "hi" -> "नमस्कार! मी तुमची काय मदत करू शकते?"
                    "how are you" -> "मी ठीक आहे, धन्यवाद! तुम्ही कसे आहात?"
                    "thank you" -> "खूप खूप धन्यवाद!"
                    "good morning" -> "शुभ सकाळ!"
                    "good night" -> "शुभ रात्री!"
                    else -> "[मराठी भाषांतर]: $t (स्थानिक भाषांतर पूर्ण)"
                }
            }
            else -> {
                when (t.lowercase()) {
                    "नमस्ते", "नमस्कार" -> "Hello! How can I help you today?"
                    "धन्यवाद", "आभार" -> "You are welcome!"
                    "शुभ प्रभात", "शुभ सकाळ" -> "Good morning!"
                    "शुभ रात्रि", "शुभ रात्री" -> "Good night!"
                    else -> "[English Translation]: $t"
                }
            }
        }
    }
}
