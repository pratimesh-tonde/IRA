package com.example.agent

import java.util.Locale

enum class DetectedLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी"),
    MARATHI("mr", "मराठी"),
    HINGLISH("hi-en", "Hinglish")
}

enum class AssistantLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String,
    val description: String,
    val detectedLang: DetectedLanguage
) {
    HINDI("hi", "Hindi", "हिंदी", "शुद्ध हिंदी संवाद व ऑन-डिवाइस आवाज", DetectedLanguage.HINDI),
    MARATHI("mr", "Marathi", "मराठी", "अस्सल मराठी संभाषण व उच्चार", DetectedLanguage.MARATHI),
    ENGLISH("en", "English", "English", "Natural English Voice & Telemetry", DetectedLanguage.ENGLISH);

    companion object {
        fun fromCode(code: String?): AssistantLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
        }
    }
}

data class PlanAnalysis(
    val detectedLanguage: DetectedLanguage,
    val selectedSubAgent: SubAgentType,
    val goalSummary: String,
    val initialThought: String,
    val steps: List<PlanStep>
)

object OfflineReasoningEngine {

    fun analyzeAndPlan(userInput: String, forcedLanguage: DetectedLanguage? = null): PlanAnalysis {
        val input = userInput.trim()
        val lower = input.lowercase()
        val lang = forcedLanguage ?: detectLanguage(input)

        // Identify multi-step conjunctions: "and", "aur", "ani", "मग", "phir", "त्यानंतर", "&", "+"
        val subCommands = splitCompoundCommands(input)

        val steps = mutableListOf<PlanStep>()
        var primarySubAgent = SubAgentType.KNOWLEDGE

        for ((index, cmd) in subCommands.withIndex()) {
            val step = formulateStepForCommand(index + 1, cmd.trim(), lang)
            steps.add(step)
            // Determine primary sub-agent
            val toolDef = ToolRegistry.tools[step.toolName]
            if (toolDef != null) {
                primarySubAgent = toolDef.subAgent
            }
        }

        val initialThought = when (lang) {
            DetectedLanguage.HINDI -> "उपयोगकर्ता के अनुरोध का विश्लेषण किया गया। चयनित भाषा: हिंदी। कुल ${steps.size} चरण निर्धारित किए गए।"
            DetectedLanguage.MARATHI -> "वापरकर्त्याच्या आदेशाचे विश्लेषण पूर्ण झाले. निवडलेली भाषा: मराठी. एकूण ${steps.size} पायऱ्या आखल्या आहेत."
            DetectedLanguage.HINGLISH -> "Input analyzed. Decomposed goal into ${steps.size} executable step(s)."
            DetectedLanguage.ENGLISH -> "Parsed user objective in English. Formulated a multi-step execution plan with ${steps.size} step(s)."
        }

        return PlanAnalysis(
            detectedLanguage = lang,
            selectedSubAgent = primarySubAgent,
            goalSummary = input,
            initialThought = initialThought,
            steps = steps
        )
    }

