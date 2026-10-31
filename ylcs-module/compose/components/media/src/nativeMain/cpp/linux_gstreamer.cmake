set(RACHEL_GSTREAMER_HEADER_ROOT "${CMAKE_CURRENT_BINARY_DIR}/gstreamer-headers-1.24.2" CACHE PATH "GStreamer and GLib headers for Linux cross compilation")

function(rachel_linux_headers name path sha)
    if(EXISTS "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}.complete")
        file(READ "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}.complete" previous)
        if(previous STREQUAL sha)
            return()
        endif()
    endif()
    set(archive "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}.deb")
    file(MAKE_DIRECTORY "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}")
    file(DOWNLOAD "https://archive.ubuntu.com/ubuntu/${path}" "${archive}" EXPECTED_HASH "SHA256=${sha}" TLS_VERIFY ON TIMEOUT 120 INACTIVITY_TIMEOUT 20)
    file(ARCHIVE_EXTRACT INPUT "${archive}" DESTINATION "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}" PATTERNS "data.tar.*")
    file(GLOB data "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}/data.tar.*")
    list(LENGTH data count)
    if(NOT count EQUAL 1)
        message(FATAL_ERROR "Cannot find the ${name} header archive")
    endif()
    set(patterns "./usr/include/*" "./usr/share/doc/*/copyright")
    if(name STREQUAL "glib")
        list(APPEND patterns "./usr/lib/x86_64-linux-gnu/glib-2.0/include/*")
    endif()
    file(ARCHIVE_EXTRACT INPUT "${data}" DESTINATION "${RACHEL_GSTREAMER_HEADER_ROOT}" PATTERNS ${patterns})
    file(WRITE "${RACHEL_GSTREAMER_HEADER_ROOT}/${name}.complete" "${sha}")
endfunction()

rachel_linux_headers(core "pool/main/g/gstreamer1.0/libgstreamer1.0-dev_1.24.2-1_amd64.deb" "63b2696e00500383ec167869c48a562a8b7e4727a1d50e0e9ecaeab8e260fde4")
rachel_linux_headers(base "pool/main/g/gst-plugins-base1.0/libgstreamer-plugins-base1.0-dev_1.24.2-1_amd64.deb" "cc3541842063e462a2ade5526b1e6ef5a9a4b7214b6cc6b6e4d5ea452b74235f")
rachel_linux_headers(glib "pool/main/g/glib2.0/libglib2.0-dev_2.80.0-6ubuntu1_amd64.deb" "da39bdb0af311c93b8033a5d1027ac6201e1af791541c62022ec020f6c507fba")

target_include_directories(rachel_audio_objects SYSTEM PRIVATE
    "${RACHEL_GSTREAMER_HEADER_ROOT}/usr/include/gstreamer-1.0"
    "${RACHEL_GSTREAMER_HEADER_ROOT}/usr/include/glib-2.0"
    "${RACHEL_GSTREAMER_HEADER_ROOT}/usr/lib/x86_64-linux-gnu/glib-2.0/include"
    "${RACHEL_GSTREAMER_HEADER_ROOT}/usr/lib/x86_64-linux-gnu/gstreamer-1.0/include"
)
