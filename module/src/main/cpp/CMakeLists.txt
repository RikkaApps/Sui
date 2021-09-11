cmake_minimum_required(VERSION 3.4.1)

project("sui")

if (NOT DEFINED MODULE_NAME)
    message(FATAL_ERROR "MODULE_NAME is not set")
endif ()

add_definitions(-DRIRU_MODULE)
add_definitions(-DRIRU_MODULE_API_VERSION=${RIRU_MODULE_API_VERSION})
add_definitions(-DRIRU_MODULE_VERSION=${RIRU_MODULE_VERSION})
add_definitions(-DRIRU_MODULE_VERSION_NAME=${RIRU_MODULE_VERSION_NAME})

message("Build type: ${CMAKE_BUILD_TYPE}")

set(CMAKE_CXX_STANDARD 17)

set(LINKER_FLAGS "-ffixed-x18 -Wl,--hash-style=both")
set(C_FLAGS "-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics")

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(C_FLAGS "${C_FLAGS} -O2 -fvisibility=hidden -fvisibility-inlines-hidden")
    set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections")
else ()
    add_definitions(-DDEBUG)
    set(C_FLAGS "${C_FLAGS} -O0")
endif ()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_FLAGS}")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

find_package(cxx REQUIRED CONFIG)
find_package(riru REQUIRED CONFIG)
find_package(nativehelper REQUIRED CONFIG)

include_directories(".")
include_directories("include")
include_directories("util/include")

add_library(util STATIC
        util/misc.cpp
        util/dex_file.cpp
        util/android.cpp
        util/plt.c
        util/selinux.cpp)
target_link_libraries(util cxx::cxx log nativehelper::nativehelper_header_only)

add_library(${MODULE_NAME} SHARED
        core/main.cpp
        core/system_server.cpp
        core/binder_hook.cpp
        core/bridge_service.cpp
        core/manager_process.cpp
        core/settings_process.cpp)
target_link_libraries(${MODULE_NAME} util cxx::cxx log riru::riru nativehelper::nativehelper_header_only)
set_target_properties(${MODULE_NAME} PROPERTIES LINK_FLAGS_RELEASE -s)

add_executable(libmain.so main/main.cpp)
target_link_libraries(libmain.so util cxx::cxx log riru::riru nativehelper::nativehelper_header_only)
set_target_properties(libmain.so PROPERTIES LINK_FLAGS_RELEASE -s)

add_executable(libadbd_wrapper.so
        adbd/adbd_wrapper_main.cpp)
target_link_libraries(libadbd_wrapper.so cxx::cxx log)
set_target_properties(libadbd_wrapper.so PROPERTIES LINK_FLAGS_RELEASE -s)

add_library(adbd_preload SHARED
        adbd/adbd_preload.cpp)
target_link_libraries(adbd_preload cxx::cxx log)
set_target_properties(adbd_preload PROPERTIES LINK_FLAGS_RELEASE -s)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET ${MODULE_NAME} POST_BUILD
            COMMAND ${CMAKE_STRIP} --strip-all --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/lib${MODULE_NAME}.so")

    add_custom_command(TARGET libmain.so POST_BUILD
            COMMAND ${CMAKE_STRIP} --strip-all --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libmain.so")

    add_custom_command(TARGET libadbd_wrapper.so POST_BUILD
            COMMAND ${CMAKE_STRIP} --strip-all --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libadbd_wrapper.so")

    add_custom_command(TARGET adbd_preload POST_BUILD
            COMMAND ${CMAKE_STRIP} --strip-all --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libadbd_preload.so")
endif ()
