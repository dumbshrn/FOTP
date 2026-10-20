# Multi-account Gmail OTP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect multiple Gmail accounts and fetch/copy the newest valid OTP across them from Settings or the Quick Settings tile.

**Architecture:** Store the ordered list of connected email addresses while using account-targeted Google authorization for each address. Fetch candidate codes from each account, choose by Gmail message `internalDate`, then route the result through the existing code/history/copy worker and show a clear progress/result state in the fetch activity.

**Tech Stack:** Kotlin, Android AppCompat Activity, Jetpack Compose Settings UI, Google Identity Authorization API, Gmail REST API, JUnit 4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-20-multi-gmail-otp-design.md`

## Global Constraints

- Continue requesting only the Gmail readonly scope.
- Do not persist access tokens. Clear each short-lived token after use, following the existing token cleanup behavior.
- Query recent Inbox messages for each connected account using the existing seven-day window and message limit as initial defaults.
- Retain the current fetch/copy/history mechanism and Quick Settings tile entry point.
- Keep the account-management UI within the existing Settings Gmail card for this iteration; no separate account-management screen.
- Existing single-account users should retain their connected account after migration to the new preference format.
- Produce the test/release APK through GitHub Actions, not on the user’s computer.

## File Map

- `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailApiClient.kt`: extract timestamped OTP candidates from individual Gmail messages.
- `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailAccountStore.kt`: ordered account list, duplicate handling, and migration from the current single-email preference.
- `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailFetchActivity.kt`: account-specific authorization/revocation, aggregation across accounts, progress and result states.
- `app/src/main/java/io/github/jd1378/otphelper/ui/screens/Settings.kt`: list connected accounts, add accounts, and disconnect one selected account.
- `app/src/main/res/values/strings.xml`: account, progress, source, and outcome text.
- `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailApiClientTest.kt`: timestamp and candidate extraction tests.
- `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailAccountStoreTest.kt`: preference migration, ordering, duplicate, and removal tests.
- `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailFetchCoordinatorTest.kt`: newest-across-accounts selection and partial-failure tests, if aggregation is factored into a pure helper.
- `.github/workflows/build_artifact.yml`: inspect/update only if the existing push-to-main workflow cannot produce test and signed release artifacts for the final commit.

## Tasks

### Task 1: Select the newest valid message within one Gmail account

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailApiClient.kt`
- Test: `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailApiClientTest.kt`

**Interfaces:**
- `FetchResult` carries `emailAddress: String`, `code: String?`, and `receivedAtMillis: Long?`.
- `fetchLatestOtp(accessToken: String)` examines candidate messages and returns the valid code whose message has the greatest `internalDate`.

- [ ] Add fake-connection tests with a newer non-code email and older/newer code-bearing emails; assert the newest code-bearing message wins and its `internalDate` is returned.
- [ ] Add a test for missing/invalid `internalDate` behavior and no-code result.
- [ ] Parse each full Gmail message’s root `internalDate`; continue scanning all listed messages instead of stopping at the first extracted code.
- [ ] Run the focused `GmailApiClientTest` through the repository’s GitHub Actions test workflow when the implementation branch is ready; do not run an Android build locally.

### Task 2: Add ordered multi-account storage with legacy migration

