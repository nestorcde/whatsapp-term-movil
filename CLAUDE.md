# CLAUDE.md

Este archivo proporciona orientación a Claude Code (claude.ai/code) cuando trabaja con código en este repositorio.

## Descripción General del Proyecto

WhatsApp Termux - Monorepo para controlar WhatsApp Web desde aplicaciones Android usando Termux. El proyecto consiste en:

1. **Server** (`/server`): API REST en Node.js + TypeScript que encapsula WPPConnect para automatización de WhatsApp Web
2. **App Android** (`/app`): Aplicación Android nativa (Kotlin + Jetpack Compose) que ejecuta campañas de WhatsApp pre-configuradas
3. **WPPConnect** (`/wppconnect`): Repositorio clonado de la librería WPPConnect

**Concepto Arquitectónico Importante**: La aplicación Android es un **EJECUTOR**, NO un **CREADOR**. Ejecuta campañas pre-configuradas desde un sistema backend externo. NO crea campañas, mensajes, ni gestiona listas de contactos.

## Arquitectura del Sistema (Modelo de 3 Componentes)

```
Backend/Panel Web (externo - no implementado)
  ↓ Crea campañas, mensajes, contactos, cuotas
Servidor Termux (localhost:3000)
  ↓ API cliente de WhatsApp Web
App Android
  → Selecciona campaña, especifica cantidad, ejecuta, monitorea
```

## Comandos Comunes de Desarrollo

### Server (Node.js + TypeScript)

Ubicado en `/server`:

```bash
cd server

# Instalar dependencias
npm install

# Modo desarrollo (con auto-reload)
npm run dev

# Compilar TypeScript
npm run build

# Inicio en producción
npm start

# Limpiar artefactos de compilación y sesiones
npm run clean

# Setup específico para Termux e inicio
npm run setup:termux
npm run install:termux
```

**El servidor corre en**: `http://localhost:3000` (o `http://127.0.0.1:3000` para apps Android en el mismo dispositivo)

### App Android (Kotlin + Jetpack Compose)

Ubicada en `/app/android`:

```bash
cd app/android

# Compilar la app
./gradlew build

# Compilar APK debug
./gradlew assembleDebug

# Compilar APK release (minificado)
./gradlew assembleRelease

# Ejecutar pruebas
./gradlew test

# Ejecutar pruebas instrumentadas de Android
./gradlew connectedAndroidTest

# Limpiar compilación
./gradlew clean
```

**Nota**: El desarrollo Android usa Android Studio. El proyecto requiere JDK 17, Android SDK 34+ y apunta a API 21+ (Android 5.0+).

## Arquitectura del Server (TypeScript)

### Estructura del Proyecto

```
server/src/
├── controllers/       # Manejadores de peticiones
│   ├── authController.ts
│   ├── messageController.ts
│   └── contactController.ts
├── routes/           # Definiciones de rutas Express
│   ├── authRoutes.ts
│   ├── messageRoutes.ts
│   └── contactRoutes.ts
├── services/         # Lógica de negocio
│   └── WhatsAppService.ts  # Wrapper principal de WPPConnect
├── utils/            # Utilidades
│   ├── logger.ts           # Logger de consola
│   └── winstonLogger.ts    # Logger de archivos/Winston
├── types/            # Definiciones de tipos TypeScript
│   └── index.ts
└── server.ts         # Punto de entrada de la app Express
```

### Conceptos Clave

- **WhatsAppService** es un singleton que gestiona el ciclo de vida del cliente WPPConnect
- **Recuperación de sesión**: El servidor intenta restaurar la sesión previa de WhatsApp al iniciar
- **Dos métodos de autenticación**: Escaneo de código QR O número de teléfono con código de vinculación de 8 dígitos
- **Cola de mensajes**: Últimos 100 mensajes recibidos almacenados en memoria (no persistente)
- **Logging**: Configurable mediante variable de entorno `LOG_LEVEL` (DEBUG, INFO, WARN, ERROR)

### Flujo de Autenticación

1. **Método QR**: `POST /api/auth/start` (body vacío) → `GET /api/auth/qr` → escanear QR
2. **Método Teléfono**: `POST /api/auth/start` (con phoneNumber) → `GET /api/auth/link-code` → verificar código de 8 dígitos en WhatsApp

