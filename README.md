# EssenceSMP Solar — recovered project package

This package was reconstructed from the JAR you supplied.

## Included
- `recovered/EssenceSMP-Solar-1.0.0-original.jar` — your original compiled plugin, unchanged.
- `pom.xml` — Maven build configuration recovered from the JAR.
- `src/main/resources/plugin.yml` — plugin metadata and command declarations recovered from the JAR.
- `recovered/SolarPlugin.javap.txt` — Java bytecode/disassembly listing for the compiled `SolarPlugin` class.

## Important limitation
The uploaded JAR did **not** contain the original `.java` source code. A JAR stores compiled `.class` files, so this package is a recovery/inspection bundle, not a complete editable source rebuild. The disassembly is useful for examining the existing implementation, but it is not Java source and cannot be dropped into `src/main/java` and compiled.

The original JAR is preserved so you can continue using the existing build. No claim is made that it has been tested on a live server.

## Existing metadata
- Plugin: `EssenceSMP-Solar`
- Version: `1.0.0`
- Main class: `com.essencesmp.solar.SolarPlugin`
- Java release: `21`
- Paper API dependency: `1.21.11-R0.1-SNAPSHOT`

## Next step for a full editable rebuild
The compiled class can be decompiled with a Java decompiler (for example CFR or Vineflower), then reviewed and corrected against the Paper API. The result should be treated as recovered source, not guaranteed original source.
