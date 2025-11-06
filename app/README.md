# App Android - WhatsApp Bulk Sender

Aplicación Android nativa que se conecta al servidor WhatsApp API (Termux) para enviar mensajes masivos de forma programada.

## 📋 Documentación

- **[Arquitectura Actualizada](ARQUITECTURA_ACTUALIZADA.md)**: ⭐ **LEER PRIMERO** - Enfoque correcto de la app
- **[Plan de Desarrollo](PLAN_DESARROLLO.md)**: Tecnologías, arquitectura, fases y estimaciones
- **[Wireframes](WIREFRAMES.md)**: Diseño de pantallas y componentes UI

## 🎯 Características Principales

### 1. Autenticación
- Login con usuario/contraseña
- Almacenamiento seguro de credenciales
- Auto-login en siguientes aperturas

### 2. Gestión de Sesión WhatsApp
- Verificar sesión existente en servidor
- Iniciar sesión con número de teléfono
- Mostrar código de vinculación de 8 dígitos
- Indicador de estado de conexión

### 3. Selección de Campaña ⭐ **ACTUALIZADO**
- **Listar campañas pre-configuradas** desde el backend
- Ver detalles de cada campaña (solo lectura):
  * Nombre de la campaña
  * Mensajes configurados (preview)
  * Lista de contactos (preview)
  * Rango de intervalo configurado
- Verificar **cuota diaria disponible**
- **Especificar cuántos mensajes enviar** (input numérico)
- Validar contra límite diario

### 4. Ejecución de Campaña ⭐ **ACTUALIZADO**
- **Ejecutar campañas pre-configuradas** (NO crearlas)
- Asignación cíclica automática (configurada en backend)
- Intervalos aleatorios (según configuración del backend)
- Envío en background con WorkManager
- Monitoreo en tiempo real
- Controles: Pausar, Reanudar, Cancelar
- Notificaciones de progreso

### 5. Dashboard y Estadísticas
- Resumen de última ejecución
- Cuota diaria restante
- Estadísticas del día
- Historial de ejecuciones anteriores

## 🛠️ Tecnología Seleccionada

### Stack Principal: Kotlin + Jetpack Compose

**Por qué Compose:**
- ✅ Estándar oficial de Google (2024)
- ✅ UI declarativa moderna (menos código)
- ✅ Hot Reload para desarrollo rápido
- ✅ Material 3 Design incluido
- ✅ Mejor performance y testing
- ✅ Futuro del desarrollo Android

### Arquitectura: MVVM + Clean Architecture

```
📁 app/
├── data/           # APIs, Database, Repositories
├── domain/         # Modelos, Use Cases
└── ui/             # Screens, ViewModels, Compose
```

### Librerías Clave

- **UI**: Jetpack Compose + Material 3
- **Networking**: Retrofit + OkHttp
- **DI**: Hilt
- **Database**: Room + DataStore
- **Async**: Kotlin Coroutines + Flow
- **Background**: WorkManager
- **Security**: EncryptedSharedPreferences

## 📱 Pantallas

1. **Splash** → Verificación de sesión
2. **Login** → Autenticación de usuario
3. **WhatsApp Session** → Vinculación con WhatsApp
4. **Campaign List** → Listar campañas disponibles ⭐ **NUEVO**
5. **Campaign Details** → Ver detalles + especificar cantidad ⭐ **NUEVO**
6. **Sending** → Progreso de ejecución en tiempo real
7. **Dashboard** → Estadísticas e historial

## ⏱️ Plan de Desarrollo (5.5 semanas)

| Fase | Duración | Entregable |
|------|----------|------------|
| **1. Setup y Autenticación** | 1 semana | Login funcional |
| **2. Sesión WhatsApp** | 1 semana | Vinculación con código |
| **3. Selección de Campaña** ⭐ | 1 semana | Listado + detalles + validación |
| **4. Ejecución y Monitoreo** ⭐ | 1.5 semanas | Ejecutar y monitorear |
| **5. Dashboard y Pulido** | 1 semana | App completa |

**Total estimado**: 110 horas de desarrollo

## 🚀 Próximos Pasos

1. ✅ **Documentación completa** (PLAN_DESARROLLO.md, WIREFRAMES.md)
2. 📝 **Crear proyecto Android**: Android Studio → Empty Compose Activity
3. 🔧 **Configurar dependencias**: Gradle, Hilt, Retrofit
4. 📐 **Definir modelos de datos**: DTOs y entidades
5. 💻 **Fase 1**: Implementar Login + Auth

## 📚 Recursos

