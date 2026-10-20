# Multi-account Gmail OTP fetching

## Goal

Allow users to connect multiple Gmail accounts, see which accounts are connected, and fetch the newest valid OTP across those accounts from either the app or the Quick Settings tile.

## User experience

- The existing Gmail Settings card lists every connected Gmail address.
- The card offers an Add account action and an individual Disconnect action for each address.
- The existing in-app fetch action and Quick Settings tile use the same fetch behavior.
- A tile-initiated fetch shows a clear progress state instead of a blank page, followed by the code and the source account. Preserve the existing automatic copy/history behavior.
- When no matching code is found or an account fails, show a useful outcome; do not silently fail. A failure for one account should not prevent checking the others.

## Account authorization and storage

- Keep a separate Gmail readonly authorization for each connected Google account; avoid replacing the currently connected account when adding another.
- Persist the account addresses as the visible account list. Obtain account-specific access tokens for fetches, and revoke only the account being disconnected.
- Do not persist access tokens. Clear each short-lived token after use, following the existing token cleanup behavior.
- Continue requesting only the Gmail readonly scope.

## Fetch and ordering

- Query recent Inbox messages for each connected account using the existing seven-day window and message limit as initial defaults.
- Extract candidate codes with the existing `GmailCodeExtractor` and app phrase settings.
- Compare received timestamps for messages that actually contain a valid code and choose the newest. Use Gmail message `internalDate` (milliseconds since epoch) rather than message-list order or email text dates.
- Return the selected code, source email address, and received timestamp to the UI/result path.
- Continue after per-account errors and report account-level failures if no account produces a code.

## Scope and compatibility

- Retain the current fetch/copy/history mechanism and Quick Settings tile entry point.
- Keep the account-management UI within the existing Settings Gmail card for this iteration; no separate account-management screen.
- Existing single-account users should retain their connected account after migration to the new preference format.
- No unrelated visual redesign is included; broader UI changes are a later phase after this feature is working.

## Verification

- Unit tests cover extracting the newest code across multiple accounts/messages, ignoring newer messages without codes, timestamp ordering, and partial account failures.
- Tests cover account preference migration and per-account removal/revocation behavior where testable.
- Run focused Gmail tests and the project’s Android build checks.
- Produce the test/release APK through GitHub Actions, not on the user’s computer.
