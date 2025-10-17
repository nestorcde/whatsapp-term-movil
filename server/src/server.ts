// Cargar variables de entorno primero
import dotenv from 'dotenv';
dotenv.config();

import express, { Application, Request, Response } from 'express';
import cors from 'cors';
import authRoutes from './routes/authRoutes';
import messageRoutes from './routes/messageRoutes';
import contactRoutes from './routes/contactRoutes';
import logger from './utils/logger';
import whatsAppService from './services/WhatsAppService';

const app: Application = express();
const PORT = process.env.PORT || 3000;

// Middlewares
app.use(cors()); // Permitir todas las peticiones CORS
app.use(express.json()); // Parser de JSON
app.use(express.urlencoded({ extended: true })); // Parser de URL-encoded

// Crear carpeta uploads si no existe
import { existsSync, mkdirSync } from 'fs';
if (!existsSync('uploads')) {
  mkdirSync('uploads');
}

// Rutas
app.use('/api/auth', authRoutes);
app.use('/api/messages', messageRoutes);
app.use('/api/contacts', contactRoutes);

// Ruta de bienvenida
app.get('/', (req: Request, res: Response) => {
  res.json({
    success: true,
    message: 'WhatsApp API REST - Termux',
    version: '1.0.0',
    endpoints: {
      auth: {
        start: 'POST /api/auth/start - Iniciar sesión',
        status: 'GET /api/auth/status - Estado de la sesión',
        qr: 'GET /api/auth/qr - Obtener código QR',
        close: 'POST /api/auth/close - Cerrar sesión',
        check: 'GET /api/auth/check - Verificar conexión',
      },
      messages: {
        send: 'POST /api/messages/send - Enviar mensaje de texto',
        sendImage: 'POST /api/messages/send-image - Enviar imagen',
        sendFile: 'POST /api/messages/send-file - Enviar archivo',
        get: 'GET /api/messages - Obtener mensajes recibidos',
        clear: 'DELETE /api/messages - Limpiar mensajes',
        unread: 'GET /api/messages/unread - Mensajes no leídos',
      },
      contacts: {
        chats: 'GET /api/contacts/chats - Obtener chats',
        contacts: 'GET /api/contacts - Obtener contactos',
        battery: 'GET /api/contacts/battery - Nivel de batería',
      },
    },
  });
});

// Ruta de health check
app.get('/health', (req: Request, res: Response) => {
  res.json({
    success: true,
    status: 'OK',
    timestamp: new Date().toISOString(),
  });
});

// Manejador de rutas no encontradas
app.use((req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: 'Ruta no encontrada',
    path: req.path,
  });
});

// Manejador de errores global
app.use((err: any, req: Request, res: Response, next: any) => {
  logger.error('Error en petición', err);
  res.status(500).json({
    success: false,
    error: err.message || 'Error interno del servidor',
  });
});

// Iniciar servidor
app.listen(PORT, async () => {
  logger.info('='.repeat(50));
  logger.info('WhatsApp API REST - Termux');
  logger.info('='.repeat(50));
  logger.info(`Servidor iniciado en puerto ${PORT}`);
  logger.info(`URL local: http://localhost:${PORT}`);
  logger.info(`URL Android: http://127.0.0.1:${PORT}`);
  logger.info('='.repeat(50));
  logger.info('Nivel de log: ' + (process.env.LOG_LEVEL || 'INFO'));
  logger.debug('Endpoints disponibles:');
  logger.debug('  - POST /api/auth/start       - Iniciar sesión');
  logger.debug('  - GET  /api/auth/status      - Estado');
  logger.debug('  - GET  /api/auth/qr          - QR');
  logger.debug('  - GET  /api/auth/link-code   - Código vinculación');
  logger.debug('  - POST /api/messages/send    - Enviar mensaje');
  logger.debug('  - GET  /api/messages         - Obtener mensajes');
  logger.info('='.repeat(50));

  // Intentar recuperar sesión existente
  try {
    const restored = await whatsAppService.tryRestoreSession();
    if (!restored) {
      logger.info('No hay sesión previa - Use POST /api/auth/start para iniciar sesión');
    }
  } catch (error) {
    logger.debug('Error al intentar recuperar sesión', error);
  }
});

// Manejo graceful de cierre
process.on('SIGINT', () => {
  logger.info('Cerrando servidor...');
  process.exit(0);
});

process.on('SIGTERM', () => {
  logger.info('Cerrando servidor...');
  process.exit(0);
});

export default app;
