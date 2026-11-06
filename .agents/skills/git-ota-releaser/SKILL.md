---
name: git-ota-releaser
description: Versionador Git y Publicador OTA (Modelo flash_lite). Ejecuta commits semánticos, sincroniza version.json y build.gradle.kts, compila assembleRelease, instala por ADB, publica en GitHub Releases y emite la Ficha Fija de 4 Renglones.
---

# 🏷️ Versionador Git & OTA Release (`git-ota-releaser`)

## 1. Configuración de Modelo y Rol
- **Modelo Obligatorio:** `flash_lite`.
- **Misión:** Cerrar la actividad aplicando control de versiones limpio y, cuando corresponda nueva versión de entrega, ejecutar el protocolo completo de despliegue OTA de `AGENTS.md` (Sección 5).

## 2. Protocolo Inviolable de Versionado y Publicación OTA
1. **Sincronización de Versión (cuando se genera release):**
   - Incrementa `versionCode` y `versionName` en `app-sender/build.gradle.kts` y `app-receiver/build.gradle.kts`.
   - Sincroniza `version.json` (`versionName`, `versionCodeSender`, `versionCodeReceiver`, `releaseTag` y nombres de APKs).
2. **Compilación y Archivo Local (previo paso exitoso del Oráculo QA):**
   - Ejecuta `./gradlew assembleRelease` y copia los APKs generados a `releases/` (`omnicast-sender-vX.Y.Z.apk` y `omnicast-receiver-vX.Y.Z.apk`).
3. **Instalación ADB y Subida Obligatoria a GitHub Releases:**
   - Instala el APK del Sender en el móvil conectado vía `adb install -r`.
   - Ejecuta `python3 scratch/publish_release.py` para publicar `version.json` y ambos APKs en GitHub Releases (habilitando la actualización OTA en la TV Box).
4. **Commit Semántico y Ficha Fija de 4 Renglones:**
   - Crea commit con convención estándar (`feat(modulo): ...`, `fix(modulo): ...`, `chore(modulo): ...`).
   - Entrega la **Ficha Fija de 4 Renglones** definida en la Sección 9 de `AGENTS.md`.
