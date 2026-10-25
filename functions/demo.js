/**
 * Landing-page demo ("Try it on a meal" in web/index.html). The page POSTs
 * { image?, description?, language? } to /api/demo (a Firebase Hosting
 * rewrite to this function) and gets back one trimmed nutrient analysis.
 *
 * Unlike openaiProxy (index.js) this is NOT a pass-through: the server builds
 * the whole OpenAI request (prompt, model, reasoning effort, output cap), so a
 * caller can only ever get a meal analysed, never arbitrary completions on our
 * key.
 *
 * Web visitors have no stable identity (a fresh anonymous one is free), so
 * openaiProxy's per-user caps don't apply. The caps here, all tunable live in
 * Firestore at config/demo without redeploying:
 *   - enabled (default true): the demo's own kill switch.
 *   - dailyBudget (default 60): every demo call per UTC day, counted in
 *     meters/demo. Deliberately separate from openaiProxy's monthly budget, so
 *     an abused demo can never lock out app users. Worst case per month is
 *     30 x this.
 *   - perIpDailyCap (default 3): per visitor IP per UTC day, counted in
 *     meters/demo/visitors/{sha256(day:ip)}. A soft limit only - IPs are shared, rotate,
 *     and the header is spoofable when the function is called directly
 *     instead of through Hosting. dailyBudget is the real ceiling. Each doc
 *     carries an expireAt for a Firestore TTL policy (see README).
 *   - requireAppCheck (default true): the request must carry a valid Firebase
 *     App Check token (reCAPTCHA Enterprise on the page) in X-Firebase-AppCheck.
 *   - model / reasoningEffort (default gpt-5.4-mini / low): the app uses
 *     "high" effort, but a landing-page visitor won't wait that long.
 *
 * Like openaiProxy, counters are incremented BEFORE the upstream call, so a
 * failed OpenAI call still uses up a try.
 */

const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const crypto = require("crypto");
const { OPENAI_KEY } = require("./lib/secrets");
const { ANALYSIS_SYSTEM, ANALYSIS_USER } = require("./lib/demoPrompt");

const db = admin.firestore();

const OPENAI_URL = "https://api.openai.com/v1/responses";

// Fallbacks used only if config/demo is missing a field.
const DEFAULT_DAILY_BUDGET = 60;
const DEFAULT_PER_IP_DAILY_CAP = 3;
const DEFAULT_MODEL = "gpt-5.4-mini";
const DEFAULT_REASONING_EFFORT = "low";

// Reasoning tokens count towards this too; "low" effort stays well under it.
const MAX_OUTPUT_TOKENS = 6000;

// The page downscales photos to 768px JPEGs (~100-200 KB as base64), so
// anything near this limit didn't come from the page.
const MAX_IMAGE_CHARS = 1_500_000;
const MAX_DESCRIPTION_CHARS = 500;
const IMAGE_DATA_URL = /^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/]+=*$/;
// English display name of the visitor's language, e.g. "German" - the same
// form the app passes as {phone_language}.
const LANGUAGE = /^[A-Za-z][A-Za-z ()-]{1,39}$/;

// Visitor docs are only needed for the day they count; TTL deletes them after.
const IP_DOC_TTL_MILLIS = 2 * 24 * 60 * 60 * 1000;

function sendError(res, status, code, message, extra) {
  res.status(status).json({ error: { message, code }, ...extra });
}

/**
 * Validates the page's request body. Returns { image, description, language }
 * or { error } with a message safe to show the visitor.
 */
function parseDemoRequest(body) {
  if (!body || typeof body !== "object") return { error: "Send a photo or a description of your meal." };

  const image = body.image == null || body.image === "" ? null : body.image;
  if (image != null && (typeof image !== "string" || image.length > MAX_IMAGE_CHARS || !IMAGE_DATA_URL.test(image))) {
    return { error: "That photo couldn't be read. Try a JPEG or PNG." };
  }

  const description = typeof body.description === "string" ? body.description.trim() : "";
  if (description.length > MAX_DESCRIPTION_CHARS) {
    return { error: `Keep the description under ${MAX_DESCRIPTION_CHARS} characters.` };
  }
  if (image == null && description === "") {
    return { error: "Send a photo or a description of your meal." };
  }

  const language = typeof body.language === "string" && LANGUAGE.test(body.language) ? body.language : "English";
  return { image, description, language };
}

