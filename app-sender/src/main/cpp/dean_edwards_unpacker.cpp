#include "dean_edwards_unpacker.h"
#include <jni.h>
#include <vector>
#include <cstdint>
#include <cctype>
#include <algorithm>
#include <string_view>

namespace {

inline int charToBase(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'z') return c - 'a' + 10;
    if (c >= 'A' && c <= 'Z') return c - 'A' + 36;
    return -1;
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

std::string DeanEdwardsUnpacker::unpack(std::string_view html) {
    if (html.empty()) return "";

    // 1. Locate .split('|') or .split("|")
    auto pos = html.find(".split('|')");
    if (pos == std::string_view::npos) {
        pos = html.find(".split(\"|\")");
    }
    if (pos == std::string_view::npos || pos == 0) return "";

    // 2. Locate eval(function(
    auto start = html.rfind("eval(function(", pos);
    if (start == std::string_view::npos) return "";

    auto callStart = html.find("}(", start);
    if (callStart == std::string_view::npos || callStart > pos) return "";
    callStart += 2;

    // 3. Find closing quote of kStr before pos
    size_t qEnd = pos;
    while (qEnd > callStart && (html[qEnd - 1] == ' ' || html[qEnd - 1] == '\t')) {
        qEnd--;
    }
    if (qEnd == callStart) return "";
    char quoteChar = html[qEnd - 1];
    if (quoteChar != '\'' && quoteChar != '"') return "";
    qEnd--; // index of closing quote

    auto kStart = html.rfind(quoteChar, qEnd - 1);
    if (kStart == std::string_view::npos || kStart < callStart) return "";

    std::string_view kStr = html.substr(kStart + 1, qEnd - (kStart + 1));

    // 4. Backward scan before kStart to extract c and a
    size_t idx = kStart;
    while (idx > callStart && (html[idx - 1] == ' ' || html[idx - 1] == ',' ||
                               html[idx - 1] == '\t' || html[idx - 1] == '\r' || html[idx - 1] == '\n')) {
        idx--;
    }
    size_t cEnd = idx;
    while (idx > callStart && (html[idx - 1] >= '0' && html[idx - 1] <= '9')) {
        idx--;
    }
    size_t cStart = idx;
    if (cStart >= cEnd) return "";

    uint32_t c = 0;
    for (size_t i = cStart; i < cEnd; ++i) {
        c = c * 10 + (html[i] - '0');
    }

    while (idx > callStart && (html[idx - 1] == ' ' || html[idx - 1] == ',' || html[idx - 1] == '\t')) {
        idx--;
    }
    size_t aEnd = idx;
    while (idx > callStart && (html[idx - 1] >= '0' && html[idx - 1] <= '9')) {
        idx--;
    }
    size_t aStart = idx;
    if (aStart >= aEnd) return "";

    uint32_t a = 0;
    for (size_t i = aStart; i < aEnd; ++i) {
        a = a * 10 + (html[i] - '0');
    }
    if (a == 0) a = 36;

    while (idx > callStart && html[idx - 1] != ',') {
        idx--;
    }
    if (idx <= callStart) return "";
    idx--; // skip ','

    // 5. pView is between callStart and idx
    std::string_view pView = html.substr(callStart, idx - callStart);
    while (!pView.empty() && (pView.front() == ' ' || pView.front() == '\t' ||
                              pView.front() == '\r' || pView.front() == '\n')) {
        pView.remove_prefix(1);
    }
    while (!pView.empty() && (pView.back() == ' ' || pView.back() == '\t' ||
                              pView.back() == '\r' || pView.back() == '\n')) {
        pView.remove_suffix(1);
    }
    if (pView.size() >= 2) {
        if ((pView.front() == '\'' && pView.back() == '\'') ||
            (pView.front() == '"' && pView.back() == '"')) {
            pView.remove_prefix(1);
            pView.remove_suffix(1);
        }
    }

    // 6. Split kStr by '|' into views (Zero-Copy)
    std::vector<std::string_view> kList;
    size_t safeReserve = std::min(static_cast<size_t>(c + 16), kStr.size() + 1);
    kList.reserve(safeReserve);
    size_t segStart = 0;
    for (size_t i = 0; i <= kStr.size(); ++i) {
        if (i == kStr.size() || kStr[i] == '|') {
            kList.emplace_back(kStr.data() + segStart, i - segStart);
            segStart = i + 1;
        }
    }

    // 7. Single-pass unpack of pView into pre-allocated string
    std::string out;
    out.reserve(pView.size() * 2 + 1024);

    size_t pLen = pView.size();
    for (size_t i = 0; i < pLen; ) {
        // Handle escaped quotes \' or \"
        if (pView[i] == '\\' && i + 1 < pLen && (pView[i + 1] == '\'' || pView[i + 1] == '"')) {
            out.push_back(pView[i + 1]);
            i += 2;
            continue;
        }

        // Check if starting a word token [a-zA-Z0-9_]
        char ch = pView[i];
        bool isWord = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z') ||
                      (ch >= 'A' && ch <= 'Z') || ch == '_';
        if (!isWord) {
            out.push_back(ch);
            i++;
            continue;
        }

        // Extract whole token and parse in base a
        size_t tokStart = i;
        bool validRadix = true;
        uint32_t val = 0;
        while (i < pLen) {
            char tc = pView[i];
            bool tcIsWord = (tc >= '0' && tc <= '9') || (tc >= 'a' && tc <= 'z') ||
                            (tc >= 'A' && tc <= 'Z') || tc == '_';
            if (!tcIsWord) break;

            if (validRadix) {
                int d = charToBase(tc);
                if (d >= 0 && static_cast<uint32_t>(d) < a) {
                    uint64_t nextVal = static_cast<uint64_t>(val) * a + static_cast<uint32_t>(d);
                    if (nextVal >= c) {
                        validRadix = false;
                    } else {
                        val = static_cast<uint32_t>(nextVal);
                    }
                } else {
                    validRadix = false;
                }
            }
            i++;
        }

        if (validRadix && val < c && val < kList.size() && !kList[val].empty()) {
            out.append(kList[val]);
        } else {
            out.append(pView.substr(tokStart, i - tokStart));
        }
    }

    return out;
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_omnicast_sender_nativeclassifier_NativeStreamClassifier_unpackDeanEdwardsNative(
    JNIEnv* env, jobject /* this */, jstring jHtml) {
    if (!jHtml) return nullptr;
    JniUtfStringView html(env, jHtml);
    if (!html.isValid()) return nullptr;

    std::string unpacked = DeanEdwardsUnpacker::unpack(html.view());
    if (unpacked.empty()) return nullptr;
    return env->NewStringUTF(unpacked.c_str());
}

} // extern "C"
