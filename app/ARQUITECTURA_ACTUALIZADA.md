# Arquitectura Actualizada - App Ejecutora de Campañas

## ⚠️ Cambio Importante en el Enfoque

La aplicación **NO crea campañas**, sino que **ejecuta campañas pre-configuradas** desde un backend externo.

---

## 1. Nuevo Flujo de la Aplicación

### Sistema Completo (3 componentes)

```
┌─────────────────────┐
│  Backend/Panel Web  │ ← Aquí se crean y configuran las campañas
│  (Sistema externo)  │   - Define mensajes
│                     │   - Define contactos
│                     │   - Define intervalos
│                     │   - Establece cuota diaria
└──────────┬──────────┘
           │ API REST
           ↓
┌─────────────────────┐
│   Servidor Termux   │ ← WhatsApp API (ya implementado)
│  (WhatsApp client)  │   - Gestiona sesión WhatsApp
│                     │   - Envía mensajes
└──────────┬──────────┘
           │ localhost:3000
           ↓
┌─────────────────────┐
│   App Android       │ ← **ESTA APP** (ejecutor)
│  (Esta aplicación)  │   - Selecciona campaña
│                     │   - Especifica cantidad
│                     │   - Ejecuta envío
│                     │   - Monitorea progreso
└─────────────────────┘
```

---

## 2. Características Reales de la App

### ✅ Lo que la app SÍ hace:

1. **Autenticación**
   - Login con usuario/contraseña

2. **Gestión de Sesión WhatsApp**
   - Verificar sesión existente
   - Iniciar sesión con número de teléfono
   - Mostrar código de vinculación de 8 dígitos

3. **Selección de Campaña** (NUEVO ENFOQUE)
   - Listar campañas disponibles desde el backend
   - Ver detalles de cada campaña (solo lectura):
     * Nombre de la campaña
     * Cantidad de mensajes configurados
     * Cantidad de contactos
     * Rango de intervalo configurado
   - Ver cuota diaria disponible

4. **Configuración de Ejecución**
   - Seleccionar una campaña
   - Especificar cuántos mensajes enviar (input numérico)
   - Validar contra cuota diaria disponible
   - Preview de parámetros antes de enviar

5. **Ejecución del Envío**
   - Enviar solicitud al backend con:
     * ID de campaña seleccionada
     * Cantidad de mensajes a enviar
   - Monitorear progreso en tiempo real
   - Pausar/Reanudar/Cancelar

6. **Dashboard**
   - Estadísticas de envíos del día
   - Cuota restante
   - Historial de ejecuciones

### ❌ Lo que la app NO hace:

- ❌ Crear mensajes
- ❌ Editar mensajes
- ❌ Agregar/eliminar contactos
- ❌ Importar listas de contactos
- ❌ Configurar intervalos de tiempo
- ❌ Modificar parámetros de campañas

---

## 3. Nuevos Endpoints Necesarios

### Backend de Campañas (Sistema externo)

```typescript
// Listar campañas disponibles
GET /api/campaigns
Response: {
  success: true,
  data: [
    {
      id: "camp-001",
      name: "Campaña Promocional 2024",
      messageCount: 3,
      contactCount: 150,
      delayRange: { min: 5, max: 15 }, // segundos
      dailyQuota: {
        total: 100,
        used: 35,
        remaining: 65
      }
    },
    ...
  ]
}

// Obtener detalles de una campaña
GET /api/campaigns/:id
Response: {
  success: true,
  data: {
    id: "camp-001",
    name: "Campaña Promocional 2024",
    messages: [
      "Hola, tenemos una oferta especial para ti...",
      "Aprovecha 50% de descuento...",
      "Últimas unidades disponibles..."
    ],
    contacts: [
      "+595973159937",
      "+595981234567",
      ...
    ],
    delayRange: { min: 5, max: 15 },
    dailyQuota: {
      total: 100,
      used: 35,
      remaining: 65
    },
    createdAt: "2024-01-15T10:00:00Z"
  }
}

// Solicitar ejecución de campaña
POST /api/campaigns/:id/execute
Body: {
  quantity: 50  // Cuántos mensajes enviar
}
Response: {
  success: true,
  data: {
    executionId: "exec-123",
    campaignId: "camp-001",
    quantity: 50,
    status: "queued"
  }
}

// Obtener estado de ejecución
GET /api/executions/:executionId
Response: {
  success: true,
  data: {
    executionId: "exec-123",
    campaignId: "camp-001",
    status: "running",  // queued | running | paused | completed | cancelled
    progress: {
      total: 50,
      sent: 23,
      failed: 2,
      pending: 25
    },
    currentMessage: {
      contact: "+595973159937",
      message: "Hola, tenemos una oferta...",
      timestamp: "2024-01-15T14:30:25Z"
    }
  }
}

// Pausar/Reanudar ejecución
POST /api/executions/:executionId/pause
POST /api/executions/:executionId/resume

// Cancelar ejecución
POST /api/executions/:executionId/cancel
```

