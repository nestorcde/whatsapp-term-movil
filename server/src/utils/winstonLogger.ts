/**
 * Logger de Winston personalizado para WPPConnect
 * Redirige los logs de wppconnect a nuestro sistema de logging
 */

import winston from 'winston';
import logger from './logger';
import { LogLevel } from './logger';

// Obtener nivel configurado
const getWinstonLevel = (): string => {
  const envLevel = process.env.LOG_LEVEL?.toUpperCase();
  const level = LogLevel[envLevel as keyof typeof LogLevel] ?? LogLevel.INFO;

  // Mapear nuestros niveles a Winston
  switch (level) {
    case LogLevel.ERROR:
      return 'error';
    case LogLevel.WARN:
      return 'warn';
    case LogLevel.INFO:
      return 'info';
    case LogLevel.DEBUG:
      return 'debug';
    default:
      return 'silent'; // Silenciar completamente
  }
};

// Crear logger de Winston que redirige a nuestro logger
const winstonLogger = winston.createLogger({
  level: getWinstonLevel(),
  silent: getWinstonLevel() === 'silent', // Silenciar si no es debug
  format: winston.format.simple(),
  transports: [
    new winston.transports.Console({
      format: winston.format.printf((info) => {
        // Redirigir a nuestro logger según el nivel
        const message = `[wppconnect] ${info.message}`;

        switch (info.level) {
          case 'error':
            logger.error(message);
            break;
          case 'warn':
            logger.warn(message);
            break;
          case 'info':
            logger.info(message);
            break;
          case 'debug':
          case 'verbose':
          case 'http':
            logger.debug(message);
            break;
        }

        return ''; // No retornar nada para evitar doble log
      }),
    }),
  ],
});

export default winstonLogger;
