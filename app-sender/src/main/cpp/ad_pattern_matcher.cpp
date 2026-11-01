#include "ad_pattern_matcher.h"
#include <jni.h>
#include <vector>
#include <array>
#include <queue>
#include <cctype>
#include <cstdint>
#include <string_view>

namespace {

// Whitelist inviolable: plataformas oficiales y CDNs protegidas (Leyes 4 y 6 de AGENTS.md)
constexpr std::string_view INVIOLABLE_WHITELIST[] = {
    "googlevideo.com",
    "ytimg.com",
    "youtube.com",
    "youtu.be",
    "dmcdn.net",
    "static1.dmcdn.net",
    "dailymotion.com",
    "dropcdn.io",
    "dr0pstream.com",
    "fastream.to",
    "ok.ru",
    "mycdn.me",
    "okcdn.ru",
    "canela.tv",
    "challenges.cloudflare.com",
    "turnstile"
};

// Firmas de redes de publicidad, popups, secuestradores y trackers para búsqueda multi-patrón
constexpr std::string_view AD_PATTERNS[] = {
    "doubleclick",
    "googleads",
    "googlesyndication",
    "adservice.google",
    "adnxs",
    "popads",
    "propeller",
    "exoclick",
    "onclick",
    "monetag",
    "adsterra",
    "adsco.re",
    "imasdk",
    "luugy",
    "deloton",
    "dolohen",
    "adkeeper",
    "trafficjunky",
    "quickcore",
    "preroll",
    "midroll",
    "commercial",
    "/vast",
    "vast.",
    "spotx",
    "popunder",
    "sponsor",
    "/vmap/",
    "cat-bolt",
    "bet365",
    "1xbet",
    "innodesdevering",
    "whocares",
    "pemsrv",
    "gdtcdn",
    "holdbitter",
    "subduepaler",
    "clickadu",
    "hilltopads",
    "frs2c",
    "banner-ads",
    "player-banner",
    "ad-delivery",
    "stub.mp4",
    "dummy.mp4",
    "blank.mp4",
    "ad.mp4",
    "ad_wrapper"
};

struct TrieNode {
    std::array<int32_t, 128> next;
    int32_t fail{0};
    bool isMatch{false};

    TrieNode() {
        next.fill(0);
    }
};

class AhoCorasickAutomaton {
private:
    std::vector<TrieNode> nodes;

public:
    AhoCorasickAutomaton() {
        nodes.emplace_back(); // Raíz en índice 0
        for (const auto& pat : AD_PATTERNS) {
            insert(pat);
        }
        buildFailures();
    }

    void insert(std::string_view pat) {
        int32_t curr = 0;
        for (char c : pat) {
            unsigned char uc = static_cast<unsigned char>(std::tolower(static_cast<unsigned char>(c)));
            if (uc >= 128) continue;
            if (nodes[curr].next[uc] == 0) {
                int32_t nextId = static_cast<int32_t>(nodes.size());
                nodes.emplace_back();
                nodes[curr].next[uc] = nextId;
            }
            curr = nodes[curr].next[uc];
        }
        nodes[curr].isMatch = true;
    }

    void buildFailures() {
        std::queue<int32_t> q;
        for (int c = 0; c < 128; ++c) {
            int32_t next = nodes[0].next[c];
            if (next != 0) {
                nodes[next].fail = 0;
                q.push(next);
            }
        }

        while (!q.empty()) {
            int32_t curr = q.front();
            q.pop();

            if (nodes[nodes[curr].fail].isMatch) {
                nodes[curr].isMatch = true;
            }

            for (int c = 0; c < 128; ++c) {
                int32_t next = nodes[curr].next[c];
                if (next != 0) {
                    int32_t f = nodes[curr].fail;
                    while (f != 0 && nodes[f].next[c] == 0) {
                        f = nodes[f].fail;
                    }
                    nodes[next].fail = nodes[f].next[c];
                    q.push(next);
                } else {
                    nodes[curr].next[c] = nodes[nodes[curr].fail].next[c];
                }
            }
        }
    }

    bool matches(std::string_view text) const {
        int32_t curr = 0;
        for (char c : text) {
            unsigned char uc = static_cast<unsigned char>(std::tolower(static_cast<unsigned char>(c)));
            if (uc >= 128) {
                curr = 0;
                continue;
            }
            curr = nodes[curr].next[uc];
            if (nodes[curr].isMatch) {
                return true;
            }
        }
        return false;
    }
};

const AhoCorasickAutomaton& getAutomaton() {
    static const AhoCorasickAutomaton automaton;
    return automaton;
}

inline bool containsIgnoreCase(std::string_view str, std::string_view sub) {
    if (sub.empty() || sub.size() > str.size()) return false;
    for (size_t i = 0; i <= str.size() - sub.size(); ++i) {
        bool match = true;
        for (size_t j = 0; j < sub.size(); ++j) {
            if (std::tolower(static_cast<unsigned char>(str[i + j])) !=
                std::tolower(static_cast<unsigned char>(sub[j]))) {
                match = false;
                break;
            }
        }
        if (match) return true;
    }
    return false;
}

} // namespace

bool AdPatternMatcher::isAdOrTracker(std::string_view url, std::string_view /* pageUrl */) {
    if (url.empty()) return false;

    // 1. Blindaje Inviolable de Plataformas Oficiales (Leyes 4 y 6)
    for (const auto& w : INVIOLABLE_WHITELIST) {
        if (containsIgnoreCase(url, w)) {
            return false;
        }
    }

    // 2. Búsqueda multi-patrón en un solo barrido O(N)
    return getAutomaton().matches(url);
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_isAdOrTrackerUrl(
    JNIEnv* env, jobject /* this */, jstring jUrl, jstring jPageUrl) {
    if (!jUrl) return JNI_FALSE;
    const char* cUrl = env->GetStringUTFChars(jUrl, nullptr);
    if (!cUrl) return JNI_FALSE;

    const char* cPageUrl = jPageUrl ? env->GetStringUTFChars(jPageUrl, nullptr) : nullptr;

    std::string_view urlView(cUrl);
    std::string_view pageView(cPageUrl ? cPageUrl : "");

    bool blocked = AdPatternMatcher::isAdOrTracker(urlView, pageView);

    env->ReleaseStringUTFChars(jUrl, cUrl);
    if (jPageUrl && cPageUrl) env->ReleaseStringUTFChars(jPageUrl, cPageUrl);

    return blocked ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
