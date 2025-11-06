# Proyecto Android - WhatsApp Bulk Sender

## Creación del Proyecto en Android Studio

### Paso 1: Crear Nuevo Proyecto

1. Abrir Android Studio
2. **File → New → New Project**
3. Seleccionar **Empty Activity** (Compose)
4. Configurar:
   - **Name**: WhatsApp Bulk Sender
   - **Package name**: `com.whatsappbulk.sender`
   - **Save location**: `D:\NodeJS\whatsapp-term-movil\app\android`
   - **Language**: Kotlin
   - **Minimum SDK**: API 21 (Android 5.0)
   - **Build configuration language**: Kotlin DSL (build.gradle.kts)
5. Click **Finish**

### Paso 2: Esperar Gradle Sync

Android Studio descargará todas las dependencias necesarias. Esto puede tomar varios minutos la primera vez.

---

## Estructura del Proyecto

Una vez creado, el proyecto tendrá esta estructura base:

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/whatsappbulk/sender/
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## Configuración Inicial

Después de crear el proyecto, aplicar los siguientes cambios:

### 1. Actualizar `build.gradle.kts` (Project level)

Ver: [../config/build.gradle.kts.project](../config/build.gradle.kts.project)

### 2. Actualizar `build.gradle.kts` (App level)

Ver: [../config/build.gradle.kts.app](../config/build.gradle.kts.app)

### 3. Actualizar `settings.gradle.kts`

Ver: [../config/settings.gradle.kts](../config/settings.gradle.kts)

### 4. Crear estructura Clean Architecture

Ejecutar el script de creación de carpetas:

```bash
cd app/android
bash ../scripts/create_structure.sh
```

O crear manualmente siguiendo: [../PLAN_DESARROLLO.md#31-capas-de-la-aplicación](../PLAN_DESARROLLO.md#31-capas-de-la-aplicación)

---

## Próximos Pasos

Una vez creado el proyecto y configuradas las dependencias:

1. ✅ Proyecto creado en Android Studio
2. ⏭️ Configurar Hilt (Application class)
3. ⏭️ Implementar tema Material 3
4. ⏭️ Crear modelos de dominio
5. ⏭️ Implementar LoginScreen

Ver [../PLAN_DESARROLLO.md](../PLAN_DESARROLLO.md) para el plan completo.

---

**Nota**: Este README será reemplazado por el README.md generado por Android Studio al crear el proyecto.
