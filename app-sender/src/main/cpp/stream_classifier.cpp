#include "stream_classifier.h"
#include <jni.h>
#include <string>
#include <string_view>
#include <algorithm>
#include <array>
#include <android/log.h>

#define LOG_TAG "NativeStreamClassifier"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace {

constexpr std::string_view STATIC_EXTS[] = {
    ".html", ".htm", ".css", ".js", ".json", ".xml",
    ".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".ico",
    ".woff", ".woff2", ".ttf", ".eot", ".otf", ".map"
};

constexpr std::string_view TELEMETRY_KEYWORDS[] = {
    "videostat", "statnew", "telemetry", "analytics", "beacon",
    "heartbeat", "logger", "tracking", "/ping", "/event",
    "report", "collect", "metric", "counter", "geo.dailymotion.com"
};

constexpr std::string_view AD_KEYWORDS[] = {
    "preroll", "midroll", "commercial", "/vast", "vast.",
    "stub.mp4", "dummy.mp4", "blank.mp4", "ad.mp4", "ad-delivery",
    "imasdk", "cat-bolt", "spotx", "doubleclick", "/ads/", "/ad/",
    "popunder", "sponsor", "dmxleo.dailymotion.com", "/vmap/"
};

bool containsIgnoreCase(std::string_view str, std::string_view sub) {
    if (sub.empty() || sub.size() > str.size()) return false;
    auto it = std::search(
        str.begin(), str.end(),
        sub.begin(), sub.end(),
        [](char ch1, char ch2) {
            return std::tolower(static_cast<unsigned char>(ch1)) ==
                   std::tolower(static_cast<unsigned char>(ch2));
        }
    );
    return it != str.end();
}

bool endsWithIgnoreCase(std::string_view str, std::string_view suffix) {
    if (suffix.size() > str.size()) return false;
    auto strEnd = str.substr(str.size() - suffix.size());
    return std::equal(
        strEnd.begin(), strEnd.end(),
        suffix.begin(),
        [](char a, char b) {
            return std::tolower(static_cast<unsigned char>(a)) ==
                   std::tolower(static_cast<unsigned char>(b));
        }
    );
}

std::string_view getPathOnly(std::string_view url) {
    auto q = url.find('?');
    if (q != std::string_view::npos) {
        return url.substr(0, q);
    }
    return url;
}

} // namespace

bool StreamClassifier::isStaticAsset(std::string_view url) {
    auto path = getPathOnly(url);
    for (const auto& ext : STATIC_EXTS) {
        if (endsWithIgnoreCase(path, ext)) return true;
    }
    return false;
}

bool StreamClassifier::isTelemetryOrBeacon(std::string_view url) {
    for (const auto& kw : TELEMETRY_KEYWORDS) {
        if (containsIgnoreCase(url, kw)) return true;
    }
    return false;
}

bool StreamClassifier::isAdOrGarbage(std::string_view url, float durationSec) {
    if (url.empty() || url.starts_with("blob:") || url.starts_with("data:") || url.starts_with("about:")) {
        return true;
    }
    if (durationSec > 0.0f && durationSec < 45.0f) {
        for (const auto& kw : AD_KEYWORDS) {
            if (containsIgnoreCase(url, kw)) return true;
        }
    }
    for (const auto& kw : AD_KEYWORDS) {
        if (containsIgnoreCase(url, kw)) return true;
    }
    return false;
}

bool StreamClassifier::isLooseChunk(std::string_view url) {
    if (containsIgnoreCase(url, ".ts?") || (endsWithIgnoreCase(getPathOnly(url), ".ts") && !containsIgnoreCase(url, ".m3u8"))) {
        return true;
    }
    if (containsIgnoreCase(url, ".m4s") || containsIgnoreCase(url, "init.mp4") ||
        containsIgnoreCase(url, "/init-") || containsIgnoreCase(url, "-init.")) {
        return true;
    }
    return false;
}