    private fun detectLanguage(text: String): DetectedLanguage {
        val lower = text.lowercase()

        // Devanagari script detection
        var devanagariCount = 0
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.DEVANAGARI) {
                devanagariCount++
            }
        }

        if (devanagariCount > 0) {
            // Distinguish Marathi vs Hindi keywords
            val marathiTokens = listOf(
                "आहे", "आहेत", "कसे", "करा", "करायचे", "लावा", "फोन", "माझे", "माझा", "आणि",
                "नाही", "बघा", "नोंद", "अलार्म", "सांगा", "दाखवा", "कॅमेरा", "उघडा", "वाजता",
                "कोणी", "बनवले", "काय", "पाहिजे", "कधी", "कुठे", "द्या", "ऐका"
            )
            val isMarathi = marathiTokens.any { text.contains(it) }
            return if (isMarathi) DetectedLanguage.MARATHI else DetectedLanguage.HINDI
        }

        // Check Romanized Marathi tokens
        val romanizedMarathiTokens = listOf(
            "kasa", "ahes", "ahe", "ahet", "sang", "bagha", "kara", "lava",
            "ugadha", "koni", "banavla", "banavlay", "maza", "mazi", "kiti", "vajta", "pahije"
        )
        if (romanizedMarathiTokens.any { lower.contains(it) }) {
            return DetectedLanguage.MARATHI
        }

        // Romanized Hindi/Urdu/Desi (Hinglish) checks
        val hinglishTokens = listOf(
            "karo", "kare", "karna", "chahiye", "mera", "meri", "mere", "hai", "hain", "kya", "aur",
            "batao", "bata", "dikhao", "dikhana", "chalu", "band", "lagao", "bhejo", "likho",
            "boliye", "bolo", "kisne", "kaun", "kiska", "pratimesh", "prathmesh", "tonde", "isko", "jo", "lang",
            "me", "vahi", "bolana", "moblie", "mobile", "task", "karn", "background", "bhi",
            "chlana", "chalana", "aisa", "kaise", "hota", "karenge", "shuru", "theek", "accha",
            "shukriya", "namaste", "dhanyawad", "battery", "yaad", "rakho"
        )
        val hasHinglish = hinglishTokens.any { lower.contains(it) }
        if (hasHinglish) {
            return DetectedLanguage.HINGLISH
        }

        return DetectedLanguage.ENGLISH
    }

    private fun splitCompoundCommands(input: String): List<String> {
        val delimiters = Regex("\\b(and then|and|aur phir|aur|ani mag|ani|त्यानंतर|आणि|और|फिर)\\b|;|&&", RegexOption.IGNORE_CASE)
        val parts = input.split(delimiters).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts else listOf(input)
    }

    private fun formulateStepForCommand(stepNum: Int, cmd: String, lang: DetectedLanguage): PlanStep {
        val lower = cmd.lowercase()

        // 0. Developer / Creator Identity & Company (AIX TECH • Pratimesh Tonde)
        if (lower.contains("who made") || lower.contains("who developed") || lower.contains("creator") ||
            lower.contains("developer") || lower.contains("pratimesh") || lower.contains("prathmesh") || lower.contains("tonde") ||
            lower.contains("aix") || lower.contains("aix tech") || lower.contains("company") || lower.contains("कंपनी") ||
            lower.contains("kisne banaya") || lower.contains("kiske dwara") || lower.contains("koni banavla") ||
            lower.contains("डेवलपर") || lower.contains("डेव्हलपर") || lower.contains("कोणी बनवले") ||
            lower.contains("किसने बनाया") || lower.contains("tum kaun ho") || lower.contains("who are you") ||
            lower.contains("tu kon ahes") || lower.contains("तुम कौन हो") || lower.contains("तू कोण आहेस")
        ) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Identify Lead Developer & Company (AIX TECH • Pratimesh Tonde)",
                toolName = "get_developer_info",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "User inquiry regarding developer and architecture identity. Acknowledging AIX TECH and Pratimesh Tonde."
            )
        }

        // 0b. Respectful Greeting
        if (lower == "hello" || lower == "hi" || lower == "hey" || lower == "namaste" || lower == "नमस्ते" ||
            lower == "नमस्कार" || lower == "kem cho" || lower == "kasa ahes" || lower == "kaise ho" || lower == "कैसे हो" ||
            lower == "boss" || lower == "sir" || lower == "bhai"
        ) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Respectful Assistant Greeting",
                toolName = "get_developer_info",
                arguments = mapOf("greeting" to "true"),
                riskLevel = RiskLevel.LOW,
                thought = "Responding to user greeting with respect and readiness."
            )
        }

        // 0c. Location / GPS check (location batao, loction batao, kaha hu, kuthe ahe)
        if (lower.contains("location") || lower.contains("loction") || lower.contains("gps") || lower.contains("where am i") ||
            lower.contains("kahan hoon") || lower.contains("kaha hu") || lower.contains("कहाँ हूँ") || lower.contains("कहा हु") ||
            lower.contains("kuthe ahe") || lower.contains("कुठे आहे") || lower.contains("स्थान") || lower.contains("माझे स्थान") ||
            lower.contains("लोकेशन") || lower.contains("स्थळ")
        ) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Check Device Location & GPS",
                toolName = "get_device_location",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Querying real-time device GPS and reverse geocoding."
            )
        }

        // 1. Navigation & Maps (navigate karo, naviagate karo, rasta dikhao, rasta dakhva)
        if (lower.contains("navigate") || lower.contains("naviagate") || lower.contains("directions") ||
            lower.contains("rasta") || lower.contains("रास्ता") || lower.contains("दिशा") || lower.contains("नकाशा") ||
            lower.contains("jana hai") || lower.contains("jaycha ahe") || lower.contains("नेव्हिगेट") || lower.contains("नेविगेट")
        ) {
            val destination = cmd.replace(Regex("(?i)(navigate to|navigate karo|naviagate karo|naviagate|navigate|directions to|rasta dikhao|rasta batao|दिशा दाखवा|रास्ता दिखाओ|रास्ता बताओ|नकाशा दाखवा|jana hai|jaycha ahe)"), "").trim()
            val targetDest = if (destination.isNotBlank()) destination else "Current Destination"
            return PlanStep(
                stepIndex = stepNum,
                title = "Navigate to $targetDest",
                toolName = "navigate_to",
                arguments = mapOf("destination" to targetDest),
                riskLevel = RiskLevel.LOW,
                thought = "Launching turn-by-turn navigation via Google Maps."
            )
        }

        // 2. WhatsApp Messaging & Chat (whatsapp chat karo, wathapp chat, whatsapp open)
        if (lower.contains("whatsapp") || lower.contains("wathapp") || lower.contains("व्हाट्स") || lower.contains("व्हॉट्सॲप") || lower.contains("व्हॉट्सॅप") || lower.contains("वॉट्सएप")) {
            val phone = Regex("[0-9]{10,12}").find(cmd)?.value ?: ""
            val msg = if (cmd.contains("msg ") || cmd.contains("that ") || cmd.contains("ki ") || cmd.contains("संदेश ")) {
                cmd.substringAfter("msg ").substringAfter("that ").substringAfter("ki ").substringAfter("संदेश ").trim()
            } else {
                ""
            }
            return PlanStep(
                stepIndex = stepNum,
                title = "WhatsApp Chat",
                toolName = "send_whatsapp",
                arguments = mapOf("phone" to phone, "message" to msg),
                riskLevel = RiskLevel.LOW,
                thought = "Opening WhatsApp conversation directly."
            )
        }

        // 3. Call / Phone (call karo, phone lagao, call mom, phone lava, call karane bola)
        if (lower.contains("call") || lower.contains("phone") || lower.contains("कॉल") ||
            lower.contains("फोन लावा") || lower.contains("फोन करा") || lower.contains("फोन करो") ||
            lower.contains("कॉल करा") || lower.contains("कॉल करो") || lower.contains("dial")
        ) {
            val recipient = extractRecipient(cmd)
            return PlanStep(
                stepIndex = stepNum,
                title = "Call Contact: $recipient",
                toolName = "call_contact",
                arguments = mapOf("phone_number" to recipient),
                riskLevel = RiskLevel.LOW,
                thought = "Executing phone call directly as instructed."
            )
        }

        // 4. Flashlight / Torch
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("टॉर्च") || lower.contains("लाइट") || lower.contains("फ्लॅश")) {
            val state = when {
                lower.contains("on") || lower.contains("chalu") || lower.contains("jalao") || lower.contains("लावा") || lower.contains("सुरू") -> "on"
                lower.contains("off") || lower.contains("band") || lower.contains("bujhao") || lower.contains("बंद") -> "off"
                else -> "toggle"
            }
            return PlanStep(
                stepIndex = stepNum,
                title = "Toggle Flashlight ($state)",
                toolName = "toggle_flashlight",
                arguments = mapOf("state" to state),
                riskLevel = RiskLevel.LOW,
                thought = "Controlling device hardware torch safely via CameraManager."
            )
        }

        // 5. Camera & Photo Capture
        if (lower.contains("photo") || lower.contains("picture") || lower.contains("तस्वीर") || lower.contains("फोटो") ||
            lower.contains("video") || lower.contains("व्हिडिओ") || lower.contains("वीडियो")
        ) {
            val mode = if (lower.contains("video") || lower.contains("व्हिडिओ") || lower.contains("वीडियो")) "video" else "photo"
            return PlanStep(
                stepIndex = stepNum,
                title = "Launch Camera ($mode)",
                toolName = "capture_media",
                arguments = mapOf("mode" to mode),
                riskLevel = RiskLevel.LOW,
                thought = "Invoking device camera capture framework."
            )
        }

        // 6. Music & Audio Controls
        if (lower.contains("play music") || lower.contains("pause music") || lower.contains("gana bajao") ||
            lower.contains("गाना बजाओ") || lower.contains("गाणे लावा") || lower.contains("गाणी वाजवा") ||
            lower.contains("spotify") || lower.contains("music player")
        ) {
            val action = when {
                lower.contains("pause") || lower.contains("stop") || lower.contains("थांबवा") || lower.contains("रोको") -> "pause"
                lower.contains("next") || lower.contains("पुढचे") -> "next"
                else -> "play"
            }
            return PlanStep(
                stepIndex = stepNum,
                title = "Control Music Playback ($action)",
                toolName = "control_music",
                arguments = mapOf("action" to action),
                riskLevel = RiskLevel.LOW,
                thought = "Dispatching media session intents to music player."
            )
        }

        // 7. Open Any App (open app [name], [name] kholo, [name] ughada, [name] open karo)
        if (lower.contains("open ") || lower.contains("kholo") || lower.contains("खोलो") ||
            lower.contains("उघडा") || lower.contains("launch ") || lower.contains("chalu karo ") ||
            lower.contains("सुरू करा ") || lower.contains("open app") || lower.contains("ॲप उघडा") || lower.contains("ऐप खोलो")
        ) {
            val cleanApp = cmd.replace(Regex("(?i)(open app|open|kholo|खोलो|उघडा|launch|chalu karo|सुरू करा|app|ॲप|ऐप)"), "").trim()
            val targetApp = if (cleanApp.isNotBlank()) cleanApp else "camera"
            return PlanStep(
                stepIndex = stepNum,
                title = "Launch Application: $targetApp",
                toolName = "open_app",
                arguments = mapOf("app_name" to targetApp),
                riskLevel = RiskLevel.LOW,
                thought = "Resolving intent for mobile application '$targetApp'."
            )
        }

        // 8. Web Search
        if (lower.contains("search on google") || lower.contains("google search") || lower.contains("search ") ||
            lower.contains("गूगल पर सर्च") || lower.contains("शोध करा")
        ) {
            val query = cmd.replace(Regex("(?i)(search on google for|google search|search on google|search for|search|गूगल पर सर्च|शोध करा)"), "").trim()
            return PlanStep(
                stepIndex = stepNum,
                title = "Search Web: '$query'",
                toolName = "search_web",
                arguments = mapOf("query" to query),
                riskLevel = RiskLevel.LOW,
                thought = "Initiating web search intent for query."
            )
        }

        // 9. Airplane Mode Settings
        if (lower.contains("airplane mode") || lower.contains("flight mode") || lower.contains("एरोप्लेन") || lower.contains("विमान")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Open Airplane Mode Settings",
                toolName = "open_settings_airplane",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Accessing device Airplane Mode settings."
            )
        }

        // 10. Hotspot Settings
        if (lower.contains("hotspot") || lower.contains("हॉटस्पॉट")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Open Hotspot Settings",
                toolName = "open_settings_hotspot",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Accessing mobile hotspot and tethering settings."
            )
        }

        // 11. Battery Saver Settings
        if (lower.contains("battery saver") || lower.contains("power saver") || lower.contains("बैटरी सेवर")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Open Battery Saver Settings",
                toolName = "open_settings_battery",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Accessing device Battery Saver settings."
            )
        }

        // 12. Battery check
        if (lower.contains("battery") || lower.contains("बैटरी") || lower.contains("बॅटरी") || lower.contains("charge") || lower.contains("चार्ज")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Check Battery Level",
                toolName = "get_battery_status",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Querying real-time BatteryManager context signals."
            )
        }

        // 4. Send SMS
        if (lower.contains("sms") || lower.contains("message") || lower.contains("मेसेज") || lower.contains("संदेश")) {
            val recipient = extractRecipient(cmd)
            val msgBody = if (cmd.contains("that ") || cmd.contains("ki ")) {
                cmd.substringAfter("that ").substringAfter("ki ").trim()
            } else {
                "Hello from IRA Assistant"
            }
            return PlanStep(
                stepIndex = stepNum,
                title = "Draft SMS to $recipient",
                toolName = "send_sms",
                arguments = mapOf("phone_number" to recipient, "message" to msgBody),
                riskLevel = RiskLevel.HIGH,
                thought = "High-risk intent detected: composing SMS."
            )
        }

        // 5. Emergency SOS
        if (lower.contains("emergency") || lower.contains("sos") || lower.contains("help me") || lower.contains("मदत") || lower.contains("बचाओ")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Trigger Emergency SOS Protocol",
                toolName = "emergency_sos",
                arguments = mapOf("reason" to "User requested SOS: $cmd"),
                riskLevel = RiskLevel.HIGH,
                thought = "Safety protocol: Emergency SOS dialer dispatch."
            )
        }

        // 6. Notes: Create or List
        if (lower.contains("note") || lower.contains("नोट") || lower.contains("नोंद")) {
            if (lower.contains("read") || lower.contains("list") || lower.contains("show") || lower.contains("dikhao") || lower.contains("दाखवा")) {
                return PlanStep(
                    stepIndex = stepNum,
                    title = "Retrieve Stored Notes",
                    toolName = "list_notes",
                    arguments = mapOf("search_query" to ""),
                    riskLevel = RiskLevel.LOW,
                    thought = "Accessing Room database for user notes."
                )
            } else {
                val cleanNote = cmd.replace(Regex("(?i)(create note|take note|save note|note banao|note likho|ek note|note:)"), "").trim()
                val title = if (cleanNote.length > 20) cleanNote.take(20) + "..." else if (cleanNote.isNotEmpty()) cleanNote else "Quick Note"
                return PlanStep(
                    stepIndex = stepNum,
                    title = "Create Note: '$title'",
                    toolName = "create_note",
                    arguments = mapOf("title" to title, "content" to cleanNote, "category" to "AgentNote"),
                    riskLevel = RiskLevel.LOW,
                    thought = "Persisting note locally in offline Room storage."
                )
            }
        }

        // 7. Alarms & Timers
        if (lower.contains("alarm") || lower.contains("अलार्म")) {
            val timeDigits = extractTime(cmd)
            return PlanStep(
                stepIndex = stepNum,
                title = "Set Alarm for ${timeDigits.first}:${timeDigits.second}",
                toolName = "set_alarm",
                arguments = mapOf(
                    "hour" to timeDigits.first.toString(),
                    "minute" to timeDigits.second.toString(),
                    "label" to "IRA Alarm"
                ),
                riskLevel = RiskLevel.MEDIUM,
                thought = "Setting device alarm using Android AlarmClock intent."
            )
        }

        if (lower.contains("timer") || lower.contains("टाइमर")) {
            val sec = extractSeconds(cmd)
            return PlanStep(
                stepIndex = stepNum,
                title = "Set Timer for $sec seconds",
                toolName = "set_timer",
                arguments = mapOf("seconds" to sec.toString(), "label" to "IRA Countdown"),
                riskLevel = RiskLevel.LOW,
                thought = "Scheduling countdown timer."
            )
        }

        // 8. Reminders
        if (lower.contains("remind") || lower.contains("याद दिलाओ") || lower.contains("आठवण")) {
            val reminderTitle = cmd.replace(Regex("(?i)(remind me to|remind me|yaad dilana ki|yaad dilao)"), "").trim()
            return PlanStep(
                stepIndex = stepNum,
                title = "Add Reminder: '$reminderTitle'",
                toolName = "add_reminder",
                arguments = mapOf("title" to reminderTitle, "target_time" to "Scheduled", "priority" to "Normal"),
                riskLevel = RiskLevel.LOW,
                thought = "Adding reminder to local productivity database."
            )
        }

        // 9. Volume & Sound
        if (lower.contains("volume") || lower.contains("sound") || lower.contains("आवाज") || lower.contains("ध्वनी")) {
            val dir = when {
                lower.contains("mute") || lower.contains("silent") || lower.contains("शांत") -> "mute"
                lower.contains("unmute") -> "unmute"
                lower.contains("up") || lower.contains("badhao") || lower.contains("वाढवा") -> "up"
                lower.contains("down") || lower.contains("kam") || lower.contains("कमी") -> "down"
                else -> "up"
            }
            return PlanStep(
                stepIndex = stepNum,
                title = "Adjust Volume ($dir)",
                toolName = "adjust_volume",
                arguments = mapOf("stream" to "media", "direction" to dir),
                riskLevel = RiskLevel.LOW,
                thought = "Modulating system AudioManager stream."
            )
        }

        // 10. Settings Shortcuts
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Open Wi-Fi Settings",
                toolName = "open_settings_wifi",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Redirecting to Wi-Fi connection manager."
            )
        }

        if (lower.contains("bluetooth")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Open Bluetooth Settings",
                toolName = "open_settings_bluetooth",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Redirecting to Bluetooth controller."
            )
        }

        if (lower.contains("dnd") || lower.contains("do not disturb") || lower.contains("शांतता")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Open Do Not Disturb Settings",
                toolName = "open_settings_dnd",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Opening DND & Sound settings."
            )
        }

        // 11. Time & Date
        if (lower.contains("time") || lower.contains("date") || lower.contains("samay") || lower.contains("वेळ") || lower.contains("तारीख") || lower.contains("समय")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Get Current Time and Date",
                toolName = "get_current_time",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Checking system clock and formatted local date."
            )
        }

        // 12. System Specs / Diagnostics
        if (lower.contains("system info") || lower.contains("specs") || lower.contains("device info") || lower.contains("माहिती") || lower.contains("फोन की जानकारी")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Inspect System Hardware Specs",
                toolName = "get_system_info",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                thought = "Gathering on-device hardware telemetry and JVM metrics."
            )
        }

        // 13. Math Calculation
        val hasMathSymbols = lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/") || lower.contains("% of") || lower.contains("sqrt")
        val mathWords = lower.contains("calculate") || lower.contains("multiply") || lower.contains("divide") || lower.contains("plus") || lower.contains("गुणाकार") || lower.contains("जोड़ो")
        if (hasMathSymbols || mathWords) {
            val expr = cmd.replace(Regex("(?i)(calculate|what is|compute|solve|is|कितना होता है|किती होते)"), "").trim()
            return PlanStep(
                stepIndex = stepNum,
                title = "Evaluate Math Expression: $expr",
                toolName = "evaluate_math",
                arguments = mapOf("expression" to expr),
                riskLevel = RiskLevel.LOW,
                thought = "Processing mathematical logic on-device with custom parser."
            )
        }

        // 14. Unit Conversion
        if (lower.contains("convert") || lower.contains("miles to km") || lower.contains("km to miles") || lower.contains("kg to lbs") || lower.contains("lbs to kg") || lower.contains("c to f") || lower.contains("f to c")) {
            val from = when {
                lower.contains("km") -> "km"
                lower.contains("mile") -> "miles"
                lower.contains("kg") -> "kg"
                lower.contains("lb") -> "lbs"
                lower.contains("c") -> "c"
                lower.contains("f") -> "f"
                else -> "km"
            }
            val to = when {
                from == "km" -> "miles"
                from == "miles" -> "km"
                from == "kg" -> "lbs"
                from == "lbs" -> "kg"
                from == "c" -> "f"
                from == "f" -> "c"
                else -> "miles"
            }
            val num = Regex("[0-9.]+").find(cmd)?.value ?: "10"
            return PlanStep(
                stepIndex = stepNum,
                title = "Convert Units: $num $from -> $to",
                toolName = "convert_units",
                arguments = mapOf("value" to num, "from" to from, "to" to to),
                riskLevel = RiskLevel.LOW,
                thought = "Calculating unit metrics conversion."
            )
        }

        // 15. Semantic Memory: Save fact ("remember that...", "yaad rakhna ki...")
        if (lower.contains("remember") || lower.contains("yaad rakh") || lower.contains("लक्षात ठेव") || lower.contains("save fact")) {
            val fact = cmd.replace(Regex("(?i)(remember that|remember|yaad rakhna ki|yaad rakho|लक्षात ठेव की)"), "").trim()
            return PlanStep(
                stepIndex = stepNum,
                title = "Store Semantic Fact",
                toolName = "save_user_fact",
                arguments = mapOf("category" to "UserPreference", "key" to "learned_fact", "value" to fact),
                riskLevel = RiskLevel.LOW,
                thought = "Committing fact into 3-Tier Local Semantic Memory."
            )
        }

        // 16. Semantic Memory: Query fact ("what is my...", "mera... kya hai")
        if (lower.contains("my name") || lower.contains("mera naam") || lower.contains("माझे नाव") || lower.contains("who am i")) {
            return PlanStep(
                stepIndex = stepNum,
                title = "Query Semantic Memory for User Identity",
                toolName = "query_semantic_memory",
                arguments = mapOf("query" to "user_name"),
                riskLevel = RiskLevel.LOW,
                thought = "Retrieving personal knowledge graph node from Room database."
            )
        }

        // 17. Offline Translation
        if (lower.contains("translate") || lower.contains("भाषांतर") || lower.contains("अनुवाद")) {
            val target = when {
                lower.contains("marathi") || lower.contains("मराठी") -> "marathi"
                lower.contains("hindi") || lower.contains("हिंदी") -> "hindi"
                else -> "english"
            }
            val textToTranslate = cmd.replace(Regex("(?i)(translate|into marathi|into hindi|into english|in hindi|in marathi)"), "").trim()
            return PlanStep(
                stepIndex = stepNum,
                title = "Translate Text to $target",
                toolName = "translate_text",
                arguments = mapOf("text" to textToTranslate, "target_lang" to target),
                riskLevel = RiskLevel.LOW,
                thought = "Executing on-device lexical translation model."
            )
        }

        // 18. Default Knowledge Query
        return PlanStep(
            stepIndex = stepNum,
            title = "Lookup Knowledge Base: '$cmd'",
            toolName = "query_knowledge_base",
            arguments = mapOf("query" to cmd),
            riskLevel = RiskLevel.LOW,
            thought = "Scanning local vector and offline encyclopedic index."
        )
    }

    private fun extractRecipient(cmd: String): String {
        // 1. Phone number check (digits, plus)
        val numMatch = Regex("[0-9+]{5,15}").find(cmd)
        if (numMatch != null) return numMatch.value

        // 2. Hindi/Marathi: "[name] ko call karo" or "[name] la phone lava"
        val koMatch = Regex("([a-zA-Z\\u0900-\\u097F]+)\\s+(ko|la|lai|var|laa)\\s+(call|phone|फोन|कॉल)", RegexOption.IGNORE_CASE).find(cmd)
        if (koMatch != null) {
            val name = koMatch.groupValues[1].trim()
            if (name.isNotBlank()) return name
        }

        // 3. English/Common: "call [name]" or "phone [name]"
        val afterCallMatch = Regex("(call|phone|dial|फोन|कॉल)\\s+(to\\s+)?([a-zA-Z\\u0900-\\u097F]+)", RegexOption.IGNORE_CASE).find(cmd)
        if (afterCallMatch != null) {
            val name = afterCallMatch.groupValues[3].trim()
            val ignoreWords = listOf("karo", "lagao", "lava", "kara", "karane", "bola", "mines", "me", "phone", "call", "please")
            if (name.isNotBlank() && !ignoreWords.contains(name.lowercase())) {
                return name
            }
        }

        // 4. Fallback: strip command keywords
        val clean = cmd.replace(Regex("(?i)(call|phone|dial|karo|lagao|lava|kara|karane|bola|mines|ko|la|to|please|kr|kar|फोन|कॉल)"), "").trim()
        return if (clean.isNotBlank()) clean else "Contacts"
    }

    private fun extractTime(cmd: String): Pair<Int, Int> {
        val timeRegex = Regex("(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(cmd)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 7
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3].lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return Pair(hour, minute)
        }
        return Pair(7, 0)
    }

    private fun extractSeconds(cmd: String): Int {
        val minRegex = Regex("(\\d+)\\s*(min|minute|minutes|मिनिट)")
        val minMatch = minRegex.find(cmd)
        if (minMatch != null) {
            val m = minMatch.groupValues[1].toIntOrNull() ?: 5
            return m * 60
        }
        val secRegex = Regex("(\\d+)\\s*(sec|second|seconds|सेकंड)")
        val secMatch = secRegex.find(cmd)
        if (secMatch != null) {
            return secMatch.groupValues[1].toIntOrNull() ?: 30
        }
        return 300
    }

    fun formulateFinalResponse(
        originalGoal: String,
        lang: DetectedLanguage,
        stepResults: List<Pair<PlanStep, ToolResult>>
    ): Pair<String, String> {
        val allSucceeded = stepResults.all { it.second.isSuccess }

        // Localize every step's output to the requested language
        val localizedSummaries = stepResults.map { (step, result) ->
            localizeToolOutput(step, result, lang)
        }.joinToString(" ")

        val responseText = when (lang) {
            DetectedLanguage.HINDI -> {
                if (allSucceeded) {
                    localizedSummaries
                } else {
                    "क्षमा करें जी, कार्य करने में थोड़ी परेशानी आई: $localizedSummaries"
                }
            }
            DetectedLanguage.MARATHI -> {
                if (allSucceeded) {
                    localizedSummaries
                } else {
                    "क्षमस्व, कार्य करताना थोडी अडचण आली: $localizedSummaries"
                }
            }
            DetectedLanguage.HINGLISH -> {
                if (allSucceeded) {
                    localizedSummaries
                } else {
                    "Kshama karein ji, task complete karne me thodi dikkat aayi: $localizedSummaries"
                }
            }
            DetectedLanguage.ENGLISH -> {
                if (allSucceeded) {
                    localizedSummaries
                } else {
                    "I apologize, there was an issue completing the task: $localizedSummaries"
                }
            }
        }

        val sentiment = if (allSucceeded) "CALM" else "ALERT"
        return Pair(responseText, sentiment)
    }

    private fun localizeToolOutput(step: PlanStep, result: ToolResult, lang: DetectedLanguage): String {
        val tool = step.toolName
        val args = step.arguments
        val outMsg = result.outputMessage

        if (!result.isSuccess) {
            return when (lang) {
                DetectedLanguage.HINDI -> "क्षमा करें जी, यह कार्य करने में थोड़ी कठिनाई आई: $outMsg।"
                DetectedLanguage.MARATHI -> "क्षमस्व, हे काम करताना थोडी अडचण आली: $outMsg."
                DetectedLanguage.HINGLISH -> "Kshama karein ji, ye task execute karne me dikkat aayi: $outMsg."
                DetectedLanguage.ENGLISH -> "I apologize, there was an issue executing this step: $outMsg."
            }
        }

        return when (tool) {
            "get_developer_info" -> {
                val isGreeting = args["greeting"] == "true"
                if (isGreeting) {
                    when (lang) {
                        DetectedLanguage.HINDI ->
                            "नमस्ते जी! मैं आपकी सेवा में सदैव हाज़िर हूँ। बताइए, आज मैं आपके लिए क्या सहायता कर सकती हूँ?"
                        DetectedLanguage.MARATHI ->
                            "नमस्कार! मी नम्रपणे आपल्या सेवेत सदैव हजर आहे. सांगा, आज मी आपल्यासाठी कोणते काम करू शकते?"
                        DetectedLanguage.HINGLISH ->
                            "Namaste ji! Main aapki seva me sadaiv hazir hoon. Boliye, aaj main aapki kya madad kar sakti hoon?"
                        DetectedLanguage.ENGLISH ->
                            "Hello! It is my pleasure to be at your service. How may I assist you today?"
                    }
                } else {
                    when (lang) {
                        DetectedLanguage.HINDI ->
                            "नमस्ते जी! मुझे AIX TECH कंपनी में डेवलपर प्रतिमेश तोंडे (Pratimesh Tonde) जी ने विकसित किया है। मैं आपकी हर मोबाइल टास्क को 100% ऑन-डिवाइस, सुरक्षित और अत्यंत आदरपूर्वक पूरा करने वाली व्यक्तिगत AI सहायक हूँ।"
                        DetectedLanguage.MARATHI ->
                            "नमस्कार! मला AIX TECH कंपनीमध्ये डेव्हलपर प्रतिमेश तोंडे (Pratimesh Tonde) यांनी विकसित केले आहे. मी आपले सर्व मोबाइल कार्य १००% ऑन-डिव्हाइस आणि संपूर्ण आदराने पूर्ण करण्यासाठी सदैव तत्पर आहे."
                        DetectedLanguage.HINGLISH ->
                            "Namaste ji! Mujhe AIX TECH company me developer Pratimesh Tonde ji ne create kiya hai. Main aapke phone ke sabhi tasks 100% offline aur poori respect ke sath execute karti hoon."
                        DetectedLanguage.ENGLISH ->
                            "Hello! I was developed at AIX TECH by developer Pratimesh Tonde. I am IRA, an autonomous on-device personal assistant designed to serve you with care and respect."
                    }
                }
            }
            "toggle_flashlight" -> {
                val state = args["state"] ?: "on"
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, मैंने आपके फोन की फ्लैशलाइट ${if (state == "off") "बंद कर दी है" else "चालू कर दी है"}।"
                    DetectedLanguage.MARATHI -> "होय, मी आपल्या फोनची टॉर्च ${if (state == "off") "बंद केली आहे" else "सुरू केली आहे"}."
                    DetectedLanguage.HINGLISH -> "Ji, maine aapke phone ki flashlight ${if (state == "off") "off" else "on"} kar di hai."
                    DetectedLanguage.ENGLISH -> "I have turned ${if (state == "off") "off" else "on"} the flashlight for you."
                }
            }
            "open_app" -> {
                val app = args["app_name"] ?: "Application"
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, मैंने आपके लिए $app ऐप खोल दिया है।"
                    DetectedLanguage.MARATHI -> "हो नक्कीच, मी आपल्यासाठी $app ॲप उघडले आहे."
                    DetectedLanguage.HINGLISH -> "Ji, maine aapke liye $app app open kar diya hai."
                    DetectedLanguage.ENGLISH -> "Certainly, I have opened $app for you."
                }
            }
            "capture_media" -> {
                val mode = args["mode"] ?: "photo"
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके लिए कैमरा ${if (mode == "video") "वीडियो रिकॉर्डिंग" else "फोटो खींचने के"} लिए खोल दिया गया है।"
                    DetectedLanguage.MARATHI -> "होय, आपल्यासाठी कॅमेरा ${if (mode == "video") "व्हिडिओ" else "फोटो"} मोडमध्ये उघडला आहे."
                    DetectedLanguage.HINGLISH -> "Ji, camera ${if (mode == "video") "video" else "photo"} mode me open kar diya gaya hai."
                    DetectedLanguage.ENGLISH -> "The camera has been opened in $mode mode for you."
                }
            }
            "call_contact" -> {
                val target = args["phone_number"] ?: ""
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके निर्देशानुसार $target को फोन कॉल मिलाया जा रहा है।"
                    DetectedLanguage.MARATHI -> "होय, आपल्या सांगण्यानुसार $target यांना फोन कॉल जोडला जात आहे."
                    DetectedLanguage.HINGLISH -> "Ji, aapke kahne par $target ko call lagaya ja raha hai."
                    DetectedLanguage.ENGLISH -> "Connecting your call to $target right now."
                }
            }
            "send_sms" -> {
                val target = args["phone_number"] ?: ""
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, $target के लिए संदेश तैयार कर दिया गया है।"
                    DetectedLanguage.MARATHI -> "हो नक्कीच, $target यांच्यासाठी संदेश तयार केला आहे."
                    DetectedLanguage.HINGLISH -> "Ji, $target ke liye SMS message draft kar diya gaya hai."
                    DetectedLanguage.ENGLISH -> "The SMS message has been drafted for $target."
                }
            }
            "search_contacts" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपकी फोनबुक से संपर्क विवरण यह रहे:\n$outMsg"
                    DetectedLanguage.MARATHI -> "आपल्या फोनमधील संपर्कांची माहिती खालीलप्रमाणे आहे:\n$outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, aapke phone contacts me se ye details mili hain:\n$outMsg"
                    DetectedLanguage.ENGLISH -> "Here are the contact details from your address book:\n$outMsg"
                }
            }
            "get_device_location" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके फोन की वर्तमान लोकेशन और स्थिति यह है:\n$outMsg"
                    DetectedLanguage.MARATHI -> "आपल्या फोनचे सध्याचे अचूक स्थान खालीलप्रमाणे आहे:\n$outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, aapke device ki current location ye hai:\n$outMsg"
                    DetectedLanguage.ENGLISH -> "Here is your current location and satellite telemetry:\n$outMsg"
                }
            }
            "navigate_to" -> {
                val dest = args["destination"] ?: ""
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, मैंने $dest के लिए गूगल मैप्स नेविगेशन शुरू कर दिया है। आपकी यात्रा मंगलमय हो।"
                    DetectedLanguage.MARATHI -> "हो नक्कीच, $dest साठी गुगल मॅप्स नेव्हिगेशन सुरू केले आहे. आपला प्रवास सुखकर आणि सुरक्षित होवो."
                    DetectedLanguage.HINGLISH -> "Ji, $dest ke liye Google Maps navigation start kar diya gaya hai. Shubh yatra!"
                    DetectedLanguage.ENGLISH -> "I have started turn-by-turn navigation to $dest for you. Have a pleasant and safe journey!"
                }
            }
            "send_whatsapp" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, मैंने आपके लिए व्हाट्सएप खोल दिया है, आप आसानी से चैट कर सकते हैं।"
                    DetectedLanguage.MARATHI -> "होय, मी आपल्यासाठी व्हॉट्सॲप उघडले आहे, आपण थेट संवाद साधू शकता."
                    DetectedLanguage.HINGLISH -> "Ji, aapke liye WhatsApp open kar diya gaya hai, aap chat kar sakte hain."
                    DetectedLanguage.ENGLISH -> "WhatsApp is open and ready for your chat."
                }
            }
            "search_web" -> {
                val q = args["query"] ?: ""
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, '$q' के बारे में जानकारी खोजने के लिए वेब सर्च शुरू कर दी गई है।"
                    DetectedLanguage.MARATHI -> "हो नक्कीच, '$q' बद्दल सविस्तर माहिती मिळवण्यासाठी वेब शोध सुरू केला आहे."
                    DetectedLanguage.HINGLISH -> "Ji, '$q' ke liye web search open kar di gayi hai."
                    DetectedLanguage.ENGLISH -> "Web search query launched for '$q'."
                }
            }
            "control_music" -> {
                val action = args["action"] ?: "play"
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके लिए म्यूजिक प्लेयर चालू कर दिया गया है।"
                    DetectedLanguage.MARATHI -> "होय, आपल्यासाठी संगीत प्लेअर सुरू केले आहे."
                    DetectedLanguage.HINGLISH -> "Ji, music player start kar diya gaya hai."
                    DetectedLanguage.ENGLISH -> "Music player controls have been activated for you."
                }
            }
            "get_battery_status" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके फोन की बैटरी स्थिति इस समय $outMsg है।"
                    DetectedLanguage.MARATHI -> "आपल्या फोनची बॅटरी सध्या $outMsg आहे."
                    DetectedLanguage.HINGLISH -> "Ji, aapke phone ki battery status $outMsg hai."
                    DetectedLanguage.ENGLISH -> "Your device battery status is currently $outMsg."
                }
            }
            "create_note" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपका नोट सुरक्षित रूप से सहेज लिया गया है।"
                    DetectedLanguage.MARATHI -> "होय, आपली नोंद सुरक्षितपणे सेव्ह केली आहे."
                    DetectedLanguage.HINGLISH -> "Ji, aapka note successfully save kar liya gaya hai."
                    DetectedLanguage.ENGLISH -> "Your note has been securely saved."
                }
            }
            "list_notes" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके सहेजे गए नोट्स की सूची यह रही:\n$outMsg"
                    DetectedLanguage.MARATHI -> "आपल्या सेव्ह केलेल्या नोंदींची यादी खालीलप्रमाणे आहे:\n$outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, aapke saved notes ye rahe:\n$outMsg"
                    DetectedLanguage.ENGLISH -> "Here are your saved notes:\n$outMsg"
                }
            }
            "adjust_volume" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके फोन की आवाज का स्तर सेट कर दिया गया है।"
                    DetectedLanguage.MARATHI -> "होय, फोनची आवाज पातळी आपल्या आवडीनुसार सेट केली आहे."
                    DetectedLanguage.HINGLISH -> "Ji, volume level set kar diya gaya hai."
                    DetectedLanguage.ENGLISH -> "The volume level has been adjusted for you."
                }
            }
            "set_alarm" -> {
                val h = args["hour"] ?: "7"
                val m = args["minute"] ?: "00"
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके लिए $h:$m बजे का अलार्म तय कर दिया गया है।"
                    DetectedLanguage.MARATHI -> "होय, आपल्यासाठी $h:$m वाजताचा अलार्म सेट करण्यात आला आहे."
                    DetectedLanguage.HINGLISH -> "Ji, $h:$m baje ka alarm schedule kar diya gaya hai."
                    DetectedLanguage.ENGLISH -> "Alarm scheduled for $h:$m as requested."
                }
            }
            "set_timer" -> {
                val sec = args["seconds"] ?: "60"
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, मैंने $sec सेकंड का टाइमर शुरू कर दिया है।"
                    DetectedLanguage.MARATHI -> "हो नक्कीच, $sec सेकंदांचा टायमर सुरू करण्यात आला आहे."
                    DetectedLanguage.HINGLISH -> "Ji, $sec seconds ka timer start kar diya gaya hai."
                    DetectedLanguage.ENGLISH -> "Countdown timer has been set for $sec seconds."
                }
            }
            "open_settings_wifi", "open_settings_bluetooth", "open_settings_dnd",
            "open_settings_airplane", "open_settings_hotspot", "open_settings_battery" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके फोन की सेटिंग्स स्क्रीन खोल दी गई है।"
                    DetectedLanguage.MARATHI -> "होय, फोनची सिस्टीम सेटिंग्ज स्क्रीन उघडण्यात आली आहे."
                    DetectedLanguage.HINGLISH -> "Ji, device ki settings screen open kar di gayi hai."
                    DetectedLanguage.ENGLISH -> "The device settings screen has been opened for you."
                }
            }
            "get_current_time" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, वर्तमान समय और दिनांक यह है:\n$outMsg"
                    DetectedLanguage.MARATHI -> "सध्याची अचूक वेळ आणि तारीख खालीलप्रमाणे आहे:\n$outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, current date aur time ye hai:\n$outMsg"
                    DetectedLanguage.ENGLISH -> "The current time and date are:\n$outMsg"
                }
            }
            "get_system_info" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, आपके फोन की हार्डवेयर व सिस्टम जानकारी यह है:\n$outMsg"
                    DetectedLanguage.MARATHI -> "आपल्या फोनची तांत्रिक माहिती खालीलप्रमाणे आहे:\n$outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, aapke phone ki hardware details ye hain:\n$outMsg"
                    DetectedLanguage.ENGLISH -> "Here is your system specification summary:\n$outMsg"
                }
            }
            "evaluate_math" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, इस गणना का सही उत्तर $outMsg है।"
                    DetectedLanguage.MARATHI -> "या हिशोबाचे अचूक उत्तर $outMsg आहे."
                    DetectedLanguage.HINGLISH -> "Ji, is calculation ka sahi answer $outMsg hai."
                    DetectedLanguage.ENGLISH -> "The exact calculation result is $outMsg."
                }
            }
            "query_knowledge_base" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> {
                        when {
                            outMsg.contains("New Delhi") -> "जी, भारत की राजधानी नई दिल्ली (New Delhi) है।"
                            outMsg.contains("Mumbai") -> "जी, महाराष्ट्र की राजधानी मुंबई और उप-राजधानी नागपुर है।"
                            outMsg.contains("Narendra Modi") -> "जी, भारत के माननीय प्रधानमंत्री श्री नरेंद्र मोदी हैं।"
                            outMsg.contains("299,792,458") -> "जी, निर्वात में प्रकाश की गति लगभग 299,792 किमी प्रति सेकंड (~3 लाख किमी/सेकंड) है।"
                            outMsg.contains("Mount Everest") -> "जी, विश्व का सबसे ऊँचा पर्वत शिखर माउंट एवरेस्ट (8,848.86 मीटर) है।"
                            outMsg.contains("9.80665") -> "जी, पृथ्वी का मानक गुरुत्वाकर्षण लगभग 9.8 m/s² है।"
                            outMsg.contains("3.14159") -> "जी, पाई (π) का मान लगभग 3.14159 या 22/7 होता है।"
                            outMsg.contains("Photosynthesis") -> "जी, प्रकाश संश्लेषण (Photosynthesis) वह प्रक्रिया है जिससे पौधे सूर्य के प्रकाश से अपना भोजन बनाते हैं।"
                            outMsg.contains("Pratimesh Tonde") -> "नमस्ते जी! मुझे AIX TECH में डेवलपर प्रतिमेश तोंडे (Pratimesh Tonde) जी ने बनाया है।"
                            else -> "जी, आपके प्रश्न का उत्तर है: $outMsg"
                        }
                    }
                    DetectedLanguage.MARATHI -> {
                        when {
                            outMsg.contains("New Delhi") -> "भारताची राजधानी नवी दिल्ली (New Delhi) आहे."
                            outMsg.contains("Mumbai") -> "महाराष्ट्राची राजधानी मुंबई असून उपराजधानी नागपूर आहे."
                            outMsg.contains("Narendra Modi") -> "भारताचे माननीय पंतप्रधान श्री नरेंद्र मोदी आहेत."
                            outMsg.contains("299,792,458") -> "प्रकाशाचा वेग सेकंदाला सुमारे ३ लाख किलोमीटर (२,९९,७९२ किमी/सेकंद) आहे."
                            outMsg.contains("Mount Everest") -> "जगातील सर्वात उंच पर्वत शिखर माउंट एव्हरेस्ट (८,८४८.८६ मीटर) आहे."
                            outMsg.contains("9.80665") -> "पृथ्वीचे प्रमाण गुरुत्वाकर्षण सुमारे ९.८ m/s² आहे."
                            outMsg.contains("3.14159") -> "पाय (π) ची किंमत सुमारे ३.१४१५९ आहे."
                            outMsg.contains("Photosynthesis") -> "प्रकाश संश्लेषण (Photosynthesis) द्वारे वनस्पती सूर्यप्रकाशापासून अन्न तयार करतात."
                            outMsg.contains("Pratimesh Tonde") -> "नमस्कार! मला AIX TECH मध्ये डेव्हलपर प्रतिमेश तोंडे (Pratimesh Tonde) यांनी बनवले आहे."
                            else -> "नक्कीच, या प्रश्नाचे उत्तर खालीलप्रमाणे आहे: $outMsg"
                        }
                    }
                    DetectedLanguage.ENGLISH -> "Certainly! Here is what you requested: $outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, aapke prashna ka uttar hai: $outMsg"
                }
            }
            "translate_text" -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, अनुवाद पूर्ण हो गया है: $outMsg"
                    DetectedLanguage.MARATHI -> "होय, भाषांतर पूर्ण झाले आहे: $outMsg"
                    DetectedLanguage.ENGLISH -> "Translation complete: $outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, translated text: $outMsg"
                }
            }
            else -> {
                when (lang) {
                    DetectedLanguage.HINDI -> "जी, $outMsg"
                    DetectedLanguage.MARATHI -> "होय, $outMsg"
                    DetectedLanguage.HINGLISH -> "Ji, $outMsg"
                    DetectedLanguage.ENGLISH -> "Certainly! $outMsg"
                }
            }
        }
    }
}
