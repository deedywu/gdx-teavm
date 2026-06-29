/*
 * macOS compatibility shim for TeaVM's native GLFW backend.
 *
 * This is backend/runtime support, not FreeType-specific logic:
 * TeaVM-generated C uses char16_t and c16rtomb/mbrtoc16 for string conversion
 * (window titles, file paths, logs, etc). Apple toolchains may not provide a
 * usable <uchar.h> implementation for this target, so GLFW native builds carry
 * this local fallback in backend-glfw.
 */
#pragma once

#if defined(__APPLE__)

#include <stddef.h>
#include <stdint.h>
#include <wchar.h>

typedef uint16_t char16_t;
typedef uint32_t char32_t;

static inline size_t c16rtomb(char* s, char16_t c16, mbstate_t* ps) {
    (void)ps;
    if(s == NULL) {
        return 1;
    }

    uint32_t code = c16;
    if(code < 0x80) {
        s[0] = (char)code;
        return 1;
    }
    if(code < 0x800) {
        s[0] = (char)(0xC0 | (code >> 6));
        s[1] = (char)(0x80 | (code & 0x3F));
        return 2;
    }
    if(code >= 0xD800 && code <= 0xDFFF) {
        s[0] = '?';
        return 1;
    }

    s[0] = (char)(0xE0 | (code >> 12));
    s[1] = (char)(0x80 | ((code >> 6) & 0x3F));
    s[2] = (char)(0x80 | (code & 0x3F));
    return 3;
}

static inline size_t mbrtoc16(char16_t* pc16, const char* s, size_t n, mbstate_t* ps) {
    (void)ps;
    if(s == NULL) {
        return 0;
    }
    if(n == 0) {
        return (size_t)-2;
    }

    unsigned char c0 = (unsigned char)s[0];
    if(c0 == 0) {
        if(pc16) {
            *pc16 = 0;
        }
        return 0;
    }
    if(c0 < 0x80) {
        if(pc16) {
            *pc16 = (char16_t)c0;
        }
        return 1;
    }
    if((c0 & 0xE0) == 0xC0) {
        if(n < 2) {
            return (size_t)-2;
        }
        if(pc16) {
            *pc16 = (char16_t)(((c0 & 0x1F) << 6) | ((unsigned char)s[1] & 0x3F));
        }
        return 2;
    }
    if((c0 & 0xF0) == 0xE0) {
        if(n < 3) {
            return (size_t)-2;
        }
        if(pc16) {
            *pc16 = (char16_t)(((c0 & 0x0F) << 12)
                | (((unsigned char)s[1] & 0x3F) << 6)
                | ((unsigned char)s[2] & 0x3F));
        }
        return 3;
    }
    if((c0 & 0xF8) == 0xF0) {
        if(n < 4) {
            return (size_t)-2;
        }
        if(pc16) {
            *pc16 = (char16_t)'?';
        }
        return 4;
    }
    return (size_t)-1;
}

#elif defined(__has_include_next)

#if __has_include_next(<uchar.h>)
#include_next <uchar.h>
#else

#include <stddef.h>
#include <stdint.h>
#include <wchar.h>

typedef uint16_t char16_t;
typedef uint32_t char32_t;

static inline size_t c16rtomb(char* s, char16_t c16, mbstate_t* ps) {
    (void)ps;
    if(s) {
        *s = (char)c16;
    }
    return 1;
}

static inline size_t mbrtoc16(char16_t* pc16, const char* s, size_t n, mbstate_t* ps) {
    (void)ps;
    if(pc16 && s && n > 0) {
        *pc16 = (char16_t)*s;
    }
    return 1;
}

#endif

#else

#include <stddef.h>
#include <stdint.h>
#include <wchar.h>

typedef uint16_t char16_t;
typedef uint32_t char32_t;

static inline size_t c16rtomb(char* s, char16_t c16, mbstate_t* ps) {
    (void)ps;
    if(s) {
        *s = (char)c16;
    }
    return 1;
}

static inline size_t mbrtoc16(char16_t* pc16, const char* s, size_t n, mbstate_t* ps) {
    (void)ps;
    if(pc16 && s && n > 0) {
        *pc16 = (char16_t)*s;
    }
    return 1;
}

#endif
