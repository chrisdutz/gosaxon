# SoSaxon
Go library allowing XSLT 2.0 transformations in Go by wrapping a GraalVM cross-compiled Saxon-HE engine.

The library comes with pre-built executors for Windows, Linux and Mac. 
When building locally however only the executor for the current system is been built.

In order to have changes to the executor included in all supported types, the build needs to be run on all supported platforms.

## Prerequisites

In order to compile this module, we need to run the build using a GraalVM version of the Java 8 or 11 JDK.
(Haven't Tested with the version 17, but it should work too)

This can be downloaded from (Use the Community edition):
https://www.graalvm.org/downloads/

After unpacking the GraalVM SDK, you need to manually install the `native-image`.
This is done by simply running the following command in the GraalVM SDKs `bin` directory:

    gu install native-image

### Windows

The GraalVM cross-compiler also requires the installation of VisualStudio (Not VSStudio Code) as well as the Windows 10 SDK.

I needed to set the following environment variables for everything to work:

LIB=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\atlmfc\lib\x64;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\lib\x64;C:\Program Files (x86)\Windows Kits\10\Lib\10.0.19041.0\um\x64;C:\Program Files (x86)\Windows Kits\10\Lib\10.0.19041.0\ucrt\x64

INCLUDE=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\atlmfc\include;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\include;C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\ucrt;C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\um;C:\Program Files (x86)\Windows Kits\10\Include\10.0.19041.0\shared

PATH=%PATH%;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133\bin\Hostx64\x64;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin;C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja

## Usage

Using GoSaxon is actually super easy. 
It should run on any of the supported operating systems without any setup.

Here is an example:

```
package main

import (
	"fmt"
	"github.com/chrisdutz/gosaxon/library/pkg/gosaxon"
)

func main() {
	fmt.Printf("Starting Transformer")

	xml := "<fruits>\n  <apple/>\n  <pair/>\n</fruits>"
	fmt.Printf("Sending XML:\n%s\n", xml)

	xslt := "<xsl:stylesheet version=\"2.0\"\n                xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xs=\"http://www.w3.org/1999/XSL/Transform\">\n    <xsl:output method=\"xml\" indent=\"yes\"/>\n\n    <xsl:template match=\"/fruits\">\n        <apples-and-friends>\n            <xsl:for-each select=\"*\">\n                <xsl:choose>\n                    <xsl:when test=\"name() = 'pair'\">\n                        <apple-like-fruit/>\n                    </xsl:when>\n                    <xsl:otherwise>\n                        <xsl:copy-of select=\".\"/>\n                    </xsl:otherwise>\n                </xsl:choose>\n            </xsl:for-each>\n        </apples-and-friends>\n    </xsl:template>\n\n</xsl:stylesheet>"
	fmt.Printf("Sending XSLT:\n%s\n", xslt)

	result, err := gosaxon.Transform([]byte(xml), []byte(xslt))
	if err != nil {
		fmt.Printf("got error: %s", err.Error())
		return
	}
	fmt.Printf("got result: \n%s", string(result))
}
```

NOTE:
Per default, the library will dump the executor matching the current system to the temp filesystem of your OS and execute if from there.
Especially when running a large number of transformations, this might cause problems, as it has been the case that the OS can't keep up with the cleaning up.

For these cases, we have implemented the option to provide the path to a fixed local executor, which is then shared with all executions.

In order to use this static shared executor, please use the `TransformWithExecutable` function and provide the path to the executor as first argument.
