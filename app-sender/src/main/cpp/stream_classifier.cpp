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

class JniUtfStringView {
public:
    JniUtfStringView(JNIEnv* env, jstring jstr) noexcept
        : env_(env), jstr_(jstr) {
        if (env_ != nullptr && jstr_ != nullptr) {
            chars_ = env_->GetStringUTFChars(jstr_, nullptr);
            if (chars_ != nullptr) {
                view_ = std::string_view(chars_);
            }
        }
    }

    ~JniUtfStringView() {
        if (env_ != nullptr && jstr_ != nullptr && chars_ != nullptr) {
            env_->ReleaseStringUTFChars(jstr_, chars_);
        }
    }

    JniUtfStringView(const JniUtfStringView&) = delete;
    JniUtfStringView& operator=(const JniUtfStringView&) = delete;
    JniUtfStringView(JniUtfStringView&&) = delete;
    JniUtfStringView& operator=(JniUtfStringView&&) = delete;

    [[nodiscard]] bool isValid() const noexcept { return chars_ != nullptr; }
    [[nodiscard]] std::string_view view() const noexcept { return view_; }
    [[nodiscard]] const char* c_str() const noexcept { return chars_; }
    operator std::string_view() const noexcept { return view_; }

private:
    JNIEnv* env_{nullptr};
    jstring jstr_{nullptr};
    const char* chars_{nullptr};
    std::string_view view_{};
};

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

bool StreamClassifier::isAdOrGarbage(std::string_view url, float /* durationSec */) {
    if (url.empty() || url.starts_with("blob:") || url.starts_with("data:") || url.starts_with("about:")) {
        return true;
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

    if (containsIgnoreCase(url, "live") || containsIgnoreCase(url, "stream") || containsIgnoreCase(url, "chunklist_live") ||
        containsIgnoreCase(url, "linear") || containsIgnoreCase(url, "lin-") || containsIgnoreCase(url, "envivo") ||
        containsIgnoreCase(url, "directo")) {
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
    JniUtfStringView url(env, jUrl);
    if (!url.isValid()) return static_cast<jint>(MediaBranchCode::DISCARD);

    JniUtfStringView docUrl(env, jDocUrl);
    MediaBranchCode result = StreamClassifier::classify(url.view(), docUrl.view());
    return static_cast<jint>(result);
}

JNIEXPORT jboolean JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_isDiscardCandidate(
    JNIEnv* env, jobject /* this */, jstring jUrl, jfloat durationSec) {
    if (!jUrl) return JNI_TRUE;
    JniUtfStringView url(env, jUrl);
    if (!url.isValid()) return JNI_TRUE;

    bool discard = StreamClassifier::isStaticAsset(url.view()) ||
                   StreamClassifier::isTelemetryOrBeacon(url.view()) ||
                   StreamClassifier::isAdOrGarbage(url.view(), durationSec) ||
                   StreamClassifier::isLooseChunk(url.view());

    return discard ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_isTelemetryOrBeacon(
    JNIEnv* env, jobject /* this */, jstring jUrl) {
    if (!jUrl) return JNI_FALSE;
    JniUtfStringView url(env, jUrl);
    if (!url.isValid()) return JNI_FALSE;

    bool result = StreamClassifier::isTelemetryOrBeacon(url.view());
    return result ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
