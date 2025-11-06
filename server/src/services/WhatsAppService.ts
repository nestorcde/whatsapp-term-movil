import * as wppconnect from '@wppconnect-team/wppconnect';
import QRCode from 'qrcode';
import * as fs from 'fs';
import * as path from 'path';
import { SessionData, SessionStatus, MessageReceived } from '../types';
import logger from '../utils/logger';
import winstonLogger from '../utils/winstonLogger';

class WhatsAppService {
  private session: SessionData = {
    client: null,
    status: SessionStatus.DISCONNECTED,
    qrCode: null,
    qrCodeBase64: null,
    linkCode: null,
    connectedPhone: null,
    usePhoneNumber: false
  };

  private messageQueue: MessageReceived[] = [];
  private maxQueueSize = 100;
  private sessionName = 'api-session';
  private lastStartAttempt: number = 0;
  private minTimeBetweenStarts = 60000; // 1 minuto entre intentos de inicio

  /**
   * Obtener el estado de la sesión
   */
  getSessionStatus(): SessionData {
    return {
      ...this.session,
      client: null, // No enviar el cliente en la respuesta
    };
  }

  /**
   * Configurar handlers del cliente
   */
  private setupClientHandlers(client: any): void {
    // Configurar manejador de mensajes
    client.onMessage(async (message: any) => {
      if (!message.body) return;

      const messageData: MessageReceived = {
        id: message.id,
        from: message.from,
        body: message.body,
        timestamp: message.timestamp,
        isGroupMsg: message.isGroupMsg,
        type: message.type,
      };

      // Agregar a la cola de mensajes
      this.messageQueue.push(messageData);

      // Mantener solo los últimos N mensajes
      if (this.messageQueue.length > this.maxQueueSize) {
        this.messageQueue.shift();
      }

      logger.debug(`Mensaje recibido de ${message.from}`);
    });
  }

