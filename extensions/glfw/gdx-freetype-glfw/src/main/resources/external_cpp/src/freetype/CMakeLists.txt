cmake_minimum_required(VERSION 3.10)

project(gdx2d_freetype_bridge C)

set(CMAKE_C_STANDARD 11)

set(GDX_FREETYPE_BRIDGE_DIR "${CMAKE_CURRENT_LIST_DIR}")
if(NOT DEFINED GDX_FREETYPE_SOURCE_DIR OR GDX_FREETYPE_SOURCE_DIR STREQUAL "")
    set(GDX_FREETYPE_SOURCE_DIR "${GDX_FREETYPE_BRIDGE_DIR}/../../thirdparty/freetype")
endif()

option(GDX_FREETYPE_BUILD_SHARED "Build the FreeType bridge as a shared library" OFF)

if(NOT EXISTS "${GDX_FREETYPE_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "FreeType source tree was not found: ${GDX_FREETYPE_SOURCE_DIR}")
endif()

set(FT_DISABLE_ZLIB ON CACHE BOOL "" FORCE)
set(FT_DISABLE_BZIP2 ON CACHE BOOL "" FORCE)
set(FT_DISABLE_PNG ON CACHE BOOL "" FORCE)
set(FT_DISABLE_BROTLI ON CACHE BOOL "" FORCE)
set(FT_DISABLE_HARFBUZZ ON CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
if(GDX_FREETYPE_BUILD_SHARED)
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
endif()

add_subdirectory("${GDX_FREETYPE_SOURCE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/freetype-build" EXCLUDE_FROM_ALL)

if(GDX_FREETYPE_BUILD_SHARED)
    add_library(gdx2d_freetype_bridge SHARED
            "${GDX_FREETYPE_BRIDGE_DIR}/src/gdx2d_freetype_bridge.c"
    )
else()
    add_library(gdx2d_freetype_bridge STATIC
            "${GDX_FREETYPE_BRIDGE_DIR}/src/gdx2d_freetype_bridge.c"
    )
endif()

target_include_directories(gdx2d_freetype_bridge PUBLIC
        "${GDX_FREETYPE_BRIDGE_DIR}/include"
)

target_include_directories(gdx2d_freetype_bridge PRIVATE
        "${GDX_FREETYPE_SOURCE_DIR}/include"
        "${GDX_FREETYPE_SOURCE_DIR}/include/freetype"
)

target_compile_options(gdx2d_freetype_bridge PRIVATE
        $<$<BOOL:${WIN32}>:/FI${GDX_FREETYPE_BRIDGE_DIR}/include/gdx2d_freetype_bridge.h>
        $<$<BOOL:${UNIX}>:-include>
        $<$<BOOL:${UNIX}>:${GDX_FREETYPE_BRIDGE_DIR}/include/gdx2d_freetype_bridge.h>
)

if(TARGET freetype)
    target_link_libraries(gdx2d_freetype_bridge PUBLIC freetype)

    if(UNIX)
        target_compile_definitions(freetype PRIVATE _DEFAULT_SOURCE HAVE_UNISTD_H HAVE_FCNTL_H)
    endif()
endif()

if(GDX_FREETYPE_BUILD_SHARED AND WIN32)
    set_target_properties(gdx2d_freetype_bridge PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS ON)
endif()

install(TARGETS gdx2d_freetype_bridge
        RUNTIME DESTINATION .
        LIBRARY DESTINATION .
        ARCHIVE DESTINATION .
)
