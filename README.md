# PersoFile — Simple smartcard interaction example

This repository contains a minimal Java example that demonstrates how to interact with specific smartcards using the javax.smartcardio API.

Key code:
- Main example: [`PersoFile`](src/main/java/PersoFile.java)
- Important methods:
  - [`PersoFile.selectFile`](src/main/java/PersoFile.java)
  - [`PersoFile.readFile`](src/main/java/PersoFile.java)
  - [`PersoFile.readPerso`](src/main/java/PersoFile.java)
  - [`PersoFile.readCert`](src/main/java/PersoFile.java)

Overview
- Scans all available card terminals and connects to any present card.
- Matches card ATRs for known card types (IDEMIA Cosmo 8.1, 8.2, X / Thales IAS Classic v5.2.1).
- Selects applets and files (AID/DF/EF) and reads:
  - Document number (printed as UTF-8)
  - Perso file rows (printed as UTF-8)
  - Auth and Sign certificates (printed as hex)

Prerequisites
- Java 17 (configured in [build.gradle](build.gradle))
- A smartcard reader and a supported smartcard inserted
- Permission to access the smartcard reader on your OS

Build and run
- Use the included wrapper:
  - Unix/macOS: ./gradlew build
  - Windows: gradlew.bat build
- Run the jar:
  - java -jar build/libs/*.jar
- Or run from an IDE using the `PersoFile` main class.

Other files
- Gradle wrapper scripts: [gradlew](gradlew), [gradlew.bat](gradlew.bat)
- Wrapper properties: [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties)
- Jar manifest used by the build: [build/tmp/jar/MANIFEST.MF](build/tmp/jar/MANIFEST.MF)

Notes
- This is an example for demonstration and debugging only. Use caution when accessing or modifying smartcard data.
- If no card is detected, nothing will be printed.

## Support
Official builds are provided through official distribution point [id.ee](https://www.id.ee/en/article/install-id-software/). If you want support, you need to be using official builds. Contact our support via www.id.ee for assistance.

Source code is provided on "as is" terms with no warranty (see license for more information). Do not file Github issues with generic support requests.