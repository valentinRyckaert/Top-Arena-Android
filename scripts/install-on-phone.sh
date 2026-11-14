#!/usr/bin/env bash
# Compile l'application et l'installe sur le téléphone branché en USB
# (débogage USB activé), puis la lance.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f local.properties ]; then
    echo "SDK Android absent : installation…"
    ./scripts/setup-android-sdk.sh
fi
SDK=$(sed -n 's/^sdk.dir=//p' local.properties)
ADB="$SDK/platform-tools/adb"
[ -x "$ADB" ] || ADB=adb

# Appareils prêts (« device ») ; un téléphone branché passe avant un émulateur.
DEVICES=$("$ADB" devices | tail -n +2 | awk '$2 == "device" {print $1}')
PHONES=$(echo "$DEVICES" | grep -v '^emulator-' || true)
if [ -z "${ANDROID_SERIAL:-}" ]; then
    if [ -z "$DEVICES" ]; then
        if "$ADB" devices | grep -q unauthorized; then
            echo "Le téléphone n'a pas encore autorisé cet ordinateur : acceptez la fenêtre « Autoriser le débogage USB ? » puis relancez."
        else
            cat <<'MSG'
Aucun téléphone détecté. Sur le téléphone :
  1. Paramètres > À propos du téléphone : touchez 7 fois « Numéro de build » (options pour les développeurs) ;
  2. Paramètres > Système > Options pour les développeurs : activez « Débogage USB » ;
  3. branchez le câble USB (mode « Transfert de fichiers » si on vous le demande) ;
  4. acceptez « Autoriser le débogage USB ? » sur le téléphone, puis relancez ce script.
MSG
        fi
        exit 1
    fi
    CANDIDATES=${PHONES:-$DEVICES}
    if [ "$(echo "$CANDIDATES" | wc -l)" -gt 1 ]; then
        echo "Plusieurs appareils branchés : choisissez-en un avec ANDROID_SERIAL=<numéro> $0"
        echo "$CANDIDATES"
        exit 1
    fi
    export ANDROID_SERIAL=$CANDIDATES
fi

echo "Appareil : $("$ADB" shell getprop ro.product.model | tr -d '\r'), Android $("$ADB" shell getprop ro.build.version.release | tr -d '\r') ($ANDROID_SERIAL)"
./gradlew --quiet assembleDebug
if ! OUT=$("$ADB" install -r app/build/outputs/apk/debug/app-debug.apk 2>&1); then
    echo "$OUT"
    if echo "$OUT" | grep -q INSTALL_FAILED_UPDATE_INCOMPATIBLE; then
        echo "Une version signée sur un autre ordinateur est déjà installée. Désinstallez-la d'abord"
        echo "(attention : cela efface les toupies enregistrées sur le téléphone) : $ADB uninstall fr.toupiearena.mobile"
    fi
    exit 1
fi
"$ADB" shell am start -n fr.toupiearena.mobile/.MainActivity >/dev/null
echo "Top Arena est installée et lancée."
