# ZipInputStreamPatch64

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chandler/ZipInputStreamPatch64/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chandler/ZipInputStreamPatch64)

A bug in Java's ``ZipInputStream`` causes it to choke on certain ZIP records under the following circumstances:
 - File size and compressed size are under 4 GiB
 - The local file header is ZIP64-encoded
 - The data descriptor is present

See [https://chandler.io/software/2022/12/09/Hotfixing-ZipInputStream.html](https://chandler.io/software/2022/12/09/Hotfixing-ZipInputStream.html) for a complete description of the bug.

This repository contains a reflection-based [patch](https://github.com/cjgriscom/) that reliably detects the bug and advances the stream to avoid interruption.

The patch handles exceptions similar to this one:
```
java.util.zip.ZipException: invalid entry size (expected 0 but got 199 bytes)
	at java.util.zip.ZipInputStream.readEnd(ZipInputStream.java:384)
	at java.util.zip.ZipInputStream.read(ZipInputStream.java:196)
	at java.util.zip.InflaterInputStream.read(InflaterInputStream.java:122)
```

This ``ZipInputStream`` bug is known to affect the following JDKs:
 - JDK 1.8.0_341
 - OpenJDK 11.0.17
 - OpenJDK 17.0.2
 - OpenJDK 19.0.1
 
This patch is tested and working on JDK 8 and 11.  Currently it fails on newer JDKs due to reflection issues.

## License

The code for this patch is dual-licensed under either the MIT license or public domain (CC0), at the user's discretion.

## Maven

The patch is available through Maven Central:

```xml
<dependency>
    <groupId>io.chandler</groupId>
    <artifactId>ZipInputStreamPatch64</artifactId>
    <version>1.0.0</version>
</dependency>
```
