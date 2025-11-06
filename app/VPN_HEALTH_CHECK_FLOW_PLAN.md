# Plan de Implementación: Flujo de Health Check y VPN FortiClient

## Descripción General

Implementar un flujo completo de verificación de conectividad con el backend que **requiere obligatoriamente** FortiClient VPN cuando la conexión directa falla. No hay opción de continuar sin VPN.

## Flujo de Usuario

```
App Inicio
    ↓
[SplashScreen]
    ↓ (Check Health)
    ├─ ✓ Conexión OK → [LoginScreen]
    │
    └─ ✗ Sin Conexión
        ↓ (Verificar FortiClient)
        ├─ FortiClient Instalado
        │   ↓
        │   [VPN Connection Screen]
        │   - Botón: "Abrir FortiClient"
        │   - Botón: "Verificar Conexión"
        │   ↓
        │   Usuario conecta VPN
        │   ↓
        │   Verificar Conexión → ✓ → [LoginScreen]
        │
        └─ FortiClient NO Instalado
            ↓
            [VPN Install Screen]
            - Botón: "Ir a Play Store" (Obligatorio)
            ↓
            Abre Play Store + Navega a [VPN Connection Screen]
            ↓
            Usuario instala y vuelve
            ↓
            [VPN Connection Screen]
            - Botón: "Abrir FortiClient"  (ahora disponible)
            - Botón: "Verificar Conexión"
```

## Arquitectura de Componentes

### 1. Data Layer

#### 1.1 Health Check API
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/data/remote/api/HealthApi.kt`

```kotlin
interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): HealthResponse
}
```

#### 1.2 Health DTO
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/data/remote/dto/HealthDto.kt`

```kotlin
@Serializable
data class HealthResponse(
    val success: Boolean,
    val message: String,
    val timestamp: String
)
```

#### 1.3 VPN Repository
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/data/repository/VpnRepository.kt`

**Responsabilidades**:
- Verificar conectividad con backend (health check)
- Detectar si FortiClient está instalado
- Lanzar FortiClient VPN
- Abrir Play Store para instalación

**Métodos**:
```kotlin
suspend fun checkBackendHealth(): Result<Boolean>
fun isFortiClientInstalled(): Boolean
fun launchFortiClient(): Result<Unit>
fun openPlayStoreFortiClient(): Result<Unit>
```

### 2. Domain Layer

#### 2.1 Models
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/domain/model/ConnectionStatus.kt`

```kotlin
sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    object Disconnected : ConnectionStatus()
    object Checking : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

data class VpnStatus(
    val isFortiClientInstalled: Boolean,
    val connectionStatus: ConnectionStatus
)
```

#### 2.2 Use Cases
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/domain/usecase/CheckConnectivityUseCase.kt`

```kotlin
class CheckConnectivityUseCase @Inject constructor(
    private val vpnRepository: IVpnRepository
) {
    suspend operator fun invoke(): VpnStatus
}
```

#### 2.3 Repository Interface
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/domain/repository/IVpnRepository.kt`

```kotlin
interface IVpnRepository {
    suspend fun checkBackendHealth(): Result<Boolean>
    fun isFortiClientInstalled(): Boolean
    fun launchFortiClient(): Result<Unit>
    fun openPlayStoreFortiClient(): Result<Unit>
}
```

### 3. Presentation Layer

#### 3.1 SplashScreen
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/ui/screens/splash/SplashScreen.kt`

**Funcionalidad**:
- Mostrar logo de la app
- Ejecutar health check automáticamente
- Navegar a Login si hay conexión
- Navegar a VPN Connection/Install screen si no hay conexión

**UI State**:
```kotlin
data class SplashUiState(
    val isChecking: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Checking,
    val errorMessage: String? = null
)
```

#### 3.2 SplashViewModel
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/ui/screens/splash/SplashViewModel.kt`

**Responsabilidades**:
- Ejecutar health check al iniciar
- Gestionar navegación según resultado
- Timeout de 10 segundos para health check

