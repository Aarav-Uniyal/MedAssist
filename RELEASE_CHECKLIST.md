# MedAssist — Phase 7 final validation and release checklist

This checklist is for the college/demo build. It is not a claim that MedAssist is a production-ready clinical service. Use synthetic medical records and throw-away test accounts only.

## 1. Local setup

- [ ] Open the inner `MedAssist/` project folder in Android Studio (the folder containing `settings.gradle.kts`).
- [ ] Use JDK 21, allow Gradle sync to complete, and resolve any plugin/dependency warning shown by the IDE.
- [ ] Confirm `applicationId` / package name `com.example.medassist` matches the Android app registered in the Firebase project.
- [ ] Ensure `app/google-services.json` came from that same Firebase project. Do not use another project's file. Keep this configuration and all signing keys out of public source control.
- [ ] Confirm Firebase Email/Password Authentication is enabled, the Firestore database exists, and the current `firestore.rules` has been published to the intended project.
- [ ] Confirm Firebase AI Logic uses **Gemini Developer API**, not a paid/provider path that the project does not document. Confirm Remote Config has `gemini_model_name` set to an accepted model, e.g. `gemini-3.5-flash-lite`.
- [ ] For debug testing, register the debug App Check token from that emulator/device. Keep it private and do not ship it.

## 2. Offline/static validation

From the project root:

```bash
python scripts/validate_release.py
```

This parses XML, verifies English/Hindi resource parity and referenced string IDs, checks basic source/key hygiene, checks the presence of a Firestore owner guard, and checks that key Analyzer navigation regressions remain covered. These are static checks only.

## 3. Automated Android tests

First let Android Studio finish syncing dependencies. Then run:

**Windows:**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

**macOS/Linux:**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

The connected UI suite needs an emulator or a physical device with USB debugging enabled. The app must be installed/run from the freshly opened project directory. Save the first failing stack trace, fix the root cause, and rerun the test suite. Do not count a command blocked by a missing Gradle distribution or dependency download as a test pass.

## 4. Firestore security rules emulator tests

Requirements: Node.js 20+, Java 21+, and Firebase CLI installed. From the project root:

```bash
cd firestore-tests
npm ci
npm test
```

The test command launches the **local Firestore emulator** with project ID `demo-medassist`; it is not supposed to modify the live Firebase database. The suite covers owner-only access, unauthenticated denial, profile validation, document metadata validation, chunk-size/type validation, sealed uploads, rename/category edits, and batch deletion. Do not deploy rules until this suite passes and you have reviewed the selected Firebase CLI project.

To deploy rules only after validation, return to the project root and run:

```bash
firebase use <your-firebase-project-id>
firebase deploy --only firestore:rules
```

Replace the placeholder with the intended project ID. Check `firebase projects:list` and `firebase use` before deployment; never guess the destination project.

## 5. Manual release-acceptance matrix

Use a clean install and a test account. Check both English and Hindi where relevant.

| Area | Action | Expected result |
|---|---|---|
| Authentication | Register, verify email, sign in, reset password, sign out | Correct screens, readable errors, persistent authenticated session after verification |
| Ownership | Sign in with test user A and user B | Documents/profile from A are never visible to B |
| Empty state | Open Vault and Analyzer when no PDF exists | Clear empty states; no crash; Analyzer's Open Vault button works |
| Navigation | Home Analyzer shortcut → bottom Home; then open Analyzer from its tab | Home is reachable; Analyzer has no top-left back button; bottom tabs remain available |
| PDF vault | Upload a synthetic PDF, view it, search/filter, rename, categorize, delete | Metadata and binary chunks remain consistent; failure states permit safe retry |
| Invalid files | Try an image/non-PDF, empty/corrupted, password-protected, scanned-only, and over-limit PDF | Rejected or explained; no fabricated extracted values; app stays usable |
| Analyzer consent | Select a report and cancel consent, then repeat and confirm | Cancel sends no analysis request; confirm explains sharing and produces a structured educational analysis |
| Analyzer safety | Compare generated result with source PDF; inspect values and ranges | The app does not show the extracted-text panel; the original PDF is available; the disclaimer stays visible |
| Analyzer narration | Tap the Analyzer read-aloud action | Complete structured analysis and disclaimer are spoken in the selected language |
| Chatbot | Ask a benign health-information question, follow up, switch language, try offline/retry | Context/language and errors work; no diagnosis/prescription claims; no chat saved unless opted in |
| Chatbot narration/mic | Read an assistant reply; test microphone permission allow/deny | The selected reply is read; speech input appends text for review before sending |
| Accessibility | TalkBack, large font, narration pause/resume/stop, dark mode and language persistence | Labels and touch targets remain usable; preferences survive restart |
| App Check | Test debug build with registered debug token; verify production provider before release | Debug-only provider stays in debug source; release uses Play Integrity as configured |

## 6. Build and sign

1. Build and smoke-test the **debug** APK first. Output is normally `app/build/outputs/apk/debug/app-debug.apk`.
2. In Android Studio, use **Build > Generate Signed Bundle / APK** when ready to distribute a release.
3. Choose APK for direct sideloading/demo or Android App Bundle (AAB) for Google Play submission.
4. Create a keystore only once. Keep the `.jks`/`.keystore` file and passwords in a secure backup outside this project and outside this ZIP. The same signing identity is needed for compatible future updates.
5. Never use or include an App Check debug token in a release. The release source set uses Play Integrity, so configure the matching Firebase App Check provider and test the release distribution path.
6. Before publication, check app name/icon, version code/name, privacy notice, data safety declarations, account deletion flow, accessibility, and the current Firebase/Gemini terms and quotas. Keep R8/minification disabled until a separately tested release build proves it works, or enable it and test the minified build before release.

## 7. Known limitations before any real-world deployment

- Step tracking remains intentionally hidden/skipped in this project build; it does not fabricate sensor readings.
- The Vault stores PDF pieces as Firestore binary blobs for Spark compatibility (10 MB per PDF); Cloud Storage and Cloud Functions are not used.
- Scanned-PDF OCR is not implemented. Analysis only uses locally extractable PDF text.
- Gemini output is educational and may be wrong. This is not a diagnosis or treatment plan.
- Chat history, narration preferences, and dark-mode preferences are local by default. Report analysis is not saved as history.
- App Check is configured for Firebase AI Logic; Firestore access is controlled by Firestore Rules, but app-level Firestore App Check enforcement is not configured in this build.
- Account deletion is performed from the client, so interruption can leave partial cleanup that needs a retry. Do not use this design as-is for a production clinical service.
- Hindi translations require review by a fluent speaker. Voice availability depends on Android's installed speech engines/voice data.
- Release minification is off and the project has not been independently assessed for healthcare regulatory compliance, penetration-tested, or clinically validated.

Firebase AI Logic requires App Check protection; Firebase's current guidance says enforcement becomes mandatory on November 2, 2026. Check the official docs immediately before release: <https://firebase.google.com/docs/ai-logic/app-check>.
