#pragma once

#include <string>
#include <string_view>

class DeanEdwardsUnpacker {
public:
    /**
     * Unpacks a Dean Edwards packed JavaScript snippet using modern C++20 zero-copy
     * string_view slicing and single-pass token deobfuscation.
     * Returns the deobfuscated JavaScript or an empty string if the input is invalid.
     */
    static std::string unpack(std::string_view html);
};
