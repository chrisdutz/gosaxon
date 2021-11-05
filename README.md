# SoSaxon
Go library allowing XSLT 2.0 transformations in Go by wrapping a GraalVM cross-compiled Saxon-HE engine.

## Prerequisites

In order to compile this module, we need to run the build using a GraalVM version of the Java 8 JDK.

This can be downloaded from (Use the Community edition):
https://www.graalvm.org/downloads/

### Windows

The GraalVM cross-compiler also requires the installation of VisualStudio (Not VSStudio Code) as well as the Windows 10 SDK.

I needed to set the following environment variables for everything to work:

LIB=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\atlmfc\lib\x64;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\lib\x64;C:\Program Files (x86)\Windows Kits\10\Lib\10.0.19041.0\um\x64;C:\Program Files (x86)\Windows Kits\10\Lib\10.0.19041.0\ucrt\x64

INCLUDE=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\atlmfc\include;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\include;C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\ucrt;C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\um;C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\shared

PATH=%PATH%;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja
