# MedAssist

**MedAssist** is an Android medical-assistance application built with Kotlin and Jetpack Compose. It helps users organize personal medical documents, ask health-information questions with an AI assistant, and understand text-based medical reports through an accessible, bilingual interface.

## Features

### Account and profile
- Email-and-password registration and sign-in with Firebase Authentication.
- Email verification, password reset, persistent sessions, sign-out, and account deletion.
- Profile settings for language, appearance, and narration preferences.

### Medical Vault
- Upload PDF documents using Android's system file picker.
- Browse, search, filter by category, sort, rename, view, and delete documents.
- Read PDFs in the app with page navigation and zoom controls.
- Store document metadata and PDF content in user-scoped Cloud Firestore records. PDF content is stored as binary chunks, not Base64 text; each PDF is limited to 10 MB.
- Apply Firestore security rules so users can access only their own records.

### AI Assistant
- Ask general health-information questions and request plain-language explanations of common medical terminology.
- Receive responses in English or Hindi, with relevant context maintained during the current conversation.
- Use microphone input through Android's speech-recognition interface.
- Read individual assistant responses aloud with Android TextToSpeech.
- View emergency-related guidance when a message suggests an urgent situation.
- Keep chat history off by default; optional saved chat history is stored locally on the device.

### Report Analyzer
- Select a PDF from the Medical Vault and review a consent notice before report text is sent for AI processing.
- Extract readable PDF text on the device using PDFBox-Android.
- Receive a structured explanation covering the overview, findings, terminology, values and ranges identified in the report, questions for a healthcare professional, and limitations.
- Read the complete analysis aloud and open the original PDF to compare the explanation with its source.
- Avoid automatically storing extracted report text or analysis history.

### Accessibility and personalization
- English and Hindi interface localization.
- Screen narration and dedicated read-aloud controls for assistant messages and report analyses.
- Narration preferences for speech speed, volume, and automatic screen introductions.
- Android TalkBack semantics and announcements for important interface states.
- Persistent light/dark appearance selection.

## Technology stack

| Area | Technologies |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM, repositories, Kotlin Coroutines and Flow |
| Authentication | Firebase Authentication |
| Data | Cloud Firestore with owner-scoped security rules |
| AI | Firebase AI Logic with the Gemini Developer API |
| Configuration and app verification | Firebase Remote Config and Firebase App Check |
| PDF processing | PDFBox-Android for on-device text extraction; Android PDF rendering APIs |
| Accessibility | Android TextToSpeech and Android speech recognition |
| Tests | JUnit, Compose UI tests, Firebase Rules Unit Testing with the local Firestore Emulator |

## Project architecture

The app separates presentation, domain logic, and data access:

```text
app/src/main/java/com/example/medassist/
├── data/
│   ├── local/         # App preferences, PDF handling, local chat history, narration
│   ├── model/         # Session, document, chat, and report models
│   └── repository/    # Authentication, vault, assistant, and analyzer repositories
├── domain/
│   ├── assistant/     # AI prompts, safety policies, request limits, response handling
│   ├── validation/    # Authentication and document validation
│   └── vault/         # Document queries, PDF chunking, report-text policies
├── navigation/        # App navigation destinations and navigation host
├── ui/
│   ├── analyzer/      # Report Analyzer screen and ViewModel
│   ├── auth/          # Login, registration, verification, password reset
│   ├── chatbot/       # AI Assistant screen and ViewModel
│   ├── home/          # Home dashboard
│   ├── settings/      # Profile and preferences
│   ├── vault/         # Medical Vault and PDF viewer
│   └── theme/         # Material 3 theme, colors, typography, and shapes
└── utils/             # Shared utilities, including connectivity observation

firestore.rules        # Firestore security rules
firestore-tests/       # Local Firestore security-rule tests
scripts/               # Repository validation scripts
```

## Getting started

### Prerequisites

- Android Studio compatible with the Android Gradle Plugin version configured in this repository.
- JDK 25 for the configured Gradle daemon toolchain.
- Android SDK platforms/build tools required by the project (`compileSdk 37`, `targetSdk 36`).
- An Android device or emulator running Android 8.0 (API 26) or later.
- A Firebase project you control.

