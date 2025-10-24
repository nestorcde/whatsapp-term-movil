# Plan de Desarrollo - App Android WhatsApp Bulk Sender

## 1. Resumen del Proyecto

Aplicación Android nativa que **ejecuta campañas pre-configuradas** de mensajes masivos vía WhatsApp. Las campañas (mensajes, contactos, parámetros) se definen en un backend separado; la app solo permite seleccionarlas, especificar la cantidad de mensajes a enviar y monitorear el progreso.

### Características Principales

1. **Autenticación de Usuario**: Login con usuario/contraseña
2. **Gestión de Sesión WhatsApp**:
   - Verificar sesión existente
   - Iniciar sesión con número de teléfono
   - Ingresar código de vinculación de 8 dígitos
3. **Selección de Campaña**:
   - Listar campañas disponibles desde el backend
   - Ver detalles de cada campaña (solo lectura)
   - Verificar cuota diaria disponible
4. **Configuración de Ejecución**:
   - Seleccionar una campaña
   - Especificar cuántos mensajes enviar (respetando límite diario)
   - Preview de parámetros antes de ejecutar
5. **Ejecución de Campaña**:
   - Enviar solicitud de ejecución al backend
   - Monitorear progreso en tiempo real con polling
   - Pausar/Reanudar/Cancelar ejecución
6. **Dashboard**: Estadísticas del día, cuota restante, historial de ejecuciones

---

## 2. Stack Tecnológico: Kotlin + Jetpack Compose

### Por qué esta tecnología

- ✅ **Estándar Oficial Google 2024**: Compose es el futuro del desarrollo Android
- ✅ **UI Declarativa Moderna**: Similar a React, menos código que XML
- ✅ **Productividad**: 40% menos código boilerplate
- ✅ **Hot Reload**: Previews en tiempo real en Android Studio
- ✅ **Performance**: Recomposición inteligente optimizada
- ✅ **Testing**: Compose Test API fácil de usar
- ✅ **Material 3**: Design System moderno incluido
- ✅ **Comunidad Activa**: Amplia documentación y soporte

### Stack Completo

- **Lenguaje**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Arquitectura**: MVVM con Clean Architecture
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow
- **DI**: Hilt (Dependency Injection)
- **Navegación**: Compose Navigation
- **Persistencia Local**: Room Database + DataStore
- **Background Tasks**: WorkManager
- **Seguridad**: EncryptedSharedPreferences

---

## 3. Arquitectura Clean Architecture + MVVM

### 3.1 Capas de la Aplicación

```
app/
├── data/               # Capa de datos
│   ├── remote/         # API clients (Retrofit)
│   │   ├── CampaignApi.kt        # Backend de campañas
│   │   ├── WhatsAppApi.kt        # Servidor Termux
│   │   └── dto/                  # Data Transfer Objects
│   │       ├── CampaignDto.kt
│   │       ├── ExecutionDto.kt
│   │       └── SessionStatusDto.kt
│   ├── local/          # Base de datos local
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── ExecutionDao.kt
│   │   │   └── UserDao.kt
│   │   └── entities/
│   │       ├── ExecutionEntity.kt
│   │       └── UserEntity.kt
│   └── repository/     # Implementaciones de repositorios
│       ├── AuthRepository.kt
│       ├── WhatsAppRepository.kt
│       ├── CampaignRepository.kt
│       └── ExecutionRepository.kt
│
├── domain/             # Lógica de negocio
│   ├── model/          # Modelos de dominio
│   │   ├── User.kt
│   │   ├── Campaign.kt
│   │   ├── Execution.kt
│   │   ├── DailyQuota.kt
│   │   └── WhatsAppSession.kt
│   ├── repository/     # Interfaces de repositorios
│   │   ├── IAuthRepository.kt
│   │   ├── IWhatsAppRepository.kt
│   │   ├── ICampaignRepository.kt
│   │   └── IExecutionRepository.kt
│   └── usecase/        # Casos de uso
│       ├── LoginUseCase.kt
│       ├── StartWhatsAppSessionUseCase.kt
│       ├── GetAvailableCampaignsUseCase.kt
│       ├── StartCampaignExecutionUseCase.kt
│       └── MonitorExecutionUseCase.kt
│
└── ui/                 # Presentación (Jetpack Compose)
    ├── navigation/     # Navegación
    │   └── AppNavigation.kt
    ├── theme/          # Tema de la app
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    └── screens/        # Pantallas
        ├── splash/
        │   └── SplashScreen.kt
        ├── login/
        │   ├── LoginScreen.kt
        │   └── LoginViewModel.kt
        ├── whatsapp/
        │   ├── WhatsAppSessionScreen.kt
        │   └── WhatsAppSessionViewModel.kt
        ├── campaigns/
        │   ├── list/
        │   │   ├── CampaignListScreen.kt
        │   │   └── CampaignListViewModel.kt
        │   ├── details/
        │   │   ├── CampaignDetailsScreen.kt
        │   │   └── CampaignDetailsViewModel.kt
        │   └── components/
        │       ├── CampaignCard.kt
        │       ├── MessagePreviewCard.kt
        │       └── QuotaIndicator.kt
        ├── execution/
        │   ├── SendingScreen.kt
        │   └── SendingViewModel.kt
        └── dashboard/
            ├── DashboardScreen.kt
            └── DashboardViewModel.kt
```

