# Arafat Dual AI

A lightweight native Android starter app. It sends a question to OpenAI, asks Gemini to review the first answer, and asks OpenAI to prepare a final answer.

## Build the APK from a phone
1. Open the **Actions** tab in this repository.
2. Select **Build Android APK**.
3. Tap **Run workflow** on the main branch. A push to main also starts a build.
4. When the run succeeds, open it and download the **ArafatDualAI-debug-apk** artifact.
5. Extract the downloaded ZIP and install `app-debug.apk` on your Android phone.

## Cloudflare Worker setup
Deploy `cloudflare-worker/index.js` as a Worker. In Worker Settings → Variables and Secrets, add:

- Secret `APP_PASSWORD`: choose a strong password you will enter in the app
- Secret `OPENAI_API_KEY`: your own OpenAI API key
- Secret `GEMINI_API_KEY`: your own Gemini API key
- Text variable `OPENAI_MODEL`: a model available to your OpenAI API account
- Text variable `GEMINI_MODEL`: a model available to your Gemini API account

Save and deploy. Open the Worker URL in a browser; it should show JSON with status online. Then enter that Worker URL and the same app password in Arafat Dual AI.

Never put API keys in this repository or in the APK. Do not send API keys to anyone. The shared app password is only basic access protection; add rate limiting and stronger authentication before wider public use.

## Costs and status
Cloudflare's free plan does not make OpenAI or Gemini API inference automatically free. Check current provider quotas and prices. This is a debug APK workflow, not a signed release pipeline. The Android launcher currently uses a simple vector brand icon; the supplied portrait photo still needs to be uploaded as a binary image asset if it must be the exact launcher icon.
