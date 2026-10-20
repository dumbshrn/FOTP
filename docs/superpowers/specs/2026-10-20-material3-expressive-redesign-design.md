# OTP Helper Material 3 Expressive Redesign

## Status

Approved design specification. Implementation is tracked in the corresponding plan.

## Goal

Redesign the full OTP Helper Android interface around Material 3 Expressive while preserving existing OTP detection, Gmail authorization/fetching, Quick Settings behavior, history, and settings functionality. Use the supplied Vivi-Music screenshots and repository as visual references, but implement the UI independently with the existing AndroidX Compose Material 3 dependency. Do not copy Vivi source code, assets, or branding.

## Design principles

- Make the latest usable OTP the primary focus of the app.
- Make source, account, and arrival time easy to understand at a glance.
- Keep core actions direct, predictable, and accessible.
- Ask for permissions contextually and explain why before opening Android settings.
- Give useful loading, success, empty, and error states; never leave a blank page.
- Use expressive type, shapes, colors, and motion selectively so codes remain highly legible.
- Preserve privacy choices and avoid showing sensitive codes in notifications unless the user preference explicitly allows it.
- Keep Gmail fetch actions in the background without opening a temporary or blank Activity.

## Visual system

- Material You dynamic color is the default design foundation: on supported Android versions, derive light/dark Material 3 color schemes from the user's system wallpaper and respect system theme mode.
- Provide an explicit app theme choice for system, light, and dark modes. Offer an optional fixed palette inspired by Vivi's screenshots (near-black/plum surfaces with blush/rose accents) for users who prefer that look over system dynamic color.
- Keep the selected OTP Helper palette and code readability coherent when dynamic color is unavailable or disabled.
- Use Material 3 typography with expressive screen titles and restrained body text. Give OTP values a large, high-contrast, easily selectable/copyable treatment.
- Use rounded Material surfaces and grouped setting cards. Reserve stronger shape/color emphasis for the newest OTP and primary actions.
- Use subtle motion for screen transitions and selection feedback. Never delay fetch or copy interactions for animation.
- Support font scaling, screen readers, clear icon labels, and generously sized touch targets.

## Information architecture

### Primary destinations

Use a compact Material 3 navigation bar with three destinations:

1. **Codes** — latest detected OTP, source filters, Gmail fetch action, and recent detections.
2. **History** — saved detected codes, newest first.
3. **Settings** — grouped preferences and links to secondary tools.

Secondary pages use an appropriate top app bar with a clear back action. They remain available through Settings rather than expanding primary navigation.

### Codes screen

- Compact app bar with app identity and access to source/account filtering.
- A featured **Latest code** card showing the OTP, service or sender, source type, arrival time, and Gmail address when applicable.
- A prominent copy action; tapping the code may also copy it. Respect existing copy confirmation and auto-copy settings.
- A **Fetch from Gmail** action that starts the same background operation as the Quick Settings tile and leaves the user on Codes.
- A concise recent-codes list ordered by message/detection timestamp descending.
- Empty states explain how to enable a source, connect Gmail, or wait for an incoming code.
- Loading and completion state is communicated without blocking the screen.

### History

- Chronological entries show code, sender/service, time, and source; Gmail entries also identify the account.
- Copy is available on each entry.
- Search/filter is included only where supported by existing data and storage.
- Clearing history requires confirmation and states what will be deleted.
- Honor the existing history-disable and privacy preferences.

### Gmail accounts

- A dedicated account-management page linked from Settings.
- Show each connected address and a clear healthy/reconnect-needed status.
- Support adding, reconnecting, and removing accounts using the current OAuth implementation.
- Fetch across selected/connected accounts concurrently, then choose the newest matching message based on its timestamp.
- Display the source account for Gmail-derived codes.
- Clearly explain authorization failures and how to reconnect.

### Settings

Use grouped, rounded preference sections inspired by Vivi’s screenshots:

