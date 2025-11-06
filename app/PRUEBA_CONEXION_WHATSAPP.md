# Prueba de Conexión WhatsApp - Android App

## ✅ Implementación Completada

Hemos implementado la conexión completa entre la app Android y el servidor Termux de WhatsApp.

### Funcionalidades Implementadas:

1. ✅ **Login con credenciales fijas** (admin/admin)
2. ✅ **Verificación de sesión existente** al abrir la pantalla WhatsApp
3. ✅ **Inicio de sesión con número de teléfono**
4. ✅ **Generación y visualización del código de vinculación de 8 dígitos**
5. ✅ **Polling automático del estado** de la sesión (cada 3 segundos)
6. ✅ **Envío de mensajes de prueba** cuando está conectado
7. ✅ **Cerrar sesión de WhatsApp**

## 🚀 Cómo Probar

### Paso 1: Iniciar el Servidor Termux

En Termux (en el mismo dispositivo Android):

```bash
cd server
npm run dev
```

El servidor debe estar corriendo en `http://127.0.0.1:3000`

### Paso 2: Compilar e Instalar la App

```bash
cd app/android
./gradlew assembleDebug
```

Instalar el APK generado en: `app/android/app/build/outputs/apk/debug/app-debug.apk`

### Paso 3: Probar el Flujo Completo

1. **Abrir la app**
   - Verás la pantalla de Login

2. **Login**
   - Usuario: `admin`
   - Contraseña: `admin`
   - Presionar "Iniciar Sesión"

3. **Pantalla WhatsApp Session**
   - La app verificará si hay una sesión activa
   - Si NO hay sesión:
     * Ingresar tu número de teléfono (formato: `595973159937`)
     * Presionar "Iniciar Sesión"
     * Esperar 3 segundos a que se genere el código

4. **Código de Vinculación**
   - Se mostrará un código de 8 dígitos grande (ej: `1234-5678`)
   - Abrir WhatsApp en tu teléfono
   - Ir a: Menú → Dispositivos Vinculados → Vincular Dispositivo
   - Verificar que el código coincida con el mostrado en la app
   - Confirmar en WhatsApp

5. **Conexión Exitosa**
   - La app detectará la conexión automáticamente (polling cada 3s)
   - Se mostrará "WhatsApp Conectado ✅"
   - Ahora puedes enviar mensajes de prueba

6. **Enviar Mensaje de Prueba**
   - Ingresar un número de destino
   - Presionar "Enviar Mensaje de Prueba"
   - El mensaje se enviará: "Hola! Este es un mensaje de prueba desde WhatsApp Bulk Sender 📱"

## 📱 Arquitectura Implementada

```
MainActivity
  └── LoginScreen (con LoginViewModel)
        └── WhatsAppSessionScreen (con WhatsAppViewModel)
              ├── WhatsAppRepository
              │     └── WhatsAppApi (Retrofit)
              │           └── Servidor Termux (localhost:3000)
              └── Polling automático de estado
```

## 🎯 Estados de la Sesión

La app maneja estos estados automáticamente:

- **DISCONNECTED** → Muestra formulario para ingresar número
- **CONNECTING** → Muestra loading
- **LINK_CODE_READY** → Muestra código de 8 dígitos
- **CONNECTED** → Muestra opciones para enviar mensajes

## 🔧 Configuración Técnica

### Retrofit Configurado

- **Base URL**: `http://127.0.0.1:3000/`
- **Timeout**: 30 segundos
- **Logging**: Habilitado (nivel BODY)
- **Cleartext Traffic**: Habilitado en AndroidManifest

### Módulos de Hilt

1. **NetworkModule** - Provee Retrofit y WhatsAppApi
2. **RepositoryModule** - Provee WhatsAppRepository y AuthRepository

### Permisos Requeridos

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🐛 Solución de Problemas

### Error: "Error de conexión"

**Causa**: El servidor Termux no está corriendo o no está accesible.

**Solución**:
1. Verificar que el servidor esté corriendo: `curl http://127.0.0.1:3000/health`
2. Verificar que responda: debería devolver `{"success":true,"status":"OK"}`

### Error: "Número de teléfono inválido"

**Causa**: Formato incorrecto del número.

**Solución**:
- Usar formato: `{códigoPaís}{número}` sin +, espacios ni guiones
- Ejemplo correcto: `595973159937` (Paraguay)
- Ejemplo incorrecto: `+595 973 159937`

### El código no aparece después de 3 segundos

**Causa**: El servidor puede tardar más en generar el código.

**Solución**:
- Esperar hasta 5-10 segundos
- Verificar logs del servidor
- Verificar que WhatsApp Web esté funcionando correctamente

### No detecta la conexión después de confirmar en WhatsApp

**Causa**: El polling puede estar desactivado o el servidor no responde.

**Solución**:
- Verificar que el polling esté activo (cada 3 segundos)
- Hacer `GET /api/auth/status` manualmente para ver el estado
- Reiniciar la app

## 📝 Notas Importantes

1. **Servidor local**: La app solo funciona si el servidor Termux está en el mismo dispositivo
2. **Sesión persistente**: Si cierras y vuelves a abrir la app, verificará si hay sesión activa
3. **Polling automático**: Se detiene cuando se conecta o falla (para ahorrar batería)
4. **Cleartext HTTP**: Solo para desarrollo local, en producción usar HTTPS

## 🎉 Próximos Pasos

Ahora que la conexión WhatsApp está funcionando, podemos continuar con:

1. Implementar pantallas de Campaign List
2. Campaign Details con selección de cantidad
3. Ejecución de campañas con WorkManager
4. Dashboard con estadísticas

---

**Estado**: ✅ Conexión WhatsApp completamente funcional
**Última actualización**: 2025-10-21
