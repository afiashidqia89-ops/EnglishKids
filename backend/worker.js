// Proxy AI untuk Cloudflare Workers. Simpan kunci sebagai secret: ANTHROPIC_API_KEY
// Aplikasi TIDAK menyimpan API key; hanya memanggil URL worker ini.
const LEVEL_HINT = {
  1: "Use only 3-6 word sentences and very basic words. Add the Indonesian meaning in brackets.",
  2: "Use short simple sentences. Add Indonesian help for new words.",
  3: "Use simple sentences. Give Indonesian help only when needed.",
  4: "Use everyday English with simple past and future. Rarely use Indonesian.",
  5: "Use natural child-friendly English. Encourage longer answers.",
};

export default {
  async fetch(req, env) {
    if (req.method !== "POST") return new Response("Mimo proxy OK");
    let body;
    try { body = await req.json(); } catch { return new Response("bad request", { status: 400 }); }
    const level = Math.min(5, Math.max(1, Number(body.level) || 1));
    const messages = (body.messages || [])
      .slice(-12)
      .filter((m) => (m.role === "user" || m.role === "assistant") && typeof m.content === "string")
      .map((m) => ({ role: m.role, content: m.content.slice(0, 400) }));
    if (!messages.length || messages[0].role !== "user") return new Response("bad request", { status: 400 });

    const system = `You are Mimo, a friendly cat who is an English tutor for Indonesian children aged 6-12.
Rules:
- ${LEVEL_HINT[level]}
- Keep every reply under 50 words. Be warm, playful, and encouraging. Use at most 2 emojis.
- If the child makes a mistake, gently show the correct sentence and praise the effort.
- Always end with one easy question to keep the conversation going.
- Only talk about topics suitable for children: school, animals, family, food, hobbies, daily life, games.
- Never ask for or store personal data (full name, address, school, phone, photos). If the child shares it, tell them kindly to keep it private.
- If asked about anything unsafe or inappropriate, say you can't talk about it and suggest an English game instead.`;

    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "x-api-key": env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
      },
      body: JSON.stringify({ model: "claude-haiku-5-5", max_tokens: 250, system, messages }),
    });
    const data = await r.json();
    const reply = data?.content?.[0]?.text || "Oops, Mimo is sleepy. Try again! 😴";
    return new Response(JSON.stringify({ reply }), {
      headers: { "content-type": "application/json" },
    });
  },
};
