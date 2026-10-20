# Material You Expressive Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign OTP Helper's Android interface around Material You dynamic color and Vivi-Music-inspired expressive layouts while preserving the app's OTP, Gmail, history, and setup behavior.

**Architecture:** Keep the current Compose, Hilt, Navigation, DataStore, Room, and WorkManager architecture. Add an app shell for Codes, History, and Settings; reuse existing repositories and route ViewModels; build OTP Helper-owned Material 3 components and preferences. Separate theme, account, and UI state from Gmail fetch/domain behavior.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Material 3, Navigation Compose, Hilt, Proto DataStore, Room/Paging, WorkManager, JUnit4, Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-10-20-material3-expressive-redesign-design.md`

## Global Constraints

- Use Material You dynamic color from the system wallpaper by default on Android 12+ and honor system/light/dark mode.
- Use Vivi-Music only as visual inspiration; do not copy its source code, assets, or branding.
- Preserve OTP detection, Gmail OAuth, concurrent fetch behavior, database semantics, deep links, and Quick Settings tile behavior.
- Keep Gmail fetch in the background; never open a temporary or blank Activity.
- Do not run local APK builds; use GitHub Actions for Android build validation.
- Do not commit changes unless the user explicitly requests a commit.

---

### Task 1: Persist Material You appearance preferences

**Files:**
- Modify: `app/src/main/proto/user_settings.proto`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/UserSettingsRepository.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/UserSettingsRepositoryImpl.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/UserSettingsRepositoryMock.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/theme/Theme.kt`
- Create: `app/src/main/java/io/github/jd1378/otphelper/ui/theme/ThemeMode.kt`
- Test: `app/src/test/java/io/github/jd1378/otphelper/ui/theme/ThemeModeTest.kt`

**Interfaces:**
- `ThemeMode` exposes `SYSTEM`, `LIGHT`, and `DARK` values with stable persisted proto values.
- `ThemeMode.isDark(systemIsDark: Boolean): Boolean` resolves the effective dark setting.
- `UserSettingsRepository.setThemeMode(mode: ThemeMode)` persists the choice.

- [ ] **Step 1: Write theme-mode tests** for system-following, forced-light, and forced-dark resolution.
- [ ] **Step 2: Run the targeted JVM test** with `./gradlew :app:testDebugUnitTest --tests io.github.jd1378.otphelper.ui.theme.ThemeModeTest` and verify it fails because the theme-mode API is absent.
- [ ] **Step 3: Add the proto field, enum, repository setter, and pure mode resolver.** Keep new proto field numbers unique and preserve old default behavior as system mode.
- [ ] **Step 4: Make `OtpHelperTheme` consume the persisted mode, use dynamic color on API 31+, and use the existing generated light/dark schemes as fallback.** Preserve existing status-bar icon contrast.
- [ ] **Step 5: Rerun the targeted JVM test** and verify all three cases pass.

### Task 2: Build reusable expressive components and app shell

**Files:**
- Create: `app/src/main/java/io/github/jd1378/otphelper/ui/components/ExpressiveCard.kt`
- Create: `app/src/main/java/io/github/jd1378/otphelper/ui/components/SettingGroup.kt`
- Create: `app/src/main/java/io/github/jd1378/otphelper/ui/navigation/AppNavigationBar.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/OtpHelperApp.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/navigation/TheNavController.kt`
- Test: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/navigation/AppNavigationBarTest.kt`

**Interfaces:**
- `ExpressiveCard(content: @Composable ColumnScope.() -> Unit)` supplies shared shape, surface, and spacing tokens.
- `SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit)` renders related settings as a single rounded group.
- `AppNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit)` maps the three primary routes to Codes, History, and Settings.

- [ ] **Step 1: Add an instrumented navigation test** that renders the bar, verifies the three labeled destinations, and selects History.
- [ ] **Step 2: Run `./gradlew :app:connectedDebugAndroidTest --tests io.github.jd1378.otphelper.ui.navigation.AppNavigationBarTest` in GitHub Actions** and confirm the test fails for the missing component.
- [ ] **Step 3: Add the reusable expressive card and settings group** using Material 3 surface roles and shapes; keep all colors theme-driven.
- [ ] **Step 4: Add the three-item navigation bar** to the app shell, showing it only on primary destinations and preserving detail-route back behavior and deep links.
- [ ] **Step 5: Run the same instrumented test in GitHub Actions** and verify destination selection passes.

### Task 3: Replace the placeholder home with Codes

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/Home.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/HomeViewModel.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/data/local/dao/DetectedCodeDao.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/DetectedCodeRepository.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/DetectedCodeRepositoryImpl.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/CodesScreenTest.kt`

**Interfaces:**
- `DetectedCodeRepository.getLatest(limit: Int): Flow<List<DetectedCode>>` returns newest entries first.
- `HomeViewModel.latestCodes` exposes recent saved detections; `HomeViewModel.fetchGmail(context)` enqueues the existing `GmailFetchWorker`.

