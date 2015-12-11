cmake_minimum_required(VERSION 2.8)
project(FluxBondingServer)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES main.cpp Channel.cpp Channel.h User.cpp User.h Server.cpp Server.h)

find_package (Threads)

link_libraries(boost_system)
add_executable(FluxBondingServer ${SOURCE_FILES})
target_link_libraries(FluxBondingServer ${CMAKE_THREAD_LIBS_INIT})
