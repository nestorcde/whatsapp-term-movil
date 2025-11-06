#!/bin/bash

echo "======================================"
echo "Creando estructura Clean Architecture"
echo "======================================"
echo ""

# Base path
BASE_PATH="app/src/main/java/com/whatsappbulk/sender"

# Crear estructura de carpetas
echo "📁 Creando carpetas..."

# Data layer
mkdir -p "$BASE_PATH/data/remote"
mkdir -p "$BASE_PATH/data/remote/dto"
mkdir -p "$BASE_PATH/data/local/dao"
mkdir -p "$BASE_PATH/data/local/entities"
mkdir -p "$BASE_PATH/data/repository"

# Domain layer
mkdir -p "$BASE_PATH/domain/model"
mkdir -p "$BASE_PATH/domain/repository"
mkdir -p "$BASE_PATH/domain/usecase"

# UI layer
mkdir -p "$BASE_PATH/ui/navigation"
mkdir -p "$BASE_PATH/ui/theme"
mkdir -p "$BASE_PATH/ui/screens/splash"
mkdir -p "$BASE_PATH/ui/screens/login"
mkdir -p "$BASE_PATH/ui/screens/whatsapp"
mkdir -p "$BASE_PATH/ui/screens/campaigns/list"
mkdir -p "$BASE_PATH/ui/screens/campaigns/details"
mkdir -p "$BASE_PATH/ui/screens/campaigns/components"
mkdir -p "$BASE_PATH/ui/screens/execution"
mkdir -p "$BASE_PATH/ui/screens/dashboard"
mkdir -p "$BASE_PATH/ui/components"

# DI
mkdir -p "$BASE_PATH/di"

# Util
mkdir -p "$BASE_PATH/util"

echo "✅ Estructura de carpetas creada"
echo ""
echo "Estructura:"
echo ""
tree -L 4 "$BASE_PATH" 2>/dev/null || find "$BASE_PATH" -type d | sed 's|[^/]*/| |g'

echo ""
echo "======================================"
echo "✅ Estructura completada"
echo "======================================"
echo ""
echo "Próximos pasos:"
echo "1. Copiar archivos de configuración de Gradle"
echo "2. Sync Gradle en Android Studio"
echo "3. Crear Application class para Hilt"
echo "4. Implementar tema Material 3"
