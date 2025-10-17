# WhatsApp Termux API

Cliente de WhatsApp Web para Termux usando WPPConnect con API REST completa para integración con aplicaciones Android.

## Características

- **API REST completa** para control desde aplicaciones Android
- Inicio de sesión con código QR desde la app
- Envío de mensajes, imágenes y archivos
- Recepción de mensajes en tiempo real
- Gestión de chats y contactos
- Persistencia de sesión (no requiere escanear QR cada vez)
- Modo terminal standalone (opcional)

## Requisitos

### En Termux (Android)

```bash
# Actualizar paquetes
pkg update && pkg upgrade

# Instalar Node.js
pkg install nodejs

# Instalar dependencias del sistema
pkg install git
```

## Instalación

1. Clonar o navegar al proyecto:
```bash
cd whatsapp-term-movil
```

2. Instalar dependencias:
```bash
npm install
```

3. Compilar el proyecto:
```bash
npm run build
```

## Configuración

### Variables de entorno (opcional)

Copia el archivo de ejemplo y personaliza:

```bash
cp .env.example .env
```

Configuración disponible:

```env
# Puerto del servidor
PORT=3000

# Nivel de log (ERROR, WARN, INFO, DEBUG)
LOG_LEVEL=INFO
```

#### Niveles de log:

- `ERROR`: Solo errores críticos
- `WARN`: Errores y advertencias
- `INFO`: Información general (predeterminado)
- `DEBUG`: Todos los logs incluyendo depuración detallada

## Uso

### Modo API REST (Recomendado)

Inicia el servidor API para controlar desde tu app Android:

```bash
npm run dev
```

O en producción:
```bash
npm start
```

El servidor estará disponible en:
- `http://localhost:3000` (desde Termux)
- `http://127.0.0.1:3000` (desde app Android en el mismo dispositivo)

**🔄 Recuperación automática de sesiones:**
- Al iniciar el servidor, intentará recuperar automáticamente la sesión previa
- Si existe una sesión válida, se restaurará automáticamente sin necesidad de escanear QR
- Los logs mostrarán si la sesión fue restaurada o si necesitas iniciar una nueva

Para cambiar el nivel de log sin archivo .env:

```bash
LOG_LEVEL=DEBUG npm run dev
```

### Modo Bot Terminal (Opcional)

Para usar el bot directamente desde la terminal:

```bash
npm run bot
```

---

## API REST

### Endpoints Principales

#### Autenticación

- `POST /api/auth/start` - Iniciar sesión
- `GET /api/auth/status` - Estado de la sesión
- `GET /api/auth/qr` - Obtener código QR
- `POST /api/auth/close` - Cerrar sesión

#### Mensajes

- `POST /api/messages/send` - Enviar mensaje de texto
- `POST /api/messages/send-image` - Enviar imagen
- `POST /api/messages/send-file` - Enviar archivo
- `GET /api/messages` - Obtener mensajes recibidos
- `DELETE /api/messages` - Limpiar cola de mensajes

#### Contactos

- `GET /api/contacts/chats` - Obtener chats
- `GET /api/contacts` - Obtener contactos
- `GET /api/contacts/battery` - Nivel de batería

### Documentación Completa

Ver [API_DOCS.md](API_DOCS.md) para documentación detallada con ejemplos de código para Android (Kotlin/Retrofit).

## Ejemplo de Uso desde Android

### 1. Iniciar sesión y obtener QR

```kotlin
// Retrofit setup
val api = RetrofitClient.create<WhatsAppApi>()

// Iniciar sesión
lifecycleScope.launch {
    try {
        // 1. Iniciar sesión
        api.startSession()

        // 2. Esperar y obtener QR
        delay(2000)
        val qrResponse = api.getQRCode()

        // 3. Mostrar QR en ImageView
        val qrBitmap = decodeBase64ToBitmap(qrResponse.data.qrCodeBase64)
        imageView.setImageBitmap(qrBitmap)

        // 4. Polling del estado
        while (true) {
            val status = api.getStatus()
            if (status.data.status == "connected") {
                Toast.makeText(this@MainActivity, "¡Conectado!", Toast.LENGTH_SHORT).show()
                break
            }
            delay(3000)
        }
    } catch (e: Exception) {
        Log.e("WhatsApp", "Error", e)
    }
}
```

### 2. Enviar mensaje

```kotlin
lifecycleScope.launch {
    val request = SendMessageRequest(
        phone = "5215512345678",
        message = "Hola desde mi app!"
    )
    val response = api.sendMessage(request)
    if (response.success) {
        Toast.makeText(context, "Mensaje enviado", Toast.LENGTH_SHORT).show()
    }
}
```

## Estructura del proyecto

```
whatsapp-term-movil/
├── src/
│   ├── server.ts              # Servidor API REST
│   ├── index.ts               # Bot de terminal
│   ├── types/
│   │   └── index.ts           # Tipos e interfaces
│   ├── services/
│   │   └── WhatsAppService.ts # Lógica de WhatsApp
│   ├── controllers/           # Controladores de endpoints
│   │   ├── authController.ts
│   │   ├── messageController.ts
│   │   └── contactController.ts
│   ├── routes/                # Rutas de la API
│   │   ├── authRoutes.ts
│   │   ├── messageRoutes.ts
│   │   └── contactRoutes.ts
│   └── utils/
│       └── logger.ts          # Sistema de logging
├── dist/                      # Archivos compilados (generado)
├── session/                   # Datos de sesión (generado)
├── tokens/                    # Tokens de autenticación (generado)
├── uploads/                   # Archivos subidos (generado)
├── wppconnect/                # Repositorio clonado
├── .env.example               # Ejemplo de configuración
├── package.json
├── tsconfig.json
├── README.md
└── API_DOCS.md                # Documentación completa de la API
```

## API de WPPConnect

Para ver todas las funciones disponibles, consulta:
- [Documentación oficial de WPPConnect](https://wppconnect.io/wppconnect)
- Ejemplos en la carpeta `wppconnect/examples/`

### Funciones comunes

```typescript
// Enviar mensaje de texto
await client.sendText('5521999999999@c.us', 'Hola!');

// Enviar imagen
await client.sendImage(
  '5521999999999@c.us',
  'path/to/image.jpg',
  'caption',
  'Descripción de la imagen'
);

// Obtener todos los chats
const chats = await client.getAllChats();

// Obtener contactos
const contacts = await client.getAllContacts();

// Crear grupo
await client.createGroup('Nombre del Grupo', ['5521999999999@c.us']);
```

## Solución de problemas

### Error: "Failed to launch browser"

En Termux, asegúrate de tener suficiente espacio y memoria. Cierra otras aplicaciones.

### El QR no se muestra correctamente

Intenta hacer la terminal más grande o reducir el tamaño de fuente.

### La sesión no se guarda

Verifica que tienes permisos de escritura en la carpeta del proyecto.

## Limpieza

Para limpiar archivos generados:
```bash
npm run clean
```

Esto eliminará:
- Carpeta `dist/`
- Carpeta `session/`
- Carpeta `tokens/`

## Licencia

MIT

## Créditos

Este proyecto utiliza [WPPConnect](https://github.com/wppconnect-team/wppconnect) desarrollado por wppconnect-team.
