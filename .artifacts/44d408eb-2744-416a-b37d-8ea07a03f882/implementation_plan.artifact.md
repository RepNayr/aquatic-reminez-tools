# Implementation Plan — Aquatic Reminez Tools Settings Ecosystem (Stages 01–04)

This implementation plan outlines the complete, production-oriented Settings ecosystem for Aquatic Reminez Tools across Stages 01 to 04, built using Kotlin and XML layouts adhering to the app's dark glassmorphism design system.

## User Review Required

> [!IMPORTANT]
> **Architecture & Navigation**: The Settings ecosystem will feature a central hub (`SettingsActivity`) with 4 top-level sections corresponding to Stages 01–04, branching into dedicated management activities/screens for deep configuration, ensuring zero clutter and high maintainability.
>
> **Persistence**: A centralized `SettingsPreferences` manager will persist all appearance, display, region, security preferences, tool visibility, favorites, collections, presets, workflows, and workspace profiles using `SharedPreferences` and local Room/JSON storage.

## Open Questions

- None. All requirements, feature groups, and explicit exclusions from Stages 01–04 are fully mapped and accounted for.

## Proposed Changes

### Core Settings Infrastructure & Preferences
- [NEW] [SettingsPreferences.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/core/settings/SettingsPreferences.kt)
  - Manages persistent key-value storage for themes, accent colors, wallpaper, display density, font size, language, accessibility options, security timeouts, activity retention, tool visibility, favorites, collections, presets, workflows, and workspace profiles.

### Stage 01 — Appearance & Personalization
- [NEW] [AppearanceSettingsActivity.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/presentation/settings/AppearanceSettingsActivity.kt)
- [NEW] [activity_appearance_settings.xml](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/res/layout/activity_appearance_settings.xml)
  - Implements Appearance & Theme (Dark, Light, System, Accent color presets: Cyan, Teal, Violet, Blue, Emerald, Custom picker, Wallpaper selection, Opacity, Glass transparency, Blur intensity), Display & Layout (Font size, UI density, Card style, Corner radius, Reduce motion, Live preview), Language & Region (Bahasa Indonesia, English, Date/Time formats), Accessibility & Performance (Larger text, reduce transparency, high contrast, lightweight effects), and Personalization Presets (Ocean Cyber, Neon Violet, Emerald, Clean Light).
  - *Explicit exclusions avoided*: Startup page, remember last page, haptic feedback, keep screen on, restore last search.

### Stage 02 — Security & Data Management
- [NEW] [SecurityDataSettingsActivity.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/presentation/settings/SecurityDataSettingsActivity.kt)
- [NEW] [activity_security_data_settings.xml](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/res/layout/activity_security_data_settings.xml)
  - Implements Security Center (App Lock, auto-lock timeout, biometric unlock, privacy shield in recent apps, screenshot protection, security status), History & Activity Management (logging, automatic cleanup 7/30/90 days/disabled, retention policy, export CSV/JSON, clear history, activity insights), Storage Manager (overview, cache size, DB size/records, temp files, clear cache, cleanup confirmation), Backup & Restore (create/restore backup, preview, validation, conflict handling, export activity report), Tool Behavior & Safety (execution confirmation, target confirmation, timeout, result logging, authorized testing reminder), Privacy & Permission Center (permissions overview, explanation, manage permissions), and Recovery Center (reset preferences, security, history, backup, app data with separate confirmation flows).

### Stage 03 — App Information, Games, Updates & Support
- [NEW] [AppSupportSettingsActivity.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/presentation/settings/AppSupportSettingsActivity.kt)
- [NEW] [activity_app_support_settings.xml](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/res/layout/activity_app_support_settings.xml)
  - Implements App & Device Information (version, build, device info, feature availability, copy debug info with redacted secrets), Game Preferences (sound, vibration, display mode, high scores, game data/prefs reset), Update Center (current version, check updates, what's new, release info, update status), and Support & Feedback (bug report, feature request, contact developer, documentation, FAQ with official links).
  - *Explicit exclusions avoided*: App health check, runtime diagnostics, diagnostic report generation, log viewer, diagnostic history, open-source licenses, developer diagnostics mode, diagnostic report sharing.

### Stage 04 — Tool Workspace & Workflow Management
- [NEW] [WorkspaceWorkflowSettingsActivity.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/presentation/settings/WorkspaceWorkflowSettingsActivity.kt)
- [NEW] [activity_workspace_workflow_settings.xml](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/res/layout/activity_workspace_workflow_settings.xml)
  - Implements Tool Library Manager (show/hide tools, categories, search, descriptions, implemented vs in-dev status), Favorites & Quick Access (add/remove favorites, pin to Quick Menu 3-column grid, reorder, dedicated favorites page), Custom Tool Collections (create collection, name, description, add tools across categories, reorder, edit, duplicate, delete), Tool Profiles & Presets (create preset, parameters, edit, duplicate, delete, select for tool), Workflow Builder (create workflow, steps, reorder, status, outputs, network troubleshooting example), Home Workspace Manager (visible info cards, reorder dashboard sections, quick menu categories, reset), Android Shortcuts (add tool shortcut to home screen, favorite shortcuts), and Workspace Profiles (create profile, save collections, favorites, shortcuts, switch, set default, School Lab / Home Network / Development profiles).

### Central Settings Hub
- [NEW] [SettingsActivity.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/presentation/settings/SettingsActivity.kt)
- [NEW] [activity_settings.xml](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/res/layout/activity_settings.xml)
  - Main Settings hub organizing all 4 stages with search, quick access cards, and polished glassmorphic UI.
- [MODIFY] [HomeActivity.kt](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/java/com/repp/aquaticremineztools/presentation/home/HomeActivity.kt)
  - Wire up `tabSettings` and drawer settings click to open `SettingsActivity`.
- [MODIFY] [AndroidManifest.xml](file:///C:/Users/trewa/AndroidStudioProjects/AquaticReminezTools/app/src/main/AndroidManifest.xml)
  - Register `SettingsActivity`, `AppearanceSettingsActivity`, `SecurityDataSettingsActivity`, `AppSupportSettingsActivity`, and `WorkspaceWorkflowSettingsActivity`.

## Verification Plan

### Automated Tests
- Build project successfully using Gradle (`app:assembleDebug`).

### Manual Verification
- Launch app on device / emulator.
- Tap Settings in Bottom Navigation or Navigation Drawer to open `SettingsActivity`.
- Navigate through Stages 01 to 04 activities, toggling preferences, testing themes, accent colors, backups, collections, workflows, and workspace profiles.
