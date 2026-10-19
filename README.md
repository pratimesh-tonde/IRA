<div align="center">

<img src="docs/assets/ira-banner.png" alt="IRA Banner" width="100%"/>

# 🌟 IRA : Intelligent Reasoning Autonomous Agent

### 100% On-Device • Zero-Cloud • Zero-Telemetry • Ultra-Private Autonomous AI

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](#)
[![Material 3](https://img.shields.io/badge/Material%203-Glassmorphism-7C3AED?style=for-the-badge&logo=materialdesign&logoColor=white)](#)
[![Offline](https://img.shields.io/badge/Mode-100%25%20Offline-00F5FF?style=for-the-badge)](#)
[![Privacy](https://img.shields.io/badge/Telemetry-Zero-10B981?style=for-the-badge)](#)

**Lead Architect & Developer:** [Pratimesh Tonde](https://github.com/pratimesh-tonde) &nbsp;|&nbsp; **Organization:** AIX TECH

[Overview](#-overview) •
[Features](#-key-features) •
[Architecture](#-architecture) •
[Tools](#-tool-ecosystem-30) •
[Build](#-build--install) •
[Structure](#-project-structure) •
[Permissions](#-permissions)

</div>

---

## 🚀 Overview

**IRA** is an advanced, offline-first personal AI built natively for Android. Unlike cloud-dependent assistants (Google Assistant, Alexa, Siri), IRA understands instructions, plans multi-step tasks, controls Android hardware APIs, and speaks back **without sending a single byte of personal data over the internet.**

> 💡 Airplane mode on? IRA still works perfectly.

## ✨ Key Features

| | Feature | Description |
|---|---|---|
| 🔒 | **Zero Data Leakage** | Reasoning, speech recognition, text-to-speech, and tool execution all run locally on the device. |
| ⚡ | **Instant Execution** | No network latency, no API quotas, fully functional offline. |
| 🗣️ | **Trilingual Support** | Understands and replies by voice in English, Hindi (हिंदी), Marathi (मराठी), and Hinglish. |
| 🧩 | **Autonomous Multi-Step Planning** | Breaks complex prompts into sequential tool calls with observation checks. |
| 🛡️ | **Human-in-the-Loop Safety** | High-risk actions require explicit user approval before execution. |
| 💎 | **Glassmorphic UI** | Frosted glass surfaces, Aurora gradients, neon accents, and a 3D Holographic Orb. |

### 🎯 Example Command

```text
"Check my battery, turn on flashlight, take a note, and set an alarm for 7 AM"
```

IRA turns it into this plan:

```text
Plan = [ battery_check → flashlight_toggle → quick_note_save → alarm_create ]
```

Each step is validated. If one fails, IRA recovers automatically or informs you.

---

## 🧠 Architecture

```text
┌──────────────────────────────────────────┐
│   USER INPUT (Voice STT / Text)          │
└────────────────────┬─────────────────────┘
                     ▼
┌──────────────────────────────────────────┐
│   IraViewModel & StateFlow               │
└──────────┬───────────────────┬───────────┘
           ▼                   ▼
┌─────────────────────┐ ┌──────────────────────┐
│ Offline Reasoning   │◄►│ Multi-Agent          │
│ Engine (ReAct)      │ │ Coordinator          │
└──────────┬──────────┘ └──────────────────────┘
           ▼
┌──────────────────────────────────────────┐
│   Tool Execution Registry (30+ tools)    │
└──────────┬───────────────────┬───────────┘
   HIGH    ▼                   ▼    LOW
┌─────────────────────┐ ┌──────────────────┐
│ Confirmation Dialog │ │ Direct Execution │
└──────────┬──────────┘ └────────┬─────────┘
           └──────────┬──────────┘
                      ▼
┌──────────────────────────────────────────┐
│   Audit & Memory (Room DB, DataStore)    │
└────────────────────┬─────────────────────┘
                     ▼
┌──────────────────────────────────────────┐
│   Output (TTS, Orb Animation, UI)        │
└──────────────────────────────────────────┘
```
---

## 🔍 Deep Dive

<details>
<summary><b>1️⃣ Offline Reasoning & Planning Engine</b></summary>

- **File:** `app/src/main/java/com/example/agent/OfflineReasoningEngine.kt`
- **Pattern:** ReAct (Reasoning + Acting + Observing)

**Mechanisms**
- **NLP Intent Classifier:** Normalizes English, Hindi, and Marathi input, extracting entities, values, units, and time references.
- **Multi-Step Decomposer:** Converts multi-intent requests into an ordered plan:
  `Plan = [S1(tool1, args1), S2(tool2, args2), …, Sn(tool_n, args_n)]`
- **Observation & Reflection Loop:** After each step, checks for `ToolResult.Success` or `ToolResult.Failure`, then triggers autonomous recovery or informs the user.

</details>

<details>
<summary><b>2️⃣ Multi-Agent Delegation Model</b></summary>

| Agent | Responsibilities |
|---|---|
| 🛡️ **System Sentinel** | Battery, storage, RAM/CPU load, Wi-Fi, Bluetooth, volume, flashlight, brightness |
| 💼 **Productivity Specialist** | Alarms, calendar events, timers, quick notes, calculations, unit conversions |
| 💬 **Communication Officer** | Offline calls, SMS preparation, contact lookup, emergency SOS |
| 📚 **Knowledge Curator** | Trilingual encyclopedia queries, definitions, diagnostics, session recall |

</details>

<details>
<summary><b>3️⃣ Audio & Speech Pipeline</b></summary>

- **File:** `app/src/main/java/com/example/audio/OfflineSpeechManager.kt`
- **STT:** Android `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = true`
- **TTS:** Low-latency local synthesis with pitch and speech-rate control for `en-IN / en-US`, `hi-IN`, and `mr-IN`
- **Wake Word:** Background listening via `IraBackgroundService` for phrases like **"Hey IRA"**, **"नमस्ते इरा"**, and **"ऐक इरा"**

</details>

<details>
<summary><b>4️⃣ Human-in-the-Loop & Safety Engine</b></summary>

- **File:** `app/src/main/java/com/example/agent/ConfirmationDialog.kt`
- Critical actions (phone calls, SMS, deleting notes, launching external intents) are tagged **HIGH risk**.
- Execution is suspended and a frosted-glass modal asks for explicit user approval (touch/biometric) before touching the OS API.

</details>

<details>
<summary><b>5️⃣ Persistent Storage & Memory Vault</b></summary>

**Engine:** Jetpack Room (SQLite) + DataStore Preferences

| Entity | Purpose |
|---|---|
| `AuditLogEntity` | Immutable, timestamped record of every command, tool, arguments, and outcome |
| `NoteEntity` | Locally encrypted notes and memory snippets |
| `AgentPreference` | Persona tone, speech language, vibration feedback, custom hotwords |

</details>

<details>
<summary><b>6️⃣ Glassmorphic UI & Design System</b></summary>

**Palette**

| Color | Hex | Usage |
|---|---|---|
| Obsidian Canvas | `#080C14` | Deep space background |
| Frosted Glass | `#1E293B` (30–50% alpha) | Cards with 1dp iridescent borders |
| Cyan Aurora | `#00F5FF` | Primary accent |
| Neon Violet | `#7C3AED` | Secondary accent |
| Emerald Glow | `#10B981` | Success |
| Amber Warning | `#F59E0B` | Warnings |

**Components**
- 🔮 **HolographicOrb:** 3D canvas with rotation, pulsation, radial refraction, and a particle aura reacting to agent states: `IDLE`, `LISTENING`, `THINKING`, `EXECUTING`, `SPEAKING`
- 🧠 **ThinkingProcessCard:** Collapsible step-by-step view of the agent's reasoning and tool chain
- 🧭 **FloatingGlassNavigationBar:** Translucent pill dock with a glowing active indicator

</details>

---

## 🛠️ Tool Ecosystem (30+)

> **File:** `app/src/main/java/com/example/agent/ToolRegistry.kt`

| Category | Tools | APIs Used |
|---|---|---|
| ⚙️ **System & Hardware** | `flashlight_toggle` `battery_check` `wifi_toggle` `bluetooth_toggle` `volume_control` `brightness_set` `vibrate_pattern` | `CameraManager`, `BatteryManager`, `WifiManager`, `AudioManager` |
| 💼 **Productivity** | `alarm_create` `timer_start` `calendar_add_event` `quick_note_save` `calculator_eval` `stopwatch_control` | `AlarmClock`, `CalendarContract`, Room DB |
| 📞 **Communication** | `call_phone_contact` `send_sms_message` `search_contacts` `emergency_sos` | `TelecomManager`, `SmsManager`, `ContactsContract` |
| 📡 **Sensors & Telemetry** | `compass_heading` `step_counter` `light_sensor_lux` `device_specs` | `SensorManager` |
| 🧪 **Device Intelligence** | `storage_analyzer` `app_launcher` `memory_cleanup_stats` `network_speed_ping` | Native system diagnostics, no third-party SDKs |

---

###  Gradle CLI

```bash
# Clone the repository
git clone https://github.com/pratimesh-tonde/IRA.git
cd IRA

# Debug APK
gradle :app:assembleDebug

# Release APK
gradle :app:assembleRelease

# Unit & architecture tests
gradle :app:testDebugUnitTest
```

### 📍 APK Output Paths

| Build | Path |
|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

### 📲 Sideloading
1. Transfer `app-debug.apk` to your phone via USB, Bluetooth, or Google Drive.
2. Tap the APK and choose **Install** (enable *"Allow from this source"* if prompted).
3. Open IRA and grant the requested permissions.

---

## 📁 Project Structure

```text
IRA/
├── app/
│   ├── build.gradle.kts                 # Dependencies, SDK versions, compiler flags
│   └── src/main/
│       ├── AndroidManifest.xml          # Permissions, background service, launcher intent
│       ├── java/com/example/
│       │   ├── MainActivity.kt          # Entry point, WindowInsets, Navigation host
│       │   ├── IraApp.kt                # Application class & initialization
│       │   ├── agent/
│       │   │   ├── AgentState.kt                # Sealed UI states
│       │   │   ├── OfflineReasoningEngine.kt    # NLP parser, planner, ReAct loop
│       │   │   ├── ToolRegistry.kt              # 30+ Android hardware & system tools
│       │   │   ├── SubAgentCoordinator.kt       # Domain agent delegation
│       │   │   ├── IraBackgroundService.kt      # Foreground wake-word service
│       │   │   └── ConfirmationDialog.kt        # Human-in-the-loop modal
│       │   ├── audio/
│       │   │   └── OfflineSpeechManager.kt      # Trilingual STT & TTS
│       │   ├── data/
│       │   │   ├── IraDatabase.kt               # Room DB config
│       │   │   ├── AuditLogEntity.kt            # Audit log model
│       │   │   ├── AuditDao.kt                  # DB access operations
│       │   │   └── IraRepository.kt             # Data repository bridge
│       │   ├── viewmodel/
│       │   │   └── IraViewModel.kt              # Reactive orchestrator & StateFlow
│       │   └── ui/
│       │       ├── components/
│       │       │   ├── HolographicOrb.kt        # 3D reactive orb
│       │       │   └── ThinkingProcessCard.kt   # Reasoning plan UI
│       │       ├── screens/
│       │       │   ├── MainAgentScreen.kt       # Terminal & voice dock
│       │       │   ├── ToolDeckScreen.kt        # Grid of 30+ tools
│       │       │   ├── MemoryVaultScreen.kt     # Encrypted notes & history
│       │       │   ├── AuditSafetyScreen.kt     # Tool logs & safety controls
│       │       │   └── SettingsScreen.kt        # Language, wake-word, theme
│       │       └── theme/
│       │           ├── Color.kt                 # Obsidian & neon palette
│       │           ├── Theme.kt                 # Material3 dark theme
│       │           └── Type.kt                  # Typography
│       └── res/
│           ├── values/strings.xml
│           └── mipmap-*/                        # Adaptive launcher icons
├── gradle/libs.versions.toml            # Version catalog
├── metadata.json                        # AI Studio project metadata
├── settings.gradle.kts                  # Root project & plugin repositories
└── README.md
```

---

## 🔒 Permissions

| Permission | Scope | Why it's needed |
|---|---|---|
| `RECORD_AUDIO` | Runtime | Voice input for offline Speech-to-Text |
| `CALL_PHONE` | Runtime | Starts calls only when explicitly ordered |
| `SEND_SMS` | Runtime | Prepares or sends SMS after user approval |
| `READ_CONTACTS` | Runtime | Offline contact search by name |
| `CAMERA` | Normal | Used only for the flashlight toggle |
| `VIBRATE` | Normal | Haptic feedback for buttons, wake-word, and completion |
| `FOREGROUND_SERVICE` | Normal | Continuous wake-word listener in the background |
| `POST_NOTIFICATIONS` | Runtime | Persistent status notification for the background service |

---

## 🧰 Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Coroutines](https://img.shields.io/badge/Coroutines%20%2F%20Flow-0095D5?style=flat-square&logo=kotlin&logoColor=white)
![Room](https://img.shields.io/badge/Room%20DB-003B57?style=flat-square&logo=sqlite&logoColor=white)
![Material3](https://img.shields.io/badge/Material%203-757575?style=flat-square&logo=materialdesign&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)

---

## 📸 Screenshots

<div align="center">

| Main Agent | Tool Deck | Memory Vault | Audit & Safety |
|:---:|:---:|:---:|:---:|
| <img src="docs/assets/main.png" width="200"/> | <img src="docs/assets/tools.png" width="200"/> | <img src="docs/assets/vault.png" width="200"/> | <img src="docs/assets/audit.png" width="200"/> |

</div>

---

## 📜 License & Credits

**Lead Architect & Developer:** Pratimesh Tonde
**Organization:** AIX TECH
**Copyright:** © 2026 AIX TECH. All rights reserved.

🔐 *Privacy Assurance: free from third-party tracking, telemetry, or external cloud dependencies.*

<div align="center">

**⭐ If you like IRA, please give the repo a star! ⭐**

Made with ❤️ by **AIX TECH**

</div>