MediaBranchCode StreamClassifier::classify(std::string_view url, std::string_view docUrl) {
    if (url.empty()) return MediaBranchCode::DISCARD;
    if (isStaticAsset(url)) return MediaBranchCode::DISCARD;
    if (isTelemetryOrBeacon(url)) return MediaBranchCode::DISCARD;
    if (isAdOrGarbage(url)) return MediaBranchCode::DISCARD;
    if (isLooseChunk(url)) return MediaBranchCode::DISCARD;

    if (containsIgnoreCase(url, ":8089") || containsIgnoreCase(url, "localhost:8089")) {
        return MediaBranchCode::LOCAL_MEDIA;
    }

    if (containsIgnoreCase(url, ".mpd") || containsIgnoreCase(url, "/dash/") || containsIgnoreCase(url, "format=mpd")) {
        return MediaBranchCode::DASH_MPD;
    }

    if (containsIgnoreCase(url, ".mp3") || containsIgnoreCase(url, ".aac") ||
        containsIgnoreCase(url, ".ogg") || containsIgnoreCase(url, ".flac") ||
        containsIgnoreCase(url, ".wav") || containsIgnoreCase(url, "/icecast") ||
        containsIgnoreCase(url, "/shoutcast")) {
        return MediaBranchCode::AUDIO_ONLY;
    }

    if (containsIgnoreCase(url, "/iptv") || containsIgnoreCase(url, "playlist.m3u") ||
        (containsIgnoreCase(url, "/live/") && containsIgnoreCase(url, ".ts"))) {
        return MediaBranchCode::IPTV_STREAM;
    }

    if (containsIgnoreCase(url, "live") || containsIgnoreCase(url, "stream") || containsIgnoreCase(url, "chunklist_live")) {
        if (containsIgnoreCase(url, ".m3u8") || containsIgnoreCase(url, "/hls")) {
            return MediaBranchCode::LIVE_HLS;
        }
    }

    if (containsIgnoreCase(url, ".m3u8") || containsIgnoreCase(url, "/hls") ||
        containsIgnoreCase(url, ".mp4") || containsIgnoreCase(url, ".webm") ||
        containsIgnoreCase(url, ".mkv") || containsIgnoreCase(url, "/video/") ||
        containsIgnoreCase(url, "dropcdn.io") || containsIgnoreCase(url, "dr0pstream.com") ||
        containsIgnoreCase(url, "fastream.to")) {
        return MediaBranchCode::VOD_HLS_MP4;
    }

    return MediaBranchCode::DISCARD;
}

extern "C" {

JNIEXPORT jint JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_classifyUrl(
    JNIEnv* env, jobject /* this */, jstring jUrl, jstring jDocUrl) {
    if (!jUrl) return static_cast<jint>(MediaBranchCode::DISCARD);
    const char* cUrl = env->GetStringUTFChars(jUrl, nullptr);
    const char* cDocUrl = jDocUrl ? env->GetStringUTFChars(jDocUrl, nullptr) : nullptr;

    std::string_view urlView(cUrl);
    std::string_view docUrlView(cDocUrl ? cDocUrl : "");

    MediaBranchCode result = StreamClassifier::classify(urlView, docUrlView);

    env->ReleaseStringUTFChars(jUrl, cUrl);
    if (jDocUrl && cDocUrl) env->ReleaseStringUTFChars(jDocUrl, cDocUrl);

    return static_cast<jint>(result);
}

JNIEXPORT jboolean JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_isDiscardCandidate(
    JNIEnv* env, jobject /* this */, jstring jUrl, jfloat durationSec) {
    if (!jUrl) return JNI_TRUE;
    const char* cUrl = env->GetStringUTFChars(jUrl, nullptr);
    std::string_view urlView(cUrl);

    bool discard = StreamClassifier::isStaticAsset(urlView) ||
                   StreamClassifier::isTelemetryOrBeacon(urlView) ||
                   StreamClassifier::isAdOrGarbage(urlView, durationSec) ||
                   StreamClassifier::isLooseChunk(urlView);

    env->ReleaseStringUTFChars(jUrl, cUrl);
    return discard ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_isTelemetryOrBeacon(
    JNIEnv* env, jobject /* this */, jstring jUrl) {
    if (!jUrl) return JNI_FALSE;
    const char* cUrl = env->GetStringUTFChars(jUrl, nullptr);
    std::string_view urlView(cUrl);

    bool result = StreamClassifier::isTelemetryOrBeacon(urlView);

    env->ReleaseStringUTFChars(jUrl, cUrl);
    return result ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
