#!/bin/bash
# Dark Glass — instalação Mac → TCL Android TV por ADB sem fio.
# Uso:  ./instalar-na-tv.sh [caminho/DarkGlass.apk | URL do APK]
# Não altera nada na TV além de instalar/atualizar o app com.darkglass.launcher.
set -euo pipefail

PKG="com.darkglass.launcher"
APK_SRC="${1:-}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "== Dark Glass: instalação Mac → TV =="

# 1) adb (Android platform-tools)
if ! command -v adb >/dev/null 2>&1; then
  if command -v brew >/dev/null 2>&1; then
    read -r -p "adb não encontrado. Instalar 'android-platform-tools' via Homebrew? [s/N] " r
    [[ "$r" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 1; }
    brew install --cask android-platform-tools
  else
    echo "Instale o adb: https://developer.android.com/tools/releases/platform-tools (ou Homebrew)."; exit 1
  fi
fi
adb version | head -1

# 2) APK: arquivo local, URL da release ou build local
if [[ -z "$APK_SRC" ]]; then
  if [[ -f app/build/outputs/apk/debug/app-debug.apk ]]; then
    APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
  else
    read -r -p "Caminho ou URL do DarkGlass.apk: " APK_SRC
  fi
fi
APK="$WORK/DarkGlass.apk"
if [[ "$APK_SRC" =~ ^https:// ]]; then
  curl -fL --proto '=https' -o "$APK" "$APK_SRC"
  if curl -fsL --proto '=https' -o "$WORK/hash" "${APK_SRC}.sha256"; then
    esperado=$(awk '{print $1}' "$WORK/hash"); obtido=$(shasum -a 256 "$APK" | awk '{print $1}')
    [[ "$esperado" == "$obtido" ]] || { echo "ERRO: hash não confere."; exit 1; }
    echo "Hash SHA-256 conferido."
  fi
else
  cp "$APK_SRC" "$APK"
fi
shasum -a 256 "$APK"

# 3) Pareamento (uma vez por Mac) — código digitado só aqui, nunca salvo
echo
echo "Na TV: Configurações → Opções do desenvolvedor → Depuração sem fio → Parear dispositivo com código."
read -r -p "Já pareou este Mac com a TV antes? [s/N] " ja
if [[ ! "$ja" =~ ^[sS]$ ]]; then
  read -r -p "IP:porta de PAREAMENTO mostrado na TV (ex. 192.168.0.20:37123): " PAIR
  adb pair "$PAIR"   # o adb pede o código de 6 dígitos no terminal
fi

# 4) Conexão (porta de CONEXÃO, diferente da de pareamento)
read -r -p "IP:porta de CONEXÃO da tela 'Depuração sem fio' (ex. 192.168.0.20:41234): " CONN
adb connect "$CONN"
adb -s "$CONN" get-state >/dev/null
echo "TV conectada: $(adb -s "$CONN" shell getprop ro.product.manufacturer) $(adb -s "$CONN" shell getprop ro.product.model) — Android $(adb -s "$CONN" shell getprop ro.build.version.release)"
read -r -p "É a TV correta? Instalar o Dark Glass nela? [s/N] " ok
[[ "$ok" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 1; }

# 5) Instalar/atualizar e abrir
adb -s "$CONN" install -r "$APK"
# Permite abrir ao ligar a TV (reversível: ... SYSTEM_ALERT_WINDOW default)
adb -s "$CONN" shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow || echo "AVISO: não foi possível liberar a abertura automática."
adb -s "$CONN" shell am start -n "$PKG/.MainActivity"
echo
echo "Instalado. Versão: $(adb -s "$CONN" shell dumpsys package $PKG | grep -m1 versionName | tr -d ' ')"
echo "Desinstalar quando quiser: adb -s $CONN uninstall $PKG"
