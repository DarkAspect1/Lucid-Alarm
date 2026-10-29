<div align="center">

  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Lucid Clock App Icon" width="160" height="160" />

  # Lucid Alarm
  ### A Fully customizable Alarm App for Android

  <p align="center">
    <b>Deep Alarm Ringing Screen Customization • Animated Backgrounds • Custom Wallpapers</b><br/>
    <b>Alarm Groups •  Multi-Week Repeat • Multi-Select • Filters and Sorting • Recycle Bin</b><br/>
    <b>Stopwatch with Laps • Timer with Presets • World Clock • Interactive Live Activities (Nowbar)</b><br/>
    <b>Post-Alarm Wake Check • Upcoming Alarm Alerts • Custom Vibration Patterns • Custom Ringtones • Custom Fonts</b><br/>
    <b>HSV Color Picker • Dynamic Accent Tinting • JSON Backup and Restore</b>
  </p>

  <p align="center">
    <a href="https://github.com/DarkAspect1/Lucid-Alarm/releases"><img src="https://img.shields.io/github/v/release/DarkAspect1/Lucid-Alarm?style=for-the-badge&logo=android&logoColor=white&color=E05D44" alt="Release Version" /></a>
    <a href="https://developer.android.com/about/versions/12"><img src="https://img.shields.io/badge/Android-12%2B%20(API%2031%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Target" /></a>
    <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.04-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  </p>

  <p align="center">
    <a href="https://github.com/DarkAspect1/Lucid-Alarm/stargazers"><img src="https://img.shields.io/github/stars/DarkAspect1/Lucid-Alarm?style=flat-square&color=007EC6" alt="GitHub Stars" /></a>
    <a href="https://github.com/DarkAspect1/Lucid-Alarm/commits"><img src="https://img.shields.io/github/last-commit/DarkAspect1/Lucid-Alarm?style=flat-square" alt="Last Commit" /></a>
    <a href="https://github.com/DarkAspect1/Lucid-Alarm/issues"><img src="https://img.shields.io/github/issues/DarkAspect1/Lucid-Alarm?style=flat-square" alt="Open Issues" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/DarkAspect1/Lucid-Alarm?style=flat-square" alt="License" /></a>
  </p>

  <p align="center">
    <b>
      <a href="#Screenshots">Screenshots</a> &nbsp;|&nbsp;
      <a href="#features">Features</a> &nbsp;|&nbsp;
      <a href="#alarm-customization">Alarm Customization</a> &nbsp;|&nbsp;
      <a href="#nowbar-and-live-activities">Nowbar</a> &nbsp;|&nbsp;
      <a href="#stopwatch-timer-and-world-clock">StopWatch-Timer-World Clock</a> &nbsp;|&nbsp;
      <a href="#alarm-options--notifications">Alarm Options & Notifications</a> &nbsp;|&nbsp;
      <a href="#backup-and-restore">Backup and Restore</a> &nbsp;|&nbsp;
      <a href="#installation--build-requirements">Installation & Build Requirements</a> &nbsp;|&nbsp;
    </b>
  </p>

</div>

---

## Overview

Lucid Alarm is an open-source Android application built with Jetpack Compose. Designed to extend the Samsung's One UI Alarm App aesthetic, it focuses on removing visual clutter while keeping the app fully customizable.

Everything adapts to the user's chosen accent color including Dark Mode Wallpaper, Nowbar, and Settings

---

## Screenshots

<p align="center">
  <img src="Assets/1.jpg" width="15%" alt="Alarms" />
  <img src="Assets/25.jpg" width="15%" alt="World clock" />
  <img src="Assets/3.jpg" width="15%" alt="Stopwatch" />
  <img src="Assets/4.jpg" width="15%" alt="Stopwatch laps" />
  <img src="Assets/5.jpg" width="15%" alt="Timer" />
  <img src="Assets/6.jpg" width="15%" alt="Timer running" />
</p>

<p align="center">
  <img src="Assets/7.jpg" width="15%" alt="Alarms, light theme" />
  <img src="Assets/8.jpg" width="15%" alt="World clock, light theme" />
  <img src="Assets/9.jpg" width="15%" alt="Stopwatch, light theme" />
  <img src="Assets/10.jpg" width="15%" alt="Timer, light theme" />
  <img src="Assets/27.jpg" width="15%" alt="Recycle bin" />
  <img src="Assets/23.jpg" width="15%" alt="Recycle bin selection" />
</p>

<p align="center">
  <img src="Assets/21.jpg" width="30%" alt="Pink accent" />
  <img src="Assets/20.jpg" width="30%" alt="Teal accent" />
  <img src="Assets/19.jpg" width="30%" alt="Gold accent" />
</p>

