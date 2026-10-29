package com.arafat.dualai

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("arafat_dual_ai", Context.MODE_PRIVATE) }
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var question: EditText
    private lateinit var send: ImageButton
    private lateinit var status: TextView
    private lateinit var workerUrl: EditText
    private lateinit var appPassword: EditText
    private var busy = false

    private val ink = Color.rgb(28, 32, 38)
    private val muted = Color.rgb(104, 112, 122)
    private val pageColor = Color.rgb(245, 247, 250)
    private val teal = Color.rgb(0, 137, 123)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildChatScreen()
        addBubble("Arafat Dual AI", "আসসালামু আলাইকুম! আমি প্রস্তুত। নিচে প্রশ্ন লিখে পাঠাও। ChatGPT উত্তর দেবে, Gemini সেটি পর্যালোচনা করবে, তারপর চূড়ান্ত উত্তর আসবে।", false, "AI")
        status.text = if (prefs.getString("worker_url", "").isNullOrBlank()) "প্রথমবার ব্যবহার করতে ⚙ সেটিংস খুলে সংযোগ দাও" else "সংযোগ সেটিংস সংরক্ষিত"
    }

    private fun buildChatScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageColor)
        }
        setContentView(root)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        val avatar = ImageView(this).apply {
            setImageResource(R.drawable.arafat_logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Arafat Dual AI"
            background = rounded(Color.LTGRAY, 100)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }
        header.addView(avatar)
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        titles.addView(text("Arafat Dual AI", 18, true, ink))
        titles.addView(text("ChatGPT + Gemini • AI বন্ধুরা", 12, false, muted))
        header.addView(titles)
        val settings = Button(this).apply {
            text = "⚙"
            textSize = 20f
            isAllCaps = false
            setTextColor(ink)
            background = rounded(Color.rgb(240, 243, 246), 14)
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { showSettings() }
        }
        header.addView(settings)
        root.addView(header)

        status = text("প্রস্তুত", 12, false, muted).apply {
            setPadding(dp(16), dp(7), dp(16), dp(7))
            setBackgroundColor(Color.rgb(239, 243, 246))
        }
        root.addView(status)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(8))
        }
        scroll.addView(messages)
        root.addView(scroll)

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        question = EditText(this).apply {
            hint = "মেসেজ লেখো…"
            textSize = 16f
            minLines = 1
            maxLines = 4
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rounded(Color.rgb(243, 245, 247), 22)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        composer.addView(question)
        send = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_send)
            contentDescription = "প্রশ্ন পাঠাও"
            background = rounded(teal, 50)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(8) }
            setOnClickListener { sendQuestion() }
        }
        composer.addView(send)
        root.addView(composer)
    }

    private fun showSettings() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        panel.addView(text("Worker URL ও অ্যাপ পাসওয়ার্ড একবার সেট করলেই সংরক্ষিত থাকবে।", 14, false, muted))
        panel.addView(text("Cloudflare Worker URL", 14, true, ink).apply { setPadding(0, dp(12), 0, 0) })
        workerUrl = EditText(this).apply {
            hint = "https://your-name.workers.dev"
            textSize = 14f
            setSingleLine(true)
            setText(prefs.getString("worker_url", "") ?: "")
        }
        panel.addView(workerUrl)
        panel.addView(text("App password (Cloudflare-এর APP_PASSWORD)", 14, true, ink).apply { setPadding(0, dp(10), 0, 0) })
        appPassword = EditText(this).apply {
            hint = "তুমি যে পাসওয়ার্ড সেট করবে"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("app_password", "") ?: "")
        }
        panel.addView(appPassword)
        AlertDialog.Builder(this)
            .setTitle("সংযোগ সেটিংস")
            .setView(panel)
            .setNegativeButton("বাতিল", null)
            .setPositiveButton("সংরক্ষণ") { _, _ ->
                val url = workerUrl.text.toString().trim().trimEnd('/')
                val pass = appPassword.text.toString()
                if (!url.startsWith("https://") || url.contains("your-name") || pass.isBlank()) {
                    status.text = "সঠিক Worker URL এবং পাসওয়ার্ড দিতে হবে"
                    Toast.makeText(this, "URL ও পাসওয়ার্ড ঠিক করে আবার সংরক্ষণ করো", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("worker_url", url).putString("app_password", pass).apply()
                    status.text = "সংযোগ সেটিংস সংরক্ষিত"
                    Toast.makeText(this, "সেটিংস সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun sendQuestion() {
        if (busy) return
        val base = prefs.getString("worker_url", "")?.trim()?.trimEnd('/') ?: ""
        val pass = prefs.getString("app_password", "") ?: ""
        val q = question.text.toString().trim()
        if (base.isBlank() || pass.isBlank()) {
            showSettings()
            status.text = "আগে ⚙ সেটিংসে সংযোগের তথ্য দাও"
            return
        }
        if (q.isBlank()) {
            question.error = "প্রশ্ন লিখো"
            return
        }
        addBubble("তুমি", q, true, "তুমি")
        question.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(question.windowToken, 0)
        busy = true
        send.isEnabled = false
        status.text = "ChatGPT ও Gemini আলোচনা করছে…"
        val pending = addBubble("Arafat Dual AI", "উত্তর তৈরি হচ্ছে…", false, "AI")
        executor.execute {
            try {
                val conn = URL("$base/api/debate").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 20000
                conn.readTimeout = 90000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("x-app-password", pass)
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                    it.write(JSONObject().put("question", q).toString())
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                conn.disconnect()
                val data = JSONObject(body)
                if (code !in 200..299) throw Exception(data.optString("error", "HTTP $code"))
                runOnUiThread {
                    messages.removeView(pending)
                    addBubble("ChatGPT • প্রথম উত্তর", data.optString("first", "উত্তর পাওয়া যায়নি"), false, "ChatGPT")
                    addBubble("Gemini • পর্যালোচনা", data.optString("review", "পর্যালোচনা পাওয়া যায়নি"), false, "Gemini")
                    addBubble("চূড়ান্ত উত্তর", data.optString("final", "চূড়ান্ত উত্তর পাওয়া যায়নি"), false, "Final")
                    status.text = "আলোচনা সম্পন্ন"
                    busy = false
                    send.isEnabled = true
                    scrollToBottom()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    messages.removeView(pending)
                    addBubble("সংযোগে সমস্যা", e.message ?: "অনুরোধ ব্যর্থ হয়েছে", false, "Error")
                    status.text = "অনুরোধ ব্যর্থ — URL, পাসওয়ার্ড, Worker ও API সেটিংস দেখো"
                    busy = false
                    send.isEnabled = true
                    scrollToBottom()
                }
            }
        }
    }

    private fun addBubble(title: String, body: String, fromUser: Boolean, type: String): View {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(9), dp(13), dp(10))
            background = rounded(
                if (fromUser) Color.rgb(214, 245, 232) else Color.WHITE,
                18
            )
            elevation = dp(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                if (fromUser) (resources.displayMetrics.widthPixels * 0.78f).toInt() else (resources.displayMetrics.widthPixels * 0.86f).toInt(),
                -2
            )
        }
        card.addView(text(title, 12, true, if (fromUser) Color.rgb(0, 105, 85) else teal))
        card.addView(text(body, 15, false, ink).apply {
            setPadding(0, dp(4), 0, 0)
            textIsSelectable = true
        })
        line.addView(card)
        messages.addView(line)
        scrollToBottom()
        return line
    }

    private fun scrollToBottom() {
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
