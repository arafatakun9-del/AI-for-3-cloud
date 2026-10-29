export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, x-app-password"
    };
    const reply = (data, status = 200) => new Response(JSON.stringify(data), {
      status, headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
    });
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/")
      return reply({ app: "Arafat Dual AI", status: "online" });
    if (request.method !== "POST" || url.pathname !== "/api/debate")
      return reply({ error: "Not found" }, 404);
    if (!env.APP_PASSWORD || request.headers.get("x-app-password") !== env.APP_PASSWORD)
      return reply({ error: "Unauthorized" }, 401);
    let body;
    try { body = await request.json(); } catch { return reply({ error: "Invalid JSON" }, 400); }
    const question = String(body.question || "").trim();
    if (!question || question.length > 4000)
      return reply({ error: "Question must be 1-4000 characters" }, 400);
    if (!env.OPENAI_API_KEY || !env.GEMINI_API_KEY || !env.OPENAI_MODEL || !env.GEMINI_MODEL)
      return reply({ error: "Server configuration incomplete" }, 500);
    try {
      const askOpenAI = async input => {
        const r = await fetch("https://api.openai.com/v1/responses", {
          method: "POST",
          headers: { "Authorization": `Bearer ${env.OPENAI_API_KEY}`, "Content-Type": "application/json" },
          body: JSON.stringify({ model: env.OPENAI_MODEL, input })
        });
        if (!r.ok) throw new Error("OpenAI HTTP " + r.status);
        const d = await r.json();
        return (d.output || []).flatMap(x => x.content || [])
          .filter(x => x.type === "output_text").map(x => x.text).join("\n");
      };
      const first = await askOpenAI(question);
      const gr = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(env.GEMINI_MODEL)}:generateContent`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
          body: JSON.stringify({ contents: [{ parts: [{ text:
            `Review the answer critically. Find errors and gaps; do not agree blindly.\nQuestion: ${question}\nAnswer: ${first}`
          }] }] })
        }
      );
      if (!gr.ok) throw new Error("Gemini HTTP " + gr.status);
      const gd = await gr.json();
      const review = (gd.candidates?.[0]?.content?.parts || []).map(p => p.text || "").join("\n");
      const final = await askOpenAI(
        `Give a clear final answer. Consider the review critically and correct valid issues.\nQuestion: ${question}\nInitial answer: ${first}\nGemini review: ${review}`
      );
      return reply({ first, review, final });
    } catch (e) {
      console.error("Provider request failed", e.message);
      return reply({ error: "AI service failed. Check model IDs, API quota and billing." }, 502);
    }
  }
};