<p align="center">
  <img src="Assets/11.jpg" width="18%" alt="Ringing screen, vertical layout" />
  <img src="Assets/12.jpg" width="18%" alt="Ringing screen, horizontal layout" />
  <img src="Assets/13.jpg" width="18%" alt="Dismiss button styling" />
  <img src="Assets/14.jpg" width="18%" alt="Snooze button styling" />
  <img src="Assets/15.jpg" width="18%" alt="Background style" />
</p>

<p align="center">
  <img src="Assets/24.jpg" width="30%" alt="Clock settings" />
  <img src="Assets/26.jpg" width="30%" alt="New alarm" />
  <img src="Assets/16.jpg" width="30%" alt="Alarm appearance options" />
</p>

<p align="center">
  <img src="Assets/30.jpg" width="90%" alt="Alarm toast" />
</p>

---

## Features

### Design System and UI Architecture

| Feature | Description |
| :--- | :--- |
| **Full Alarm Customization** | Design your ringing screen with clock layouts, animated or custom backgrounds, a full color picker, gradients, custom fonts, button styling, and a resizable clock and date, applied globally or per alarm. |
| **Wake Check** | After an alarm is dismissed, a notification asks you to confirm you're awake within a set time. If you don't answer, the alarm rings again at maximum volume and loops until you tap "I'm Awake" in the notification. |
| **Multi-week repeat:** | Set an alarm to repeat every 2 weeks (one week on, one week off), every 3 weeks, or any custom number of weeks. Choose whether the cycle starts this week or next time. |
| **Accent Color Theming** | Choose an accent color and the app adapts around it, including Nowbar and Dark Mode background. |
| **Nowbar Support** | Live stopwatch and timer pills with lap, pause, resume, and cancel controls and no need to open the app. |
| **Multi-Week Repeat** | Repeat an alarm every 2 weeks (one week on, one week off), every 3 weeks, or any custom number of weeks. Choose whether the cycle starts this week or next time. |
| **Alarm Groups and Recycle Bin** | Organize alarms into groups with bulk switches, and restore deleted alarms individually or all at once. |
| **Translation Languages** | English, Arabic, Turkish, Italian, Spanish, French, and German, with full RTL support for Arabic. |
| **Drag-and-drop reordering** | In selection mode, hold an alarm's selection box and drag it to a new position. Reordering only starts from the selection box, so you never move an alarm by accident.
| **Floating Controls** | A floating back button appears when the header scrolls out of view, and a back-to-top button appears on long lists. |
| **Borderless Design Language** | Cards, menus, floating buttons, and pills use no outlines, reducing visual clutter across the interface. |
| **Edge Fade Masks** | Alpha masks to let scrollable content fade naturally at the top and bottom of the viewport. |

### Selection and Organization

* **Multi-select batch actions:** Long-press any alarm or world clock card to enter selection mode, with the following actions available:
  * Select all / deselect all
  * Move to group
  * Duplicate
  * Toggle on or off
  * Move to recycle bin
* **Two-column alarm groups:** Groups render in a two-column grid (`groups.chunked(2)`) with live alarm counts, outline status icons (`Alarm` / `AlarmOff`), and bulk group switches.
* **Filtering and sorting:**
  * Status filter: All, Turned On, or Turned Off
  * Day filter: Restrict alarms to specific days of the week
  * Sort order: Alarm Time, Alarm Day, or drag-and-drop Custom Order
* **Recycle bin:** A two-stage deletion workflow supporting individual restore, bulk restore, and permanent deletion.

---

## Alarm Customization

The capability of Lucid Clock is its alarm personalization engine, configurable globally or per alarm.

### Clock Layouts and Canvas Scaling

* **Six selectable layouts:**
  * Horizontal
  * Vertical Stacked
  * Bold Horizontal
  * Bold Vertical
  * Compact Analog
  * Numbered Analog

### Interactive Background Engine

* **Built-in ambient styles:**
  * Dark Midnight Gradient and Pure AMOLED Black
  * Warm Sunrise and Dynamic Fluid Gradient
  * Starfield Particle Drift (physics-based particle layers)
  * Interactive Mesh Gradient and Interactive Aurora Borealis
  * Interactive Fireflies and Solid Color Palette
* **Custom media:**
  * Custom image wallpaper loader
  * Looping, muted custom video wallpaper (`.mp4`, `.mkv`) with hardware decoding
* **Dimming & Blur control**

### Coloring and Element Control

* **Color selection:** A 24-preset curated grid, or a full HSV spectrum with hex input (`#RRGGBB`).
* **Color and gradient options for:**
  * Clock
  * Date display
  * Dismiss button (container and label)
  * Snooze button (container and label)
