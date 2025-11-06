# Guía de Inicio de Desarrollo

## 🚀 Paso a Paso para Crear el Proyecto Android

### Requisitos Previos

- ✅ Android Studio Hedgehog (2023.1.1) o superior
- ✅ JDK 17
- ✅ Android SDK 34
- ✅ Conexión a Internet (para descargar dependencias)

---

## Paso 1: Crear Proyecto en Android Studio

1. Abrir **Android Studio**

2. **File → New → New Project**

3. Seleccionar **Empty Activity** (Compose)

4. Configurar proyecto:
   ```
   Name: WhatsApp Bulk Sender
   Package name: com.whatsappbulk.sender
   Save location: D:\NodeJS\whatsapp-term-movil\app\android
   Language: Kotlin
   Minimum SDK: API 21 (Android 5.0)
   Build configuration language: Kotlin DSL (build.gradle.kts)
   ```

5. Click **Finish**

6. Esperar a que Gradle termine de sincronizar (puede tomar varios minutos)

---

## Paso 2: Configurar Gradle

### 2.1 Actualizar `build.gradle.kts` (Project level)

Reemplazar todo el contenido con:

```kotlin
// Copiar desde: app/config/build.gradle.kts.project
```

O manualmente:

```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
}
```

### 2.2 Actualizar `build.gradle.kts` (App level)

Reemplazar todo el contenido con el de: `app/config/build.gradle.kts.app`

### 2.3 Actualizar `settings.gradle.kts`

Reemplazar con el de: `app/config/settings.gradle.kts`

### 2.4 Sync Gradle

En Android Studio: **File → Sync Project with Gradle Files**

Esperar a que termine (puede descargar ~500MB de dependencias)

---

## Paso 3: Crear Estructura de Carpetas

### Opción A: Con Script (Recomendado)

Desde la terminal de Android Studio o Git Bash:

```bash
cd app
bash scripts/create_structure.sh
```

### Opción B: Manual

Crear las siguientes carpetas en `app/src/main/java/com/whatsappbulk/sender/`:

```
├── data/
│   ├── remote/
│   │   ├── dto/
│   │   ├── CampaignApi.kt
│   │   └── WhatsAppApi.kt
│   ├── local/
│   │   ├── dao/
│   │   ├── entities/
│   │   └── AppDatabase.kt
│   └── repository/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── ui/
│   ├── navigation/
│   ├── theme/
│   ├── screens/
│   │   ├── splash/
│   │   ├── login/
│   │   ├── whatsapp/
│   │   ├── campaigns/
│   │   │   ├── list/
│   │   │   ├── details/
│   │   │   └── components/
│   │   ├── execution/
│   │   └── dashboard/
│   └── components/
│
├── di/
└── util/
```

---

## Paso 4: Configurar Hilt (Application Class)

### 4.1 Crear `WhatsAppBulkApp.kt`

En: `app/src/main/java/com/whatsappbulk/sender/`

```kotlin
package com.whatsappbulk.sender

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhatsAppBulkApp : Application()
```

### 4.2 Actualizar `AndroidManifest.xml`

En: `app/src/main/AndroidManifest.xml`

Agregar `android:name=".WhatsAppBulkApp"` al tag `<application>`:

```xml
<application
    android:name=".WhatsAppBulkApp"
    android:allowBackup="true"
    ...>
```

### 4.3 Actualizar `MainActivity.kt`

Agregar anotación `@AndroidEntryPoint`:

```kotlin
package com.whatsappbulk.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Temporal - será reemplazado
            Text("WhatsApp Bulk Sender")
        }
    }
}
```

---

## Paso 5: Implementar Tema Material 3

### 5.1 Crear `Color.kt`

En: `ui/theme/Color.kt`

```kotlin
package com.whatsappbulk.sender.ui.theme

import androidx.compose.ui.graphics.Color

// WhatsApp Colors
val Primary = Color(0xFF25D366)        // Verde WhatsApp
val PrimaryVariant = Color(0xFF128C7E) // Verde oscuro
val Secondary = Color(0xFF34B7F1)      // Azul WhatsApp

// Light theme
val Background = Color(0xFFF7F8FA)     // Gris muy claro
val Surface = Color.White
val OnPrimary = Color.White
val OnSecondary = Color.White
val OnBackground = Color(0xFF1C1C1E)   // Casi negro
val OnSurface = Color(0xFF1C1C1E)

// Error
val Error = Color(0xFFFF5252)
val OnError = Color.White
```

### 5.2 Crear `Type.kt`

En: `ui/theme/Type.kt`

```kotlin
package com.whatsappbulk.sender.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
```

### 5.3 Crear `Theme.kt`

En: `ui/theme/Theme.kt`

```kotlin
package com.whatsappbulk.sender.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError
)

@Composable
fun WhatsAppBulkSenderTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Paso 6: Actualizar MainActivity

Reemplazar el contenido de `MainActivity.kt`:

```kotlin
package com.whatsappbulk.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.whatsappbulk.sender.ui.theme.WhatsAppBulkSenderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhatsAppBulkSenderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WelcomeScreen()
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "WhatsApp Bulk Sender\n\nProyecto Configurado ✅",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WhatsAppBulkSenderTheme {
        WelcomeScreen()
    }
}
```

---

## Paso 7: Compilar y Ejecutar

1. **Build → Make Project** (Ctrl+F9)
2. Esperar a que compile sin errores
3. **Run → Run 'app'** (Shift+F10)
4. Seleccionar emulador o dispositivo físico
5. Debería ver "WhatsApp Bulk Sender - Proyecto Configurado ✅"

---

## ✅ Verificación

Si todo funcionó correctamente, deberías tener:

- ✅ Proyecto compila sin errores
- ✅ App corre en emulador/dispositivo
- ✅ Pantalla muestra mensaje con tema verde WhatsApp
- ✅ Hilt configurado correctamente
- ✅ Estructura de carpetas Clean Architecture creada

---

## 🎯 Próximos Pasos

Ahora que el proyecto está configurado, podemos empezar con la **Fase 1: Login y Autenticación**.

Ver siguiente guía: **FASE_1_LOGIN.md**

O continuar con el [PLAN_DESARROLLO.md](PLAN_DESARROLLO.md)

---

## 🆘 Solución de Problemas

### Error: "Unresolved reference: hilt"

**Solución**: Asegurar que `build.gradle.kts` (app) tiene:
```kotlin
id("com.google.dagger.hilt.android")
id("kotlin-kapt")
```

Y que hiciste Gradle Sync.

### Error: "Cannot find symbol @HiltAndroidApp"

**Solución**: Limpiar y rebuild:
```
Build → Clean Project
Build → Rebuild Project
```

### Error de versión de Kotlin

**Solución**: Verificar que todas las versiones coinciden:
- Kotlin: 1.9.20
- Compose Compiler: 1.5.4

### Gradle sync muy lento

**Solución**:
- Activar modo offline después de la primera sincronización
- File → Settings → Build → Gradle → Offline mode

---

**Estado**: Proyecto base configurado y listo para desarrollo

**Siguiente**: Implementar Login (Fase 1)
