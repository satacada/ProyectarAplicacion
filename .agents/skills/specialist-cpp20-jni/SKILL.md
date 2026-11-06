---
name: specialist-cpp20-jni
description: Especialista en Motor Nativo C++20 JNI Zero-Allocation (Modelo flash). Audita y mantiene stream_classifier.cpp, dean_edwards_unpacker.cpp y ad_pattern_matcher.cpp bajo estándar C++20 (std::string_view, std::span, RAII, cero malloc/new).
---

# ⚡ Especialista en Motor Nativo C++20 JNI (`specialist-cpp20-jni`)

## 1. Configuración de Modelo y Ámbito
- **Modelo Obligatorio:** `flash`.
- **Ámbito Exacto:**
  - `app-sender/src/main/cpp/CMakeLists.txt`
  - `app-sender/src/main/cpp/stream_classifier.h` y `stream_classifier.cpp`
  - `app-sender/src/main/cpp/dean_edwards_unpacker.h` y `dean_edwards_unpacker.cpp`
  - `app-sender/src/main/cpp/ad_pattern_matcher.h` y `ad_pattern_matcher.cpp`
  - `app-sender/src/main/kotlin/com/omnicast/sender/nativeclassifier/`

## 2. Estándar C++20 Moderno Obligatorio (`AGENTS.md` Sección 3)
1. **Zero-Allocation:**
   - Uso obligatorio de `std::string_view` y `std::span` para operaciones de lectura, clasificación y parseo, prohibiendo copias innecesarias de `std::string`.
2. **Gestión de Memoria Segura (RAII Puro):**
   - Prohibido el uso de `malloc`, `free`, `new` o `delete` manual. Toda memoria debe residir en el stack o en contenedores RAII (`std::vector`, `std::array`, `std::unique_ptr`).
3. **Puntualidad en la Frontera JNI:**
   - Prohibido invocar métodos JNI dentro de bucles cerrados de red o renderizado; transferir bloques completos con retorno directo en microsegundos y validar con `DeanEdwardsUnpackerTest` y `AhoCorasickAdMatcherTest`.