  /**
   * Intentar recuperar sesión existente
   */
  async tryRestoreSession(): Promise<boolean> {
    try {
      logger.info('Verificando sesión existente...');

      const config: any = {
        session: this.sessionName,
        headless: true,
        devtools: false,
        useChrome: false,
        debug: false,
        logQR: false,
        disableWelcome: true,
        updatesLog: false,
        logger: winstonLogger,
        browserArgs: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-accelerated-2d-canvas',
          '--no-first-run',
          '--no-zygote',
          '--disable-gpu',
        ],
        catchQR: () => {
          // No hacer nada con QR en recuperación
          logger.debug('QR solicitado durante recuperación - sesión expirada');
        },
        statusFind: (statusSession: string) => {
          logger.debug(`Recuperación - Estado: ${statusSession}`);

          if (statusSession === 'inChat') {
            this.session.status = SessionStatus.CONNECTED;
            logger.info('Sesión recuperada exitosamente');
          }

          if (statusSession === 'notLogged') {
            logger.info('No hay sesión previa para recuperar');
          }

          if (statusSession === 'browserClose' || statusSession === 'serverClose') {
            this.session.status = SessionStatus.DISCONNECTED;
          }
        },
      };

      const client = await wppconnect.create(config);
      const isConnected = await client.isConnected();

      if (isConnected) {
        this.session.client = client;
        this.session.status = SessionStatus.CONNECTED;
        this.setupClientHandlers(client);

        // Obtener información del dispositivo
        try {
          const hostDevice = await client.getHostDevice();
          this.session.connectedPhone = hostDevice.phone.device_manufacturer + ' ' + hostDevice.phone.device_model;
          logger.info(`Sesión restaurada - Dispositivo: ${this.session.connectedPhone}`);
        } catch (error) {
          logger.debug('No se pudo obtener info del dispositivo');
        }

        return true;
      } else {
        // Cerrar cliente si no está conectado
        await client.close();
        return false;
      }
    } catch (error) {
      logger.debug('No se pudo recuperar sesión', error);
      return false;
    }
  }

  /**
   * Iniciar sesión de WhatsApp
   */
  async startSession(phoneNumber?: string): Promise<void> {
    // Verificar límite de tasa
    const now = Date.now();
    const timeSinceLastAttempt = now - this.lastStartAttempt;

    if (this.lastStartAttempt > 0 && timeSinceLastAttempt < this.minTimeBetweenStarts) {
      const waitTime = Math.ceil((this.minTimeBetweenStarts - timeSinceLastAttempt) / 1000);
      throw new Error(
        `Por favor espera ${waitTime} segundos antes de intentar iniciar sesión nuevamente. ` +
        `WhatsApp limita la frecuencia de intentos de vinculación.`
      );
    }

    this.lastStartAttempt = now;

    // Si ya existe una sesión, cerrarla primero
    if (this.session.client) {
      logger.info('Cerrando sesión anterior antes de iniciar nueva...');
      await this.closeSession();
    }

    // Siempre esperar un poco antes de iniciar nueva sesión
    // Esto asegura que cualquier proceso del navegador anterior haya terminado
    logger.debug('Esperando a que procesos anteriores finalicen...');
    await new Promise(resolve => setTimeout(resolve, 5000));

    this.session.status = SessionStatus.CONNECTING;
    this.session.usePhoneNumber = !!phoneNumber;

    try {
      const config: any = {
        session: this.sessionName,
        headless: true,
        devtools: false,
        useChrome: false,
        debug: false, // Desactivar modo debug de wppconnect
        logQR: false, // Desactivar QR en consola
        disableWelcome: true, // Desactivar mensaje de bienvenida
        updatesLog: false, // Desactivar logs de actualizaciones
        logger: winstonLogger, // Usar nuestro logger personalizado
        browserArgs: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-accelerated-2d-canvas',
          '--no-first-run',
          '--no-zygote',
          '--disable-gpu',
        ],
        catchQR: async (base64Qr: string, asciiQR: string) => {
          logger.debug('QR Code generado');
          this.session.qrCode = asciiQR;
          this.session.qrCodeBase64 = base64Qr;
          this.session.status = SessionStatus.QR_READY;

          // Generar QR como data URL para la app
          try {
            const qrDataUrl = await QRCode.toDataURL(base64Qr);
            this.session.qrCodeBase64 = qrDataUrl;
          } catch (error) {
            logger.error('Error generando QR data URL', error);
          }
        },
        catchLinkCode: (code: string) => {
          logger.info(`Código de vinculación: ${code}`);
          this.session.linkCode = code;
          this.session.status = SessionStatus.LINK_CODE_READY;
        },
        statusFind: (statusSession: string, session: string) => {
          logger.debug(`Estado: ${statusSession}`);

          if (statusSession === 'qrReadSuccess') {
            logger.info('QR escaneado exitosamente');
          }

          if (statusSession === 'inChat') {
            this.session.status = SessionStatus.CONNECTED;
            this.session.qrCode = null;
            this.session.qrCodeBase64 = null;
            this.session.linkCode = null;
            logger.info('Sesión conectada');
          }

          if (statusSession === 'browserClose' || statusSession === 'serverClose') {
            this.session.status = SessionStatus.DISCONNECTED;
            logger.warn(`Sesión desconectada: ${statusSession}`);
          }
        },
      };

      // Si se proporciona número de teléfono, agregarlo a la configuración
      if (phoneNumber) {
        config.phoneNumber = phoneNumber;
        logger.info(`Iniciando sesión con número: ${phoneNumber}`);
      } else {
        logger.info('Iniciando sesión con QR');
      }

      const client = await wppconnect.create(config);

      this.session.client = client;
      this.setupClientHandlers(client);

      // Obtener información del teléfono conectado
      const isConnected = await client.isConnected();
      if (isConnected) {
        try {
          const hostDevice = await client.getHostDevice();
          this.session.connectedPhone = hostDevice.phone.device_manufacturer + ' ' + hostDevice.phone.device_model;
          logger.debug(`Dispositivo conectado: ${this.session.connectedPhone}`);
        } catch (error) {
          logger.warn('No se pudo obtener info del dispositivo');
        }
      }

    } catch (error) {
      logger.error('Error al iniciar sesión', error);
      this.session.status = SessionStatus.FAILED;
      throw error;
    }
  }

  /**
   * Cerrar sesión de WhatsApp
   */
  async closeSession(): Promise<void> {
    if (this.session.client) {
      try {
        await this.session.client.close();
        logger.info('Sesión cerrada');
      } catch (error) {
        logger.error('Error al cerrar sesión', error);
      }
      this.session.client = null;
    }

    // Esperar a que el navegador se cierre completamente
    await new Promise(resolve => setTimeout(resolve, 3000));

    // Eliminar archivos de tokens para que no se pueda recuperar la sesión
    try {
      const tokensPath = path.join(process.cwd(), 'tokens', this.sessionName);
      if (fs.existsSync(tokensPath)) {
        // Intentar varias veces por si hay locks de archivos
        let attempts = 0;
        const maxAttempts = 5;

        while (attempts < maxAttempts) {
          try {
            fs.rmSync(tokensPath, { recursive: true, force: true });
            logger.info(`Tokens eliminados: ${tokensPath}`);
            break;
          } catch (error: any) {
            attempts++;
            if (attempts >= maxAttempts) {
              throw error;
            }
            logger.debug(`Intento ${attempts} de eliminar tokens falló, reintentando...`);
            await new Promise(resolve => setTimeout(resolve, 1000));
          }
        }
      }
    } catch (error) {
      logger.warn('No se pudieron eliminar los tokens automáticamente. Puede que haya procesos usando los archivos.');
      logger.debug('Error al eliminar tokens', error);
    }

    this.session.status = SessionStatus.DISCONNECTED;
    this.session.qrCode = null;
    this.session.qrCodeBase64 = null;
    this.session.linkCode = null;
    this.session.connectedPhone = null;
    this.session.usePhoneNumber = false;
    this.messageQueue = [];
  }

  /**
   * Verificar si la sesión está conectada
   */
  async isConnected(): Promise<boolean> {
    if (!this.session.client) return false;

    try {
      return await this.session.client.isConnected();
    } catch (error) {
      return false;
    }
  }

  /**
   * Enviar mensaje de texto
   */
  async sendText(phone: string, message: string): Promise<any> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    // Formatear número de teléfono
    const formattedPhone = this.formatPhone(phone);
    return await this.session.client.sendText(formattedPhone, message);
  }

  /**
   * Enviar imagen
   */
  async sendImage(phone: string, imagePath: string, caption?: string): Promise<any> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    const formattedPhone = this.formatPhone(phone);
    return await this.session.client.sendImage(
      formattedPhone,
      imagePath,
      'image',
      caption || ''
    );
  }

  /**
   * Enviar archivo
   */
  async sendFile(phone: string, filePath: string, filename: string, caption?: string): Promise<any> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    const formattedPhone = this.formatPhone(phone);
    return await this.session.client.sendFile(
      formattedPhone,
      filePath,
      filename,
      caption || ''
    );
  }

  /**
   * Obtener todos los chats
   */
  async getAllChats(): Promise<any[]> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    return await this.session.client.getAllChats();
  }

  /**
   * Obtener contactos
   */
  async getAllContacts(): Promise<any[]> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    return await this.session.client.getAllContacts();
  }

  /**
   * Obtener mensajes no leídos
   */
  async getUnreadMessages(): Promise<any[]> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    return await this.session.client.getUnreadMessages(true, true, true);
  }

  /**
   * Obtener cola de mensajes recibidos
   */
  getMessageQueue(): MessageReceived[] {
    return [...this.messageQueue];
  }

  /**
   * Limpiar cola de mensajes
   */
  clearMessageQueue(): void {
    this.messageQueue = [];
  }

  /**
   * Obtener información de batería
   */
  async getBatteryLevel(): Promise<number> {
    if (!this.session.client) {
      throw new Error('No hay sesión activa');
    }

    return await this.session.client.getBatteryLevel();
  }

  /**
   * Formatear número de teléfono para WhatsApp
   */
  private formatPhone(phone: string): string {
    // Remover caracteres no numéricos
    let cleaned = phone.replace(/\D/g, '');

    // Si no tiene @c.us al final, agregarlo
    if (!phone.includes('@c.us')) {
      cleaned = `${cleaned}@c.us`;
    }

    return cleaned;
  }
}

// Exportar instancia singleton
export default new WhatsAppService();
