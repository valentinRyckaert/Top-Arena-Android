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

# Un seul appareil doit être prêt (« device ») ; sinon on explique quoi faire.
STATES=$("$ADB" devices | tail -n +2 | awk 'NF {print $2}')
if [ -z "$STATES" ]; then
    cat <<'MSG'
Aucun téléphone détecté. Sur le téléphone :
  1. Paramètres > À propos du téléphone : touchez 7 fois « Numéro de build » (options pour les développeurs) ;
  2. Paramètres > Système > Options pour les développeurs : activez « Débogage USB » ;
  3. branchez le câble USB (mode « Transfert de fichiers » si on vous le demande) ;
  4. acceptez « Autoriser le débogage USB ? » sur le téléphone, puis relancez ce script.
MSG
    exit 1
fi
if echo "$STATES" | grep -q unauthorized; then
    echo "Le téléphone n'a pas encore autorisé cet ordinateur : acceptez la fenêtre « Autoriser le débogage USB ? » puis relancez."
    exit 1
fi
if [ "$(echo "$STATES" | grep -c '^device$')" -gt 1 ]; then
    echo "Plusieurs appareils branchés : gardez-en un seul (ou exportez ANDROID_SERIAL=<numéro> ; voir « adb devices »)."
    [ -n "${ANDROID_SERIAL:-}" ] || exit 1
fi

echo "Téléphone : $("$ADB" shell getprop ro.product.model | tr -d '\r'), Android $("$ADB" shell getprop ro.build.version.release | tr -d '\r')"
./gradlew --quiet assembleDebug
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n fr.toupiearena.mobile/.MainActivity >/dev/null
echo "Toupie Arena est installée et lancée sur le téléphone."
