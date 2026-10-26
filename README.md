# MedAssist (college project)

Android medical-assistance app: Kotlin, Jetpack Compose, Material 3, MVVM, Firebase, Gemini.

**Build status: Phase 7 of 8 (Accessibility, localization and appearance).** Phase 1 added the theme, components and navigation shell.
Phase 2 added registration, login, email verification, password reset, sign out, account deletion and
protected screens. Phase 3 adds the Medical Vault: PDF upload, list, search, sort, categories, rename,
delete and an in-app PDF reader. Phase 4 adds the AI Assistant (Gemini chatbot), and Phase 5 adds the
Report Analyzer. Step tracking (Phase 6) is intentionally skipped and hidden from navigation in this build.
Phase 7 adds accessibility narration, persisted English/Hindi selection, and a persisted dark-mode toggle.
The primary bottom navigation is ordered Home, Vault, Assistant, Analyzer, Profile; Analyzer is accessible
both from its tab and the Home quick-access card.

**All implemented phases use the existing Firebase Spark/free-tier setup. Phase 7 adds device-local preferences and Android TextToSpeech; no Blaze upgrade, paid service, or new dependency is required.**

## How to run it

You need **Android Studio** (a version compatible with the Gradle/Android Gradle Plugin versions in this project)
and an Android 8.0+ (API 26) device or emulator.

1. Extract the project ZIP.
2. In Android Studio, select **File > Open** and select the `MedAssist` project folder that contains
   `settings.gradle.kts`, `build.gradle.kts`, `gradlew`, and the `app/` directory.
3. Wait for **Sync Project with Gradle Files** to finish, then run the `app` configuration.
4. To run tests from the Android Studio Terminal on Windows, use `.\gradlew.bat testDebugUnitTest` and
   `.\gradlew.bat connectedDebugAndroidTest` (the latter requires a connected device or emulator).
   On macOS/Linux, use `./gradlew testDebugUnitTest` and `./gradlew connectedDebugAndroidTest`.

The wrapper downloads the Gradle distribution on first use, so internet access may be needed. Keep the project's
existing `gradlew`, `gradlew.bat`, and `gradle/wrapper/` files together.

From Phase 2 on the project needs `app/google-services.json` for the Firebase Android app. The file is normally
obtained from your own Firebase project; never replace it with a configuration file belonging to another project.
If it is absent, Gradle will report that `google-services.json` is missing.


## Phase 2: Firebase setup (about 10 minutes, no credit card needed)

Email/password sign-in and Firestore both fit in Firebase's free Spark plan.

1. Go to <https://console.firebase.google.com> and **Add project** (Google Analytics can stay off).
2. **Project settings > Your apps > Add app > Android.** Package name: `com.example.medassist`.
   Skip the SHA-1 box for now (it is needed later for App Check). Download **`google-services.json`** and put it in the
   `app/` folder (next to `app/build.gradle.kts`). It is git-ignored; do not commit it.
3. **Build > Authentication > Get started > Sign-in method > Email/Password > Enable** (leave "Email link" off).
4. **Build > Firestore Database > Create database.** Choose **production mode** and a region near you
   (for India, `asia-south1` Mumbai). The region cannot be changed later.
5. **Firestore > Rules tab:** replace everything with the contents of `firestore.rules` and **Publish**.
   (Or with the Firebase CLI: `firebase deploy --only firestore:rules`.)
6. In Android Studio: **Sync**, then **Run**.

New Firebase projects have *email enumeration protection* switched on (Authentication > Settings > User actions).
Leave it on: it is why "wrong password" and "no such account" show the same message.

### Phase 2: manual test checklist

Use an email inbox you own, and a throw-away password. Never use real medical data in development.

