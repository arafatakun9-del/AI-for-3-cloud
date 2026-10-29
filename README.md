# Arafat Dual AI

A lightweight native Android app with a messenger-style conversation screen. Ask a question once; OpenAI gives an initial answer, Gemini reviews it, and OpenAI prepares the final answer.

## Build the APK using only a phone
1. Open the **Actions** tab.
2. Select **Build Android APK**.
3. Pushes to `main` automatically trigger a build.
4. When the run succeeds, open it and download **ArafatDualAI-debug-apk**.
5. Extract the ZIP and install `app-debug.apk` on Android.

## First-time connection setup
Open the app and tap the ⚙ settings button. Enter the deployed Cloudflare Worker URL and the app password you created as the Worker secret `APP_PASSWORD`. The app saves these settings on the device.

The app password is **not** an OpenAI or Gemini API key and is not automatically provided by ChatGPT. You choose it yourself in Cloudflare Worker Settings → Variables and Secrets → Add a secret → `APP_PASSWORD`, then enter that same password in the app.

## Cloudflare Worker setup
Deploy `cloudflare-worker/index.js` as a Worker. In Worker Settings → Variables and Secrets, add:
- Secret `APP_PASSWORD`: a strong password you choose
- Secret `OPENAI_API_KEY`: your OpenAI API key
- Secret `GEMINI_API_KEY`: your Gemini API key
- Text variable `OPENAI_MODEL`: a model available to your OpenAI API account
- Text variable `GEMINI_MODEL`: a model available to your Gemini API account

Save and deploy. Open the Worker URL in a browser; it should show JSON with status online. Then enter that URL and the same `APP_PASSWORD` in the app settings.

Never put provider API keys in this repository or in the APK. Keep the repository private if it contains personal assets. The app password is basic access protection; add rate limiting before wider use.

## Cost and release status
Cloudflare's free plan does not make OpenAI or Gemini inference automatically free. Check the providers' current quota and billing details. The workflow builds a debug APK; it is not a signed release build.
