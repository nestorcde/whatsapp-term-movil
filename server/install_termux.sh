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

# 3) Limpiar posibles proxies
echo "🧹 Limpiando configuración de proxy (env, npm, git)..."
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY no_proxy || true
npm config delete proxy >/dev/null 2>&1 || true
npm config delete https-proxy >/dev/null 2>&1 || true
git config --global --unset http.proxy >/dev/null 2>&1 || true
git config --global --unset https.proxy >/dev/null 2>&1 || true

# 4) Entorno
export PUPPETEER_SKIP_DOWNLOAD=1
export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
export PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN"
export CHROME_PATH="$CHROME_BIN"
export PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server"
export SHARP_BACKEND=wasm

# 5) Overrides y postinstall seguros
echo "📝 Preparando overrides de sharp y bloqueo de puppeteer..."
cp package.json "package.json.bak.$(date +%s)" || true
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));
pkg.overrides = Object.assign({}, pkg.overrides, {
  "sharp": "^0.34.4",
  "@wppconnect-team/wppconnect": { "sharp": "^0.34.4" }
});
pkg.scripts = pkg.scripts || {};
if (!pkg.scripts.postinstall) {
  pkg.scripts.postinstall = "node -e \"process.env.npm_config_arch='wasm32'\" && npm i --include=optional @img/sharp-wasm32 --no-audit --no-fund || true";
}
fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
console.log('✅ package.json actualizado con overrides y postinstall.');
NODE

# 6) Limpieza previa
echo "🧽 Limpiando node_modules y lockfile..."
rm -rf node_modules package-lock.json

# 7) Instalar dependencias forzando wasm32
echo "📥 Instalando dependencias del proyecto (sin descargar Chromium)..."
export npm_config_arch=wasm32
export npm_config_force=true
npm install --no-audit --no-fund

# 7.1) Toolchain TypeScript (sin ocultar errores)
echo "🧩 Instalando toolchain TypeScript (typescript, ts-node, @types/node)..."
npm i -D typescript ts-node @types/node --no-audit --no-fund

# 8) Quitar overrides temporalmente para reinstalar sharp
echo "🔧 Removiendo overrides temporalmente para reinstalar Sharp..."
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));
delete pkg.overrides;
delete pkg.scripts.postinstall;
fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
NODE

# 9) Reinstalar sharp WASM
echo "🖼️ Reinstalando Sharp con runtime WASM para Android ARM64..."
npm install --cpu=wasm32 sharp --no-audit --no-fund || true

# 10) Asegurar @img/sharp-wasm32
echo "🖼️ Instalando @img/sharp-wasm32..."
npm_config_arch=wasm32 npm install --include=optional @img/sharp-wasm32 --no-audit --no-fund || true

# 11) Restaurar overrides
echo "🔧 Restaurando overrides de Sharp..."
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));
pkg.overrides = Object.assign({}, pkg.overrides, {
  "sharp": "^0.34.4",
  "@wppconnect-team/wppconnect": { "sharp": "^0.34.4" }
});
fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
NODE

# 12) Borrar sharp anidado (si quedó)
echo "🧹 Eliminando sharp anidado (si existe) para forzar resolución a la raíz..."
rm -rf node_modules/@wppconnect-team/wppconnect/node_modules/sharp || true

# 13) Dedupe
echo "🧩 Ejecutando dedupe de npm..."
npm dedupe || true

# 14) Verificar Sharp
echo "🔎 Verificando Sharp (WASM desde raíz)..."
node -e "console.log(require('sharp').versions)" || {
  echo "⚠️  Advertencia: no se pudo cargar 'sharp' aún desde raíz."
}

# 15) config.json Puppeteer → Chromium
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

# 16) Compilar TypeScript (siempre sin depender del shebang /usr/bin/env)
echo "🔨 Compilando proyecto..."

run_tsc () {
  # Preferir TS local con Node absoluto de Termux
  if [ -f "node_modules/typescript/lib/tsc.js" ]; then
    "$PREFIX/bin/node" node_modules/typescript/lib/tsc.js -p .
  else
    # Fallback efímero: usa typescript del registro sin instalar devDeps globales
    npx -y -p typescript tsc -p .
  fi
}

if [ -f tsconfig.json ]; then
  # Asegurar que el script build (si existe) no rompa; ejecutamos nuestra función sí o sí
  if npm run | grep -qE 'build'; then
    # Intentar el build del proyecto primero (por si tiene otros steps)
    if ! npm run build; then
      run_tsc
    fi
  else
    run_tsc
  fi
else
  # Sin tsconfig, no compila; continuar
  echo "ℹ️ No se encontró tsconfig.json; se omite tsc."
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
