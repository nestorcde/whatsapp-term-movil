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

# 5) Limpiar instalación previa de npm
echo "🧽 Limpiando node_modules y lockfile..."
rm -rf node_modules package-lock.json

# 6) Instalar dependencias del proyecto SIN bajar navegador y SIN sharp
echo "📥 Instalando dependencias del proyecto (sin descargar Chromium ni Sharp)..."
# Primero instalamos todo EXCEPTO sharp, sin ejecutar scripts
npm install --no-audit --no-fund --ignore-scripts

# 7) Instalar Sharp con WASM explícitamente (ANTES de agregar overrides)
echo "🖼️ Instalando Sharp en modo WASM para Android ARM64..."
npm install --cpu=wasm32 sharp --no-audit --no-fund
npm install @img/sharp-wasm32 --no-audit --no-fund

# 8) Ahora SÍ agregar overrides de sharp en package.json para forzar una sola versión
echo "📝 Agregando overrides de sharp a package.json..."
cp package.json package.json.bak.$(date +%s) || true

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

fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
console.log('✅ package.json actualizado con overrides.');
NODE

# 9) Ejecutar scripts postinstall ahora que Sharp ya está instalado
echo "🔧 Ejecutando scripts postinstall..."
npm rebuild --no-audit --no-fund || true

# 10) Aplicar overrides ejecutando install nuevamente (esto respeta los overrides)
echo "🔄 Aplicando overrides de Sharp..."
npm install --no-audit --no-fund || true

# 11) Eliminar 'sharp' anidados que rompan (si quedaron instalados debajo de wppconnect)
echo "🧹 Eliminando sharp anidado (si existe) para forzar resolución a la raíz..."
rm -rf node_modules/@wppconnect-team/wppconnect/node_modules/sharp || true

# 12) Deduplicar dependencias para garantizar única instancia de sharp
echo "🧩 Ejecutando dedupe de npm..."
npm dedupe || true

# 13) Verificar Sharp en WASM
echo "🔎 Verificando Sharp (WASM desde raíz)..."
node -e "console.log(require('sharp').versions)" || {
  echo "⚠️  Advertencia: no se pudo cargar 'sharp' aún desde raíz."
}

# 14) Crear/actualizar config.json con Puppeteer apuntando a Chromium
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

# 15) Compilar TypeScript (si existe script build o tsconfig.json)
echo "🔨 Compilando proyecto..."
if npm run | grep -qE 'build'; then
  npm run build
else
  if [ -f tsconfig.json ]; then
    npx tsc -p .
  fi
fi

# 16) Mostrar puertos/URLs útiles
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

# 17) Iniciar el servidor con entorno correcto
echo "🚀 Iniciando servidor..."
exec env \
  SHARP_BACKEND=wasm \
  PUPPETEER_SKIP_DOWNLOAD=1 \
  PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
  PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN" \
  CHROME_PATH="$CHROME_BIN" \
  PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server" \
  npm start
