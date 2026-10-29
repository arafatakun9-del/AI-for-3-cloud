package com.arafat.dualai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.content.Context
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
    private lateinit var server: EditText
    private lateinit var password: EditText
    private lateinit var question: EditText
    private lateinit var send: Button
    private lateinit var status: TextView
    private lateinit var first: TextView
    private lateinit var review: TextView
    private lateinit var finalAnswer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
            setBackgroundColor(Color.rgb(247, 248, 250))
        }
        scroll.addView(page)
        setContentView(scroll)

        page.addView(ImageView(this).apply {
            setImageResource(R.drawable.arafat_logo)
            contentDescription = "Arafat Dual AI logo"
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(88), dp(88)).apply {
                bottomMargin = dp(10)
            }
        })
        page.addView(label("Arafat Dual AI", 27, true))
        page.addView(label("ChatGPT + Gemini • AI review", 14, false))
        page.addView(space(14))
        page.addView(label("Cloudflare Worker URL", 15, true))
        server = edit("https://your-worker.workers.dev")
        page.addView(server)
        page.addView(space(8))
        page.addView(label("App password", 15, true))
        password = edit("Worker APP_PASSWORD")
        password.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        page.addView(password)
        page.addView(space(8))
        page.addView(label("তোমার প্রশ্ন", 15, true))
        question = edit("এখানে প্রশ্ন লিখো")
        question.minLines = 3
        question.maxLines = 7
        question.gravity = android.view.Gravity.TOP
        page.addView(question)
        send = Button(this).apply { text = "দুই AI-কে জিজ্ঞেস করো"; isAllCaps = false }
        page.addView(send)
        status = label("প্রস্তুত", 13, false)
        page.addView(status)
        first = section(page, "ChatGPT-এর প্রথম উত্তর")
        review = section(page, "Gemini-এর পর্যালোচনা")
        finalAnswer = section(page, "চূড়ান্ত উত্তর")

        send.setOnClickListener {
            val base = server.text.toString().trim().trimEnd('/')
            val pass = password.text.toString()
            val q = question.text.toString().trim()
            if (!base.startsWith("https://") || base.contains("your-worker")) {
                status.text = "নিজের Worker URL বসাও"; return@setOnClickListener
            }
            if (pass.isBlank() || q.isBlank()) {
                status.text = "Password এবং প্রশ্ন দুটোই লিখো"; return@setOnClickListener
            }
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(question.windowToken, 0)
            send.isEnabled = false
            status.text = "দুই AI কাজ করছে…"
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
                        first.text = data.optString("first", "No response")
                        review.text = data.optString("review", "No review")
                        finalAnswer.text = data.optString("final", "No final answer")
                        status.text = "সম্পন্ন"
                        send.isEnabled = true
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        status.text = "সমস্যা: ${e.message ?: "অনুরোধ ব্যর্থ"}"
                        send.isEnabled = true
                    }
                }
            }
        }
    }

    private fun section(parent: LinearLayout, heading: String): TextView {
        parent.addView(space(10))
        parent.addView(label(heading, 16, true))
        val box = TextView(this).apply {
            text = "এখানে ফলাফল দেখা যাবে"
            textSize = 14f
            setTextColor(Color.rgb(30, 35, 42))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        parent.addView(box)
        return box
    }
    private fun edit(h: String) = EditText(this).apply {
        hint = h; textSize = 14f; setPadding(dp(12), dp(8), dp(12), dp(8))
    }
    private fun label(t: String, size: Int, bold: Boolean) = TextView(this).apply {
        text = t; textSize = size.toFloat(); setTextColor(Color.rgb(30, 35, 42))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun space(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