- [ ] **Step 1: Add DAO/repository tests** proving newest-first ordering and an empty result for a fresh store.
- [ ] **Step 2: Run the targeted JVM tests in GitHub Actions** and verify they fail before `getLatest` exists.
- [ ] **Step 3: Add a Room `LIMIT` query and repository flow** without changing the paging history query or schema.
- [ ] **Step 4: Replace the button-list Home content** with a latest-code hero card, copy action, recent-code list, source/account filter affordance, Gmail fetch action, and explicit empty/setup states. Reuse the existing clipboard and Gmail worker entry points.
- [ ] **Step 5: Add Compose UI assertions** for populated and empty states, then run the targeted unit and instrumented tests in GitHub Actions.

### Task 4: Redesign History and detail screens

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/History.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/HistoryDetail.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/components/TitleBar.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/HistoryScreenTest.kt`

- [ ] **Step 1: Add UI tests** for empty history, a populated entry, and the clear-history confirmation.
- [ ] **Step 2: Run the targeted instrumented tests in GitHub Actions** and verify the empty-state assertion fails before the new UI exists.
- [ ] **Step 3: Restyle history rows and details** with shared expressive surfaces, readable code text, timestamps, source labels, and per-row copy while preserving Paging and clear-history behavior.
- [ ] **Step 4: Run the same instrumented tests** and verify all history states pass.

### Task 5: Organize Settings and theme controls

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/Settings.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/SettingsViewModel.kt`
- Modify: `app/src/main/proto/user_settings.proto`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/UserSettingsRepository.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/repository/UserSettingsRepositoryImpl.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/theme/Theme.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/SettingsScreenTest.kt`

- [ ] **Step 1: Add UI tests** for the Appearance section, theme mode selection, Gmail accounts entry, and existing utility links.
- [ ] **Step 2: Run the targeted instrumented tests in GitHub Actions** and verify missing labels/controls fail as expected.
- [ ] **Step 3: Replace the flat settings layout with grouped sections** for Accounts, Detection, Copying, Notifications, History & privacy, Appearance, and Help. Keep existing settings actions and route links reachable.
- [ ] **Step 4: Add segmented system/light/dark selection and a palette preview**; dynamic system colors remain default and a fixed Vivi-inspired palette is optional.
- [ ] **Step 5: Run the targeted tests** and verify selection persists after navigating away and returning.

### Task 6: Redesign Gmail account management and fetch affordances

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/Settings.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailAccountStore.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailFetchActivity.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/navigation/TheNavGraph.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailAccountStoreTest.kt`
- Test: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/GmailAccountsScreenTest.kt`

- [ ] **Step 1: Extend account-store tests** for normalized duplicate handling, removal, and empty-account state.
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests io.github.jd1378.otphelper.gmail.GmailAccountStoreTest` in GitHub Actions** and verify the relevant new cases fail before implementation.
- [ ] **Step 3: Add a Gmail accounts page** with address rows, add/reconnect/remove actions, and authorization status, reusing existing OAuth and account-store APIs.
- [ ] **Step 4: Add UI tests** for zero, one, and multiple accounts and execute them in GitHub Actions.
- [ ] **Step 5: Ensure both Codes fetch and the Quick Settings tile call the same existing worker entry point** without launching `GmailFetchActivity`; test the coordinator/worker dispatch and validate notifications remain the feedback surface.

### Task 7: Restyle existing settings subpages and onboarding

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/ModeChoose.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/Permissions.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/LanguageSelection.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/IgnoredAppList.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/IgnoredAppDetail.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/IgnoredPhrases.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/SensitivePhrases.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/CleanupPhrases.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/DetectionTest.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/About.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/SetupFlowTest.kt`

- [ ] **Step 1: Add setup-flow UI tests** for source selection, a denied permission with retry, skipped Gmail, and completion navigation.
- [ ] **Step 2: Run the targeted instrumented tests in GitHub Actions** and confirm the new flow assertions fail against the existing route flow.
- [ ] **Step 3: Restyle each existing secondary page** using the shared headers, cards, spacing, and Material You colors while retaining current ViewModel actions and navigation.
- [ ] **Step 4: Update setup screens** to explain source/permission value before opening Android settings, allow optional Gmail skip, show status/retry, and finish at Codes without marking unavailable sources active.
- [ ] **Step 5: Run setup, permissions, and secondary-page instrumented tests** and verify prior deep links and setup-finished behavior still work.

### Task 8: Accessibility and release validation

**Files:**
- Modify: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/CodesScreenTest.kt`
- Modify: `app/src/androidTest/java/io/github/jd1378/otphelper/ui/screens/SettingsScreenTest.kt`
- Modify: `.github/workflows/android-build.yml` (use the existing workflow filename if different; inspect before editing)

- [ ] **Step 1: Add accessibility/large-font assertions** for OTP readability, button labels, and navigation destinations.
- [ ] **Step 2: Add GitHub Actions validation steps** for JVM tests, Android instrumented UI tests on an emulator, and the existing signed release APK job without exposing signing values.
- [ ] **Step 3: Push only when the user authorizes it; then inspect the Actions run** and fix redesign-specific failures.
- [ ] **Step 4: Confirm the final workflow artifact is a signed release APK** and report its Actions run/artifact location. Do not build an APK locally.
