#!/bin/sh

mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j 4
echo "Build cpp libs for desktop targets complete."