/** The OpenAI /v1/responses body, shaped like the app's NutrientAnalysisPrompt.toApiModel(). */
function buildOpenAiRequest({ image, description, language }, { model, reasoningEffort }) {
  const input = [
    { role: "system", content: [{ type: "input_text", text: ANALYSIS_SYSTEM.replaceAll("{phone_language}", language) }] },
  ];
  if (image != null) {
    input.push({ role: "user", content: [{ type: "input_image", image_url: image }] });
  }
  // The prompt only fills in "title" when the input has none, and an empty
  // Title: line reads as "specified" often enough to come back untitled.
  const userText = ANALYSIS_USER
    .replaceAll("{phone_language}", language)
    .replace("{title}", "(none yet, please provide one)")
    .replace("{description}", description);
  input.push({ role: "user", content: [{ type: "input_text", text: userText }] });

  return {
    model,
    reasoning: { effort: reasoningEffort },
    text: { format: { type: "json_object" } },
    max_output_tokens: MAX_OUTPUT_TOKENS,
    input,
  };
}

function firstOutputText(responseBody) {
  for (const item of responseBody.output || []) {
    for (const content of item.content || []) {
      if (content.type === "output_text" && typeof content.text === "string") return content.text;
    }
  }
  return null;
}

function grams(nutrient) {
  const value = Number(nutrient?.grams);
  return Number.isFinite(value) ? value : null;
}

/**
 * Turns OpenAI's response body into what the page renders: { result } on
 * success, { notFood } when the model refused (no food, unreadable photo), or
 * throws if the response isn't in the agreed shape.
 */
function toDemoResult(responseBody) {
  const text = firstOutputText(responseBody);
  if (text == null) throw new Error(`No output_text in response (status: ${responseBody.status})`);
  const parsed = JSON.parse(text);

  if (typeof parsed.error === "string" && parsed.error !== "") return { notFood: parsed.error };

  const n = parsed.nutrients;
  const calories = Number(n?.calories);
  if (!n || !Number.isFinite(calories)) throw new Error("Response has no calories");

  const components = Array.isArray(parsed.components) ? parsed.components : [];
  return {
    result: {
      title: typeof parsed.title === "string" ? parsed.title : "",
      calories,
      protein: grams(n.protein),
      fat: grams(n.fat),
      saturated: grams(n.ofWhichSaturated),
      carbs: grams(n.carbohydrate),
      sugar: grams(n.ofWhichSugar),
      addedSugar: grams(n.ofWhichAddedSugar),
      salt: grams(n.salt),
      fibre: grams(n.fibre),
      components: components
        .filter((c) => c && typeof c.name === "string")
        .slice(0, 12)
        .map((c) => ({
          name: c.name,
          amount: typeof c.estimatedAmount === "string" ? c.estimatedAmount : "",
          confidence: ["high", "medium", "low"].includes(c.confidence) ? c.confidence : "medium",
        })),
      notes: typeof parsed.notes === "string" ? parsed.notes : "",
    },
  };
}

/**
 * The visitor's IP. Firebase Hosting sets fastly-client-ip on rewritten
 * requests; req.ip is the fallback for direct calls (and the emulator).
 */
function clientIp(req) {
  return req.get("fastly-client-ip") || req.ip || "unknown";
}

