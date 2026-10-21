# FOTP (Fetch-OTP)

FOTP (Fetch-OTP) is an open-source app that detects one-time codes in SMS and notifications and copies them automatically. You can also fetch OTPs on demand from Gmail using the Quick Settings tile or the Settings screen.

FOTP is a modified fork of [OTP Helper](https://github.com/jd1378/otphelper), originally created by [jd1378](https://github.com/jd1378). This fork retains the upstream project’s AGPL-3.0 license, copyright and attribution notices, and translator credits. See [LICENSE](LICENSE) for the complete license and the source in this repository for the corresponding source of this modified version.

SMS and notification detection work locally. The optional Gmail feature uses Gmail's read-only API only when you explicitly fetch; it checks up to 10 recent inbox messages and does not monitor Gmail in the background or save message bodies. The detected code is handled using your existing copy, notification, and history preferences.

Download the test APK from the [GitHub Actions workflow artifacts](https://github.com/dumbshrn/FOTP/actions). When the release signing secrets are configured, the workflow also uploads a signed release APK.

## Gmail OTP setup (testing)

Gmail fetching is optional. This guide sets up your own Google OAuth project so you can test the Gmail feature with accounts you control. Google OAuth **Testing** allows up to 100 listed test users; it is not approval for a public release, and other people cannot connect Gmail unless they are added as test users.

### 1. Create a Google Cloud project and enable Gmail API

1. Open the [Google Cloud Console](https://console.cloud.google.com/) and create or select a project.
2. Open **APIs & Services → Library**, find **Gmail API**, and enable it for that project.
3. Open **Google Auth Platform** (or **APIs & Services → OAuth consent screen**, depending on the Console layout) and configure the app name, support email, and developer contact email.
4. Choose **External** for the audience if you are testing with ordinary Google accounts. Leave the publishing status as **Testing**.
5. In the **Data Access** page, add the Gmail read-only scope: `https://www.googleapis.com/auth/gmail.readonly`.
6. In **Audience → Test users**, add every Google account that will try the Gmail feature, then save.

### 2. Create the Android OAuth client

Google checks both the Android package name and the signing certificate. They must match the exact APK installed on the device.

1. In Google Cloud, open **Google Auth Platform → Clients** (or **APIs & Services → Credentials**) and create an **Android** OAuth client.
2. Enter this package name exactly: `com.dumbshrn.otphelper`.
3. Enter the SHA-1 certificate fingerprint for the APK you plan to install:
   - For an APK downloaded from this repository's GitHub Actions, open the successful run's summary and copy **Debug signer SHA-1** for the test APK, or **APK signer SHA-1** for the signed release APK.
   - For a locally built APK, run `./gradlew signingReport` from the repository and use the SHA-1 for the variant you installed.
4. Create the client. If you install APKs signed with different certificates (for example, a CI test APK and a locally built APK), register a separate Android OAuth client for each package/SHA-1 combination.

### 3. Install FOTP and connect Gmail

1. Download and install the **test APK** from a successful [Build artifact workflow run](https://github.com/dumbshrn/FOTP/actions/workflows/build_artifact.yml). The APK is in the `otp-helper-test-apk` artifact.
2. Open FOTP → **Settings → Gmail OTP → Add Gmail account** (or **Connect Gmail**, depending on the screen version).
3. Choose an account you added as a test user and approve Gmail read-only access. If Google displays an unverified-app warning while the project is in Testing, proceed only for your own test account after verifying you selected your own test project and understand the requested read-only access.
4. Tap **Fetch OTP from Gmail**. The app checks up to 100 inbox messages from the last seven days only when you request a fetch; it does not continuously monitor Gmail. You can also add FOTP's Gmail tile from Android's Quick Settings tile editor and use it to fetch on demand.

### Troubleshooting

- **`Access blocked` or account cannot authorize:** confirm the Google account is listed under **Test users**, the consent screen is still in Testing, and the Gmail read-only scope is configured.
- **Developer error / OAuth client mismatch:** confirm the package name is exactly `com.dumbshrn.otphelper` and the SHA-1 belongs to the installed APK's signer. Recheck the workflow summary for the correct artifact fingerprint.
- **No code found:** check that the message is in the inbox and contains a supported one-time code. FOTP parses the message body and checks recent messages; booking references, fares, seat numbers, and other unrelated numbers should not be treated as OTPs.
- **Testing authorization expires:** Google may expire refresh tokens for External apps left in Testing after seven days when they use non-basic scopes. Reconnect the account to authorize again.

### Before a public release

The Gmail feature requests a restricted Gmail scope. Keeping the project in Testing and adding test users is for development and personal testing; it does not make Gmail access available to everyone. A public release that offers Gmail fetching must follow Google's OAuth verification and Gmail user-data policy requirements. See [Google's restricted-scope verification guide](https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification) and [Gmail API scopes](https://developers.google.com/workspace/gmail/api/auth/scopes). If you do not want to pursue verification, release a version that does not offer Gmail fetching.

The Android OAuth client is registered in Google Cloud; FOTP uses Google Play services authorization and does not embed a web client secret or persist OAuth access tokens. For signed APK builds, configure this repository's Actions secrets `ANDROID_SIGNING_KEY`, `ANDROID_ALIAS`, `ANDROID_KEY_STORE_PASSWORD`, and `ANDROID_KEY_PASSWORD`; secrets in a different repository are not shared.

## How it works

The app works in 2 modes:

### 1. Notification

The app sets up a [notification listener](https://github.com/dumbshrn/otphelper/blob/main/app/src/main/java/io/github/jd1378/otphelper/NotificationListener.kt) and reads notifications sent by apps. This lets FOTP detect codes in email notifications and other app notifications.
When a notification arrives, FOTP combines its text, [checks whether it should be ignored](https://github.com/dumbshrn/otphelper/blob/main/app/src/main/java/io/github/jd1378/otphelper/utils/CodeIgnore.kt), and if not, matches it against the [code detection regex](https://github.com/dumbshrn/otphelper/blob/main/app/src/main/java/io/github/jd1378/otphelper/utils/CodeExtractor.kt). The extracted code is handled according to your settings.

### 2. SMS

The app listens to incoming SMS messages and processes them in the same way as notification mode: it checks whether the message should be ignored, then matches it against the code detection regex. The extracted code is handled according to your settings.

## How to build

Build the normal release APK with:

```bash
./gradlew :app:assembleNormalRelease
```

The APK is available under `app/build/outputs/apk/normal/release/`.

## Credits

FOTP is based on [OTP Helper by jd1378](https://github.com/jd1378/otphelper). Upstream license and translator acknowledgements are retained in this repository.

Feature graphic image generated by [hotpot.ai](https://hotpot.ai/templates/google-play-feature-graphic)

### Translated by these awesome people

German:

- [@Dacid99](https://github.com/Dacid99)

Spanish:

- [@nilp0inter](https://github.com/nilp0inter)

Turkish:

- [@SirCrownguard](https://github.com/SirCrownguard)

Bangla (Bangladesh):

- [@0xNaimulHasanTaky](https://github.com/0xNaimulHasanTaky)

Vietnamese:

- [@Sharethebest](https://github.com/Sharethebest)

Chinese (Hans):

- [@BackMountainDevil](https://github.com/BackMountainDevil)

Chinese (Hant):

- [@sntc06](https://github.com/sntc06)

Russian:

- [@darkspacer](https://github.com/darkspacer)

Italian:

- [@lollo03](https://github.com/lollo03)

French:

- [@trev0r-STA](https://github.com/trev0r-STA)

Arabic:

- [@Bunny-77X](https://github.com/Bunny-77X)

Hindi:

- [@chintanjoshi01](https://github.com/chintanjoshi01)

Ukrainian:

- [@balaraz](https://github.com/balaraz)

Portuguese:

- [@HackerSinhos](https://github.com/HackerSinhos)

Polish:

- [@pakin1](https://github.com/pakin1)

Japanese:

- [@KAWASAKICHIRO](https://github.com/KAWASAKICHIRO)

Korean:

- [@alexkoala](https://github.com/alexkoala)

You can help translate this app to your langauge on weblate:

<a href="https://hosted.weblate.org/engage/copy-sms-code-otp-helper/">
<img src="https://hosted.weblate.org/widget/copy-sms-code-otp-helper/287x66-grey.png" alt="Translation status" />
</a>

--------

### Donation

You can show your love and support for this project by donating to the following addresses:

BTC: bc1q8prgxcfcqpcq5ennyv08sxg6ymx2pm4azvcu0l

LTC: LVnTy14pHFRoSbqWbmhGBNjV4k2dBnY2Zn

---------

### Stars over time

<img src="https://star-history.dera.page/svg?repos=jd1378/otphelper&type=Date&theme=dark" alt="Chart of github stars over time"/>