### 3.2 Flujo de Navegación

```
Splash → Login → WhatsApp Session → Campaign List → Campaign Details → Sending → Dashboard
   ↓                                                                         ↑
(auto-login)                                                         (nueva ejecución)
```

### 3.3 Gestión de Estado (MVVM + StateFlow)

```kotlin
class CampaignDetailsViewModel @Inject constructor(
    private val getCampaignUseCase: GetCampaignUseCase,
    private val startExecutionUseCase: StartCampaignExecutionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampaignDetailsUiState())
    val uiState: StateFlow<CampaignDetailsUiState> = _uiState.asStateFlow()

    fun loadCampaign(campaignId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            when (val result = getCampaignUseCase(campaignId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            campaign = result.data,
                            maxQuantity = result.data.dailyQuota.remaining
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(loading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun startExecution(quantity: Int) {
        viewModelScope.launch {
            when (val result = startExecutionUseCase(campaignId, quantity)) {
                is Result.Success -> navigateToSending(result.data.id)
                is Result.Error -> showError(result.message)
            }
        }
    }
}
```

---

## 4. Plan de Desarrollo por Fases (5.5 semanas)

### Fase 1: Setup y Autenticación (Semana 1 - 20h)

**Tareas:**
1. Crear proyecto Android con Kotlin + Compose
2. Configurar Hilt para Dependency Injection
3. Configurar Retrofit para conexión con backends
4. Implementar LoginScreen (Compose)
5. Implementar AuthRepository con EncryptedSharedPreferences
6. Implementar LoginViewModel con StateFlow
7. Testing: Login flow

**Entregables:**
- ✅ App con login funcional
- ✅ Token guardado de forma segura
- ✅ Auto-login en siguiente apertura
- ✅ Arquitectura base (data/domain/ui)

**Componentes clave:**
```kotlin
// LoginScreen.kt
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column {
        TextField(
            value = uiState.username,
            onValueChange = { viewModel.updateUsername(it) },
            label = { Text("Usuario") }
        )
        TextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = { viewModel.login() }) {
            Text("Iniciar Sesión")
        }
    }
}
```

---

### Fase 2: Gestión de Sesión WhatsApp (Semana 2 - 20h)

**Tareas:**
1. Implementar WhatsAppSessionScreen con estados
2. API: Verificar sesión existente (GET /api/auth/status)
3. API: Iniciar sesión con teléfono (POST /api/auth/start)
4. API: Obtener código de vinculación (GET /api/auth/link-code)
5. UI: Mostrar código de vinculación con countdown
6. Implementar polling para verificar conexión
7. Manejo de estados: Checking, NotConnected, LinkCodeReady, Connected
8. Testing: WhatsApp session flow

