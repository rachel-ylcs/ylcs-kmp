@echo off

mkdir build
pushd build

rem MSBuild构建出来的库在Release目录下, 所以换构建器, 高版本VS自带Ninja
cmake .. -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_SHARED_LINKER_FLAGS="/NOEXP /NOIMPLIB"
if %errorlevel% neq 0 goto failed
cmake --build . --config Release
if %errorlevel% neq 0 goto failed
echo Build cpp libs for desktop targets complete.
goto eof

:failed
echo Please run this script in VS Developer Command Prompt, do not double click to run it!
pause
goto eof

:eof
popd