### 1. Clone and open the project

```bash
git clone <repository-url>
cd MedAssist
```

In Android Studio, open the directory containing `settings.gradle.kts`, `build.gradle.kts`, and `app/`. Allow Gradle sync to finish before running the application.

### 2. Configure Firebase

1. In the Firebase console, create a project and register an Android app with package name `com.example.medassist`.
2. Download that Firebase project's `google-services.json` and place it at `app/google-services.json`. This project-specific file is excluded from Git by `.gitignore`; do not commit it to the repository.
3. In **Firebase Authentication**, enable the **Email/Password** sign-in provider.
4. Create a **Cloud Firestore** database.
5. Publish the repository's `firestore.rules` to the same Firebase project. You can use the Firebase CLI after selecting the correct project:

   ```bash
   firebase login
   firebase use --add
   firebase deploy --only firestore:rules
   ```

6. In Firebase AI Logic, configure the **Gemini Developer API** provider.
7. Configure Firebase App Check for the Android app. For local debug builds, register the debug token generated by the app; use the appropriate production provider for release builds. Never publish a debug token.
8. In Remote Config, the optional `gemini_model_name` parameter can be used to configure the Gemini model. The app contains a fallback when the parameter is missing or unsupported.

Firebase console navigation and service availability may change. Confirm that the Firebase project selected in Android Studio/CLI is the project you intend to use before publishing rules or changing settings.

### 3. Run the app

In Android Studio, select the `app` run configuration and click **Run**. The first Gradle sync/build may need internet access to download dependencies and the configured Gradle distribution.

## Build and test

Run commands from the project root.

**Windows (PowerShell):**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest
python scripts/validate_release.py
```

**macOS/Linux:**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
python scripts/validate_release.py
```

`connectedDebugAndroidTest` requires a connected Android device or running emulator. `scripts/validate_release.py` performs static repository checks; it does not replace compilation, runtime testing, Firebase integration checks, or Firestore emulator tests.

### Firestore security-rule tests

The `firestore-tests/` directory contains tests for the Firestore rules using the local emulator. They require Node.js 20 or later, Java 21 or later, and the Firebase CLI.

```bash
cd firestore-tests
npm ci
npm test
```

These tests run against a local demo project and are intended to verify access-control behavior without changing rules in your live Firebase project.

## Data handling and security

- Firebase Authentication identifies the signed-in user; document paths and Firestore rules scope medical records to that user.
- PDF content is stored as binary chunks in Firestore, with metadata stored separately. The current upload limit is 10 MB per PDF.
- The Report Analyzer extracts PDF text locally. After the user confirms the consent notice, the extracted text is sent to Gemini through Firebase AI Logic for analysis. Reports containing no readable text cannot be analyzed successfully.
- Report text and analysis results are not automatically saved as an additional record. Optional chat history is stored on-device rather than uploaded as a transcript.
- `google-services.json`, signing keystores, passwords, local configuration, and App Check debug tokens should not be committed to a public repository.
- Review Google's current Gemini Developer API data-handling terms and your Firebase project's settings before processing sensitive information. Use synthetic or de-identified documents for demos and testing.

## Release notes

Before distributing a release build:

1. Run the unit tests, static checks, Firestore security-rule tests, and a device/emulator smoke test.
2. Verify Firebase Authentication, Firestore rules, Firebase AI Logic, Remote Config, and App Check in the intended Firebase project.
3. Configure the release App Check provider and test the release distribution path.
4. Create a signed APK or Android App Bundle using a keystore stored outside the repository. Back up the keystore and passwords securely; the same signing identity is needed for future updates.

See [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) for release procedures and [`TEST_RESULTS.md`](TEST_RESULTS.md) for the repository's recorded validation status.

## Responsible use

MedAssist is an educational project and is not a substitute for professional medical care. Always compare extracted or summarized values with the original report and discuss medical concerns with a qualified healthcare professional. Do not use the app to make urgent medical decisions or to start, stop, or change medication.
