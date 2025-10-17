# Documentación API REST - WhatsApp Termux

API REST completa para controlar WhatsApp Web desde aplicaciones Android o cualquier cliente HTTP.

## URL Base

```
http://localhost:3000
```

Desde tu app Android en el mismo dispositivo:
```
http://127.0.0.1:3000
```

---

## Autenticación y Sesión

WhatsApp soporta dos métodos de inicio de sesión:
1. **Con QR Code** (predeterminado): Escanea el código QR con tu teléfono
2. **Con número de teléfono**: Recibe un código de 8 dígitos en WhatsApp que debes ingresar en tu app

### 1. Iniciar Sesión

Inicia una nueva sesión de WhatsApp Web.

**Endpoint:** `POST /api/auth/start`

#### Opción A: Inicio con QR Code (sin body)

**Body (JSON):** Vacío o `{}`

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Iniciando sesión con QR. Use /auth/qr para obtener el código QR.",
  "data": {
    "status": "connecting",
    "qrCode": null,
    "qrCodeBase64": null,
    "linkCode": null,
    "connectedPhone": null,
    "usePhoneNumber": false
  }
}
```

#### Opción B: Inicio con número de teléfono

**Body (JSON):**
```json
{
  "phoneNumber": "5215512345678"
}
```

**Formato del número:** Código de país + número (sin espacios, guiones, ni símbolos)
- ✅ Correcto: `"5215512345678"` (México)
- ✅ Correcto: `"5491112345678"` (Argentina)
- ❌ Incorrecto: `"+52 1 55 1234 5678"`

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Iniciando sesión con número de teléfono. Use /auth/link-code para obtener el código de vinculación.",
  "data": {
    "status": "connecting",
    "qrCode": null,
    "qrCodeBase64": null,
    "linkCode": null,
    "connectedPhone": null,
    "usePhoneNumber": true
  }
}
```

### 2. Obtener Estado de la Sesión

Verifica el estado actual de la conexión.

**Endpoint:** `GET /api/auth/status`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": {
    "status": "connected",
    "qrCode": null,
    "qrCodeBase64": null,
    "connectedPhone": "Samsung Galaxy",
    "isConnected": true
  }
}
```

**Estados posibles:**
- `disconnected` - Sin conexión
- `connecting` - Conectando...
- `qr_ready` - QR listo para escanear
- `link_code_ready` - Código de vinculación listo (8 dígitos)
- `connected` - Conectado y listo
- `failed` - Error en la conexión

### 3. Obtener Código QR

Obtiene el código QR para escanear con WhatsApp (solo cuando se inicia sesión sin phoneNumber).

**Endpoint:** `GET /api/auth/qr`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": {
    "qrCode": "█▀▀▀▀▀█ ▀▀█▄...",
    "qrCodeBase64": "data:image/png;base64,iVBORw0KGgo...",
    "status": "qr_ready"
  }
}
```

**Ejemplo de uso en Android (Kotlin):**
```kotlin
// El campo qrCodeBase64 contiene un Data URL que puedes mostrar directamente
val qrCodeDataUrl = response.data.qrCodeBase64
imageView.setImageBitmap(decodeBase64ToBitmap(qrCodeDataUrl))
```

### 4. Obtener Código de Vinculación (Link Code)

Obtiene el código de 8 dígitos para vincular el dispositivo (solo cuando se inicia sesión con phoneNumber).

**Endpoint:** `GET /api/auth/link-code`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": {
    "linkCode": "12345678",
    "status": "link_code_ready",
    "phoneNumber": true
  }
}
```

**Flujo de uso:**
1. Usuario inicia sesión con su número: `POST /api/auth/start` con `{"phoneNumber": "5215512345678"}`
2. App obtiene el código: `GET /api/auth/link-code`
3. Usuario recibe un mensaje en WhatsApp con el código de 8 dígitos
4. App muestra el código al usuario para que lo compare
5. Usuario confirma en WhatsApp
6. Estado cambia a `connected`

**Ejemplo en Android:**
```kotlin
// Mostrar código en la UI
lifecycleScope.launch {
    delay(3000) // Esperar generación
    val linkCodeResponse = apiService.getLinkCode()
    if (linkCodeResponse.success) {
        textViewCode.text = linkCodeResponse.data.linkCode
        // Formatear como: "1234-5678" para mejor lectura
        val formatted = linkCodeResponse.data.linkCode.chunked(4).joinToString("-")
        textViewCode.text = formatted
    }
}
```

### 5. Verificar Conexión

Verifica si la sesión está activa.

**Endpoint:** `GET /api/auth/check`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": {
    "isConnected": true
  }
}
```

### 5. Cerrar Sesión

Cierra la sesión actual de WhatsApp.

