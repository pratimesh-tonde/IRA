# 🌟 IRA — Intelligent Reasoning Autonomous Agent

> **100% On-Device • Zero-Cloud • Zero-Telemetry • Ultra-Private Autonomous AI**  
> **Lead Architect & Developer:** Pratimesh Tonde  
> **Organization:** AIX TECH  
> **Technology Stack:** Kotlin, Jetpack Compose, Coroutines/Flow, Room DB, Material 3 Glassmorphism

---

## 📑 Table of Contents
1. [Project Overview](#-project-overview)
2. [Core Philosophy & Architecture](#-core-philosophy--architecture)
3. [Deep Architectural Breakdown](#-deep-architectural-breakdown)
   - [1. Offline Reasoning & Planning Engine](#1-offline-reasoning--planning-engine)
   - [2. Multi-Agent Delegation Model](#2-multi-agent-delegation-model)
   - [3. Comprehensive Tool Ecosystem (30+ Tools)](#3-comprehensive-tool-ecosystem-30-tools)
   - [4. Audio & Speech Processing Pipeline](#4-audio--speech-processing-pipeline)
   - [5. Human-in-the-Loop & Safety Engine](#5-human-in-the-loop--safety-engine)
   - [6. Persistent Storage & Memory Vault](#6-persistent-storage--memory-vault)
   - [7. Glassmorphic UI & Design System](#7-glassmorphic-ui--design-system)
4. [APK File & Build Guide](#-apk-file--build-guide)
   - [Downloading APK from AI Studio](#downloading-apk-from-ai-studio)
   - [Building APK via Gradle CLI](#building-apk-via-gradle-cli)
   - [APK Output Paths](#apk-output-paths)
   - [Device Sideloading & Permissions](#device-sideloading--permissions)
5. [Directory & File Tree](#-directory--file-tree)
6. [Hardware & Runtime Permissions](#-hardware--runtime-permissions)
7. [License & Credits](#-license--credits)

---

## 🚀 Project Overview

**IRA (Intelligent Reasoning Autonomous Agent)** is an advanced, offline-first personal artificial intelligence built natively for the Android platform. Unlike cloud-dependent voice assistants (Google Assistant, Alexa, Siri), IRA processes instructions, plans multi-step executions, interacts with Android hardware APIs, and generates real-time audio responses **without sending a single byte of personal data over the internet**.

### Key Highlights:
* 🔒 **Zero Data Leakage:** All reasoning, audio transcription, text-to-speech, and tool execution take place 100% locally on the device.
* ⚡ **Instant Execution:** Zero network latency, zero cloud API quotas, and fully operational in airplane mode.
* 🗣️ **Trilingual Natural Language Support:** Native comprehension and voice response in **English**, **Hindi (हिंदी)**, **Marathi (मराठी)**, and **Hinglish**.
* 🛡️ **Autonomous Multi-Step Planning:** Deconstructs complex user prompts (e.g., *"Check my battery, turn on flashlight, take a note, and set an alarm for 7 AM"*) into sequential tool invocations with observation validation.
* 💎 **Ultra-Modern Glassmorphic UI:** Fluid frosted glass surfaces, ambient Aurora gradient backgrounds, luminous cyan/violet neon accents, and a dynamic 3D-effect Holographic Orb.

---

## 🧠 Core Philosophy & Architecture

```
                       ┌──────────────────────────────────────────────┐
                       │           USER MULTI-MODAL INPUT            │
                       │    (Voice STT / Text Input / Suggestion)     │
                       └──────────────────────┬───────────────────────┘
                                              │
                                              ▼
                       ┌──────────────────────────────────────────────┐
                       │          IraViewModel & StateFlow            │
                       │          (Agent State & Orchestration)       │
                       └──────────────────────┬───────────────────────┘
                                              │
                      ┌───────────────────────┴───────────────────────┐
                      ▼                                               ▼
     ┌────────────────────────────────┐              ┌────────────────────────────────┐
     │    Offline Reasoning Engine    │              │    Multi-Agent Coordinator     │
     │   - Intent Classification      │◄────────────►│   - System Sentinel            │
     │   - Multi-Step Plan Generator  │              │   - Productivity Specialist    │
     │   - ReAct Execution Loop       │              │   - Communication Officer      │
     └────────────────┬───────────────┘              │   - Knowledge Curator          │
                      │                              └────────────────────────────────┘
                      ▼
     ┌────────────────────────────────────────────────────────────────┐
     │                     Tool Execution Registry                    │
     │             (30+ Native Android Hardware & OS APIs)            │
     └────────────────┬──────────────────────────────┬────────────────┘
                      │                              │
         [Risk: HIGH] │                 [Risk: LOW]  │
                      ▼                              ▼
     ┌────────────────────────────────┐  ┌────────────────────────────┐
     │ Confirmation Dialog (HITL)     │  │ Direct Execution & State   │
     │ (User Approves / Rejects)      │  │ Observation Capture        │
     └────────────────┬───────────────┘  └───────────┬────────────────┘
                      │ (Approved)                   │
                      └───────────────┬──────────────┘
                                      ▼
     ┌────────────────────────────────────────────────────────────────┐
     │                     Audit & Memory Layer                       │
     │    - Room Database (Tool Execution Logs & Session Memory)      │
     │    - DataStore (User Preferences & Persona Tuning)             │
     └────────────────┬───────────────────────────────────────────────┘
                      │
                      ▼
     ┌────────────────────────────────────────────────────────────────┐
     │                   Output & Response Synthesizer                │
     │   - Natural Language Response Generator (Trilingual)           │
     │   - Offline TTS Audio Engine (Hindi / Marathi / English)       │
     │   - Glassmorphic UI Update (Orb Animation & Telemetry)         │
     └────────────────────────────────────────────────────────────────┘
```

---

## 🔍 Deep Architectural Breakdown

### 1. Offline Reasoning & Planning Engine
* **File:** `app/src/main/java/com/example/agent/OfflineReasoningEngine.kt`
* **Pattern:** ReAct (Reasoning + Acting + Observing)
* **Mechanisms:**
  1. **Pattern Matching & NLP Intent Classifier:** Normalizes incoming natural language in English, Hindi, and Marathi, extracting named entities, values, units, and temporal references.
  2. **Multi-Step Decomposer:** If a request involves multiple intents, the engine creates an ordered directed execution plan:
     $$\text{Plan} = [S_1(tool_1, args_1), S_2(tool_2, args_2), \dots, S_n(tool_n, args_n)]$$
  3. **Observation & Reflection Loop:** After executing each step, IRA checks the tool's return status (`ToolResult.Success` or `ToolResult.Failure`). If a step fails, the engine triggers an autonomous recovery or informs the user.

### 2. Multi-Agent Delegation Model
IRA features specialized virtual sub-agents to process domain-specific operations:
* 🛡️ **System Sentinel:** Battery status, storage monitoring, RAM/CPU load, Wi-Fi, Bluetooth, volume controls, flashlight, and display brightness.
* 💼 **Productivity Specialist:** Alarms, calendar events, timer routines, quick note taking, calculations, and unit conversions.
* 💬 **Communication Officer:** Direct offline calls, SMS dispatch preparation, contact lookup, and emergency SOS broadcasts.
* 📚 **Knowledge Curator:** Trilingual encyclopedia queries, definitions, device diagnostics, and historical session recall.

### 3. Comprehensive Tool Ecosystem (30+ Tools)
* **File:** `app/src/main/java/com/example/agent/ToolRegistry.kt`

| Category | Tools & APIs | Description |
| :--- | :--- | :--- |
| **System & Hardware** | `flashlight_toggle`, `battery_check`, `wifi_toggle`, `bluetooth_toggle`, `volume_control`, `brightness_set`, `vibrate_pattern` | Directly interfaces with `CameraManager`, `BatteryManager`, `WifiManager`, and `AudioManager`. |
| **Productivity** | `alarm_create`, `timer_start`, `calendar_add_event`, `quick_note_save`, `calculator_eval`, `stopwatch_control` | Uses system intents (`AlarmClock`, `CalendarContract`) and internal database persistence. |
| **Communication** | `call_phone_contact`, `send_sms_message`, `search_contacts`, `emergency_sos` | Interacts with `TelecomManager`, `SmsManager`, and `ContactsContract`. |
| **Sensors & Telemetry** | `compass_heading`, `step_counter`, `light_sensor_lux`, `device_specs` | Reads hardware sensor streams via `SensorManager`. |
| **Device Intelligence** | `storage_analyzer`, `app_launcher`, `memory_cleanup_stats`, `network_speed_ping` | Deep system diagnostics with zero third-party SDK overhead. |

### 4. Audio & Speech Processing Pipeline
* **File:** `app/src/main/java/com/example/audio/OfflineSpeechManager.kt`
* **Speech-to-Text (STT):** Integrates Android's native `SpeechRecognizer` configured for offline recognition via `EXTRA_PREFER_OFFLINE = true`.
* **Text-to-Speech (TTS):** Low-latency local voice synthesis with voice pitch and speech rate modulation, dynamically configured for English (`en-IN` / `en-US`), Hindi (`hi-IN`), and Marathi (`mr-IN`).
* **Continuous Listening & Wake Word:** Background listening mode utilizing `IraBackgroundService` to detect wake-phrases like *"Hey IRA"*, *"नमस्ते इरा"*, or *"ऐक इरा"*.

### 5. Human-in-the-Loop & Safety Engine
* **File:** `app/src/main/java/com/example/agent/ConfirmationDialog.kt`
* Critical actions (such as initiating phone calls, sending SMS, deleting notes, or launching external system intents) are categorized with a **HIGH** risk tier.
* The execution engine suspends execution and displays a frosted glass confirmation modal requiring explicit user biometric/touch approval before touching the OS API.

### 6. Persistent Storage & Memory Vault
* **Engine:** Android Jetpack Room (SQLite ORM) + DataStore Preferences.
* **Entities:**
  * `AuditLogEntity`: Immutable timestamped records of every executed command, tool, arguments, and outcome.
  * `NoteEntity`: Locally encrypted user notes and memory snippets.
  * `AgentPreference`: Persona tone, speech language, vibration feedback, and custom hotword triggers.

### 7. Glassmorphic UI & Design System
* **Theme Colors (`Color.kt` & `Theme.kt`):**
  * **Obsidian Canvas:** `#080C14` (Deep space black)
  * **Frosted Glass Cards:** Semi-transparent containers (`#1E293B` at 30–50% alpha) with 1dp iridescent border gradients (`White` at 15% alpha $\to$ `HologramCyan` at 30% alpha).
  * **Luminous Accents:** Cyan Aurora (`#00F5FF`), Neon Violet (`#7C3AED`), Emerald Glow (`#10B981`), Amber Warning (`#F59E0B`).
* **Components:**
  * `HolographicOrb`: Multi-layered 3D Canvas with animated rotation, pulsation, radial refraction, and particle aura responding to agent states (`IDLE`, `LISTENING`, `THINKING`, `EXECUTING`, `SPEAKING`).
  * `ThinkingProcessCard`: Step-by-step collapsible visualization of the agent's internal monologue and tool execution chain.
  * `FloatingGlassNavigationBar`: Translucent rounded pill dock at the bottom of the screen with active indicator glows.

---

## 📦 APK File & Build Guide

### Downloading APK from AI Studio
If you are using Google AI Studio in your browser:
1. Click on the **Settings** or **Export** icon located in the top-right toolbar.
2. Select **"Export as APK / AAB"** or **"Download Project ZIP"**.
3. AI Studio's cloud build pipeline compiles the release APK and provides an instant one-click download link.

### Building APK via Gradle CLI
To build the APK locally or on your own machine:

1. **Clone or unzip the project folder.**
2. **Build Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
3. **Build Release APK:**
   ```bash
   gradle :app:assembleRelease
   ```
4. **Run Unit & Architecture Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

### APK Output Paths
Upon a successful build, the generated APK binary files are located at:
* **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`
* **Release APK:** `app/build/outputs/apk/release/app-release.apk`

### Device Sideloading & Permissions
1. Transfer `app-debug.apk` to your Android device via USB, Bluetooth, or Google Drive.
2. On your device, tap the APK and choose **Install** (Enable *"Allow from this source"* if prompted).
3. Open **IRA** and grant requested permissions (**Microphone** for voice STT, **Phone/SMS** for communication tools, and **Notifications** for background service).

---

## 📁 Directory & File Tree

```
IRA/
├── app/
│   ├── build.gradle.kts                # Module dependencies, SDK versions, compiler flags
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml     # App permissions, background service, launcher intent
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt     # App entry point, WindowInsets, Navigation host
│   │   │   │   ├── IraApp.kt           # Application class & DI / initialization
│   │   │   │   ├── agent/
│   │   │   │   │   ├── AgentState.kt               # Sealed UI states (IDLE, THINKING, etc.)
│   │   │   │   │   ├── OfflineReasoningEngine.kt   # NLP parser, multi-step planner, ReAct loop
│   │   │   │   │   ├── ToolRegistry.kt             # 30+ Android Hardware & System tools
│   │   │   │   │   ├── SubAgentCoordinator.kt      # Domain agent delegation manager
│   │   │   │   │   ├── IraBackgroundService.kt     # Foreground wake-word service
│   │   │   │   │   └── ConfirmationDialog.kt       # Human-in-the-loop frosted glass modal
│   │   │   │   ├── audio/
│   │   │   │   │   └── OfflineSpeechManager.kt     # Trilingual STT & TTS controller
│   │   │   │   ├── data/
│   │   │   │   │   ├── IraDatabase.kt              # Room DB configuration
│   │   │   │   │   ├── AuditLogEntity.kt           # Audit log model
│   │   │   │   │   ├── AuditDao.kt                 # Database access operations
│   │   │   │   │   └── IraRepository.kt            # Data repository bridge
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── IraViewModel.kt             # Reactive orchestrator & StateFlow
│   │   │   │   └── ui/
│   │   │   │       ├── components/
│   │   │   │       │   ├── HolographicOrb.kt       # 3D canvas reactive holographic orb
│   │   │   │       │   └── ThinkingProcessCard.kt  # Step-by-step reasoning plan UI
│   │   │   │       ├── screens/
│   │   │   │       │   ├── MainAgentScreen.kt      # Main terminal & voice interaction dock
│   │   │   │       │   ├── ToolDeckScreen.kt       # Grid of 30+ hardware tools
│   │   │   │       │   ├── MemoryVaultScreen.kt    # Encrypted notes & history
│   │   │   │       │   ├── AuditSafetyScreen.kt    # Immutable tool logs & safety controls
│   │   │   │       │   └── SettingsScreen.kt       # Language, wake-word, and theme selector
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt                # Glassmorphic obsidian & neon palette
│   │   │   │           ├── Theme.kt                # Material3 dark theme setup
│   │   │   │           └── Type.kt                 # Clean modern typography
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   └── strings.xml             # Resource identifiers & app name
│   │   │       └── mipmap-*/                   # Adaptive launcher icons
├── gradle/
│   └── libs.versions.toml              # Version catalog for AndroidX, Compose, Room, Coroutines
├── metadata.json                       # AI Studio project identity metadata
├── settings.gradle.kts                 # Root project definition and plugin repositories
└── README.md                           # Comprehensive documentation and architecture
```

---

## 🔒 Hardware & Runtime Permissions

| Permission | Scope | Justification |
| :--- | :--- | :--- |
| `RECORD_AUDIO` | Runtime | Captures voice input for offline Speech-to-Text (`SpeechRecognizer`). |
| `CALL_PHONE` | Runtime | Allows initiating phone calls when explicitly ordered by user. |
| `SEND_SMS` | Runtime | Prepares or sends SMS messages following user approval. |
| `READ_CONTACTS` | Runtime | Enables offline search of phone contacts by name. |
| `CAMERA` | Normal | Required exclusively to operate the device flashlight toggle. |
| `VIBRATE` | Normal | Haptic feedback for button presses, wake-word alerts, and completion. |
| `FOREGROUND_SERVICE` | Normal | Runs continuous wake-word listener service in the background. |
| `POST_NOTIFICATIONS` | Runtime | Displays persistent status notification for foreground background service. |

---

## 📜 License & Credits

* **Lead Architect & Developer:** Pratimesh Tonde
* **Organization:** AIX TECH
* **Copyright:** © 2026 AIX TECH. All rights reserved.
* **Privacy Assurance:** Free from third-party tracking, telemetry, or external cloud dependencies.
