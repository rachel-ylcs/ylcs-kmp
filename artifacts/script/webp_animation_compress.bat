@echo off
setlocal enabledelayedexpansion

set i=input.webp
set o=output.webp
set q=70
set f=15
set s=128:128

:loop
if "%~1"=="" goto execute
if "%~1"=="-q" (set q=%~2 & shift & shift & goto loop)
if "%~1"=="-f" (set f=%~2 & shift & shift & goto loop)
if "%~1"=="-s" (set s=%~2 & shift & shift & goto loop)
if "%~1"=="-o" (set o=%~2 & shift & shift & goto loop)

if not defined input_set (
    set i=%~1
    set input_set=1
)
shift
goto loop

:execute
ffmpeg -i "%i%" -vcodec libwebp -vf fps=%f% -lossless 0 -compression_level 3 -q:v %q% -loop 0 -preset default -an -s %s% "%o%"

endlocal