#!/usr/bin/env bash
set -euo pipefail

echo "=================================================="
echo " WhatsApp Termux API - Instalación/Start Automático"
echo "=================================================="
echo

# 0) Validaciones básicas
if [ ! -f package.json ]; then
  echo "❌ No se encontró package.json en el directorio actual."
  echo "   Por favor, cd a la RAÍZ del proyecto y vuelve a ejecutar."
  exit 1
fi

# 1) Paquetes del sistema (Termux)
echo "📦 Instalando/actualizando paquetes del sistema (Termux)..."
yes | pkg update -y >/dev/null 2>&1 || true
yes | pkg install -y x11-repo >/dev/null 2>&1 || true
yes | pkg install -y chromium fontconfig ttf-dejavu git nodejs termux-auth curl jq >/dev/null 2>&1 || true

# 2) Detectar ruta de Chromium (Termux)
CHROME_BIN="/data/data/com.termux/files/usr/bin/chromium-browser"
if [ ! -x "$CHROME_BIN" ]; then
  if command -v chromium >/dev/null 2>&1; then
    CHROME_BIN="$(command -v chromium)"
  elif command -v headless_shell >/dev/null 2>&1; then
    CHROME_BIN="$(command -v headless_shell)"
  else
    echo "❌ No se encontró Chromium en Termux."
    echo "   Intentá: pkg install -y x11-repo chromium"
    exit 1
  fi
fi
echo "🧭 Chromium detectado en: $CHROME_BIN"
"$CHROME_BIN" --version || true
echo

# 3) Limpiar posibles proxies que rompen web.whatsapp.com
echo "🧹 Limpiando configuración de proxy (env, npm, git)..."
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY no_proxy || true
npm config delete proxy >/dev/null 2>&1 || true
npm config delete https-proxy >/dev/null 2>&1 || true
git config --global --unset http.proxy >/dev/null 2>&1 || true
git config --global --unset https.proxy >/dev/null 2>&1 || true

# 4) Variables de entorno para instalación/ejecución
#    - Evitar descarga de navegador por Puppeteer
#    - Forzar WASM para sharp
export PUPPETEER_SKIP_DOWNLOAD=1
export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
export PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN"
export CHROME_PATH="$CHROME_BIN"
export PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server"
export SHARP_BACKEND=wasm

# 5) Respaldo y parcheo de package.json (overrides + scripts seguros)
echo "📝 Preparando overrides de sharp y bloqueo de puppeteer..."
cp package.json package.json.bak.$(date +%s) || true

# Si no hay jq, ya lo instalamos arriba. Ahora aplicamos overrides y garantizamos scripts.
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));

pkg.overrides = Object.assign({}, pkg.overrides, {
  // fuerza una sola versión de sharp en todo el árbol
  "sharp": "^0.34.4",
  "@wppconnect-team/wppconnect": {
    "sharp": "^0.34.4"
  }
});

// scripts: no tocamos los existentes, solo agregamos postinstall seguro si falta
pkg.scripts = pkg.scripts || {};
if (!pkg.scripts.postinstall) {
  // instala el runtime wasm sin romper si ya existe
  pkg.scripts.postinstall = "node -e \"process.env.npm_config_arch='wasm32'\" && npm i --include=optional @img/sharp-wasm32 --no-audit --no-fund || true";
}

fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
console.log('✅ package.json actualizado con overrides y postinstall.');
NODE

# 6) Limpiar instalación previa de npm
echo "🧽 Limpiando node_modules y lockfile..."
rm -rf node_modules package-lock.json

# 7) Instalar dependencias del proyecto SIN bajar navegador y SIN sharp primero
echo "📥 Instalando dependencias del proyecto (sin descargar Chromium)..."
# Forzamos arch wasm32 durante la instalación para que sharp se resuelva a wasm
export npm_config_arch=wasm32
export npm_config_force=true
npm install --no-audit --no-fund

# 8) Remover overrides temporalmente para poder reinstalar Sharp
echo "🔧 Removiendo overrides temporalmente para reinstalar Sharp..."
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));
delete pkg.overrides;
delete pkg.scripts.postinstall;
fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
NODE

# 9) Reinstalar Sharp específicamente con --cpu=wasm32 para forzar runtime correcto
echo "🖼️ Reinstalando Sharp con runtime WASM para Android ARM64..."
npm install --cpu=wasm32 sharp --no-audit --no-fund || true

# 10) Asegurar runtime wasm de sharp (@img/sharp-wasm32)
echo "🖼️ Instalando @img/sharp-wasm32..."
npm_config_arch=wasm32 npm install --include=optional @img/sharp-wasm32 --no-audit --no-fund || true

# 11) Restaurar overrides en package.json
echo "🔧 Restaurando overrides de Sharp..."
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));

pkg.overrides = Object.assign({}, pkg.overrides, {
  "sharp": "^0.34.4",
  "@wppconnect-team/wppconnect": {
    "sharp": "^0.34.4"
  }
});

fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
NODE

# 12) Eliminar 'sharp' anidados que rompan (si quedaron instalados debajo de wppconnect)
echo "🧹 Eliminando sharp anidado (si existe) para forzar resolución a la raíz..."
rm -rf node_modules/@wppconnect-team/wppconnect/node_modules/sharp || true

# 13) Deduplicar dependencias para garantizar única instancia de sharp
echo "🧩 Ejecutando dedupe de npm..."
npm dedupe || true

# 14) Verificar Sharp en WASM
echo "🔎 Verificando Sharp (WASM desde raíz)..."
node -e "console.log(require('sharp').versions)" || {
  echo "⚠️  Advertencia: no se pudo cargar 'sharp' aún desde raíz."
}

# 15) Crear/actualizar config.json con Puppeteer apuntando a Chromium
echo "📝 Escribiendo config.json (puppeteerOptions → chromium headless)..."
cat > config.json <<JSON
{
  "puppeteerOptions": {
    "executablePath": "$CHROME_BIN",
    "args": [
      "--headless=new",
      "--no-sandbox",
      "--disable-setuid-sandbox",
      "--disable-dev-shm-usage",
      "--disable-gpu",
      "--proxy-server=direct://",
      "--no-proxy-server"
    ]
  }
}
JSON

# 16) Compilar TypeScript (si existe script build o tsconfig.json)
echo "🔨 Compilando proyecto..."
if npm run | grep -qE 'build'; then
  npm run build
else
  if [ -f tsconfig.json ]; then
    npx tsc -p .
  fi
fi

# 17) Mostrar puertos/URLs útiles
PORT="$(node -e "try{console.log((require('./config.json')?.server?.port)||process.env.PORT||3000)}catch(e){console.log(process.env.PORT||3000)}" 2>/dev/null || echo 3000)"
echo
echo "=================================================="
echo "✅ Instalación completada"
echo "=================================================="
echo "🌐 Puerto configurado: ${PORT}"
echo "📍 URL local:   http://localhost:${PORT}"
echo "📍 URL Android: http://127.0.0.1:${PORT}"
echo "=================================================="
echo

# 18) Iniciar el servidor con entorno correcto
echo "🚀 Iniciando servidor..."
exec env \
  SHARP_BACKEND=wasm \
  PUPPETEER_SKIP_DOWNLOAD=1 \
  PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
  PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN" \
  CHROME_PATH="$CHROME_BIN" \
  PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server" \
  npm start
