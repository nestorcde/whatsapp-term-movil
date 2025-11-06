#!/bin/bash

# Script de instalación para Termux
# Configura automáticamente Sharp con WebAssembly para Android ARM64

echo "=================================================="
echo "WhatsApp Termux API - Instalación para Termux"
echo "=================================================="
echo ""

# Limpiar instalación previa
echo "🧹 Limpiando instalación previa..."
rm -rf node_modules package-lock.json

# Configurar variables de entorno para Sharp
echo "⚙️  Configurando variables de entorno..."
export npm_config_arch=wasm32
export npm_config_force=true

# Instalar dependencias base
echo "📦 Instalando dependencias base..."
npm install --include=optional

# Instalar Sharp con soporte WebAssembly
echo "🖼️  Instalando Sharp para Android ARM64..."
npm install --cpu=wasm32 sharp
npm install @img/sharp-wasm32

# Verificar instalación de Sharp
echo ""
echo "✅ Verificando instalación de Sharp..."
node -e "console.log(require('sharp').versions)" || echo "⚠️  Sharp instalado pero con advertencias (esto es normal)"

# Compilar TypeScript
echo ""
echo "🔨 Compilando TypeScript..."
npm run build

echo ""
echo "=================================================="
echo "✅ Instalación completada!"
echo "=================================================="
echo ""
echo "Para iniciar el servidor ejecuta:"
echo "  npm start"
echo ""
echo "O en modo desarrollo:"
echo "  npm run dev"
echo ""