**Entregables:**
- ✅ Pantalla de vinculación WhatsApp funcional
- ✅ Código de 8 dígitos con countdown
- ✅ Indicador visual del estado de conexión
- ✅ Manejo de errores (timeout, código expirado)

**Estados de la pantalla:**
```kotlin
sealed class WhatsAppSessionState {
    object Checking : WhatsAppSessionState()
    object NotConnected : WhatsAppSessionState()
    data class LinkCodeReady(val code: String, val expiresIn: Int) : WhatsAppSessionState()
    data class Connected(val phoneNumber: String) : WhatsAppSessionState()
    data class Error(val message: String) : WhatsAppSessionState()
}
```

---

### Fase 3: Selección de Campaña (Semana 3 - 20h)

**Tareas:**
1. Implementar CampaignListScreen
2. API: Listar campañas (GET /api/campaigns)
3. UI: Lista de campañas con LazyColumn
4. Implementar CampaignDetailsScreen
5. API: Detalles de campaña (GET /api/campaigns/:id)
6. UI: Preview de mensajes (solo lectura)
7. UI: Preview de contactos (solo lectura)
8. UI: Input de cantidad con validación
9. UI: Indicador de cuota diaria
10. Validar contra cuota disponible
11. Room Database: Cache de campañas
12. Testing: Campaign selection y validation

**Entregables:**
- ✅ Pantalla de listado de campañas
- ✅ Pantalla de detalles con preview
- ✅ Input de cantidad validado
- ✅ Validación de cuota diaria
- ✅ Cache local de campañas

**Componentes clave:**
```kotlin
// CampaignCard.kt
@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(padding = 16.dp) {
            Text(campaign.name, style = MaterialTheme.typography.titleMedium)
            Spacer(height = 8.dp)
            Row {
                Icon(Icons.Default.Message)
                Text("${campaign.messages.size} mensajes")
                Spacer(width = 16.dp)
                Icon(Icons.Default.People)
                Text("${campaign.contacts.size} contactos")
            }
            Spacer(height = 8.dp)
            QuotaIndicator(
                used = campaign.dailyQuota.used,
                total = campaign.dailyQuota.total
            )
        }
    }
}
```

---

### Fase 4: Ejecución y Monitoreo (Semana 4-5 - 30h)

**Tareas:**
1. API: Iniciar ejecución (POST /api/campaigns/:id/execute)
2. Implementar SendingScreen con progreso
3. API: Estado de ejecución (GET /api/executions/:id)
4. Implementar polling cada 3 segundos con Flow
5. WorkManager: Polling en background
6. UI: Barra de progreso animada
7. UI: Estadísticas en tiempo real (enviados/fallidos/pendientes)
8. UI: Mensaje actual siendo enviado
9. UI: Countdown hasta próximo envío
10. API: Pausar/Reanudar/Cancelar ejecución
11. UI: Botones de control (Pausar/Reanudar/Cancelar)
12. Notificaciones de progreso
13. Room: Guardar historial de ejecuciones
14. Testing: Execution monitoring y controles

**Entregables:**
- ✅ Ejecución de campaña funcional
- ✅ Monitoreo en tiempo real con polling
- ✅ Controles de pausa/cancelación
- ✅ Progreso continúa en background
- ✅ Notificaciones

**Polling con Flow:**
```kotlin
class MonitorExecutionUseCase @Inject constructor(
    private val executionRepository: ExecutionRepository
) {
    operator fun invoke(executionId: String): Flow<Execution> = flow {
        while (true) {
            val result = executionRepository.getExecutionStatus(executionId)
            if (result is Result.Success) {
                emit(result.data)

                // Dejar de hacer polling si completado o cancelado
                if (result.data.status in listOf(
                    ExecutionStatus.COMPLETED,
                    ExecutionStatus.CANCELLED
                )) {
                    break
                }
            }
            delay(3000) // Poll cada 3 segundos
        }
    }.flowOn(Dispatchers.IO)
}
```

