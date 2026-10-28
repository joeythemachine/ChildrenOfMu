#!/bin/sh
# Compile-check the mod against the real NeoForge/Minecraft jars (FML stubbed). Prints errors, or "OK".
# Needs the laptop's build/ folder and the CurseForge install staged under /mnt/user-data/uploads.
D=$(cd "$(dirname "$0")" && pwd)
SRC="$D/../../src/main/java"
U=/mnt/user-data/uploads
CP=$(find $U/ChildrenOfMu/build $U/curseforge -name "*.jar" 2>/dev/null | tr '\n' ':')
OUT=$(mktemp -d)
javac -nowarn -proc:none -implicit:none -sourcepath /nonexistent -d "$OUT" -cp "$CP" -Xmaxerrs 500 \
  $(find "$D/stub" -name "*.java") $(find "$SRC" -name "*.java") > "$OUT/log.txt" 2>&1
if grep -q "error:" "$OUT/log.txt"; then grep -E "error:" -A3 "$OUT/log.txt" | sed "s|$SRC/com/childrenofmu/||"; else echo OK; fi
rm -rf "$OUT"
