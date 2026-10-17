#pragma once

#include <string_view>
#include <cstdint>

enum class MediaBranchCode : uint8_t {
    DISCARD = 0,
    VOD_HLS_MP4 = 1,
    LIVE_HLS = 2,
    DASH_MPD = 3,
    AUDIO_ONLY = 4,
    IPTV_STREAM = 5,
    LOCAL_MEDIA = 6
};

class StreamClassifier {
public:
    static bool isStaticAsset(std::string_view url);
    static bool isTelemetryOrBeacon(std::string_view url);
    static bool isAdOrGarbage(std::string_view url, float durationSec = 0.0f);
    static bool isLooseChunk(std::string_view url);
    static MediaBranchCode classify(std::string_view url, std::string_view docUrl = "");
};
