#!/usr/bin/env python3
import json
import os
import sys
import urllib.request
import urllib.error

GITHUB_REPO = "satacada/ProyectarAplicacion"
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def get_token():
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        return token.strip()
    lp_path = os.path.join(BASE_DIR, "local.properties")
    if os.path.exists(lp_path):
        with open(lp_path, "r") as f:
            for line in f:
                if line.startswith("github.token="):
                    return line.split("=", 1)[1].strip()
    return ""

GITHUB_TOKEN = get_token()

def api_request(url, method="GET", data=None, content_type="application/json"):
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "OmniCast-Release-Script"
    }
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read()
            if res_body:
                return json.loads(res_body.decode("utf-8"))
            return None
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode("utf-8")
        print(f"HTTP {e.code} Error en {url}: {error_msg}")
        return None

def upload_asset(upload_url_template, file_path, content_type="application/octet-stream"):
    file_name = os.path.basename(file_path)
    clean_upload_url = upload_url_template.split("{")[0] + f"?name={file_name}"
    print(f"📤 Subiendo asset: {file_name} ({os.path.getsize(file_path)} bytes)...")
    with open(file_path, "rb") as f:
        file_bytes = f.read()
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json",
        "Content-Type": content_type,
        "User-Agent": "OmniCast-Release-Script"
    }
    req = urllib.request.Request(clean_upload_url, data=file_bytes, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"✅ Asset subido con éxito: {file_name}")
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print(f"❌ Error subiendo {file_name}: {e.read().decode('utf-8')}")
        return None

def main():
    version_file = os.path.join(BASE_DIR, "version.json")
    with open(version_file, "r") as f:
        vinfo = json.load(f)

    tag_name = vinfo["releaseTag"]
    version_name = vinfo["versionName"]
    print(f"🚀 Iniciando publicación de Release {tag_name} en {GITHUB_REPO}...")

    # 1. Obtener y eliminar versiones anteriores en GitHub para no saturar espacio
    releases = api_request(f"https://api.github.com/repos/{GITHUB_REPO}/releases")
    if releases:
        for r in releases:
            rel_id = r["id"]
            rel_tag = r["tag_name"]
            print(f"🗑️ Eliminando release remoto previo en GitHub: {rel_tag} (ID: {rel_id})...")
            api_request(f"https://api.github.com/repos/{GITHUB_REPO}/releases/{rel_id}", method="DELETE")
            # Eliminar el tag remoto asociado
            api_request(f"https://api.github.com/repos/{GITHUB_REPO}/git/refs/tags/{rel_tag}", method="DELETE")

    # 2. Crear nuevo release
    release_payload = json.dumps({
        "tag_name": tag_name,
        "name": f"OmniCast {tag_name}",
        "body": f"Versión oficial {tag_name}\n\nChangelog: {vinfo.get('changelog', '')}",
        "draft": False,
        "prerelease": False
    }).encode("utf-8")

    new_release = api_request(f"https://api.github.com/repos/{GITHUB_REPO}/releases", method="POST", data=release_payload)
    if not new_release:
        print("❌ Error creando el nuevo release en GitHub.")
        sys.exit(1)

    upload_url = new_release["upload_url"]
    print(f"✨ Release {tag_name} creado. Subiendo APKs...")

    # 3. Subir APKs y version.json
    sender_apk = os.path.join(BASE_DIR, "releases", f"omnicast-sender-{tag_name}.apk")
    receiver_apk = os.path.join(BASE_DIR, "releases", f"omnicast-receiver-{tag_name}.apk")

    if os.path.exists(sender_apk):
        upload_asset(upload_url, sender_apk, content_type="application/vnd.android.package-archive")
    else:
        print(f"⚠️ No se encontró {sender_apk}")

    if os.path.exists(receiver_apk):
        upload_asset(upload_url, receiver_apk, content_type="application/vnd.android.package-archive")
    else:
        print(f"⚠️ No se encontró {receiver_apk}")

    upload_asset(upload_url, version_file, content_type="application/json")
    print(f"🎉 ¡Publicación completada exitosamente! Solo {tag_name} reside ahora en GitHub Releases.")

if __name__ == "__main__":
    main()