* **Custom font support:** Load any `.ttf`, `.otf`, or `.apk` font file from device storage.
* **Display controls:** Independent visibility toggles for the date, AM/PM indicator, and alarm label.
* **Reset Button:** Moved or resized the clock or date too far? One tap returns both to their default position and size.

---

## Nowbar and Live Activities

* **Floating heads-up pill:** Real-time countdown and stopwatch counters rendered on the Nowbar.
* **Accent color adaptation:** The Nowbar automatically adopts the app’s accent color, so the floating pill and its controls always match your chosen theme.
* **In-place controls without launching the app:**
  * Stopwatch: Lap, Stop, resume, or cancel
  * Timer: Pause, Resume, and Cancel
* **Multi-instance stacking:** The stopwatch and timer can run simultaneously in stacked floating capsules.

---

## Stopwatch, Timer, and World Clock

| Component | Key Capabilities |
| :--- | :--- |
| **Stopwatch** | Dual-mode horizontal pager with analog dial and minimal digital views with a Three-column lap table highlighting the fastest (blue) and slowest (red) laps |
| **Timer** | Three-column wheel picker (HH:MM:SS) with 10, 15, and 30 minute presets<br/>Circular progress ring with a bell indicator and completion time<br/>Automatic leading-zero removal (for example, `53s`, `1:07m`, `1:05:09h`) |
| **World Clock** | Centered clock with live seconds and full timezone descriptor<br/>City cards with relative time offsets<br/>Real-time search across the global timezone database |

---

## Alarm Options & Notifications

* **Featured Wake Check:** After an alarm is dismissed, a configurable delay (1 to 15 minutes) triggers a confirmation notification with a countdown to respond, and if it goes unanswered the alarm rings again at maximum volume and awake check notification keeps repeating until you tap “I’m Awake” in the notification, which ends the check.
* **Multi-week repeat:** Set an alarm to repeat every 2 weeks (one week on, one week off), every 3 weeks, or any custom number of weeks. Choose whether the cycle starts this week or next time, and the schedule stays accurate across reboots and time changes.
* **Upcoming alarm alerts:** Dismiss or modify an upcoming alarm directly from its notification, with configurable lead times from 15 to 60 minutes.
* **Alarm toast confirmation:** A toast appears whenever you turn on an alarm or create a new one, showing how much time is left until it rings.
* **Audio and haptics:** Unified or per-alarm volume override, custom ringtone selection, and distinct vibration patterns.

---

## Backup and Restore

Configurations can be exported and restored at any time using a structured JSON schema, that includes: Alarms, World Clocks, Recycle Bin, and Settings/Appearance

```json
{
  "formatVersion": 1,
  "app": "Lucid Clock",
  "exportedAt": 1793290000000,
  "settings": [
    ["theme_mode", "s", "DARK"],
    ["haptic_feedback_enabled", "b", "true"],
    ["snooze_interval_minutes", "i", "5"],
    ["awake_check_delay_minutes", "i", "2"]
  ],
  "alarms": [
    {
      "id": 1,
      "hour": 7,
      "minute": 30,
      "label": "Work Alarm",
      "isEnabled": true,
      "repeatDays": "MON,TUE,WED,THU,FRI",
      "groupName": "Morning",
      "soundUri": "content://media/internal/audio/media/42",
      "soundTitle": "Default",
      "vibrationPattern": "HEARTBEAT",
      "vibrationEnabled": true,
      "snoozeEnabled": true,
      "snoozeMinutes": 5,
      "snoozeLimit": 3,
      "areYouAwakeEnabled": true,
      "areYouAwakeMinutes": 2,
      "upcomingNotificationEnabled": true,
      "upcomingNotificationLeadMinutes": 30,
      "weekInterval": 1,
      "anchorWeekStartEpochDay": 0,
      "customAppearanceEnabled": false,
      "volumeLevel": 0.85,
      "sortOrder": 0,
      "deletedAt": 0,
      "trashedGroup": ""
    }
  ],
  "worldClock": [
    "America/New_York",
    "Europe/London",
    "Asia/Tokyo"
  ],
  "recycleBin": [
    {
      "zoneId": "Australia/Sydney",
      "deletedAt": 1793285000000
    }
  ]
}
```

---

| Area | Implementation |
| :--- | :--- |
| **UI framework** | 100% Jetpack Compose built on Material 3 base |
| **State management** | Kotlin Coroutines, StateFlow, and Jetpack ViewModel |
| **Persistence** | Room (alarms, recycle bin, groups) and Jetpack DataStore (preferences) |
| **Minification** | R8 and ProGuard rules protecting Room entities and JSON serialization models |

---

## Installation & Build Requirements

* Android Studio Ladybug or Meerkat, or newer
* JDK 17 or newer
* Android SDK 34 or newer
