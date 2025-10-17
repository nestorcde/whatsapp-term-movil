# WhatsApp Termux - Monorepo

Proyecto completo para controlar WhatsApp Web desde aplicaciones Android usando Termux.

## 📁 Estructura del Proyecto

```
whatsapp-term-movil/
├── server/          # API REST de WhatsApp (Node.js + TypeScript)
├── app/             # Aplicación móvil Android (para desarrollo futuro)
└── wppconnect/      # Repositorio clonado de WPPConnect
```

## 🚀 Quick Start

### 1. Servidor API

```bash
cd server
npm install
npm run build
npm start
```

El servidor estará disponible en `http://localhost:3000`

Ver documentación completa en [server/README.md](server/README.md)

### 2. Aplicación Móvil (próximamente)

```bash
cd app
# Instrucciones de instalación aquí
```

## 📚 Documentación

- **[server/README.md](server/README.md)** - Documentación del servidor API
- **[server/API_DOCS.md](server/API_DOCS.md)** - Documentación completa de la API REST con ejemplos

## 🔧 Características del Servidor

- ✅ API REST completa para WhatsApp Web
- ✅ Dos métodos de autenticación (QR Code y número de teléfono)
- ✅ Recuperación automática de sesiones
- ✅ Sistema de logging configurable
- ✅ Envío de mensajes, imágenes y archivos
- ✅ Recepción de mensajes en tiempo real
- ✅ Gestión de chats y contactos

## 🎯 Roadmap

- [x] Servidor API REST
- [x] Autenticación con QR
- [x] Autenticación con número de teléfono
- [x] Recuperación automática de sesiones
- [x] Sistema de logging
- [ ] Aplicación móvil Android
- [ ] Interfaz de usuario para la app
- [ ] Notificaciones push
- [ ] Gestor de sesiones múltiples

## 📝 Requisitos

### Para el servidor (Termux):

- Node.js v16 o superior
- npm o yarn
- Termux (Android)

```bash
# En Termux
pkg update && pkg upgrade
pkg install nodejs git
```

### Para la app móvil (próximamente):

- Android Studio
- JDK 11+
- React Native o Flutter (por definir)

## 🤝 Contribuir

Las contribuciones son bienvenidas! Por favor:

1. Fork el proyecto
2. Crea tu rama de feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

MIT

## 🙏 Créditos

Este proyecto utiliza [WPPConnect](https://github.com/wppconnect-team/wppconnect) desarrollado por wppconnect-team.