### Archivos Críticos

- `server/src/services/WhatsAppService.ts` - Gestión del cliente WhatsApp principal (sesión, autenticación, mensajería)
- `server/src/server.ts` - Configuración de app Express, rutas, middleware, manejo de errores
- `server/API_DOCS.md` - Documentación completa de la API REST con ejemplos

## Arquitectura de la App Android (Kotlin + Compose)

### Estructura del Proyecto

```
app/android/app/src/main/java/com/whatsappbulk/sender/
├── data/                    # Capa de datos
│   └── repository/
│       └── AuthRepository.kt
├── domain/                  # Capa de dominio
│   ├── model/              # Modelos de dominio
│   │   ├── Campaign.kt     # Modelo de campaña
│   │   ├── Execution.kt    # Modelo de estado de ejecución
│   │   ├── User.kt
│   │   ├── WhatsAppSession.kt
│   │   └── Result.kt       # Tipo Result sellado
│   └── repository/         # Interfaces de repositorio
│       └── IAuthRepository.kt
├── ui/                      # Capa de presentación
│   ├── screens/
│   │   └── login/
│   │       ├── LoginScreen.kt
│   │       └── LoginViewModel.kt
│   └── theme/              # Tema Material 3
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── di/                      # Inyección de Dependencias (Hilt)
│   └── RepositoryModule.kt
├── MainActivity.kt          # Punto de entrada
└── WhatsAppBulkApp.kt      # Clase Application
```

### Patrón de Arquitectura: MVVM + Clean Architecture

- **Capa de Datos**: Repositorios, clientes API, base de datos local
- **Capa de Dominio**: Modelos, Casos de Uso, interfaces de Repositorio
- **Capa de UI**: Pantallas Compose, ViewModels, navegación

### Stack Tecnológico

- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp
- **Base de datos**: Room + DataStore
- **Async**: Kotlin Coroutines + Flow
- **Background**: WorkManager
- **Seguridad**: EncryptedSharedPreferences

### Pantallas Planificadas (Ver app/WIREFRAMES.md)

1. Splash → Verificación de auto-login
2. Login → Autenticación de usuario
3. WhatsApp Session → Vinculación de teléfono (código de 8 dígitos)
4. Campaign List → Seleccionar campaña
5. Campaign Details → Ver detalles + especificar cantidad
6. Sending → Monitoreo de progreso en tiempo real
7. Dashboard → Estadísticas + historial

### Decisión Clave de Diseño: Modelo Ejecutor

La app **ejecuta** campañas pre-configuradas, NO:
- Crea o edita campañas
- Gestiona listas de contactos
- Configura plantillas de mensajes
- Establece intervalos de demora

Toda la configuración se gestiona mediante un sistema backend externo (aún no implementado). La app:
- Lista campañas disponibles (solo lectura)
- Valida contra cuota diaria
- Ejecuta campaña con cantidad especificada
- Monitorea progreso mediante polling

## Integración con APIs

### API del Server (Termux - localhost:3000)

Ver [server/API_DOCS.md](server/API_DOCS.md) para documentación completa.

**Endpoints clave:**
- `POST /api/auth/start` - Iniciar sesión de WhatsApp (phoneNumber opcional)
- `GET /api/auth/status` - Obtener estado de sesión
- `GET /api/auth/link-code` - Obtener código de vinculación de 8 dígitos
- `POST /api/messages/send` - Enviar mensaje de texto
- `GET /api/messages` - Obtener mensajes recibidos

### API del Backend de Campañas (Externo - NO implementado)

La app Android espera estos endpoints de un sistema externo:

- `GET /api/campaigns` - Listar campañas disponibles
- `GET /api/campaigns/:id` - Obtener detalles de campaña
- `POST /api/campaigns/:id/execute` - Iniciar ejecución (body: `{quantity: number}`)
- `GET /api/executions/:executionId` - Obtener estado de ejecución
- `POST /api/executions/:executionId/pause|resume|cancel` - Controlar ejecución

## Flujo de Trabajo de Desarrollo

### Al trabajar en el servidor:

1. Hacer cambios en archivos TypeScript en `server/src/`
2. Ejecutar `npm run dev` para auto-reload durante desarrollo
3. Ejecutar `npm run build` antes de commitear para verificar compilación TypeScript
4. Probar endpoints API usando curl o Postman (ejemplos en API_DOCS.md)
5. Revisar logs para errores (configurable mediante LOG_LEVEL)

