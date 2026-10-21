# StudyMate AI — Android MVP

StudyMate AI is a starter project for university students. It lets a student choose a PDF lecture, sends it to a small Node.js backend, extracts text, and asks a real AI model to summarize it or answer questions grounded in the document.

## Architecture

- `android/`: native Android app written in Kotlin + Jetpack Compose.
- `backend/`: Node.js + Express API. It extracts PDF text and calls the OpenAI Responses API.
- The OpenAI API key stays on the backend. Never put it in the Android app.

## MVP features

- Select a PDF from the phone.
- Upload PDF to the backend.
- Ask a question about the uploaded lecture.
- Generate a structured summary.
- Basic loading/error states.

Not included yet: login, cloud history, quiz generator, OCR for scanned PDFs, payment/subscriptions, production rate limiting, and Play Store release signing.

## Requirements

- Android Studio (current stable) on a 64-bit computer.
- Android SDK installed by Android Studio.
- Node.js 20+ (64-bit recommended) and npm.
- OpenAI API key with API billing enabled.
- Internet connection.
- Android device/emulator running Android 8.0+ (API 26+).

## 1. Start the backend

1. Install Node.js 20 or newer.
2. Open a terminal in `backend/`.
3. Run:
   ```bash
   npm install
   ```
4. Copy `.env.example` to `.env` and add your key:
   ```env
   OPENAI_API_KEY=your_key_here
   OPENAI_MODEL=gpt-4.1-mini
   PORT=3000
   MAX_PDF_MB=15
   ```
5. Start:
   ```bash
   npm start
   ```
6. Test on the same computer:
   `http://localhost:3000/health`

### Connect the Android app

- Android Emulator: set `BASE_URL` in `android/app/src/main/java/com/studymateai/app/ApiConfig.kt` to `http://10.0.2.2:3000/`.
- Physical Android phone: computer and phone must be on the same Wi-Fi. Set `BASE_URL` to the computer's LAN IP, e.g. `http://192.168.1.20:3000/`. Allow port 3000 in the computer firewall.
- For real deployment, use HTTPS. Do not expose a development server publicly.

The included app uses cleartext HTTP for local development only. For production, deploy the backend with HTTPS and remove the cleartext exception from the Android manifest.

## 2. Open the Android project

1. Open Android Studio.
2. Choose **Open** and select the `android/` folder.
3. Allow Gradle sync and install any requested Android SDK components.
4. Check `ApiConfig.kt` and set `BASE_URL`.
5. Run on an emulator or connect a phone with Developer options + USB debugging enabled.

## How to use

1. Start the backend first.
2. Open StudyMate AI.
3. Tap **Choose lecture PDF**.
4. Select a text-based PDF (scanned image PDFs are not supported in this MVP).
5. Enter a question and tap **Ask AI**, or tap **Summarize lecture**.

## API

- `GET /health` — status check.
- `POST /api/study` — multipart form:
  - `file`: PDF file
  - `mode`: `ask` or `summarize`
  - `question`: required for `ask`

The backend returns JSON with `answer`, `filename`, and `characters`.

## Cost and security

- The Android app itself can be built with free tools.
- The OpenAI API is usage-billed; exact cost depends on the selected model, input length, and output length. Set a low output-token limit and check the provider's current pricing before launch.
- This MVP limits PDF size and extracted text length, but it is not production hardened. Add authentication, per-user quotas, request timeouts, rate limiting, logging without sensitive document contents, and a privacy policy before public release.
- Do not upload confidential student records or copyrighted course materials without permission.
- The server currently sends extracted PDF text to the AI provider for processing. Tell users this clearly in the privacy notice.