#### 3.3 VPN Connection Screen
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/ui/screens/vpn/VpnConnectionScreen.kt`

**Funcionalidad**:
- Mostrar mensaje "Sin conexión con el servidor - VPN Requerida"
- **Si FortiClient instalado**:
  - Botón "Abrir FortiClient" → Lanza app FortiClient
  - Botón "Verificar Conexión" → Reintenta health check
- **Si FortiClient NO instalado**:
  - Mensaje: "FortiClient no detectado. Por favor instálalo."
  - Botón "Verificar Conexión" → Verifica instalación + health check
  - Instrucción para volver a VpnInstallScreen si es necesario

**UI State**:
```kotlin
data class VpnConnectionUiState(
    val isChecking: Boolean = false,
    val isFortiClientInstalled: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val lastCheckTime: Long? = null,
    val errorMessage: String? = null
)
```

**Lógica onResume**:
- Cada vez que VpnConnectionScreen se muestra (onResume), debe verificar si FortiClient fue instalado
- Actualizar estado `isFortiClientInstalled`

#### 3.4 VPN Install Screen
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/ui/screens/vpn/VpnInstallScreen.kt`

**Funcionalidad**:
- Mostrar mensaje "Se requiere FortiClient VPN para continuar"
- Instrucciones de instalación
- Botón "Ir a Play Store" → **Abre Play Store + Navega a VpnConnectionScreen**
- **NO hay opción de continuar sin VPN**

**UI State**:
```kotlin
data class VpnInstallUiState(
    val isNavigating: Boolean = false
)
```

**Flujo del Botón "Ir a Play Store"**:
1. Abrir Play Store con intent
2. **Inmediatamente** navegar a VpnConnectionScreen
3. Usuario queda en VpnConnectionScreen esperando
4. Cuando vuelve de Play Store (después de instalar), VpnConnectionScreen detecta la app instalada (onResume)
5. Habilita botón "Abrir FortiClient"

#### 3.5 ViewModels
**Archivos**:
- `ui/screens/vpn/VpnConnectionViewModel.kt`
- `ui/screens/vpn/VpnInstallViewModel.kt`

**Responsabilidades**:
- Gestionar estado de conexión VPN
- Proveer métodos para verificar health check
- Mantener timestamp de última verificación exitosa
- Verificar instalación de FortiClient

### 4. Dependency Injection

#### 4.1 Actualizar NetworkModule
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/di/NetworkModule.kt`

**Agregar**:
- Provider de `HealthApi`
- Configurar timeout específico para health check (10 segundos)

```kotlin
@Provides
@Singleton
fun provideHealthApi(retrofit: Retrofit): HealthApi {
    return retrofit.create(HealthApi::class.java)
}

@Provides
@Singleton
fun provideHealthCheckOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
}
```

#### 4.2 Actualizar RepositoryModule
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/di/RepositoryModule.kt`

**Agregar**:
```kotlin
@Binds
@Singleton
abstract fun bindVpnRepository(
    vpnRepository: VpnRepository
): IVpnRepository
```

### 5. Navigation

