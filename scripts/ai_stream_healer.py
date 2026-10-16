#!/usr/bin/env python3
"""
OmniCast Pro - Autonomous AI Stream Healer (Fase 2)
Consulta la telemetría de fallos en Supabase ('UNRESOLVED_STREAM'), invoca a Google Gemini 1.5 Flash
para deducir las cabeceras/embeds correctos y actualiza automáticamente 'rules/live_stream_rules.json'.
"""

import os
import sys
import json
import time
import urllib.request
import urllib.error

SUPABASE_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"

RULES_FILE_PATH = os.path.join(os.path.dirname(__file__), "..", "rules", "live_stream_rules.json")

def fetch_unresolved_reports():
    url = f"{SUPABASE_URL}?event=eq.UNRESOLVED_STREAM&order=created_at.desc&limit=30"
    headers = {
        "apikey": SUPABASE_ANON_KEY,
        "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
        "Content-Type": "application/json"
    }
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = resp.read().decode("utf-8")
            return json.loads(data)
    except Exception as e:
        print(f"Error fetching Supabase logs: {e}")
        return []

def analyze_with_gemini(api_key, failure_log):
    prompt = f"""You are an autonomous streaming engineer AI.
A user app reported a streaming failure:
Message: {failure_log.get('message', '')}
Details: {failure_log.get('details', {})}

Analyze the stream URL and page URL. Determine what CDN pattern, embed origin, and Referer are needed to bypass hotlink protection (e.g., HTTP 403) and allow ExoPlayer to stream it.
Output ONLY a raw JSON object matching this schema without markdown fences:
{{
  "cdnPattern": "string (e.g. tnmr.org or domain.com)",
  "embedOrigin": "https://...",
  "referer": "https://...",
  "skipAdsDuration": 60,
  "secFetchMode": "cors",
  "secFetchDest": "empty",
  "secFetchSite": "cross-site",
  "enabled": true
}}"""

    endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key={api_key}"
    payload = {
        "contents": [{"parts": [{"text": prompt}]}]
    }
    req = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=25) as resp:
            res_json = json.loads(resp.read().decode("utf-8"))
            parts = res_json['candidates'][0]['content']['parts']
            text = ""
            for p in parts:
                if 'text' in p:
                    text += p['text']
            text = text.strip()
            if text.startswith("```"):
                text = text.split("\n", 1)[1].rsplit("```", 1)[0].strip()
            return json.loads(text)
    except Exception as e:
        print(f"Gemini analysis error: {e}")
        return None

def update_rules(new_rule):
    if not os.path.exists(RULES_FILE_PATH):
        print(f"Rules file not found at {RULES_FILE_PATH}")
        return False

    with open(RULES_FILE_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    cdn = new_rule.get("cdnPattern", "").lower()
    if not cdn:
        return False

    existing = [r for r in data.get("rules", []) if r.get("cdnPattern", "").lower() == cdn]
    if existing:
        print(f"Rule for CDN {cdn} already exists. Skipping duplicate.")
        return False

    data["rules"].append(new_rule)
    data["version"] = data.get("version", 1) + 1
    data["updatedAt"] = int(time.time() * 1000)

    with open(RULES_FILE_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"Successfully added new rule for {cdn} (Version: {data['version']})")
    return True

def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("GEMINI_API_KEY environment variable not set. Running in dry-run mode.")
        reports = fetch_unresolved_reports()
        print(f"Fetched {len(reports)} unresolved stream reports from Supabase.")
        return

    reports = fetch_unresolved_reports()
    print(f"Found {len(reports)} raw reports in Supabase.")

    # Deduplicate reports by message content
    seen_messages = set()
    unique_reports = []
    for r in reports:
        msg = r.get("message", "")
        # Extract stream host if possible to group
        host_key = msg.split("Flujo: ")[1].split("/")[2] if "Flujo: " in msg and len(msg.split("Flujo: ")[1].split("/")) > 2 else msg
        if host_key not in seen_messages:
            seen_messages.add(host_key)
            unique_reports.append(r)

    print(f"Deduplicated to {len(unique_reports)} unique stream incidents.")
    for r in unique_reports:
        rule = analyze_with_gemini(api_key, r)
        if rule:
            update_rules(rule)
        time.sleep(1)

if __name__ == "__main__":
    main()