### Al trabajar en la app Android:

1. Abrir `app/android/` en Android Studio
2. Usar Hot Reload en previews de Compose para iteración rápida de UI
3. Seguir patrón MVVM: UI → ViewModel → Repository → API
4. Agregar nuevas dependencias en `app/android/app/build.gradle.kts`
5. Usar Hilt para inyección de dependencias (crear módulos en `di/`)
6. Seguir guías de diseño Material 3 (colores definidos en `ui/theme/`)

### Al agregar nuevas funcionalidades:

1. **Server**: Crear controller → Agregar rutas → Actualizar service → Actualizar types
2. **Android**: Crear modelo de dominio → Interfaz de repositorio → Implementación de repositorio → Caso de uso → ViewModel → Screen

## Archivos Importantes para Referenciar

- `README.md` - Descripción general del proyecto e inicio rápido
- `app/README.md` - Descripción general de la app Android y decisiones tecnológicas
- `app/ARQUITECTURA_ACTUALIZADA.md` - **Crítico**: Explica el modelo ejecutor en detalle
- `app/PLAN_DESARROLLO.md` - Fases de desarrollo y cronograma
- `app/WIREFRAMES.md` - Diseños de pantallas y especificaciones de UI
- `server/API_DOCS.md` - Referencia completa de la API REST con ejemplos de código

## Estrategia de Testing

### Server
- Testing manual con curl/Postman
- Testing de integración con WhatsApp Web real (requiere escanear QR)
- Probar recuperación de sesión reiniciando el servidor

### App Android
- **Pruebas Unitarias**: ViewModels, Casos de Uso, Repositorios (JUnit + MockK)
- **Pruebas de Integración**: Interacciones con API + Base de datos
- **Pruebas de UI**: API de Pruebas de Compose para testing de pantallas
- **Pruebas E2E**: Flujo completo de ejecución de campaña

## Requisitos del Entorno

### Server (Termux)
- Node.js v16+
- npm o yarn
- Termux en Android
- Variables de entorno en `.env`:
  - `PORT` (default: 3000)
  - `LOG_LEVEL` (DEBUG, INFO, WARN, ERROR)

### App Android
- Android Studio Hedgehog+ (2023.1.1+)
- JDK 17
- Android SDK 34+
- Dispositivo mínimo: Android 5.0 (API 21)

## Problemas Comunes

### Server
- **Persistencia de sesión**: Sesiones almacenadas en `server/tokens/` - eliminar para forzar re-autenticación
- **Formato de número de teléfono**: Debe estar en formato `{códigoPaís}{número}` sin +, espacios ni guiones
- **Autenticación QR vs Teléfono**: No se puede cambiar de método a mitad de sesión - debe cerrar sesión primero
- **Cola de mensajes**: Solo en memoria, se limpia al reiniciar servidor

### App Android
- **Seguridad de red**: Android requiere HTTPS o excepción de seguridad para HTTP localhost
- **Corrutinas**: Todas las llamadas de red deben estar en un scope de corrutina
- **Recomposición de Compose**: Usar remember, derivedStateOf para evitar recomposiciones innecesarias
- **Hilt**: Todos los ViewModels deben tener anotación `@HiltViewModel`

## Consideraciones de Seguridad

- La API del servidor **no tiene autenticación** - diseñada solo para localhost
- La app Android usa **EncryptedSharedPreferences** para credenciales
- Tokens de sesión de WhatsApp almacenados en directorio `server/tokens/`
- Minificación ProGuard/R8 habilitada para builds de release
- Nunca commitear archivos `.env` o tokens de sesión

## Flujo de Trabajo Git

Rama actual: `feature/android-app`

Trabajo reciente:
- Setup del proyecto base Android con pantalla de Login
- Implementación del patrón Repository para autenticación
- Tematización Material 3 con colores de WhatsApp (#25D366)

Al hacer commits:
- Usar commits convencionales: `feat:`, `fix:`, `docs:`, `refactor:`
- Compilar y probar antes de commitear
- Para Android: Ejecutar `./gradlew build` para verificar
- Para Server: Ejecutar `npm run build` para verificar