---

## 4. Pantallas Actualizadas

### 4.1 Campaign Selection Screen (NUEVA)

```
┌─────────────────────────┐
│  ← Seleccionar Campaña  │
│                         │
│ 📋 Campañas Disponibles │
│                         │
│ ┌─────────────────────┐ │
│ │ Promoción 2024     ││ │
│ │ 📝 3 mensajes      ││ │
│ │ 📞 150 contactos   ││ │
│ │ ⏱️ 5-15s intervalo ││ │
│ │ 📊 Cuota: 65/100   ││ │
│ │ [Ver detalles →]   ││ │
│ └─────────────────────┘ │
│                         │
│ ┌─────────────────────┐ │
│ │ Black Friday       ││ │
│ │ 📝 4 mensajes      ││ │
│ │ 📞 300 contactos   ││ │
│ │ ⏱️ 10-30s intervalo││ │
│ │ 📊 Cuota: 0/200    ││ │
│ │ [Ver detalles →]   ││ │
│ └─────────────────────┘ │
│                         │
└─────────────────────────┘
```

### 4.2 Campaign Details Screen (NUEVA)

```
┌─────────────────────────┐
│  ← Promoción 2024       │
│                         │
│ 📊 Información          │
│ Mensajes: 3             │
│ Contactos: 150          │
│ Intervalo: 5-15 seg     │
│                         │
│ 📝 Mensajes (preview)   │
│ ┌─────────────────────┐ │
│ │ 1. Hola, tenemos...││ │
│ │ 2. Aprovecha 50%...││ │
│ │ 3. Últimas unid... ││ │
│ └─────────────────────┘ │
│                         │
│ 📞 Contactos (preview)  │
│ ┌─────────────────────┐ │
│ │ +595973159937      ││ │
│ │ +595981234567      ││ │
│ │ ... (148 más)      ││ │
│ └─────────────────────┘ │
│                         │
│ 📊 Cuota Diaria         │
│ ┌─────────────────────┐ │
│ │ Disponible: 65/100 ││ │
│ │ Usada hoy: 35      ││ │
│ │ [████████░░] 35%   ││ │
│ └─────────────────────┘ │
│                         │
│ ¿Cuántos mensajes enviar?│
│ ┌─────────────────────┐ │
│ │      [  50  ]      ││ │
│ │   (máximo: 65)     ││ │
│ └─────────────────────┘ │
│                         │
│ ┌─────────────────────┐ │
│ │ Iniciar Envío →    ││ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

**Validaciones:**
- Cantidad > 0
- Cantidad <= cuota disponible
- Cantidad <= contactos disponibles

### 4.3 Sending Screen (actualizada)

```
┌─────────────────────────┐
│  ← Enviando             │
│                         │
│  📋 Campaña: Promoción  │
│      2024               │
│                         │
│  Progreso General       │
│  ┌─────────────────────┐│
│  │ ████████░░░░░░░░   ││
│  │ 23 / 50 (46%)      ││
│  └─────────────────────┘│
│                         │
│  📊 Estadísticas        │
│  ✅ Enviados: 23        │
│  ⏳ Pendientes: 25      │
│  ❌ Fallidos: 2         │
│                         │
│  📱 Enviando ahora:     │
│  +595973159937          │
│  "Hola, tenemos una..." │
│                         │
│  ⏱️ Próximo en: 12s    │
│                         │
│  ┌─────────────────────┐│
│  │ ⏸️ Pausar          ││
│  └─────────────────────┘│
│  ┌─────────────────────┐│
│  │ ❌ Cancelar        ││
│  └─────────────────────┘│
│                         │
│  📋 Últimos envíos ▼    │
│  ┌─────────────────────┐│
│  │ ✅ +5959731... Msg1││
│  │ ✅ +5959812... Msg2││
│  │ ❌ +5959823... Error││
│  └─────────────────────┘│
└─────────────────────────┘
```

### 4.4 Dashboard Screen (actualizada)

```
┌─────────────────────────┐
│  ← Dashboard            │
│                         │
│  📊 Hoy (20/10/2024)    │
│  ┌─────────────────────┐│
│  │ Enviados: 35       ││
│  │ Cuota: 35/100      ││
│  │ [███░░░░░░░] 35%   ││
│  └─────────────────────┘│
│                         │
│  📋 Última Ejecución    │
│  ┌─────────────────────┐│
│  │ Promoción 2024     ││
│  │ Total: 50 msgs     ││
│  │ ✅ Exitosos: 48    ││
│  │ ❌ Fallidos: 2     ││
│  │ ⏱️ Duración: 12m   ││
│  └─────────────────────┘│
│                         │
│  📜 Historial           │
│  ┌─────────────────────┐│
│  │ 20/10 14:00 - 50  ││
│  │ Promoción 2024     ││
│  │                    ││
│  │ 20/10 10:00 - 30  ││
│  │ Black Friday       ││
│  │                    ││
│  │ 19/10 15:00 - 100 ││
│  │ Promoción 2024     ││
│  └─────────────────────┘│
│                         │
│  ┌─────────────────────┐│
│  │ + Nueva Ejecución  ││
│  └─────────────────────┘│
└─────────────────────────┘
```

---

## 5. Arquitectura de Datos Actualizada

### 5.1 Modelos de Dominio

```kotlin
// Campaign (desde backend)
data class Campaign(
    val id: String,
    val name: String,
    val messages: List<String>,         // Solo lectura
    val contacts: List<String>,         // Solo lectura
    val delayRange: DelayRange,         // Solo lectura
    val dailyQuota: DailyQuota,
    val createdAt: Long
)