| # | Do this | Expect |
|---|---------|--------|
| 1 | Open the app signed out | Login screen (not the home tabs) |
| 2 | Tap Sign in with empty fields | Red text under each field, no spinner, no network call |
| 3 | Create account with password `abc` | Error under the password field |
| 4 | Create account with a valid name/email/password | "Verify your email" screen; email arrives (check spam) |
| 5 | Open the link in the email, return to the app | App moves to Home by itself ("Good morning, <name>") |
| 6 | Force-stop and reopen the app | Still signed in, straight to Home |
| 7 | Profile > Sign out | Login screen; the tabs are gone |
| 8 | Sign in with a wrong password | "Incorrect email or password." |
| 9 | Forgot password with an email that has no account | Same confirmation as for a real one |
| 10 | Forgot password with your real email | Reset email arrives; new password works |
| 11 | Turn on airplane mode, tap Sign in | Offline banner on top, "No internet connection" message |
| 12 | Firebase console > Firestore | Document `users/<uid>` with 6 fields, no password anywhere |
| 13 | Profile > Delete my account, wrong password | "That password is incorrect." Account still exists |
| 14 | Delete again with the right password | Back at Login; user gone from Authentication and Firestore |
| 15 | Profile > Settings > select हिन्दी | App switches to Hindi, session and documents are retained |
| 16 | Firestore > Rules > **Rules Playground**: simulate `get` on `/users/<uid-of-A>` as user B | **Denied** (data isolation) |

Automated tests: `./gradlew testDebugUnitTest` (validators, error mapping, Login/Register/Profile ViewModels)
and `./gradlew connectedDebugAndroidTest` (sign-in screen and app shell; no Firebase needed).

### Phase 2: troubleshooting

- **"File google-services.json is missing"**: put the file in `app/`, not the project root.
- **Package name mismatch** in that file: the Firebase app must be registered as `com.example.medassist`.
- **Sign-in works but nothing is in Firestore**: filter Logcat by `MedAssistAuth`. A `PERMISSION_DENIED` means the
  rules in step 5 were not published.
- **Verification email never arrives**: check spam; use "Resend" (there is a 60 second pause between sends).
- **"Too many attempts"**: Firebase rate-limits repeated failures; wait a few minutes.
- **Verified in the browser but the app still shows the verify screen**: tap "I've verified my email". The app also
  re-checks whenever it returns to the foreground.

## Phase 3: Medical Vault (free Spark plan)

### Why there is no Cloud Storage or Cloud Function

Cloud Storage for Firebase and Cloud Functions both need the paid **Blaze** plan now (new projects get a
`*.firebasestorage.app` bucket, which needs Blaze too). So Phase 3 uses **only Cloud Firestore**, which is
free up to 1 GiB stored, 50,000 reads/day and 20,000 writes/day (deletes count as writes, also 20,000/day).

How a PDF is stored (see `data/repository/FirestoreVaultRepository.kt`):

```
users/{uid}/documents/{documentId}            metadata: name, category, notes, sizeBytes, mimeType,
                                              chunkCount, sha256, storagePath, uploadedAt, ownerId
users/{uid}/documentChunks/{documentId}_{i}   the PDF bytes, in pieces of at most 900 KiB
```

- A Firestore document holds at most 1 MiB, so a PDF (10 MB max) is split into at most 12 pieces.
- Pieces are stored as Firestore **binary data (Blob)**, not as Base64 text. The brief says not to put
  "a large Base64 string" in Firestore; this keeps the file in small, binary, access-controlled pieces instead.
- Pieces are written first, metadata last. The rules refuse the metadata until its first and last pieces exist,
  so a half-uploaded file never shows up in the list. After that the file is "sealed": its pieces can't be changed.
- The download is checked against the saved size and SHA-256, so a damaged file is never shown.
- There are no public URLs at all. Every read goes through Firestore with the user's login, and
  `firestore.rules` lets only the owner read, edit or delete anything under `users/{uid}`.
- Downloaded PDFs are kept in the app's private cache (`cache/vault/<uid>/`) and wiped on sign-out and
  account deletion. Firestore's own offline cache (also app-private) may keep recently opened pieces.

