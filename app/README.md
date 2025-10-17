# WhatsApp Termux - Aplicación Móvil Android

Aplicación Android para controlar WhatsApp Web a través de la API REST del servidor.

## 🚧 En Desarrollo

Esta carpeta está preparada para el desarrollo de la aplicación móvil.

## 🎯 Características Planeadas

- [ ] Interfaz de usuario para inicio de sesión
  - Escaneo de QR Code
  - Inicio de sesión con número de teléfono
- [ ] Gestión de sesiones
- [ ] Envío y recepción de mensajes
- [ ] Lista de chats y contactos
- [ ] Notificaciones de mensajes nuevos
- [ ] Envío de imágenes y archivos
- [ ] Estado de conexión en tiempo real

## 🛠️ Stack Tecnológico (por definir)

Opciones consideradas:

### Opción 1: React Native
- Pros: JavaScript/TypeScript, reutilización de código
- Contras: Performance en aplicaciones complejas

### Opción 2: Flutter
- Pros: Performance nativa, UI hermosa
- Contras: Dart como lenguaje adicional

### Opción 3: Kotlin Nativo
- Pros: Performance máxima, nativo Android
- Contras: Solo Android, más código

## 📋 Requisitos Previos

- Servidor API corriendo en Termux
- Android Studio instalado
- JDK 11 o superior

## 🚀 Configuración (pendiente)

```bash
# Instrucciones de instalación vendrán aquí
cd app
# npm install o flutter pub get
```

## 📱 Conexión con el Servidor

La aplicación se conectará al servidor de Termux en:
```
http://127.0.0.1:3000
```

## 📚 Documentación de la API

Ver [../server/API_DOCS.md](../server/API_DOCS.md) para la documentación completa de los endpoints disponibles.

## 🎨 Mockups y Diseño

_(Por agregar)_

## 🤝 Contribuir

¿Tienes experiencia en desarrollo móvil? Tus contribuciones son bienvenidas!

1. Elige tu stack preferido
2. Crea la estructura inicial
3. Implementa la UI básica
4. Conecta con la API del servidor

## 📝 Notas

- La app debe funcionar en el mismo dispositivo donde corre Termux
- Necesita acceso a la red local (localhost)
- Debe manejar la reconexión automática si el servidor se reinicia
