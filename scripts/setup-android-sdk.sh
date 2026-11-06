#!/usr/bin/env bash
# Installe (sans droits administrateur) le SDK Android en ligne de commande
# dans ~/Android/Sdk : outils, plate-forme et build-tools utilisés par le projet.
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
PLATFORM="platforms;android-35"
BUILD_TOOLS="build-tools;35.0.0"

mkdir -p "$SDK/cmdline-tools"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Téléchargement des outils en ligne de commande Android…"
    # Le nom du fichier change à chaque version : on le lit dans le dépôt de Google.
    ZIP=$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml \
          | grep -o 'commandlinetools-linux-[0-9]*_latest\.zip' | sort -V | tail -1)
    TMP=$(mktemp -d)
    curl -fL "https://dl.google.com/android/repository/$ZIP" -o "$TMP/tools.zip"
    unzip -q "$TMP/tools.zip" -d "$TMP"
    rm -rf "$SDK/cmdline-tools/latest"
    mv "$TMP/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$TMP"
fi

SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
# « yes » accepte les licences ; il s'arrête par SIGPIPE, d'où le pipefail suspendu.
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$SDK" "platform-tools" "$PLATFORM" "$BUILD_TOOLS" >/dev/null
set -o pipefail
test -d "$SDK/platforms/android-35" && test -d "$SDK/build-tools/35.0.0"

# Le projet trouve le SDK grâce à local.properties (non versionné).
cd "$(dirname "$0")/.."
echo "sdk.dir=$SDK" > local.properties
echo "SDK Android prêt dans $SDK"