exports.demoAnalyze = onRequest(
  {
    region: "us-central1", // must match the Hosting rewrite in firebase.json.
    secrets: [OPENAI_KEY],
    maxInstances: 3,
    timeoutSeconds: 120,
    memory: "512MiB", // base64 image payloads.
    cors: false, // same-origin via Hosting; no cross-origin caller needed.
  },
  async (req, res) => {
    res.set("Cache-Control", "no-store");
    if (req.method !== "POST") {
      sendError(res, 405, "method_not_allowed", "Only POST is supported.");
      return;
    }

    let cfg;
    try {
      cfg = (await db.doc("config/demo").get()).data() || {};
    } catch (e) {
      logger.error("Demo config read failed", e);
      sendError(res, 500, "server_error", "Something went wrong. Please try again in a moment.");
      return;
    }
    if (cfg.enabled === false) {
      sendError(res, 503, "demo_unavailable", "The demo is taking a break. Please try again later.");
      return;
    }

    if (cfg.requireAppCheck !== false) {
      const appCheckToken = req.get("X-Firebase-AppCheck");
      if (!appCheckToken) {
        sendError(res, 401, "app_check_missing", "The demo couldn't verify this browser. Please reload the page.");
        return;
      }
      try {
        await admin.appCheck().verifyToken(appCheckToken);
      } catch (e) {
        sendError(res, 401, "app_check_invalid", "The demo couldn't verify this browser. Please reload the page.");
        return;
      }
    }

    const request = parseDemoRequest(req.body);
    if (request.error) {
      sendError(res, 400, "invalid_request", request.error);
      return;
    }

    // Caps, atomically (see header comment).
    const now = new Date();
    const dayKey = now.toISOString().slice(0, 10); // YYYY-MM-DD (UTC)
    const dailyBudget = cfg.dailyBudget ?? DEFAULT_DAILY_BUDGET;
    const perIpDailyCap = cfg.perIpDailyCap ?? DEFAULT_PER_IP_DAILY_CAP;
    const ipHash = crypto.createHash("sha256").update(`${dayKey}:${clientIp(req)}`).digest("hex");
    const budgetRef = db.doc("meters/demo"); // see FIRESTORE.md
    const ipRef = budgetRef.collection("visitors").doc(ipHash);

    let decision;
    try {
      decision = await db.runTransaction(async (tx) => {
        const [budgetSnap, ipSnap] = await Promise.all([tx.get(budgetRef), tx.get(ipRef)]);
        const b = budgetSnap.data();
        const dayCount = b && b.day === dayKey ? b.count || 0 : 0;
        const ipCount = ipSnap.data()?.count || 0;

        if (ipCount >= perIpDailyCap) {
          return { allow: false, status: 429, code: "per_ip_cap", message: "That's all the demo tries for today. Join the waitlist to get the real thing." };
        }
        if (dayCount >= dailyBudget) {
          return { allow: false, status: 429, code: "daily_budget", message: "The demo is fully booked for today. Please try again tomorrow." };
        }
        tx.set(budgetRef, { day: dayKey, count: dayCount + 1 }, { merge: true });
        tx.set(ipRef, { count: ipCount + 1, expireAt: new Date(now.getTime() + IP_DOC_TTL_MILLIS) });
        return { allow: true, remaining: perIpDailyCap - ipCount - 1 };
      });
    } catch (e) {
      logger.error("Demo cap transaction failed", e);
      sendError(res, 500, "cap_check_failed", "Something went wrong. Please try again in a moment.");
      return;
    }
    if (!decision.allow) {
      logger.info("Demo request blocked", { reason: decision.code });
      sendError(res, decision.status, decision.code, decision.message, { remaining: 0 });
      return;
    }
    const remaining = decision.remaining;

    const openAiRequest = buildOpenAiRequest(request, {
      model: cfg.model || DEFAULT_MODEL,
      reasoningEffort: cfg.reasoningEffort || DEFAULT_REASONING_EFFORT,
    });

    let upstreamBody;
    try {
      const upstream = await fetch(OPENAI_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${OPENAI_KEY.value()}` },
        body: JSON.stringify(openAiRequest),
      });
      const text = await upstream.text();
      if (!upstream.ok) {
        logger.error("Demo OpenAI call failed", { status: upstream.status, body: text.slice(0, 1000) });
        sendError(res, 502, "upstream_error", "The analysis didn't go through. Please try again.", { remaining });
        return;
      }
      upstreamBody = JSON.parse(text);
    } catch (e) {
      logger.error("Demo OpenAI call failed", e);
      sendError(res, 502, "upstream_error", "The analysis didn't go through. Please try again.", { remaining });
      return;
    }

    let outcome;
    try {
      outcome = toDemoResult(upstreamBody);
    } catch (e) {
      logger.error("Demo response unparseable", { message: e.message, status: upstreamBody.status });
      sendError(res, 502, "upstream_error", "The analysis didn't go through. Please try again.", { remaining });
      return;
    }
    if (outcome.notFood) {
      sendError(res, 422, "not_food", outcome.notFood, { remaining });
      return;
    }
    res.status(200).json({ ok: true, remaining, result: outcome.result });
  },
);

// Exported for tests only.
exports._internal = { parseDemoRequest, buildOpenAiRequest, toDemoResult };