#### 5.1 Actualizar MainActivity
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/MainActivity.kt`

**Cambios**:
- Cambiar `startDestination` de `"login"` a `"splash"`
- Agregar rutas:
  - `"splash"` → SplashScreen
  - `"vpn_connection"` → VpnConnectionScreen
  - `"vpn_install"` → VpnInstallScreen

**Flujo de navegación**:
```kotlin
NavHost(navController, startDestination = "splash") {
    composable("splash") {
        SplashScreen(
            onNavigateToLogin = {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            },
            onNavigateToVpnConnection = {
                navController.navigate("vpn_connection")
            },
            onNavigateToVpnInstall = {
                navController.navigate("vpn_install")
            }
        )
    }

    composable("vpn_connection") {
        VpnConnectionScreen(
            onNavigateToLogin = {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        )
    }

    composable("vpn_install") {
        VpnInstallScreen(
            onInstallAndNavigate = {
                // Navegar a VpnConnectionScreen
                navController.navigate("vpn_connection") {
                    popUpTo("vpn_install") { inclusive = true }
                }
            }
        )
    }

    // ... resto de rutas existentes
}
```

**Lógica en VpnInstallScreen**:
```kotlin
// En VpnInstallViewModel
fun onInstallClick(context: Context) {
    viewModelScope.launch {
        _uiState.update { it.copy(isNavigating = true) }

        // 1. Abrir Play Store
        val intent = VpnHelper.openPlayStore(context, VpnHelper.FORTICLIENT_PACKAGE)
        context.startActivity(intent)

        // 2. Disparar evento de navegación
        _navigationEvent.emit(NavigationEvent.ToVpnConnection)
    }
}

// En VpnInstallScreen composable
LaunchedEffect(Unit) {
    viewModel.navigationEvent.collect { event ->
        when (event) {
            NavigationEvent.ToVpnConnection -> {
                onInstallAndNavigate()
            }
        }
    }
}
```

### 6. Manifest y Permisos

#### 6.1 Actualizar AndroidManifest.xml

**Agregar queries** (no requiere permiso especial para query específico):
```xml
<queries>
    <!-- FortiClient VPN -->
    <package android:name="com.fortinet.forticlient_vpn" />
</queries>
```

**Nota**: No es necesario `QUERY_ALL_PACKAGES` si solo verificamos paquetes específicos con `<queries>`.

### 7. Utilities

#### 7.1 VPN Helper
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/utils/VpnHelper.kt`

**Funciones**:
```kotlin
object VpnHelper {
    const val FORTICLIENT_PACKAGE = "com.fortinet.forticlient_vpn"

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun launchApp(context: Context, packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }

    fun openPlayStore(context: Context, packageName: String): Intent {
        return try {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        } catch (e: ActivityNotFoundException) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        }
    }
}
```

#### 7.2 Network Helper
**Archivo**: `app/android/app/src/main/java/com/whatsappbulk/sender/utils/NetworkHelper.kt`

**Funciones**:
```kotlin
object NetworkHelper {
    fun hasNetworkConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
```

## Implementación por Fases

### Fase 1: Data Layer (2-3 horas)
1. ✅ Crear `HealthApi` interface
2. ✅ Crear `HealthDto` data classes
3. ✅ Implementar `VpnRepository`
4. ✅ Crear `IVpnRepository` interface
5. ✅ Actualizar `NetworkModule` con HealthApi provider
6. ✅ Actualizar `RepositoryModule` con VpnRepository binding

### Fase 2: Domain Layer (1-2 horas)
1. ✅ Crear modelos `ConnectionStatus` y `VpnStatus`
2. ✅ Crear `CheckConnectivityUseCase`
3. ✅ Crear tests unitarios para use case

### Fase 3: Utilities (1 hora)
1. ✅ Implementar `VpnHelper`
2. ✅ Implementar `NetworkHelper`
3. ✅ Tests para utilities

### Fase 4: Presentation - SplashScreen (2-3 horas)
1. ✅ Crear `SplashUiState`
2. ✅ Implementar `SplashViewModel`
3. ✅ Diseñar y crear `SplashScreen` UI
4. ✅ Implementar lógica de navegación automática
5. ✅ Agregar animación de loading

### Fase 5: Presentation - VPN Screens (3-4 horas)
1. ✅ Crear `VpnConnectionScreen` + ViewModel
2. ✅ Crear `VpnInstallScreen` + ViewModel
3. ✅ Implementar botones de acción
4. ✅ Implementar navegación desde VpnInstall → VpnConnection al instalar
5. ✅ Implementar detección en onResume de FortiClient
6. ✅ Manejar eventos de usuario
7. ✅ Diseño UI consistente con Material 3
8. ✅ **Eliminar cualquier opción de "continuar sin VPN"**

### Fase 6: Navigation (1 hora)
1. ✅ Actualizar `MainActivity` con nuevas rutas
2. ✅ Configurar `startDestination = "splash"`
3. ✅ Implementar navegación condicional
4. ✅ Implementar flujo VpnInstall → VpnConnection
5. ✅ Limpiar backstack correctamente
6. ✅ **No permitir back button desde VPN screens (opcional)**

### Fase 7: Manifest y Permisos (30 min)
1. ✅ Agregar queries para FortiClient
2. ✅ Verificar compatibilidad Android 11+

### Fase 8: Testing e Integración (2-3 horas)
1. ✅ Tests unitarios para ViewModels
2. ✅ Tests de integración para Repository
3. ✅ Tests de UI con Compose
4. ✅ Testing manual del flujo completo
5. ✅ Testing del flujo: Install → Play Store → Volver → Detectar app

## Configuración Adicional

### build.gradle.kts (app level)

No requiere dependencias adicionales. Todo está incluido en Android SDK.

### strings.xml

Agregar strings necesarios:
```xml
<!-- Splash Screen -->
<string name="splash_checking_connection">Verificando conexión con el servidor...</string>

<!-- VPN Connection Screen -->
<string name="vpn_connection_title">Conexión VPN Requerida</string>
<string name="vpn_connection_message">No se pudo conectar al servidor. Debes conectarte a FortiClient VPN para continuar.</string>
<string name="vpn_connection_not_installed">FortiClient VPN no está instalado. Por favor, instálalo desde Play Store.</string>
<string name="vpn_open_forticlient">Abrir FortiClient</string>
<string name="vpn_verify_connection">Verificar Conexión</string>

<!-- VPN Install Screen -->
<string name="vpn_install_title">FortiClient VPN Requerido</string>
<string name="vpn_install_message">Para acceder al servidor, debes instalar FortiClient VPN desde Play Store.</string>
<string name="vpn_install_instructions">Una vez instalado, vuelve aquí para abrir FortiClient y conectarte a la VPN.</string>
<string name="vpn_install_button">Ir a Play Store</string>

<!-- Errors -->
<string name="error_no_connection">No se pudo conectar al servidor</string>
<string name="error_health_check_failed">La verificación de salud del servidor falló</string>
<string name="error_forticlient_not_found">FortiClient VPN no está instalado</string>
<string name="error_cannot_open_forticlient">No se pudo abrir FortiClient</string>
<string name="error_cannot_open_playstore">No se pudo abrir Play Store</string>

<!-- Info -->
<string name="info_last_check">Última verificación: %1$s</string>
<string name="info_checking">Verificando...</string>
</xml>
```

## Consideraciones Técnicas

### 1. Timeouts
- Health check timeout: **10 segundos**
- Reintentos automáticos: **Ninguno** (usuario debe activar manualmente)
- Intervalo mínimo entre checks: **2 segundos** (evitar spam)

### 2. Network Security
- Ya configurado `usesCleartextTraffic="true"` para localhost
- Health check usa HTTP para desarrollo
- Producción debe usar HTTPS

### 3. Manejo de Errores
- Timeout → Mostrar pantalla VPN
- Network error → Verificar FortiClient
- Server error (500) → Mostrar mensaje específico
- Success = 200 + `success: true`

### 4. UX Considerations
- Mostrar indicador de loading durante health check
- Disable botones durante verificación
- Mostrar timestamp de última verificación
- Mensajes de error claros y accionables
- **Al volver de Play Store, automáticamente detectar si FortiClient fue instalado**
- **No hay forma de saltarse la VPN - es obligatoria**

### 5. Compatibilidad Android
- Mínimo: API 21 (Android 5.0)
- Target: API 34 (Android 14)
- `<queries>` disponible desde Android 11, pero funciona con backward compatibility

### 6. Lifecycle Management
- **VpnConnectionScreen** debe implementar `DisposableEffect` con `LocalLifecycleOwner` para detectar cuando vuelve a estar visible (onResume)
- Verificar instalación de FortiClient cada vez que la pantalla se resume

### 7. Back Button Handling (Opcional)
- Considerar deshabilitar back button en VPN screens para forzar el flujo
- Implementar con `BackHandler` de Compose:
```kotlin
BackHandler(enabled = true) {
    // No hacer nada o mostrar dialog de confirmación
}
```

## Archivos a Crear/Modificar

### Nuevos Archivos (14 archivos)

**Data Layer (4)**:
1. `data/remote/api/HealthApi.kt`
2. `data/remote/dto/HealthDto.kt`
3. `data/repository/VpnRepository.kt`
4. `domain/repository/IVpnRepository.kt`

**Domain Layer (2)**:
5. `domain/model/ConnectionStatus.kt`
6. `domain/usecase/CheckConnectivityUseCase.kt`

**Presentation Layer (6)**:
7. `ui/screens/splash/SplashScreen.kt`
8. `ui/screens/splash/SplashViewModel.kt`
9. `ui/screens/vpn/VpnConnectionScreen.kt`
10. `ui/screens/vpn/VpnConnectionViewModel.kt`
11. `ui/screens/vpn/VpnInstallScreen.kt`
12. `ui/screens/vpn/VpnInstallViewModel.kt`

**Utils (2)**:
13. `utils/VpnHelper.kt`
14. `utils/NetworkHelper.kt`

### Archivos a Modificar (5)

1. `MainActivity.kt` - Agregar nuevas rutas y cambiar startDestination
2. `di/NetworkModule.kt` - Agregar HealthApi provider
3. `di/RepositoryModule.kt` - Agregar VpnRepository binding
4. `AndroidManifest.xml` - Agregar queries
5. `res/values/strings.xml` - Agregar strings

## Testing Strategy

### Unit Tests
- `VpnRepositoryTest` - Mock de API y Context
- `CheckConnectivityUseCaseTest` - Mock de Repository
- `SplashViewModelTest` - Mock de UseCase
- `VpnConnectionViewModelTest` - Mock de Repository
- `VpnInstallViewModelTest` - Verificar navegación
- `VpnHelperTest` - Mock de Context

### Integration Tests
- Health check con servidor real (o mock server)
- FortiClient detection con app real instalada/no instalada
- Flujo completo: VpnInstall → Play Store → VpnConnection

### UI Tests (Compose)
- SplashScreen navigation según health check
- VpnConnectionScreen botones funcionales
- VpnInstallScreen navegación correcta
- VpnConnectionScreen detecta app instalada onResume
- **Verificar que no hay forma de saltarse VPN**

## Estimación de Tiempo Total

- **Fase 1**: 2-3 horas
- **Fase 2**: 1-2 horas
- **Fase 3**: 1 hora
- **Fase 4**: 2-3 horas
- **Fase 5**: 3-4 horas
- **Fase 6**: 1 hora
- **Fase 7**: 30 min
- **Fase 8**: 2-3 horas

**Total**: **13-19 horas** de desarrollo

## Criterios de Aceptación

✅ Al iniciar la app, siempre se muestra SplashScreen
✅ Health check se ejecuta automáticamente en SplashScreen
✅ Si health check exitoso → Navega a Login
✅ Si health check falla → Verifica FortiClient instalado
✅ Si FortiClient instalado → Muestra VpnConnectionScreen
✅ Si FortiClient NO instalado → Muestra VpnInstallScreen
✅ **Botón "Ir a Play Store" en VpnInstallScreen abre Play Store Y navega a VpnConnectionScreen**
✅ **VpnConnectionScreen detecta automáticamente cuando FortiClient es instalado (onResume)**
✅ Botón "Abrir FortiClient" solo habilitado si la app está instalada
✅ Botón "Verificar Conexión" ejecuta health check nuevamente
✅ **NO existe opción de continuar sin VPN - es obligatorio**
✅ Todas las pantallas tienen manejo de errores apropiado
✅ Loading indicators muestran durante operaciones asíncronas
✅ No se puede spam de botones (debounce/disable durante operación)
✅ **No se puede usar back button para saltarse el flujo de VPN**

## Próximos Pasos

1. ✅ Revisar y aprobar este plan
2. Comenzar implementación por fases
3. Commit después de cada fase completada
4. Testing continuo durante desarrollo
5. Code review antes de merge

## Diagrama de Flujo Detallado

```
┌─────────────────┐
│  App Launch     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  SplashScreen   │
│  (Auto check)   │
└────────┬────────┘
         │
         ▼
    Health Check
         │
    ┌────┴────┐
    │         │
    ▼         ▼
Connected   Failed
    │         │
    │         ▼
    │    Check FortiClient
    │         │
    │    ┌────┴─────┐
    │    │          │
    │    ▼          ▼
    │ Installed  Not Installed
    │    │          │
    │    ▼          ▼
    │  VPN       VPN Install
    │  Connection Screen
    │  Screen      │
    │    │         │
    │    │    [Ir a Play Store]
    │    │         │
    │    │         ├─ Abre Play Store
    │    │         │
    │    │         └─ Navega a VPN Connection
    │    │                    │
    │    │         Usuario instala
    │    │                    │
    │    │         ◄──────────┘
    │    │         (vuelve a app)
    │    │
    │    ▼
    │  VPN Connection
    │  [Abrir FortiClient] ← Ahora habilitado
    │         │
    │    Usuario conecta VPN
    │         │
    │    [Verificar Conexión]
    │         │
    │         ▼
    │    Health Check
    │         │
    └─────────┴──────►
              │
              ▼
         LoginScreen
              │
              ▼
      (Resto de la app)

NOTA: No hay salida del flujo VPN hasta
      que la conexión sea exitosa
```

## Notas Importantes

1. **VPN es obligatoria**: Si no hay conexión directa, el usuario **debe** instalar y conectar FortiClient
2. **No hay bypass**: No existe opción "Continuar sin VPN" o similar
3. **Flujo bloqueante**: El usuario no puede acceder a Login hasta que health check sea exitoso
4. **Experiencia de usuario**: Aunque es bloqueante, debe ser claro y guiar al usuario paso a paso