- **Accounts:** Gmail account management and connection state.
- **Detection:** operation mode, notification listener/SMS source status, ignored apps, and detection behavior.
- **Copying:** automatic copy and copy confirmation preferences.
- **Notifications:** notification behavior, Quick Settings tile guidance, and related controls.
- **History & privacy:** history retention/disable behavior and sensitive-data preferences.
- **Appearance:** system/light/dark mode, palette selection, and dynamic color option where supported.
- **Help:** permissions, detection test, cleanup/sensitive/ignored phrase tools, and About.

Retain all existing secondary routes and actions: permission management, ignored apps and details, ignored/sensitive/cleanup phrases, detection test, history details, language selection, and About. Group them logically and use consistent page headers and state feedback.

## First-run setup

1. **Welcome:** short value statement and one primary Get started action.
2. **Choose sources:** explain SMS, notifications, and Gmail separately; allow optional sources to be skipped.
3. **Permissions:** explain each needed permission immediately before requesting/opening Android settings; reflect granted/denied state and allow retry.
4. **Gmail connection:** when selected, invoke Google’s account chooser; allow multiple accounts and make Gmail optional.
5. **Preferences:** present only the key existing choices, with safe defaults. Keep advanced controls in Settings.
6. **Review:** summarize enabled sources and outstanding setup steps; offer Quick Settings tile instructions without requiring it.
7. **Complete:** navigate to Codes.

Users who skip optional setup can reach the app and see clear, actionable setup reminders. Never represent a source as active when it lacks a required permission or account connection.

## Gmail fetch and Quick Settings behavior

- In-app Fetch and the Quick Settings tile trigger the same background fetch coordinator/worker path.
- Neither action launches an Activity or presents a blank screen.
- Use one ongoing notification while fetching when needed, updating it to the final result rather than creating a stream of notifications.
- A result notification states success, no matching code, or a helpful failure/reconnect state.
- Keep code visibility in notifications consistent with existing privacy/copy preferences.
- If no Gmail account is connected, communicate that and provide a route to account setup.

## Implementation boundaries

- Reuse the installed AndroidX Compose Material 3 library and existing app architecture.
- Build OTP Helper-owned composables and theme tokens using the existing AndroidX Compose Material 3 dependency; do not transplant Vivi source code or assets.
- Treat Vivi as visual inspiration for expressive settings groups, palette previews, rounded surfaces, and interaction polish; do not copy Vivi's music-specific UI.
- Keep domain logic, Gmail OAuth, fetch orchestration, OTP matching, database semantics, notification behavior, and Quick Settings service semantics unchanged except for UI wiring required to expose the design.
- Do not introduce new account permissions or expand data collection as part of the redesign.
- Keep existing deep links and setup completion behavior functional.

## Rollout and validation

Implement in reviewable phases while keeping the design cohesive:

1. Theme foundation, reusable Material 3 components, app navigation shell, and Codes screen.
2. History, Gmail account management, and background-fetch feedback.
3. Settings categories and all existing secondary/settings pages.
4. First-run setup, permissions, accessibility, and interaction polish.

For each phase, add or update focused Compose/ViewModel tests where the repository has a suitable test pattern, run relevant checks, and use GitHub Actions for Android builds. Validate first-run, returning-user, denied-permission, no-account, multiple-account, successful-fetch, failed-fetch, empty-history, and history-disabled states. No local APK build is part of this plan.

## Acceptance criteria

- All existing app routes and controls remain reachable after the redesign.
- A new user can understand and complete setup source-by-source, skipping optional Gmail if desired.
- A returning user can find the latest OTP and copy it immediately.
- Gmail accounts and their status are visible and manageable.
- In-app Fetch and the Quick Settings tile do not open a blank/temporary Activity.
- Fetch results and failures are communicated through notifications and clear in-app status.
- Codes remain readable at large font sizes and across light/dark themes.
- The UI uses Material 3 components and OTP Helper-owned code/assets, without copied Vivi implementation code.
- Android build and relevant tests pass in GitHub Actions.
