# Wireframes - App WhatsApp Bulk Sender

## Flujo de Pantallas

```
[Splash] → [Login] → [WhatsApp Session] → [Campaign Setup] → [Sending] → [Dashboard]
              ↓                                                    ↑
         (auto-login)                                        (volver)
```

---

## 1. Splash Screen

```
┌─────────────────────────┐
│                         │
│                         │
│      📱 WhatsApp        │
│     Bulk Sender         │
│                         │
│    [Loading spinner]    │
│                         │
│                         │
│                         │
└─────────────────────────┘
```

**Duración**: 1-2 segundos
**Lógica**: Verificar si hay sesión guardada → Auto-login o mostrar Login

---

## 2. Login Screen

```
┌─────────────────────────┐
│   WhatsApp Bulk Sender  │
│                         │
│   ┌─────────────────┐   │
│   │ 👤 Usuario      │   │
│   └─────────────────┘   │
│                         │
│   ┌─────────────────┐   │
│   │ 🔒 Contraseña   │   │
│   └─────────────────┘   │
│                         │
│   ┌─────────────────┐   │
│   │ Iniciar Sesión  │   │
│   └─────────────────┘   │
│                         │
│   [ ] Recordar sesión   │
│                         │
└─────────────────────────┘
```

**Campos:**
- Usuario (TextField)
- Contraseña (TextField, password)
- Checkbox: Recordar sesión
- Botón: Iniciar Sesión

**Validaciones:**
- Usuario no vacío
- Contraseña mínimo 4 caracteres

**Estados:**
- Idle: Formulario vacío
- Loading: Verificando credenciales
- Error: Mostrar mensaje de error
- Success: Navegar a WhatsApp Session

---

## 3. WhatsApp Session Screen

### Estado 1: Verificando sesión existente

```
┌─────────────────────────┐
│  ← Sesión de WhatsApp   │
│                         │
│   🔍 Verificando        │
│   sesión existente...   │
│                         │
│   [Progress spinner]    │
│                         │
└─────────────────────────┘
```

### Estado 2: Sin sesión - Solicitar número

```
┌─────────────────────────┐
│  ← Sesión de WhatsApp   │
│                         │
│  No hay sesión activa   │
│                         │
│  ┌───────────────────┐  │
│  │ +595 973159937   │  │
│  │ Número de teléfono│ │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ Iniciar Sesión    │  │
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

### Estado 3: Código de vinculación

```
┌─────────────────────────┐
│  ← Sesión de WhatsApp   │
│                         │
│  📱 Código de           │
│  Vinculación            │
│                         │
│  ┌───────────────────┐  │
│  │   A B C D - E F   │  │
│  │   (8 dígitos)     │  │
│  └───────────────────┘  │
│                         │
│  1. Abre WhatsApp       │
│  2. Ve a Dispositivos   │
│     Vinculados          │
│  3. Vincular con número │
│     de teléfono         │
│  4. Ingresa este código │
│                         │
│  ⏱️ Expira en: 60s     │
│                         │
│  [Verificando...]       │
│                         │
└─────────────────────────┘
```

### Estado 4: Conectado

```
┌─────────────────────────┐
│  ← Sesión de WhatsApp   │
│                         │
│  ✅ Conectado           │
│                         │
│  📱 +595 973159937      │
│                         │
│  Estado: Activo         │
│  Batería: 85%           │
│                         │
│  ┌───────────────────┐  │
│  │ Continuar →       │  │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ Cerrar Sesión     │  │
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

**Estados:**
1. **Checking**: Verificando sesión
2. **NotConnected**: Formulario de número
3. **LinkCodeReady**: Mostrando código + countdown
4. **Connected**: Sesión activa, mostrar info

---

## 4. Campaign Setup Screen