data class DelayRange(
    val min: Int,  // segundos
    val max: Int   // segundos
)

data class DailyQuota(
    val total: Int,
    val used: Int,
    val remaining: Int
)

// Execution (estado del envío)
data class Execution(
    val id: String,
    val campaignId: String,
    val campaignName: String,
    val quantity: Int,
    val status: ExecutionStatus,
    val progress: ExecutionProgress,
    val currentMessage: CurrentMessage?,
    val startedAt: Long
)

enum class ExecutionStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, CANCELLED
}

data class ExecutionProgress(
    val total: Int,
    val sent: Int,
    val failed: Int,
    val pending: Int
)

data class CurrentMessage(
    val contact: String,
    val message: String,
    val timestamp: Long
)
```

### 5.2 Repositorios Actualizados

```kotlin
interface CampaignRepository {
    suspend fun getCampaigns(): Result<List<Campaign>>
    suspend fun getCampaignById(id: String): Result<Campaign>
}

interface ExecutionRepository {
    suspend fun startExecution(
        campaignId: String,
        quantity: Int
    ): Result<Execution>

    suspend fun getExecutionStatus(executionId: String): Result<Execution>
    suspend fun pauseExecution(executionId: String): Result<Unit>
    suspend fun resumeExecution(executionId: String): Result<Unit>
    suspend fun cancelExecution(executionId: String): Result<Unit>

    // Local storage
    suspend fun saveExecution(execution: Execution)
    suspend fun getTodaysExecutions(): List<Execution>
    suspend fun getExecutionHistory(): List<Execution>
}
```

### 5.3 Use Cases Actualizados

```kotlin
class GetAvailableCampaignsUseCase @Inject constructor(
    private val campaignRepository: CampaignRepository
) {
    suspend operator fun invoke(): Result<List<Campaign>> {
        return campaignRepository.getCampaigns()
    }
}

class StartCampaignExecutionUseCase @Inject constructor(
    private val executionRepository: ExecutionRepository
) {
    suspend operator fun invoke(
        campaignId: String,
        quantity: Int
    ): Result<Execution> {
        // Validaciones
        if (quantity <= 0) {
            return Result.Error("La cantidad debe ser mayor a 0")
        }

        return executionRepository.startExecution(campaignId, quantity)
    }
}

class MonitorExecutionUseCase @Inject constructor(
    private val executionRepository: ExecutionRepository
) {
    // Polling cada X segundos
    operator fun invoke(executionId: String): Flow<Execution> = flow {
        while (true) {
            val result = executionRepository.getExecutionStatus(executionId)
            if (result is Result.Success) {
                emit(result.data)

                // Si completado o cancelado, dejar de hacer polling
                if (result.data.status in listOf(
                    ExecutionStatus.COMPLETED,
                    ExecutionStatus.CANCELLED
                )) {
                    break
                }
            }
            delay(3000) // Poll cada 3 segundos
        }
    }
}
```

---

## 6. Flujo de Navegación Actualizado

```
Splash → Login → WhatsApp Session → Campaign List → Campaign Details → Sending → Dashboard
            ↓                                                                ↑
      (auto-login)                                                    (nueva ejecución)
```

**Pantallas:**
1. **Splash**: Verificar sesión
2. **Login**: Autenticación
3. **WhatsApp Session**: Vincular WhatsApp
4. **Campaign List**: Seleccionar campaña (NUEVO)
5. **Campaign Details**: Ver detalles + especificar cantidad (NUEVO)
6. **Sending**: Monitor de progreso
7. **Dashboard**: Estadísticas e historial

---

## 7. APIs Necesarias

### 7.1 Backend de Campañas (nuevo - sistema externo)

```kotlin
interface CampaignApi {
    @GET("/api/campaigns")
    suspend fun getCampaigns(): Response<ApiResponse<List<CampaignDto>>>