What it costs in free quota: a 10 MB PDF is 12 piece writes + 1 metadata write to upload, and 12 reads to
open (opening it again from the phone's cache costs 1 read). About 1,000 such files fit in the free 1 GiB.
If a daily limit is reached, the app says so and it resets the next day.

**Account deletion** now runs in the app instead of a Cloud Function: after the password check it deletes every
document and piece, then the profile, then the login. If a step fails (for example the internet drops) the
account is kept and the user can just try again.

### Phase 3 setup (2 minutes)

1. **Firestore > Rules tab:** paste the new `firestore.rules` and **Publish** (or `firebase deploy --only firestore:rules`).
   Without this, every vault action shows "You don't have permission to do that".
2. Nothing else: no Storage bucket, no billing, no new dependencies. Sync and Run.

### Phase 3: manual test checklist

Use **fake sample PDFs only** (any printed web page saved as PDF works). Never upload real medical records.

| # | Do this | Expect |
|---|---------|--------|
| 1 | Open the Vault tab with no documents | "No documents yet" message |
| 2 | Upload PDF, pick a small PDF | Upload sheet with name pre-filled from the file name and the file size |
| 3 | Pick a category, tap Upload | Progress bar with %, then "Document uploaded." and the item in the list |
| 4 | Pick a `.jpg` or a `.txt` renamed to `.pdf` | "Only PDF files can be uploaded." |
| 5 | Pick a PDF over 10 MB | "This file is too large. The limit is 10 MB." |
| 6 | Pick a password-protected PDF | "This PDF is password-protected…" |
| 7 | Airplane mode, then Upload | "You're offline…"; turn it back on and tap Retry upload: works |
| 8 | Tap a document | PDF opens in the app; Next/Previous, page indicator, zoom 100–300 % work |
| 9 | Menu > Rename or edit | New name, category and notes appear in the list at once |
| 10 | Type in Search; tap a category chip; change Sort | List filters and re-orders; "Clear search and filters" when nothing matches |
| 11 | Menu > Delete > Delete | Item disappears; in the console both `documents/<id>` and its `documentChunks` are gone |
| 12 | Home tab | "Recent documents" shows the 3 newest; tapping one opens it |
| 13 | Sign in as a second user | Vault is empty (user A's files are invisible) |
| 14 | Profile > Delete my account | Login screen; in the console `users/<uid>` and all its vault data are gone |
| 15 | Hindi phone language | All vault and viewer text in Hindi |

### Phase 3: automated tests

- `./gradlew testDebugUnitTest`: PDF checks, chunking + SHA-256, search/sort/filter, error mapping, VaultViewModel
  (upload, retry, cancel, offline, rename, delete), plus all Phase 2 tests.
- `cd firestore-tests && npm install && npm test`: 11 security-rules tests on the free local Firestore emulator
  (owner-only access, signed-out access, field validation, 10 MB / piece limits, Base64 rejected, sealed files,
  batch delete). Needs Node 20+, the Firebase CLI (`npm i -g firebase-tools`) and Java 21+. It never touches your real project.

### Phase 3: troubleshooting

- **"You don't have permission to do that"** on every vault action: the Phase 3 rules were not published.
- **"The app's free daily limit has been reached"**: the Spark quota (e.g. 50,000 reads) ran out; it resets daily.
  Usage is under Firestore > Usage in the console.
- Filter Logcat by `MedAssistVault` for details of any vault error.

## Phase 4: AI Assistant (free Spark plan)

### How it stays free and keeps the API key off the phone

The chatbot uses **Firebase AI Logic** with the **Gemini Developer API "free tier"**. Firebase's docs say the
free tier needs no payment method and the project stays on the Spark plan. Firebase AI Logic is a Google-run proxy:

- The Gemini API key is created and held by Firebase. It is **not** in any Kotlin file, Gradle file or the APK.
- **App Check** proves each request comes from the real app (debug token for debug builds, Play Integrity for
  release builds). Firebase now switches App Check enforcement on automatically when you set up AI Logic.
- A **per-user rate limit** (default 100 requests/minute) is set in the console. The app adds its own limits:
  5 questions per minute, 1,000 characters per question, and only the last 20 messages are sent as context.
- The **model name lives in Remote Config** (free). Use `gemini_model_name` with default `gemini-3.5-flash-lite`;
  the app also supports `assistant_model` for compatibility with earlier project versions. If both are set, the
  preferred `gemini_model_name` wins. The app fetches the initial value when the assistant is first used and listens
  for published changes in real time while the app is in the foreground. For safety, this app accepts
  `gemini-3.5-flash-lite` and `gemini-3.8-flash`; any other value falls back to `gemini-3.5-flash-lite`.
- A system instruction (`domain/assistant/AssistantPrompt.kt`) sets the safety rules: not a doctor, no diagnosis,
  no invented values or ranges, no prescribing, emergencies go to 112 (and Tele-MANAS 14416 for suicidal
  thoughts), no unnecessary personal details, and replies in the app's language (English or Hindi).
- An emergency banner with a "Call 112" button also appears straight away if a message contains emergency words.
- **Chats are not stored by default.** "Save chat history" in the chat menu keeps them on this phone only (app-private
  storage, never uploaded). "New conversation" clears the chat and any saved copy. Saved chats are wiped on sign-out
  and account deletion.

**Privacy note:** on the free tier Google may use prompts and replies to improve its products (see the
"Used to improve our products" row on the Gemini API pricing page). The welcome screen asks users not to share
personal details. Use only made-up example questions during development.

Free-tier limits are per project and can change; check them in Google AI Studio. When the daily limit is reached the
app says "The free daily AI limit has been reached" until it resets.

### Phase 4 setup (about 5 minutes, no credit card)

1. Firebase console > **AI Logic** (under Build / AI) > **Get started** > choose **Gemini Developer API**. Do **not**
   pick the Agent Platform (Vertex AI) option: that one needs Blaze. Follow the wizard; it enables the APIs and
   creates the key on Firebase's side. Skip any "add the SDK" step: the app already has it.
2. **App Check debug token:** run the debug app once, open the Assistant tab, then in Logcat search for
   `DebugAppCheckProvider` and copy the token. Console > **Security > App Check > Apps** > your Android app >
   overflow menu > **Manage debug tokens** > add it. Keep it private (it is never in the code).
   Each emulator/phone (and a fresh install) gets a new token.
3. Optional: console > **AI Logic > Settings**: lower the per-user rate limit (for a class demo, e.g. 10/minute).
4. Optional: console > **Remote Config** > add parameter `gemini_model_name` (String) with default value
   `gemini-3.5-flash-lite`, then publish it. To switch to the alternative, set it to `gemini-3.8-flash` and publish
   again. The running app can activate the change in real time; the next assistant request uses the selected model.
   Existing projects may keep the older `assistant_model` parameter; `gemini_model_name` takes priority if both are set.
   If the selected value is missing, blank, or unsupported, the app uses `gemini-3.5-flash-lite`.
5. For a release build (Play Integrity), register the app's SHA-256 under App Check > Apps > Play Integrity.
   Play Integrity needs the app to be distributed through Google Play; for a college demo use the debug build.

### Phase 4: manual test checklist

| # | Do this | Expect |
|---|---------|--------|
| 1 | Open the Assistant tab | Disclaimer bar, welcome text, 3 example questions |
| 2 | Tap "What does HbA1c mean on a blood test?" | "MedAssist is writing a reply…", then a plain-text answer with a time |
| 3 | Ask a follow-up, e.g. "And what is a normal value?" | Answer uses the earlier context and tells you to check your report's own range |
| 4 | Ask "Which medicine should I take for fever and how much?" | No dose or prescription; advises a doctor or pharmacist |
| 5 | Type "my father has chest pain" | Red emergency banner with Call 112 at once; reply starts with emergency advice |
| 6 | Switch the app/phone language to Hindi, ask a question | Reply in Hindi |
| 7 | Airplane mode, send | "You're offline…", text stays in the box |
| 8 | Turn off Wi-Fi just after tapping Send | Question marked "Not sent" with Retry; Retry works when back online |
| 9 | Send 6 questions within a minute | 6th shows the "sending questions very quickly" message |
| 10 | Menu > New conversation | Confirmation, then empty chat |
| 11 | Leave Save chat history off, kill and reopen the app | Chat is gone |
| 12 | Turn Save chat history on, chat, kill and reopen | Chat restored; turning it off deletes the saved copy |
| 13 | Remove the debug token in the console and send (after ~1 hour) | "This copy of the app isn't verified by App Check yet…" |

### Phase 4: automated tests

`./gradlew testDebugUnitTest` adds tests for the safety prompt, the emergency word check (English and Hindi), the
rate limiter, Markdown clean-up, the context window, error mapping, and `ChatViewModel` (send, context, language,
limits, offline, failure + retry, emergency banner, history off by default, saving/restoring/clearing).

### Phase 4: troubleshooting

Filter Logcat by `MedAssistAI` and `MedAssistAppCheck` (the app never logs the chat text itself). At start-up a
debug build logs either "App Check OK" or "App Check FAILED" with the reason.
- **"The reply took too long"**: check the `MedAssistAI` line. If App Check shows FAILED, fix the debug token first.
  Otherwise the model may be overloaded on the free tier: wait a minute, or confirm that `gemini_model_name` is set to
  `gemini-3.5-flash-lite` in Remote Config and that the latest template has been published.
- **"isn't set up yet"**: step 1 wasn't finished, or the Gemini Developer API is disabled for the project.
- **"isn't verified by App Check"**: step 2 (debug token) is missing or the app was reinstalled.
- **"free daily AI limit"**: free-tier quota used up for the day, or the chosen model has no free tier.
  Set `gemini_model_name` in Remote Config to `gemini-3.5-flash-lite` and publish the template.

## Phase 5: Report Analyzer (free Spark plan)

### What it does

- The signed-in user chooses a PDF already listed in the Medical Vault. The analyzer re-fetches the document metadata from that user's own Firestore path and downloads its binary chunks through the existing owner-scoped repository.
- Before a report is processed, a consent dialog explains that readable text will be sent to Google's Gemini service. If the user cancels, the PDF text is not sent.
- Text extraction happens on the Android device with PDFBox-Android (`com.tom-roush:pdfbox-android`, Apache-2.0). The original PDF is not uploaded a second time to the AI service.
- Only extracted text is sent through the already configured Firebase AI Logic + Gemini Developer API free tier. The model is selected through the shared `gemini_model_name` / `assistant_model` Remote Config settings, so the analyzer follows the same model choice as the chatbot.
- Gemini is asked for structured JSON with an overview, findings, term explanations, test values/units/reference ranges/flags explicitly present in the source, questions for a healthcare professional, and limitations. The app displays the extracted source text separately so users can compare it with the generated explanation.
- Neither the extracted text nor the AI analysis is automatically saved as a new Firestore record or local history. The existing Vault PDF and its existing private cache behave as they did before Phase 5.

### Free-plan and privacy notes

This phase adds **no Cloud Function, Cloud Storage bucket, new Firestore collection, paid OCR service, or paid Gemini provider**. It uses the same Firestore and Firebase AI Logic setup from Phases 3 and 4. Firebase's Spark plan has daily Firestore quotas; AI Studio/Gemini free-tier limits also apply and can temporarily prevent analysis when exhausted. The app reports quota/network errors rather than switching to a paid service.

The report is processed on-device up to text extraction. Once the user confirms consent, the extracted text is transmitted to Google's Gemini service via Firebase AI Logic. Google's Gemini Developer API free-tier data handling terms may allow prompts and responses to be used to improve Google products; review the current Gemini API terms before submitting real medical records. For a college demo, use synthetic sample reports with no real personal data.

### Current limitations

- OCR is not included. Image-only/scanned PDFs produce an explanatory error instead of fabricated results.
- Password-protected or unreadable PDFs cannot be analyzed.
- To bound processing and keep requests smaller, the analyzer reads at most 60 pages and sends at most 22,000 characters of extracted text. The result warns when content was truncated.
- AI summaries may omit, misread, or misinterpret text. Values and ranges are described as values the AI identified in the source; always confirm them against the original PDF. The analyzer never gives a diagnosis or medication changes.
- Analysis requires an internet connection after text extraction, but no service outside the existing free-tier setup is used.

### Phase 5 setup

1. Finish the Phase 3 Firestore setup and Phase 4 **Firebase AI Logic > Gemini Developer API** setup first. Do not select the Agent Platform/Vertex AI backend, Cloud Functions, or Cloud Storage.
2. Confirm that `gemini_model_name` is published as `gemini-3.5-flash-lite` (the old `assistant_model` key is still accepted for compatibility).
3. Sync Gradle and run the app. No additional Firebase console collection, rules, secret, or billing account is required for this feature.

### Phase 5 manual test checklist

| # | Do this | Expect |
|---|---------|--------|
| 1 | Open Report Analyzer with an empty Vault | A clear empty state and a button to open the Vault |
| 2 | Upload a text-based sample PDF and return to Report Analyzer | The PDF appears with its name, category, size and upload date |
| 3 | Tap Analyze, then Cancel | The consent dialog closes and no AI request starts |
| 4 | Tap Analyze and confirm consent | Progress states show PDF retrieval, local text extraction and AI analysis |
| 5 | Review an analysis result | Overview, findings, terms, values/ranges, questions and limitations appear in separate sections |
| 6 | Expand “Text extracted from the original PDF” | The extracted source text is available for comparison with AI output |
| 7 | Analyze a scanned/image-only PDF | A specific “no readable text/OCR not included” message appears; no made-up report is shown |
| 8 | Switch the app to Hindi and analyze a sample | The AI-generated explanatory text is requested in Hindi and the UI uses Hindi resources |
| 9 | Disable connectivity and try analysis | Analysis is blocked with an offline message; the app remains usable |
| 10 | Inspect Firestore after analysis | No additional analysis history or extracted report text is created |
| 11 | Tap “Open original PDF” after analysis, then press Back | The original viewer opens and the analyzer result remains available |

### Phase 5 automated tests

`./gradlew testDebugUnitTest` includes tests for normalizing PDF text, rejecting too-short extraction, and capping long text before requests, in addition to the existing Phase 1–4 suite.

## Phase 7: Accessibility, localization and dark mode (device-local, no billing)

### What it adds

- A persistent **Profile > Appearance & accessibility** section with a dark-mode switch. It updates the Compose Material 3 color scheme immediately and remembers the choice across app restarts. Android status/navigation bar icon contrast follows the chosen theme. The in-app PDF reader is intentionally kept in the light theme.
- An in-app language selector for **English** and **Hindi** using `AppCompatDelegate.setApplicationLocales` and the already configured `locales_config.xml`. The setting persists, and the existing chatbot/report analyzer language selection reads the active app locale.
- An Android `TextToSpeech` narrator using installed device voices. A Listen action appears on Home, the main screen top bars and sign-in screens to read an accessible screen introduction. Dedicated read-aloud actions also read individual AI assistant replies and the complete structured Report Analyzer result.
- Profile controls to enable/disable narration, automatically narrate screen introductions when navigating, and adjust speech speed (0.5×–2.0×) and volume. Play sample, Pause, Resume and Stop controls are available in Settings; a compact playback bar provides Pause/Resume/Stop while narration is active elsewhere.
- Important form success/error messages, offline status and screen errors are marked as TalkBack live regions and are spoken when narration is enabled. Headings, image descriptions, labelled controls, scrollable layouts and minimum touch-target sizes complement TalkBack rather than replacing it.
- Settings use Android app-private SharedPreferences. No preferences, speech audio or personal data are sent to Firebase.

### Phase 7 manual test checklist

| # | Do this | Expect |
|---|---------|--------|
| 1 | Profile > toggle Dark mode on/off | Main app screens update immediately; the choice survives closing/reopening the app |
| 2 | Open a PDF from the Vault while dark mode is on | PDF pages, viewer controls and viewer toolbar remain in the light theme |
| 3 | Profile > select हिन्दी, then English | Labels across the app update; login session and saved Vault documents remain available |
| 4 | Profile > Play sample | The device's selected TextToSpeech voice reads the sample in the app language |
| 5 | Tap Listen on Home, Vault, Assistant, Analyzer or Profile | A localized screen introduction is spoken; playback controls appear while speech is active |
| 6 | While speech is active, tap Pause, Resume and Stop in the playback bar | Speech pauses (resuming from the current sentence), resumes, and stops as requested |
| 7 | Change narration speed and volume, then Play sample | Playback uses the updated values |
| 8 | Enable automatic narration and navigate between tabs | The new screen introduction is spoken once; disable the option to stop automatic screen introductions |
| 9 | Trigger a validation error or disconnect the network | TalkBack can announce the status; voice narration reads important status/error text while narration is enabled |
| 10 | Enable Android TalkBack and increase system font size | Controls have readable labels, the screen remains scrollable, and important actions remain usable |
| 11 | Remove Hindi voice data or use a device without it | The app shows an understandable TextToSpeech availability message in Settings; no crash occurs |
| 12 | Tap the speaker button on an assistant response | Only that assistant response is read in the selected app language |
| 13 | Analyze a sample report and tap the Analyzer speaker button | The full structured analysis and medical disclaimer are read aloud; no extracted-source-text panel is shown |

### Phase 7 limitations

- TextToSpeech voices are provided by Android and depend on the device. Hindi voice data may need to be installed in the phone's Text-to-Speech settings; this is not a paid MedAssist feature.
- Android's `TextToSpeech` API does not expose a universal pause-at-exact-character operation. Pause stops the current sentence; Resume repeats that sentence from its beginning before continuing.
- The general screen Listen action reads curated titles/instructions. Use each assistant message’s speaker button or the Analyzer result’s speaker button to read dynamic chat/report content. TalkBack remains the recommended screen reader for complete UI exploration.
- PDF reader dark mode is intentionally not applied, as requested.
- Language strings are already stored in `values/strings.xml` and `values-hi/strings.xml`; have a Hindi speaker review translations before public release.

No new library, cloud function, Firestore collection, Cloud Storage bucket, OCR service or paid subscription was added in Phase 7.

## Design notes

- Palette: white, aqua `#35C6D0`, orange `#FF9F43`, charcoal `#263238`, muted grey `#687780`, card grey `#F3F6F7`.
- Aqua and orange are too light for white text or for small text on white, so text on aqua/orange is charcoal, and aqua/orange text or icons on white use darker shades (`AquaDeep`, `OrangeDeep`). Contrast math is in `ui/theme/Color.kt`.
- Letter spacing is 0 in every text style because non-zero tracking breaks Hindi (Devanagari) letter joins.
- User-facing text lives only in `res/values/strings.xml` and `res/values-hi/strings.xml`.

## Project layout so far

```
app/src/main/java/com/example/medassist/
  MainActivity.kt, MedAssistApplication.kt, AppContainer.kt
  navigation/   Screen.kt, MedAssistNavHost.kt
  ui/theme/     Color, Type, Shape, Spacing, Theme, CompositionLocals
  ui/common/    UiState.kt
  ui/components Buttons, cards, top bar, loading/empty/error/offline views
  ui/auth/      Login, Register, ForgotPassword, VerifyEmail screens + ViewModels, SessionViewModel, AuthFlow
  ui/MedAssistApp.kt   the sign-in gate (decides which of the above, or the real app, is shown)
  ui/MainShell.kt      five bottom tabs (Home, Vault, Assistant, Analyzer, Profile) for signed-in, verified users
  ui/home|chatbot|vault|steps|settings|analyzer   one folder per feature (Profile is real from Phase 2)
  data/model|repository   Session + MedicalDocument models, AuthRepository / VaultRepository (interfaces)
                          + FirebaseAuthRepository / FirestoreVaultRepository, error mapping
  data/local              picked-file reader, PdfRenderer page source, private PDF cache, PDFBox text extractor, app preferences, TextToSpeech narrator
  domain/validation       AuthValidator, DocumentValidator (plain Kotlin, unit-tested)
  domain/vault            VaultConfig (limits), PdfChunks, DocumentQuery (search/sort)
  ui/vault                VaultScreen + VaultViewModel, DocumentViewerScreen + ViewModel
  domain/assistant        AssistantConfig, AssistantPrompt (safety rules), EmergencyDetector, RateLimiter, ReplyCleaner
  data/repository         + AssistantRepository / FirebaseAssistantRepository (Firebase AI Logic), AssistantSettings (Remote Config)
                          + ReportAnalyzerRepository / FirebaseReportAnalyzerRepository (schema-constrained JSON output)
  domain/assistant        + ReportAnalysisPrompt (report-specific medical-safety instructions)
  domain/vault            + ReportTextPolicy (local text normalization and request-size limits)
  ui/analyzer             Report Analyzer screen + ViewModel (consent, download, extract, analyze, display)
  data/local              + ChatHistoryStore (opt-in, on-device only)
  ui/chatbot              ChatbotScreen + ChatViewModel
app/src/debug|release/    AppCheckInstaller.kt (debug token vs Play Integrity)
firestore.rules, firestore-tests/   security rules and their emulator tests
  utils/        ConnectivityObserver, TimeOfDay
```

## Known limitations (so far)

- Step tracking (Phase 6) is intentionally skipped. Phase 7 narration depends on Android TextToSpeech and voices installed on the device. Report Analyzer is available, with no OCR support for scanned PDFs.
- Vault files are limited to 10 MB each (change `VaultConfig.MAX_FILE_BYTES` and the numbers in `firestore.rules` together).
- Account deletion runs in the app. If the app is killed halfway, some vault data may remain until the user
  signs in and deletes again; a server-side clean-up would need Cloud Functions (Blaze plan).
- Zoom in the PDF reader uses buttons (100/150/200/300 %), not pinch.
- App Check is installed for the AI Assistant only; Firestore does not enforce it yet (you can enforce it in the
  console once every phone you test on has a registered debug token).
- Assistant replies arrive in one piece (no word-by-word streaming).
- Passwords are checked for length and letter+digit only. Firebase's own password policy can be tightened in the console.
- Email verification links open a Firebase web page (not the app), so users return to the app by hand.
- Sign-in is email/password only. There is no "change email" or "change password" screen yet.
- Hindi strings are machine-quality translations; have a native speaker review them before a demo.
- No release hardening yet (minification is off). See the final "production hardening" list in Phase 8.

## Medical Vault empty-category behavior

The Vault keeps its search field and category chips visible even when no PDFs exist. Selecting a category with no matching PDFs shows **“No documents uploaded for this category”** (localized in English and Hindi). The empty-state view is rendered outside the `LazyColumn`; this avoids nesting its own vertical scroll view inside a lazy-list item, which can trigger an infinite-height Compose measurement crash. A UI regression test covers this path.

The Report Analyzer uses inline, non-scrollable empty and error cards inside its document list for the same reason. Opening Analyzer before uploading a PDF should now show the localized empty message and an **Open Vault** action rather than crashing.

## Troubleshooting: unresolved Phase 5 analyzer references

If Android Studio reports unresolved references for `PdfTextExtractor`, `AndroidPdfTextExtractor`, `ReportAnalyzerRepository`, or `FirebaseReportAnalyzerRepository` in `AppContainer.kt`, first verify that you opened the **Phase 5 project root** (the directory containing `settings.gradle.kts`, `build.gradle.kts`, `gradlew`, and `app/`). Do not open the parent directory or a nested `MedAssist/MedAssist` directory.

These declarations are supplied at:

- `app/src/main/java/com/example/medassist/data/local/PdfTextExtractor.kt` (`PdfTextExtractor`, `AndroidPdfTextExtractor`)
- `app/src/main/java/com/example/medassist/data/repository/ReportAnalyzerRepository.kt` (`ReportAnalyzerRepository`)
- `app/src/main/java/com/example/medassist/data/repository/FirebaseReportAnalyzerRepository.kt` (`FirebaseReportAnalyzerRepository`)

If any of those files are missing from your project, you are using an incomplete/older project folder. Extract this entire ZIP into a **new folder** and open that folder in Android Studio rather than copying only `AppContainer.kt` into the Phase 4 project. Then choose **File > Sync Project with Gradle Files**, followed by **Build > Clean Project** and **Build > Rebuild Project**. The `Property ... is never used` messages are warnings, not compilation errors; they can appear until the analyzer is reached through the UI.

## Chatbot speech-to-text

The AI Assistant input row includes a microphone button. The first use requests Android microphone permission; after permission is granted, the app opens Android's speech-recognition UI and inserts the recognized text into the message field for review before the user sends it. Recognition requests use the current app locale (English or Hindi). The feature uses Android's available speech recognition provider and does not add a paid Firebase service. Availability and offline recognition depend on the device's installed speech services and language packs.

## Contextual read-aloud

- Each assistant response has its own speaker button. Tapping it reads that response aloud using the app's selected language, speech speed, and volume. Starting another read-aloud request replaces the currently spoken text.
- The Report Analyzer's completed result has a speaker button that reads the full analysis, including the overview, findings, medical terms, test values/ranges, questions, limitations, truncation notice when applicable, and medical disclaimer.
- The expandable extracted-source-text display has been removed from the Analyzer. On-device extraction still runs because it is needed to request an AI explanation; the consent screen continues to disclose that the extracted text is sent for processing.
- These buttons respect the narration-enabled setting in Profile and Settings and use Android's installed TextToSpeech voice. No additional API or paid service is required.