```
┌─────────────────────────┐
│  ← Configurar Campaña   │
│                         │
│ 📝 Mensajes (3)         │
│ ┌─────────────────────┐ │
│ │ Mensaje 1          ││ │
│ │ [Hola, ¿cómo      ││ │
│ │  estás?]          ││ │
│ └─────────────────────┘ │
│ ┌─────────────────────┐ │
│ │ Mensaje 2          ││ │
│ │ [Te ofrecemos...] ││ │
│ └─────────────────────┘ │
│ ┌─────────────────────┐ │
│ │ Mensaje 3          ││ │
│ │ [Gracias por...]  ││ │
│ └─────────────────────┘ │
│                         │
│ 📞 Contactos (5)        │
│ ┌─────────────────────┐ │
│ │ +595973159937      ││ │
│ │ +595981234567      ││ │
│ │ +595982345678      ││ │
│ │ ...                ││ │
│ └─────────────────────┘ │
│ [+ Agregar] [Importar]  │
│                         │
│ ⏱️ Intervalo           │
│ ┌─────────────────────┐ │
│ │ Min: 5s  Max: 15s  │ │
│ │ [=====●=====]      │ │
│ └─────────────────────┘ │
│                         │
│ ┌─────────────────────┐ │
│ │ Iniciar Envío →    │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

**Secciones:**

1. **Mensajes**:
   - 3 TextFields multi-línea
   - Contador de caracteres (opcional)
   - Validación: No vacíos

2. **Contactos**:
   - Lista scrolleable
   - Botón: Agregar manualmente (diálogo)
   - Botón: Importar CSV/TXT
   - Acción: Eliminar contacto (swipe o icono)
   - Validación: Formato de número válido

3. **Intervalo**:
   - RangeSlider (min, max en segundos)
   - Valores: 1s - 120s
   - Muestra valores seleccionados

4. **Acción**:
   - Botón primario: Iniciar Envío
   - Validación: Al menos 1 mensaje y 1 contacto

---

## 5. Sending Screen

```
┌─────────────────────────┐
│  ← Enviando Mensajes    │
│                         │
│  Progreso General       │
│  ┌─────────────────────┐│
│  │ ████████░░░░░░░░   ││
│  │ 45 / 100 (45%)     ││
│  └─────────────────────┘│
│                         │
│  📊 Estadísticas        │
│  ✅ Enviados: 45        │
│  ⏳ Pendientes: 55      │
│  ❌ Fallidos: 0         │
│                         │
│  📱 Enviando ahora:     │
│  +595973159937          │
│  "Hola, ¿cómo estás?"   │
│                         │
│  ⏱️ Próximo en: 8s     │
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
│  │ ❌ +5959823... Err ││
│  └─────────────────────┘│
└─────────────────────────┘
```

**Elementos:**

1. **Progress Bar**: Progreso total
2. **Estadísticas**: Enviados, Pendientes, Fallidos
3. **Estado Actual**: Número y mensaje siendo enviado
4. **Countdown**: Tiempo hasta próximo envío
5. **Controles**:
   - Pausar (cambia a Reanudar)
   - Cancelar (confirmación)
6. **Log**: Lista de últimos 10-20 envíos

**Estados:**
- **Sending**: Enviando activamente
- **Paused**: Pausado por usuario
- **Completed**: Todos enviados
- **Cancelled**: Cancelado por usuario
- **Error**: Error crítico

---

## 6. Dashboard Screen

```
┌─────────────────────────┐
│  ← Dashboard            │
│                         │
│  📊 Última Campaña      │
│  ┌─────────────────────┐│
│  │ Total: 100 msgs    ││
│  │ ✅ Exitosos: 95    ││
│  │ ❌ Fallidos: 5     ││
│  │ ⏱️ Tiempo: 18m 45s││
│  └─────────────────────┘│
│                         │
│  📈 Distribución        │
│  ┌─────────────────────┐│
│  │ Msg 1: ████ 34    ││
│  │ Msg 2: ████ 33    ││
│  │ Msg 3: ████ 33    ││
│  └─────────────────────┘│
│                         │
│  📜 Historial           │
│  ┌─────────────────────┐│
│  │ 20/10 - 100 msgs  ││
│  │ 19/10 - 50 msgs   ││
│  │ 18/10 - 75 msgs   ││
│  └─────────────────────┘│
│                         │
│  ┌─────────────────────┐│
│  │ + Nueva Campaña    ││
│  └─────────────────────┘│
│  ┌─────────────────────┐│
│  │ ⚙️ Configuración   ││
│  └─────────────────────┘│
└─────────────────────────┘
```

**Secciones:**

1. **Resumen Última Campaña**: Stats principales
2. **Gráfico Distribución**: Barras por mensaje
3. **Historial**: Lista de campañas anteriores (tap para ver detalles)
4. **Acciones**:
   - Nueva Campaña → Vuelve a Campaign Setup
   - Configuración → Settings screen

---

## 7. Settings Screen (Opcional)

```
┌─────────────────────────┐
│  ← Configuración        │
│                         │
│  🌐 Servidor            │
│  ┌─────────────────────┐│
│  │ http://127.0.0.1:  ││
│  │ 3000               ││
│  └─────────────────────┘│
│                         │
│  ⏱️ Timeouts           │
│  ┌─────────────────────┐│
│  │ Conexión: 30s      ││
│  │ Respuesta: 60s     ││
│  └─────────────────────┘│
│                         │
│  📱 WhatsApp            │
│  ┌─────────────────────┐│
│  │ Sesión activa      ││
│  │ +595973159937      ││
│  │ [Cerrar Sesión]    ││
│  └─────────────────────┘│
│                         │
│  👤 Cuenta              │
│  ┌─────────────────────┐│
│  │ Usuario: admin     ││
│  │ [Cerrar Sesión]    ││
│  └─────────────────────┘│
│                         │
│  ℹ️ Versión: 1.0.0     │
└─────────────────────────┘
```

---

## 8. Diálogos

### Agregar Contacto

```
┌───────────────────────┐
│ Agregar Contacto      │
│                       │
│ ┌─────────────────┐   │
│ │ +595 973159937 │   │
│ └─────────────────┘   │
│                       │
│ Ejemplo: +595973159937│
│                       │
│ [Cancelar] [Agregar]  │
└───────────────────────┘
```

### Confirmar Cancelación

```
┌───────────────────────┐
│ ⚠️ Cancelar Envío    │
│                       │
│ ¿Estás seguro?        │
│                       │
│ Se detendrá el envío  │
│ de mensajes.          │
│                       │
│ Progreso: 45/100      │
│                       │
│ [No] [Sí, Cancelar]   │
└───────────────────────┘
```

### Error de Conexión

```
┌───────────────────────┐
│ ❌ Error             │
│                       │
│ No se pudo conectar   │
│ al servidor           │
│                       │
│ Verifica:             │
│ • Servidor activo     │
│ • URL correcta        │
│ • Conexión a red      │
│                       │
│ [Reintentar] [Cerrar] │
└───────────────────────┘
```

---

## 9. Componentes Reutilizables

### 1. MessageCard (Compose)

```kotlin
@Composable
fun MessageCard(
    messageNumber: Int,
    message: String,
    onMessageChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(padding = 16.dp) {
            Text("Mensaje $messageNumber", style = MaterialTheme.typography.subtitle2)
            Spacer(height = 8.dp)
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                placeholder = { Text("Escribe tu mensaje aquí...") }
            )
        }
    }
}
```

### 2. ContactListItem

```kotlin
@Composable
fun ContactListItem(
    phoneNumber: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(phoneNumber, style = MaterialTheme.typography.body1)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
        }
    }
}
```

### 3. ProgressCard

```kotlin
@Composable
fun ProgressCard(
    sent: Int,
    pending: Int,
    failed: Int,
    total: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(padding = 16.dp) {
            LinearProgressIndicator(
                progress = sent.toFloat() / total,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(height = 8.dp)
            Text("$sent / $total (${(sent * 100 / total)}%)")
            Spacer(height = 16.dp)
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("✅ Enviados", sent)
                StatItem("⏳ Pendientes", pending)
                StatItem("❌ Fallidos", failed)
            }
        }
    }
}
```

---

## 10. Paleta de Colores (Material 3)

```kotlin
// Theme.kt
val Primary = Color(0xFF25D366)        // Verde WhatsApp
val PrimaryVariant = Color(0xFF128C7E) // Verde oscuro
val Secondary = Color(0xFF34B7F1)      // Azul WhatsApp
val Background = Color(0xFFF7F8FA)     // Gris claro
val Surface = Color.White
val Error = Color(0xFFFF5252)          // Rojo para errores
val OnPrimary = Color.White
val OnBackground = Color(0xFF1C1C1E)   // Casi negro
```

---

## 11. Iconografía

**Material Icons recomendados:**
- Login: `Icons.Default.Person`, `Icons.Default.Lock`
- WhatsApp: `Icons.Default.Phone`, `Icons.Default.Message`
- Campaña: `Icons.Default.Edit`, `Icons.Default.People`
- Envío: `Icons.Default.Send`, `Icons.Default.Pause`, `Icons.Default.Cancel`
- Dashboard: `Icons.Default.BarChart`, `Icons.Default.History`
- Configuración: `Icons.Default.Settings`

---

## 12. Animaciones

### Transiciones de navegación
```kotlin
// Slide in from right
slideInHorizontally(initialOffsetX = { it }) + fadeIn()
slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
```

### Progress updates
```kotlin
// Animated progress bar
animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 500)
)
```

### Success/Error states
```kotlin
// Scale animation for icons
animateContentSize()
AnimatedVisibility(visible = showSuccess) {
    Icon(Icons.Default.CheckCircle, tint = Color.Green)
}
```

---

Este documento complementa el plan de desarrollo con visualizaciones claras de cada pantalla y sus componentes.
