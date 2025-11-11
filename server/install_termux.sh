#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

echo "=================================================="
echo " WhatsApp Termux API - Install & Start (Sharp 0.34.3 + WASM)"
echo "=================================================="
echo

# Validación
[ -f package.json ] || { echo "❌ Ejecuta este script en ~/whatsapp-term-movil/server"; exit 1; }

# Paquetes base (silencioso)
yes | pkg update -y >/dev/null 2>&1 || true
yes | pkg install -y x11-repo >/dev/null 2>&1 || true
yes | pkg install -y chromium fontconfig ttf-dejavu git nodejs curl jq >/dev/null 2>&1 || true

# Detectar Chromium
CHROME_BIN="/data/data/com.termux/files/usr/bin/chromium-browser"
if [ ! -x "$CHROME_BIN" ]; then
  if command -v chromium >/dev/null 2>&1; then
    CHROME_BIN="$(command -v chromium)"
  elif command -v headless_shell >/dev/null 2>&1; then
    CHROME_BIN="$(command -v headless_shell)"
  else
    echo "❌ Chromium no encontrado. Instala: pkg install -y x11-repo chromium"; exit 1
  fi
fi
echo "🧭 Chromium: $CHROME_BIN"
"$CHROME_BIN" --version || true
echo

# Limpiar proxies
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY no_proxy || true
npm config delete proxy >/dev/null 2>&1 || true
npm config delete https-proxy >/dev/null 2>&1 || true
git config --global --unset http.proxy >/dev/null 2>&1 || true
git config --global --unset https.proxy >/dev/null 2>&1 || true

# Entorno para puppeteer & sharp
export PUPPETEER_SKIP_DOWNLOAD=1
export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
export PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN"
export CHROME_PATH="$CHROME_BIN"
export PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server"

# Claves para sharp wasm
export SHARP_BACKEND=wasm
export SHARP_IGNORE_GLOBAL_LIBVIPS=1
export npm_config_arch=wasm32
export npm_config_force=true

# 1) Fijar EXACTAMENTE sharp y wasm a 0.34.3
echo "📝 Pin sharp@0.34.3 y @img/sharp-wasm32@0.34.3…"
cp package.json "package.json.bak.$(date +%s)" || true
node - <<'NODE'
const fs = require('fs');
const p = JSON.parse(fs.readFileSync('package.json','utf8'));
p.dependencies = p.dependencies || {};
p.dependencies.sharp = "0.34.3";
p.dependencies["@img/sharp-wasm32"] = "0.34.3";
p.overrides = Object.assign({}, p.overrides, {
  "sharp": "0.34.3",
  "@img/sharp-wasm32": "0.34.3",
  "@wppconnect-team/wppconnect": { "sharp": "0.34.3" }
});
p.scripts = p.scripts || {};
// no uses npm_config_arch aquí: lo fuerza el entorno
p.scripts.postinstall = "npm i --no-audit --no-fund @img/sharp-wasm32@0.34.3 || true";
fs.writeFileSync('package.json', JSON.stringify(p, null, 2));
console.log("✅ package.json listo (sharp 0.34.3 + @img/sharp-wasm32 0.34.3)");
NODE

# 2) Limpiar instalación previa
echo "🧽 Limpiando node_modules y lockfile…"
rm -rf node_modules package-lock.json

# 3) Instalar deps (sin bajar chromium)
echo "📥 npm install (wasm)…"
npm install --no-audit --no-fund

# 4) Asegurar que NO queden binarios nativos de sharp
echo "🧹 Borrando binarios nativos de sharp (*.node) para forzar WASM…"
find node_modules -type f -name "*.node" -path "*/sharp/*" -delete 2>/dev/null || true
rm -rf node_modules/@wppconnect-team/wppconnect/node_modules/sharp || true

# 5) Verificación de sharp con WASM (inyecta var dentro del proceso)
echo "🔎 Verificando Sharp/WASM…"
node - <<'NODE' || true
process.env.SHARP_BACKEND = 'wasm';
process.env.SHARP_IGNORE_GLOBAL_LIBVIPS = '1';
try {
  const sharp = require('sharp');
  console.log('✔ sharp versions:', sharp.versions);
} catch (e) {
  console.log('❌ sharp no cargó:', e.message);
}
NODE

# 6) Escribir config de puppeteer → chromium
echo "📝 Generando config.json para Chromium headless…"
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

# 7) Compilar TypeScript (sin depender de shebang)
echo "🔨 Compilando…"
if [ -f "node_modules/typescript/lib/tsc.js" ]; then
  "$PREFIX/bin/node" node_modules/typescript/lib/tsc.js -p .
else
  npx -y -p typescript tsc -p .
fi

# 8) Arrancar el servidor (con WASM asegurado)
PORT="$(node -e "try{console.log((require('./config.json')?.server?.port)||process.env.PORT||3000)}catch(e){console.log(process.env.PORT||3000)}" 2>/dev/null || echo 3000)"
echo
echo "=================================================="
echo "✅ Instalación completada"
echo "=================================================="
echo "🌐 Puerto: ${PORT}"
echo "📍 http://127.0.0.1:${PORT}"
echo "=================================================="
echo

echo "🚀 Iniciando servidor…"
exec env \
  SHARP_BACKEND=wasm \
  SHARP_IGNORE_GLOBAL_LIBVIPS=1 \
  PUPPETEER_SKIP_DOWNLOAD=1 \
  PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
  PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN" \
  CHROME_PATH="$CHROME_BIN" \
  PUPPETEER_ARGS="$PUPPETEER_ARGS" \
  npm start