### Documentación Oficial
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt DI](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room Database](https://developer.android.com/training/data-storage/room)

### Tutoriales
- [Compose Pathway](https://developer.android.com/courses/pathways/compose)
- [Android Developers Codelabs](https://developer.android.com/codelabs)
- [MVVM Architecture Guide](https://developer.android.com/topic/architecture)

### Proyectos de Referencia
- [Now in Android (Google)](https://github.com/android/nowinandroid)
- [Compose Samples](https://github.com/android/compose-samples)

## 🔐 Consideraciones de Seguridad

- ✅ Credenciales cifradas (EncryptedSharedPreferences)
- ✅ Conexión HTTPS al servidor (o HTTP solo en red local)
- ✅ Validación de inputs
- ✅ Ofuscación con ProGuard/R8
- ✅ Permisos mínimos necesarios

## 🧪 Testing Strategy

- **Unit Tests**: ViewModels, Use Cases, Repositories
- **Integration Tests**: API + Database
- **UI Tests**: Compose Test API
- **E2E Tests**: Flujo completo de campaña

## ⭐ Enfoque de la Aplicación

### La app es un **EJECUTOR**, NO un **CREADOR**

```
┌──────────────────┐
│ Backend/Panel Web│ ← Aquí se CREAN campañas
│ (Sistema externo)│   - Define mensajes
│                  │   - Define contactos
│                  │   - Define intervalos
│                  │   - Establece cuota diaria
└────────┬─────────┘
         │ API REST
         ↓
┌──────────────────┐
│ Servidor Termux  │ ← WhatsApp API (localhost:3000)
│ (WhatsApp client)│   - Gestiona sesión WhatsApp
│                  │   - Envía mensajes
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│  App Android     │ ← **ESTA APP** (ejecutor)
│ (Esta aplicación)│   - Selecciona campaña
│                  │   - Especifica cantidad
│                  │   - Ejecuta envío
│                  │   - Monitorea progreso
└──────────────────┘
```

### Flujo de Uso

1. Usuario abre app → Auto-login
2. Verificar sesión WhatsApp → Activa
3. Ver lista de campañas disponibles
   - "Promoción 2024" (cuota: 65/100)
4. Seleccionar "Promoción 2024"
5. Ver detalles:
   - 3 mensajes (solo lectura)
   - 150 contactos (solo lectura)
   - Intervalo 5-15s (solo lectura)
   - Cuota disponible: 65
6. **Especificar cantidad: 50 mensajes**
7. Confirmar → Iniciar ejecución
8. Monitorear progreso: 23/50 enviados
9. Completado: 48 exitosos, 2 fallidos

## 🎨 Diseño UI

### Paleta de Colores (Material 3)
- **Primary**: #25D366 (Verde WhatsApp)
- **Secondary**: #34B7F1 (Azul WhatsApp)
- **Background**: #F7F8FA (Gris claro)
- **Error**: #FF5252 (Rojo)

### Tipografía
- Material 3 default (Roboto)
- Tamaños semánticos (displayLarge, bodyMedium, etc.)

## 🔌 Integración con APIs

### Backend de Campañas (Sistema externo - nuevo) ⭐

```kotlin
// Listar campañas disponibles
GET /api/campaigns

// Obtener detalles de campaña
GET /api/campaigns/:id

// Ejecutar campaña
POST /api/campaigns/:id/execute
Body: { quantity: 50 }

// Monitorear ejecución
GET /api/executions/:executionId

// Controles
POST /api/executions/:executionId/pause
POST /api/executions/:executionId/resume
POST /api/executions/:executionId/cancel
```

### Servidor Termux (WhatsApp API - existente)

```kotlin
// Autenticación WhatsApp
POST /api/auth/start { phoneNumber?: string }
GET  /api/auth/status
GET  /api/auth/link-code
POST /api/auth/close

// Envío de mensajes
POST /api/messages/send { phone, message }
```

### Modelo de Datos ⭐ Actualizado

```kotlin
// Campaña (desde backend - solo lectura)
data class Campaign(
    val id: String,
    val name: String,
    val messages: List<String>,         // Solo lectura
    val contacts: List<String>,         // Solo lectura
    val delayRange: DelayRange,         // Solo lectura
    val dailyQuota: DailyQuota,
    val createdAt: Long
)

data class DailyQuota(
    val total: Int,
    val used: Int,
    val remaining: Int
)

// Ejecución (estado del envío)
data class Execution(
    val id: String,
    val campaignId: String,
    val quantity: Int,
    val status: ExecutionStatus,
    val progress: ExecutionProgress
)

enum class ExecutionStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, CANCELLED
}
```

## 📋 Requisitos Previos

### Backend (3 componentes necesarios)

1. **Backend de Campañas** (Sistema externo - por desarrollar)
   - Gestiona creación de campañas
   - Controla cuotas diarias
   - Coordina ejecuciones

2. **Servidor Termux** (WhatsApp API - ya implementado)
   - Corriendo en `http://127.0.0.1:3000`
   - Ver [../server/API_DOCS.md](../server/API_DOCS.md)

3. **App Android** (Esta aplicación)
   - Android Studio Hedgehog+ (2023.1.1)
   - JDK 17
   - Android SDK 34+
   - Dispositivo/Emulador con Android 5.0+ (API 21+)

## ⚠️ Importante

**El Backend de Campañas aún no está implementado.** La app asume que existe un sistema separado que:
- Gestiona campañas (CRUD)
- Almacena mensajes y contactos
- Controla cuotas diarias
- Coordina envíos a través del servidor Termux

Este backend debe ser desarrollado por separado o integrarse con un sistema existente.

---

**Estado**: 📋 Planificación completa - Listo para desarrollo

**Tecnología**: Kotlin + Jetpack Compose

**Estimación**: 5.5 semanas (110 horas)

Ver [PLAN_DESARROLLO.md](PLAN_DESARROLLO.md) para detalles completos.
