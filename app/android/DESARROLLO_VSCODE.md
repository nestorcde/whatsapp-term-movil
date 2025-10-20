# Desarrollo Android desde VS Code

## 🛠️ Configuración de VS Code

### Extensiones Recomendadas

1. **Kotlin Language** (mathiasfrohlich.Kotlin)
2. **Gradle for Java** (vscjava.vscode-gradle)
3. **Android iOS Emulator** (DiemasMichiels.emulate)

### Configuración del Entorno

Asegúrate de tener configuradas las variables de entorno:

```bash
# Windows
ANDROID_HOME=C:\Users\TuUsuario\AppData\Local\Android\Sdk
JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

# Agregar al PATH:
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\tools
%JAVA_HOME%\bin
```

---

## 📦 Comandos Gradle desde Terminal

### Compilar el Proyecto

```bash
# Desde: app/android/
./gradlew build
```

### Limpiar Build

```bash
./gradlew clean
```

### Compilar sin Tests

```bash
./gradlew assembleDebug
```

### Compilar Release

```bash
./gradlew assembleRelease
```

### Instalar en Dispositivo/Emulador

```bash
./gradlew installDebug
```

### Ver Dependencias

```bash
./gradlew app:dependencies
```

### Verificar Lint

```bash
./gradlew lint
```

---

## 🏃 Ejecutar la App

### Opción 1: Con Gradle (Recomendado)

```bash
# 1. Asegurar que el emulador esté corriendo
# 2. Compilar e instalar
./gradlew installDebug

# 3. Iniciar la app (desde adb)
adb shell am start -n com.whatsappbulk.sender/.MainActivity
```

### Opción 2: Con ADB directamente

```bash
# Compilar APK
./gradlew assembleDebug

# Instalar en dispositivo
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Iniciar app
adb shell am start -n com.whatsappbulk.sender/.MainActivity
```

---

## 🔍 Comandos ADB Útiles

### Ver Dispositivos Conectados

```bash
adb devices
```

### Ver Logs de la App

```bash
adb logcat | grep "WhatsAppBulk"
```

### Limpiar Logs

```bash
adb logcat -c
```

### Ver Logs en Tiempo Real

```bash
adb logcat -s "MainActivity"
```

### Desinstalar App

```bash
adb uninstall com.whatsappbulk.sender
```

### Capturar Screenshot

```bash
adb exec-out screencap -p > screenshot.png
```

---

## 🎨 Hot Reload (Limitado)

VS Code no tiene Hot Reload como Android Studio, pero puedes:

### Opción 1: Reinstalar Rápido

```bash
./gradlew installDebug && adb shell am start -n com.whatsappbulk.sender/.MainActivity
```

### Opción 2: Script de Desarrollo

Crear `dev.sh`:

```bash
#!/bin/bash
./gradlew clean assembleDebug installDebug
adb shell am start -n com.whatsappbulk.sender/.MainActivity
adb logcat -c
adb logcat | grep "WhatsAppBulk"
```

Ejecutar:
```bash
bash dev.sh
```

---

## 🐛 Debugging

### Ver Stack Trace Completo

```bash
adb logcat *:E
```

### Filtrar por Tag

```bash
adb logcat -s "LoginViewModel"
```

### Ver Crashes

```bash
adb logcat -s "AndroidRuntime"
```

---

## 📂 Estructura de Archivos

Cuando trabajes desde VS Code, navega así:

```
app/android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/whatsappbulk/sender/
│   │       │   ├── MainActivity.kt
│   │       │   ├── WhatsAppBulkApp.kt
│   │       │   ├── data/
│   │       │   ├── domain/
│   │       │   └── ui/
│   │       └── res/
│   └── build.gradle.kts  ← Dependencias
├── build.gradle.kts      ← Configuración root
└── gradle.properties     ← Propiedades
```

---

## ✅ Verificar que Todo Funciona

```bash
# 1. Limpiar
./gradlew clean

# 2. Compilar
./gradlew build

# 3. Instalar
./gradlew installDebug

# 4. Ver logs
adb logcat | grep -i "hilt"
```

Si ves logs de Hilt sin errores, todo está configurado correctamente.

---

## 🚀 Workflow Recomendado desde VS Code

1. **Editar archivos .kt** en VS Code
2. **Guardar cambios** (Ctrl+S)
3. **Compilar e instalar**:
   ```bash
   ./gradlew installDebug
   ```
4. **Reiniciar app** en emulador (o automáticamente se reinstala)
5. **Ver logs**:
   ```bash
   adb logcat -c && adb logcat | grep "WhatsAppBulk"
   ```

---

## 🔧 Solución de Problemas

### "gradlew: command not found"

```bash
# Windows (PowerShell)
.\gradlew.bat build

# O dar permisos (Git Bash)
chmod +x gradlew
./gradlew build
```

### "SDK location not found"

Crear `local.properties`:

```properties
sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
```

### "Java version error"

Verificar versión:

```bash
java -version
# Debe ser Java 17
```

### Gradle muy lento

Agregar a `gradle.properties`:

```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.caching=true
```

---

## 📝 Tips para VS Code

1. **Formatear código**: Shift+Alt+F
2. **Imports automáticos**: Copiar código de Android Studio primero
3. **Sintaxis Kotlin**: Funciona bien con la extensión
4. **Compose Preview**: NO funciona en VS Code (usar Android Studio para previews)
5. **Debugging**: Mejor en Android Studio, pero puedes ver logs con `adb logcat`

---

**Recomendación**:

- **Desarrollo rápido**: VS Code (editar código)
- **UI/Previews**: Android Studio (ver Compose previews)
- **Debugging complejo**: Android Studio
- **Git/Terminal**: VS Code

**Workflow híbrido**:
1. Editar en VS Code
2. Ver previews en Android Studio (sin cerrar proyecto)
3. Compilar desde terminal en VS Code

---

**Estado**: Listo para desarrollar desde VS Code

**Siguiente**: Implementar LoginScreen