---

### Fase 5: Dashboard y Pulido (Semana 6 - 20h)

**Tareas:**
1. Implementar DashboardScreen
2. UI: Estadísticas del día (enviados, cuota restante)
3. UI: Resumen de última ejecución
4. UI: Historial de ejecuciones con LazyColumn
5. Room: Consultas de estadísticas
6. Implementar Settings (opcional)
7. UI/UX: Animaciones y transiciones
8. Testing: E2E completo
9. Optimización de performance
10. Preparar para release

**Entregables:**
- ✅ App completa y funcional
- ✅ Dashboard con estadísticas
- ✅ Historial de ejecuciones
- ✅ App lista para producción

---

## 5. Tecnologías y Librerías Específicas

### build.gradle (Project level)
```gradle
buildscript {
    ext.kotlin_version = "1.9.20"
    ext.hilt_version = "2.50"

    dependencies {
        classpath "com.google.dagger:hilt-android-gradle-plugin:$hilt_version"
    }
}
```

### build.gradle (App level)
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

android {
    namespace 'com.example.whatsappbulksender'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.whatsappbulksender"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.4'
    }

    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    // Compose BOM
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    debugImplementation 'androidx.compose.ui:ui-tooling'

    // Activity Compose
    implementation 'androidx.activity:activity-compose:1.8.2'

    // Navigation Compose
    implementation 'androidx.navigation:navigation-compose:2.7.7'

    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'

    // Hilt
    implementation "com.google.dagger:hilt-android:2.50"
    kapt "com.google.dagger:hilt-compiler:2.50"
    implementation 'androidx.hilt:hilt-navigation-compose:1.2.0'

    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'

    // DataStore
    implementation 'androidx.datastore:datastore-preferences:1.0.0'

    // WorkManager
    implementation 'androidx.work:work-runtime-ktx:2.9.0'

    // Security Crypto
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
}
```

---

## 6. APIs Necesarias

### 6.1 Backend de Campañas (Sistema externo)

```kotlin
interface CampaignApi {
    @GET("/api/campaigns")
    suspend fun getCampaigns(): Response<ApiResponse<List<CampaignDto>>>

