#pragma once

#include <string_view>

class AdPatternMatcher {
public:
    /**
     * Evalúa en microsegundos (< 5 µs) si una URL pertenece a redes de publicidad,
     * popups, trackers o secuestradores mediante búsqueda multi-patrón Aho-Corasick.
     * Respeta incondicionalmente las Leyes 4 y 6 de AGENTS.md (inmunidad a plataformas oficiales).
     */
    static bool isAdOrTracker(std::string_view url, std::string_view pageUrl = "");
};