**Files:**
- Create: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailAccountStore.kt`
- Create: `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailAccountStoreTest.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailFetchActivity.kt`

**Interfaces:**
- `GmailAccountStore.getAccounts(): List<String>` returns normalized addresses in stable display order.
- `GmailAccountStore.addAccount(email: String)` adds without case-insensitive duplicates.
- `GmailAccountStore.removeAccount(email: String)` removes only the matching address.
- First read migrates existing `connected_email` into the account list and removes the legacy preference after successful persistence.

- [ ] Write tests for empty storage, legacy migration, stable order, case-insensitive duplicate prevention, and removing one of multiple accounts.
- [ ] Implement the store over the existing `gmail_connection` SharedPreferences, persisting a stable JSON/list representation rather than relying on unordered set iteration.
- [ ] Keep the existing preference key readable until migration has succeeded, so an interrupted migration cannot lose the account.
- [ ] Run the account-store JUnit tests via GitHub Actions.

### Task 3: Authorize, fetch, and revoke per account

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailFetchActivity.kt`
- Create: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailFetchCoordinator.kt`
- Create: `app/src/test/java/io/github/jd1378/otphelper/gmail/GmailFetchCoordinatorTest.kt`

**Interfaces:**
- `GmailFetchCoordinator.selectNewest(results: List<AccountFetchResult>): AccountFetchResult?` ignores results without a valid code/timestamp and returns the candidate with the greatest received timestamp.
- `AccountFetchResult` carries `emailAddress`, `code`, and `receivedAtMillis`; individual account failures are recorded separately and do not abort successful accounts.
- Fetch requests target each stored Google account; connect requests force account selection and append the selected address.
- Disconnect requests carry the selected address and revoke only that Google account’s Gmail readonly grant.

- [ ] Test newest timestamp selection across accounts, a newer message without a code, tie handling, and partial failures where another account has a valid result.
- [ ] Implement coordinator selection as a pure function so it can be covered by JUnit without Android framework dependencies.
- [ ] Update the activity to authorize each stored email in turn, fetch Gmail candidates, clear each token in `finally`, and keep processing after an individual failure.
- [ ] For account authorization resolutions, resume the pending account operation before moving to the next account; surface a reconnect/authorization-needed outcome if access cannot be granted.
- [ ] Update connect to force account choice and add the returned Gmail profile address without replacing existing entries.
- [ ] Update disconnect to target the selected account, and remove it from local storage only after revocation succeeds.
- [ ] Run focused Gmail coordinator tests via GitHub Actions.

### Task 4: Show accounts and fetch progress/results

**Files:**
- Modify: `app/src/main/java/io/github/jd1378/otphelper/ui/screens/Settings.kt`
- Modify: `app/src/main/java/io/github/jd1378/otphelper/gmail/GmailFetchActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Replace the single `connectedGmail` state with the account list, refreshed when Settings resumes.
- [ ] Render each address with its own Disconnect action, keep Add account available when accounts already exist, and retain one Fetch OTP action.
- [ ] Add the selected email as an explicit disconnect intent extra.
- [ ] Replace blank activity behavior with a themed progress/result surface: checking account count, copied code and source email on success, no-code state, and partial/unavailable-account message on failure.
- [ ] Route the winning code through the existing `CodeDetectedWorker` path so copy/history behavior stays consistent for both entry points.
- [ ] Add all user-facing text as string resources and verify the Quick Settings tile uses the same result flow.
- [ ] Run the project tests through GitHub Actions; confirm the progress and result flow on a device using the produced APK.

### Task 5: Produce the GitHub Actions APK

**Files:**
- Inspect: `.github/workflows/build_artifact.yml`
- Modify only if necessary: `.github/workflows/build_artifact.yml`

- [ ] Confirm the existing push-to-main workflow runs unit tests, builds the debug test APK, and signs/uploads the release APK when signing secrets are configured.
- [ ] If unchanged, use that workflow for the final pushed implementation; otherwise make only the workflow changes required to publish those artifacts.
- [ ] Verify the Actions run succeeds and both the test APK and signed release APK artifacts are present before sharing download links.

## Self-review

- The API task covers timestamp extraction and selecting the newest valid code within an account.
- The store task covers all connected-address display data and single-account migration/removal.
- The authorization/coordinator task covers per-account access, all-account aggregation, newest timestamp selection, and partial failures.
- The Settings/activity task covers the account list, tile/in-app shared flow, progress/result messages, and preserving copy/history behavior.
- The Actions task respects the requirement to build on GitHub Actions only.
- No access token is included in persistent storage; the plan uses the readonly scope and existing inbox limits.
