#!/usr/bin/env bash
set -euo pipefail

BOOT_DIR="${HOME}/.termux/boot"
RAW_URL="https://raw.githubusercontent.com/nestorcde/whatsapp-term-movil/main/server/10-whatsapp"
TARGET="${BOOT_DIR}/10-whatsapp"

echo "[*] Creando directorio de arranque: ${BOOT_DIR}"
mkdir -p "${BOOT_DIR}"

echo "[*] Descargando script de WhatsApp a ${TARGET}"
if command -v wget >/dev/null 2>&1; then
  wget -q -O "${TARGET}" "${RAW_URL}"
elif command -v curl >/dev/null 2>&1; then
  curl -fsSL -o "${TARGET}" "${RAW_URL}"
else
  echo "Error: ni 'wget' ni 'curl' están instalados." >&2
  echo "Solución rápida: pkg update && pkg install -y wget" >&2
  exit 1
fi

echo "[*] Asignando permisos de ejecución"
chmod +x "${TARGET}"

echo "[*] Volviendo al HOME"
cd "${HOME}"

echo "[✓] Listo. Archivo instalado en: ${TARGET}"
ls -l "${BOOT_DIR}"
echo "REINICIE EL DISPOSITIVO PARA INICIAR EL SERVICIO AUTOMÁTICAMENTE."