    @GET("/api/campaigns/{id}")
    suspend fun getCampaignById(@Path("id") id: String): Response<ApiResponse<CampaignDto>>

    @POST("/api/campaigns/{id}/execute")
    suspend fun executeCampaign(
        @Path("id") id: String,
        @Body request: ExecuteCampaignRequest
    ): Response<ApiResponse<ExecutionDto>>

    @GET("/api/executions/{id}")
    suspend fun getExecutionStatus(
        @Path("id") id: String
    ): Response<ApiResponse<ExecutionDto>>

    @POST("/api/executions/{id}/pause")
    suspend fun pauseExecution(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("/api/executions/{id}/resume")
    suspend fun resumeExecution(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("/api/executions/{id}/cancel")
    suspend fun cancelExecution(@Path("id") id: String): Response<ApiResponse<Unit>>
}

data class ExecuteCampaignRequest(
    val quantity: Int
)
```

### 7.2 WhatsApp API (existente - Termux)

```kotlin
interface WhatsAppApi {
    @POST("/api/auth/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<ApiResponse<Unit>>

    @GET("/api/auth/status")
    suspend fun getStatus(): Response<ApiResponse<SessionStatusDto>>

    @GET("/api/auth/link-code")
    suspend fun getLinkCode(): Response<ApiResponse<LinkCodeDto>>

    @POST("/api/messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<ApiResponse<MessageDto>>
}
```

---

## 8. Plan de Desarrollo Actualizado

### Fase 1: Setup y Autenticación (1 semana)
- ✅ Sin cambios

### Fase 2: Sesión WhatsApp (1 semana)
- ✅ Sin cambios

### Fase 3: Selección de Campaña (1 semana) **← ACTUALIZADO**
**Tareas:**
1. Implementar CampaignListScreen
2. API: Listar campañas (GET /api/campaigns)
3. Implementar CampaignDetailsScreen
4. API: Detalles de campaña (GET /api/campaigns/:id)
5. UI: Input de cantidad con validación
6. Validar contra cuota diaria
7. Testing: Campaign selection flow

**Entregables:**
- Pantalla de listado de campañas
- Pantalla de detalles con preview
- Validación de cuota diaria

### Fase 4: Ejecución y Monitoreo (1.5 semanas) **← ACTUALIZADO**
**Tareas:**
1. API: Iniciar ejecución (POST /api/campaigns/:id/execute)
2. Implementar SendingScreen con polling
3. API: Estado de ejecución (GET /api/executions/:id)
4. WorkManager: Polling en background
5. UI: Progreso en tiempo real
6. API: Pausar/Reanudar/Cancelar
7. Notificaciones de progreso
8. Testing: Execution monitoring

**Entregables:**
- Ejecución de campaña funcional
- Monitoreo en tiempo real
- Controles de pausa/cancelación

### Fase 5: Dashboard y Pulido (1 semana)
- ✅ Sin cambios (ajustar a nueva estructura)

**Total**: 5.5 semanas (110 horas)

---

## 9. Ejemplo de Flujo Completo

```
1. Usuario abre app → Auto-login
2. Verificar sesión WhatsApp → Activa
3. Ver lista de campañas disponibles
   - "Promoción 2024" (cuota: 65/100)
   - "Black Friday" (cuota: 200/200)
4. Seleccionar "Promoción 2024"
5. Ver detalles:
   - 3 mensajes
   - 150 contactos
   - Intervalo 5-15s
   - Cuota disponible: 65
6. Especificar cantidad: 50 mensajes
7. Confirmar → Iniciar ejecución
8. Monitorear progreso:
   - 23/50 enviados
   - 2 fallidos
   - Próximo en 12s
9. Completado: 48 exitosos, 2 fallidos
10. Ver dashboard con estadísticas actualizadas
```

---

## 10. Consideraciones Importantes

### Backend de Campañas

**La app asume que existe un backend separado que:**
- Gestiona la creación de campañas
- Almacena mensajes y contactos
- Controla cuotas diarias por usuario
- Coordina el envío a través del servidor Termux
- Registra historial de ejecuciones

**Este backend NO está implementado aún.** Necesita ser desarrollado por separado o integrado con un sistema existente.

### Sincronización

La app debe sincronizar con 2 APIs:
1. **Backend de Campañas** → Obtener campañas y ejecutar
2. **Servidor Termux (WhatsApp)** → Gestionar sesión WhatsApp

### Cuota Diaria

La cuota diaria se gestiona en el backend, no en la app. La app solo:
- Muestra la cuota disponible
- Valida que la cantidad solicitada no exceda la cuota
- No actualiza la cuota (lo hace el backend)

---

Este enfoque hace de la app un **ejecutor** en lugar de un **configurador**, lo cual simplifica la UX y mantiene la configuración centralizada en el backend.