**Endpoint:** `POST /api/auth/close`

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Sesión cerrada exitosamente"
}
```

---

## Mensajería

### 1. Enviar Mensaje de Texto

Envía un mensaje de texto a un número de WhatsApp.

**Endpoint:** `POST /api/messages/send`

**Body (JSON):**
```json
{
  "phone": "5215512345678",
  "message": "Hola desde mi app!"
}
```

**Formatos de teléfono aceptados:**
- `5215512345678` (código de país + número)
- `5215512345678@c.us` (formato WhatsApp)

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Mensaje enviado exitosamente",
  "data": {
    "id": "true_5215512345678@c.us_3EB...",
    "ack": 1
  }
}
```

**Ejemplo en Android (Kotlin con Retrofit):**
```kotlin
data class SendMessageRequest(
    val phone: String,
    val message: String
)

@POST("api/messages/send")
suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse
```

### 2. Enviar Imagen

Envía una imagen con caption opcional.

**Endpoint:** `POST /api/messages/send-image`

**Body (multipart/form-data):**
- `phone` (string): Número de teléfono
- `image` (file): Archivo de imagen
- `caption` (string, opcional): Descripción de la imagen

**Ejemplo con curl:**
```bash
curl -X POST http://localhost:3000/api/messages/send-image \
  -F "phone=5215512345678" \
  -F "image=@/path/to/image.jpg" \
  -F "caption=Mira esta foto!"
```

**Ejemplo en Android (Kotlin):**
```kotlin
val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
val phonePart = phone.toRequestBody("text/plain".toMediaTypeOrNull())
val captionPart = caption.toRequestBody("text/plain".toMediaTypeOrNull())

apiService.sendImage(phonePart, imagePart, captionPart)
```

### 3. Enviar Archivo

Envía cualquier tipo de archivo (PDF, documentos, etc.).

**Endpoint:** `POST /api/messages/send-file`

**Body (multipart/form-data):**
- `phone` (string): Número de teléfono
- `file` (file): Archivo a enviar
- `caption` (string, opcional): Descripción del archivo

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Archivo enviado exitosamente",
  "data": {
    "id": "true_5215512345678@c.us_3EB...",
    "ack": 1
  }
}
```

### 4. Obtener Mensajes Recibidos

Obtiene la cola de mensajes recibidos (últimos 100).

**Endpoint:** `GET /api/messages`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": [
    {
      "id": "true_5215512345678@c.us_3EB...",
      "from": "5215512345678@c.us",
      "body": "Hola!",
      "timestamp": 1699123456789,
      "isGroupMsg": false,
      "type": "chat"
    }
  ]
}
```

**Polling desde Android:**
```kotlin
// Hacer polling cada 5 segundos
lifecycleScope.launch {
    while (isActive) {
        val messages = apiService.getMessages()
        updateUI(messages)
        delay(5000)
    }
}
```

### 5. Limpiar Cola de Mensajes

Limpia los mensajes almacenados en la cola.

**Endpoint:** `DELETE /api/messages`

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Cola de mensajes limpiada"
}
```

### 6. Obtener Mensajes No Leídos

Obtiene mensajes no leídos directamente de WhatsApp.

**Endpoint:** `GET /api/messages/unread`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "body": "Mensaje no leído",
      "from": "5215512345678@c.us",
      "to": "...",
      "unreadCount": 3
    }
  ]
}
```

---

## Contactos y Chats

### 1. Obtener Todos los Chats

Obtiene la lista de todos los chats.

**Endpoint:** `GET /api/contacts/chats`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": [
    {
      "id": "5215512345678@c.us",
      "name": "Juan Pérez",
      "isGroup": false,
      "unreadCount": 2,
      "lastMessage": "Hola!"
    }
  ]
}
```

### 2. Obtener Todos los Contactos

Obtiene la lista de contactos.

**Endpoint:** `GET /api/contacts`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": [
    {
      "id": "5215512345678@c.us",
      "name": "Juan Pérez",
      "pushname": "Juan",
      "isMyContact": true
    }
  ]
}
```

### 3. Obtener Nivel de Batería

Obtiene el nivel de batería del teléfono conectado.

**Endpoint:** `GET /api/contacts/battery`

**Respuesta exitosa:**
```json
{
  "success": true,
  "data": {
    "batteryLevel": 85
  }
}
```

---

## Flujo de Trabajo Típico

### 1A. Inicio de Sesión con QR Code (Tradicional)

```kotlin
lifecycleScope.launch {
    // 1. Iniciar sesión sin phoneNumber
    val startResponse = apiService.startSession(StartSessionRequest())

    // 2. Esperar y obtener QR
    delay(2000)
    val qrResponse = apiService.getQRCode()

    // 3. Mostrar QR en ImageView
    val qrBitmap = decodeBase64ToBitmap(qrResponse.data.qrCodeBase64)
    imageViewQR.setImageBitmap(qrBitmap)

    // 4. Polling del estado cada 3 segundos
    while (isActive) {
        val status = apiService.getStatus()
        if (status.data.status == "connected") {
            Toast.makeText(context, "¡Conectado!", Toast.LENGTH_SHORT).show()
            break
        }
        delay(3000)
    }
}
```

### 1B. Inicio de Sesión con Número de Teléfono (Nuevo)