    @GET("/api/campaigns/{id}")
    suspend fun getCampaignById(
        @Path("id") id: String
    ): Response<ApiResponse<CampaignDto>>

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
    suspend fun pauseExecution(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @POST("/api/executions/{id}/resume")
    suspend fun resumeExecution(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @POST("/api/executions/{id}/cancel")
    suspend fun cancelExecution(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}

data class ExecuteCampaignRequest(
    val quantity: Int
)
```

### 6.2 WhatsApp API (Servidor Termux)

```kotlin
interface WhatsAppApi {
    @POST("/api/auth/start")
    suspend fun startSession(
        @Body request: StartSessionRequest
    ): Response<ApiResponse<Unit>>

    @GET("/api/auth/status")
    suspend fun getStatus(): Response<ApiResponse<SessionStatusDto>>

    @GET("/api/auth/link-code")
    suspend fun getLinkCode(): Response<ApiResponse<LinkCodeDto>>

    @POST("/api/auth/close")
    suspend fun closeSession(): Response<ApiResponse<Unit>>
}
```

---

## 7. Modelos de Dominio

```kotlin
// Campaign.kt
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
) {
    val percentage: Float get() = if (total > 0) used.toFloat() / total else 0f
}

// Execution.kt
data class Execution(
    val id: String,
    val campaignId: String,
    val campaignName: String,
    val quantity: Int,
    val status: ExecutionStatus,
    val progress: ExecutionProgress,
    val currentMessage: CurrentMessage?,
    val startedAt: Long,
    val completedAt: Long? = null
)

enum class ExecutionStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, CANCELLED
}

data class ExecutionProgress(
    val total: Int,
    val sent: Int,
    val failed: Int,
    val pending: Int
) {
    val percentage: Float get() = if (total > 0) sent.toFloat() / total else 0f
}

data class CurrentMessage(
    val contact: String,
    val message: String,
    val timestamp: Long
)
```

---

## 8. Testing Strategy

### Unit Tests (ViewModels y Use Cases)
```kotlin
@Test
fun `getCampaigns returns success with campaigns list`() = runTest {
    // Given
    val campaigns = listOf(
        Campaign(id = "1", name = "Test", ...)
    )
    coEvery { campaignRepository.getCampaigns() } returns Result.Success(campaigns)

    // When
    val result = getCampaignsUseCase()

    // Then
    assertTrue(result is Result.Success)
    assertEquals(1, (result as Result.Success).data.size)
}
```

### Integration Tests (Repository + API)
```kotlin
@Test
fun `executeCampaign sends correct request`() = runTest {
    // Given
    val campaignId = "camp-001"
    val quantity = 50

    // When
    repository.executeCampaign(campaignId, quantity)

    // Then
    verify { api.executeCampaign(campaignId, ExecuteCampaignRequest(quantity)) }
}
```

### UI Tests (Compose)
```kotlin
@Test
fun campaignListScreen_displaysAllCampaigns() {
    composeTestRule.setContent {
        CampaignListScreen(viewModel = viewModel)
    }

    composeTestRule.onNodeWithText("Promoción 2024").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 mensajes").assertIsDisplayed()
}
```

---

## 9. Consideraciones de Seguridad

1. **Credenciales**: EncryptedSharedPreferences para tokens
2. **HTTPS/HTTP**: Configurar según ambiente (localhost vs servidor remoto)
3. **Validación de Inputs**: Validar cantidad contra cuota
4. **Permisos**: Solo Internet y Notificaciones
5. **Ofuscación**: ProGuard/R8 para release
6. **No hardcodear**: URLs en BuildConfig

---

## 10. Estimación de Tiempo

| Fase | Duración | Horas | Complejidad |
|------|----------|-------|-------------|
| Setup y Autenticación | 1 semana | 20h | Media |
| Sesión WhatsApp | 1 semana | 20h | Alta |
| Selección de Campaña | 1 semana | 20h | Media |
| Ejecución y Monitoreo | 1.5 semanas | 30h | Alta |
| Dashboard y Pulido | 1 semana | 20h | Baja |
| **Total** | **5.5 semanas** | **110h** | - |

**Consideraciones:**
- Asume desarrollador con conocimiento intermedio de Kotlin
- No incluye tiempo de aprendizaje de Compose (agregar +1 semana si es nuevo)
- Buffer de 20% recomendado para imprevistos

---

## 11. Recursos de Aprendizaje

### Documentación Oficial
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room Database](https://developer.android.com/training/data-storage/room)

### Tutoriales Recomendados
- [Android Developers Codelabs](https://developer.android.com/codelabs)
- [Compose Pathway](https://developer.android.com/courses/pathways/compose)
- [MVVM Architecture](https://developer.android.com/topic/architecture)

### Proyectos de Referencia
- [Now in Android (Google)](https://github.com/android/nowinandroid)
- [Compose Samples](https://github.com/android/compose-samples)

---

## 12. Próximos Pasos

1. ✅ **Tecnología confirmada**: Kotlin + Jetpack Compose
2. 📝 **Crear proyecto**: Android Studio → New Project → Empty Compose Activity
3. 🔧 **Configurar dependencias**: build.gradle con todas las librerías
4. 📐 **Definir modelos de datos**: Campaign, Execution, etc.
5. 🎨 **Implementar tema**: Material 3 con colores de WhatsApp
6. 💻 **Iniciar Fase 1**: Implementar Login + Auth

---

## 13. Conclusión

Este plan de desarrollo está optimizado para una app **ejecutora de campañas** (no creadora) usando Kotlin + Jetpack Compose. La arquitectura Clean + MVVM garantiza:

- ✅ Separación clara de responsabilidades
- ✅ Fácil testing
- ✅ Código mantenible y escalable
- ✅ UI moderna y performante
- ✅ Desarrollo ágil

**Estado**: Listo para iniciar desarrollo
