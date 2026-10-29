# Arafat Dual AI

Lightweight Android app that sends a question to OpenAI, asks Gemini to review the first answer, then asks OpenAI to produce a final answer.

## Cloud APK build
1. Open the **Actions** tab.
2. Select **Build Android APK** and tap **Run workflow**.
3. When the run succeeds, open it and download the `ArafatDualAI-debug-apk` artifact.
4. Extract the downloaded ZIP and install `app-debug.apk` on your Android phone.

## Deploy the backend
Deploy `cloudflare-worker/index.js` as a Cloudflare Worker. Add these Worker settings:
- Secret `APP_PASSWORD`: choose a strong password
- Secret `OPENAI_API_KEY`: your OpenAI API key
- Secret `GEMINI_API_KEY`: your Gemini API key
- Variable `OPENAI_MODEL`: a model available to your OpenAI API account
- Variable `GEMINI_MODEL`: a model available to your Gemini API account

Never put API keys in this repository or the Android app. The shared app password is only basic access protection; add rate limiting before wider use.

API usage can cost money. Cloudflare's free plan does not make AI API inference automatically free. This is an initial debug build project, not yet a signed release.