```kotlin
lifecycleScope.launch {
    // 1. Solicitar número al usuario
    val phoneNumber = editTextPhone.text.toString() // ej: "5215512345678"

    // 2. Iniciar sesión con número
    val startResponse = apiService.startSession(
        StartSessionRequest(phoneNumber = phoneNumber)
    )

    // 3. Esperar y obtener código de vinculación
    delay(3000)
    val linkCodeResponse = apiService.getLinkCode()

    if (linkCodeResponse.success) {
        // 4. Mostrar código formateado al usuario
        val code = linkCodeResponse.data.linkCode
        val formatted = code.chunked(4).joinToString("-") // "1234-5678"
        textViewCode.text = formatted

        // 5. Instrucciones al usuario
        textViewInstructions.text = """
            Recibirás un código en WhatsApp.
            Verifica que coincida con:
            $formatted
        """.trimIndent()

        // 6. Polling del estado
        while (isActive) {
            val status = apiService.getStatus()
            if (status.data.status == "connected") {
                Toast.makeText(context, "¡Vinculado exitosamente!", Toast.LENGTH_SHORT).show()
                break
            }
            delay(3000)
        }
    }
}
```

### 2. Enviar Mensaje

```kotlin
val request = SendMessageRequest(
    phone = "5215512345678",
    message = "Hola desde mi app Android!"
)
val response = apiService.sendMessage(request)
if (response.success) {
    Toast.makeText(context, "Mensaje enviado", Toast.LENGTH_SHORT).show()
}
```

### 3. Recibir Mensajes (Polling)

```kotlin
lifecycleScope.launch {
    while (isActive) {
        try {
            val messages = apiService.getMessages()
            messages.data.forEach { message ->
                addMessageToUI(message)
            }
            // Limpiar después de procesarlos
            apiService.clearMessages()
        } catch (e: Exception) {
            Log.e("API", "Error obteniendo mensajes", e)
        }
        delay(5000) // Esperar 5 segundos
    }
}
```

---

## Manejo de Errores

Todas las respuestas de error siguen este formato:

```json
{
  "success": false,
  "error": "Descripción del error"
}
```

**Códigos de estado HTTP:**
- `200` - Éxito
- `400` - Petición inválida
- `404` - No encontrado
- `500` - Error del servidor

---

## Ejemplo de Interfaz Retrofit (Kotlin)

```kotlin
interface WhatsAppApi {
    // Auth
    @POST("api/auth/start")
    suspend fun startSession(@Body request: StartSessionRequest = StartSessionRequest()): ApiResponse<SessionData>

    @GET("api/auth/status")
    suspend fun getStatus(): ApiResponse<SessionData>

    @GET("api/auth/qr")
    suspend fun getQRCode(): ApiResponse<QRCodeData>

    @GET("api/auth/link-code")
    suspend fun getLinkCode(): ApiResponse<LinkCodeData>

    @POST("api/auth/close")
    suspend fun closeSession(): ApiResponse<Unit>

    // Messages
    @POST("api/messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<Any>

    @Multipart
    @POST("api/messages/send-image")
    suspend fun sendImage(
        @Part("phone") phone: RequestBody,
        @Part image: MultipartBody.Part,
        @Part("caption") caption: RequestBody?
    ): ApiResponse<Any>

    @GET("api/messages")
    suspend fun getMessages(): ApiResponse<List<MessageReceived>>

    @DELETE("api/messages")
    suspend fun clearMessages(): ApiResponse<Unit>

    // Contacts
    @GET("api/contacts/chats")
    suspend fun getChats(): ApiResponse<List<Chat>>

    @GET("api/contacts")
    suspend fun getContacts(): ApiResponse<List<Contact>>

    @GET("api/contacts/battery")
    suspend fun getBatteryLevel(): ApiResponse<BatteryData>
}

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: String?
)

data class StartSessionRequest(
    val phoneNumber: String? = null
)

data class LinkCodeData(
    val linkCode: String,
    val status: String,
    val phoneNumber: Boolean
)
```

---

## Consejos de Seguridad

1. **Red Local:** Esta API está diseñada para uso local (mismo dispositivo).
2. **No exponer:** No expongas esta API a internet sin autenticación.
3. **HTTPS:** Si necesitas usar desde otra red, considera usar un túnel HTTPS.
4. **Tokens:** Para producción, implementa autenticación con tokens.

---

## Testing con curl

```bash
# Iniciar sesión con QR
curl -X POST http://localhost:3000/api/auth/start

# Iniciar sesión con número de teléfono
curl -X POST http://localhost:3000/api/auth/start \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"5215512345678"}'

# Obtener estado
curl http://localhost:3000/api/auth/status

# Obtener QR (método QR)
curl http://localhost:3000/api/auth/qr

# Obtener código de vinculación (método número)
curl http://localhost:3000/api/auth/link-code

# Enviar mensaje
curl -X POST http://localhost:3000/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"595973159937","message":"Probando desde termux"}'

# Obtener mensajes
curl http://localhost:3000/api/messages

# Obtener chats
curl http://localhost:3000/api/contacts/chats
```
