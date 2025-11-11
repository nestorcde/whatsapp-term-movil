#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

echo "=================================================="
echo " WhatsApp Termux API - Install & Start (sharp 0.34.3 + WASM)"
echo "=================================================="
echo

# --- Validación ---
if [ ! -f package.json ]; then
  echo "❌ No se encontró package.json en el directorio actual."
  echo "   Ubicate en ~/whatsapp-term-movil/server y reintentá."
  exit 1
fi

PREFIX="/data/data/com.termux/files/usr"
CHROME_BIN="$PREFIX/bin/chromium-browser"

# --- Paquetes base ---
yes | pkg update -y >/dev/null 2>&1 || true
yes | pkg install -y x11-repo chromium fontconfig ttf-dejavu git nodejs curl jq >/dev/null 2>&1 || true

# --- Detectar Chromium ---
if [ ! -x "$CHROME_BIN" ]; then
  if command -v chromium >/dev/null 2>&1; then
    CHROME_BIN="$(command -v chromium)"
  elif command -v headless_shell >/dev/null 2>&1; then
    CHROME_BIN="$(command -v headless_shell)"
  else
    echo "❌ Chromium no encontrado. Instalá: pkg install -y x11-repo chromium"
    exit 1
  fi
fi
echo "🧭 Chromium: $CHROME_BIN"
"$CHROME_BIN" --version || true
echo

# --- Limpiar proxies ---
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY no_proxy || true
npm config delete proxy >/dev/null 2>&1 || true
npm config delete https-proxy >/dev/null 2>&1 || true
git config --global --unset http.proxy >/dev/null 2>&1 || true
git config --global --unset https.proxy >/dev/null 2>&1 || true

# --- Entorno Puppeteer + Sharp WASM ---
export PATH="$PREFIX/bin:$PATH"
export PUPPETEER_SKIP_DOWNLOAD=1
export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
export PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN"
export CHROME_PATH="$CHROME_BIN"
export PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server"

# Fuerza WASM en instalación y en runtime
export SHARP_BACKEND=wasm
export SHARP_IGNORE_GLOBAL_LIBVIPS=1
export npm_config_sharp_backend=wasm    # <- CLAVE: el instalador de sharp lee esto

# --- .npmrc del proyecto para futuras corridas (evita EBADPLATFORM) ---
cat > .npmrc <<NPMRC
arch=wasm32
force=true
audit=false
fund=false
sharp_backend=wasm
NPMRC

# --- Pin exacto: sharp y wasm32 en 0.34.3 (sin anidar overrides raros) ---
echo "📝 Pin sharp@0.34.3 y @img/sharp-wasm32@0.34.3…"
cp package.json "package.json.bak.$(date +%s)" || true
node - <<'NODE'
const fs = require('fs');
const path = 'package.json';
const pkg = JSON.parse(fs.readFileSync(path, 'utf8'));

for (const sec of ['dependencies','devDependencies','optionalDependencies']) {
  if (!pkg[sec]) continue;
  if (pkg[sec].sharp) pkg[sec].sharp = "0.34.3";
  if (pkg[sec]['@img/sharp-wasm32']) pkg[sec]['@img/sharp-wasm32'] = "0.34.3";
}

// overrides lineales (aplican a todo el árbol)
pkg.overrides = Object.assign({}, pkg.overrides, {
  "sharp": "0.34.3",
  "@img/sharp-wasm32": "0.34.3"
});

// postinstall: reasegurar wasm en misma versión
pkg.scripts = pkg.scripts || {};
if (!pkg.scripts.postinstall) {
  pkg.scripts.postinstall = "npm i --no-audit --no-fund @img/sharp-wasm32@0.34.3 || true";
}

fs.writeFileSync(path, JSON.stringify(pkg, null, 2));
console.log('✅ package.json listo (sharp 0.34.3 + @img/sharp-wasm32 0.34.3)');
NODE

# Guardar exacto en este proyecto (sin ^ ni ~)
npm --location=project config set save-exact true >/dev/null 2>&1 || true

# --- Instalar deps desde cero (arch=wasm32 + force, y backend wasm para sharp) ---
echo "🧽 Limpiando node_modules y lockfile…"
rm -rf node_modules package-lock.json

echo "📥 npm install (wasm)…"
npm_config_arch=wasm32 npm_config_force=true npm_config_sharp_backend=wasm \
  npm install --no-audit --no-fund

# --- Reasegurar wasm exacto (por si algún postinstall pisó algo) ---
echo "🖼️ Reasegurando @img/sharp-wasm32@0.34.3…"
npm_config_arch=wasm32 npm_config_force=true npm_config_sharp_backend=wasm \
  npm i --no-audit --no-fund @img/sharp-wasm32@0.34.3 || true

# --- Quitar cualquier binario nativo de sharp en TODO el árbol ---
echo "🧹 Borrando binarios nativos de sharp (*.node) para forzar WASM…"
find node_modules -type f -path "*/sharp/build/Release/*.node" -delete 2>/dev/null || true

# --- Quitar sharp anidado típico de wppconnect (si aparece) ---
rm -rf node_modules/@wppconnect-team/wppconnect/node_modules/sharp || true

# ⛔ NO ejecutar `npm dedupe` (provoca EBADPLATFORM en boot)

# --- Verificar Sharp/WASM (ENV inline para que aplique sí o sí) ---
echo "🔎 Verificando Sharp/WASM…"
env SHARP_BACKEND=wasm SHARP_IGNORE_GLOBAL_LIBVIPS=1 \
  node -e "try{const s=require('sharp');console.log('✅ sharp OK (WASM):', s.versions)}catch(e){console.error('❌ sharp no cargó:', e.message);process.exit(1)}"

# --- config.json para Puppeteer → Chromium local ---
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

# --- Build (genera dist/) ---
echo "🔨 Compilando proyecto…"
if npm run | grep -qE '(^|\\s)build(\\s|:)'; then
  npm run build
elif [ -f tsconfig.json ]; then
  npx tsc -p .
fi

# --- Validar dist/ ---
if [ ! -d dist ]; then
  echo "❌ No se generó dist/. Revisá logs de build."
  exit 1
fi

# --- URLs ---
PORT="$(node -e "try{console.log((require('./config.json')?.server?.port)||process.env.PORT||3000)}catch(e){console.log(process.env.PORT||3000)}" 2>/dev/null || echo 3000)"
echo
echo "=================================================="
echo "✅ Instalación + Build OK (sharp 0.34.3 + WASM)"
echo "=================================================="
echo "🌐 Puerto: ${PORT}"
echo "📍 URL local:   http://localhost:${PORT}"
echo "📍 URL Android: http://127.0.0.1:${PORT}"
echo "=================================================="
echo

# --- Iniciar servidor con entorno correcto ---
echo "🚀 Iniciando servidor…"
exec env \
  SHARP_BACKEND=wasm \
  SHARP_IGNORE_GLOBAL_LIBVIPS=1 \
  PUPPETEER_SKIP_DOWNLOAD=1 \
  PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
  PUPPETEER_EXECUTABLE_PATH="$CHROME_BIN" \
  CHROME_PATH="$CHROME_BIN" \
  PUPPETEER_ARGS="--headless=new --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --proxy-server=direct:// --no-proxy-server" \
  npm start
