/**
 * Sistema de logging con niveles configurables
 */

export enum LogLevel {
  ERROR = 0,
  WARN = 1,
  INFO = 2,
  DEBUG = 3,
}

class Logger {
  private level: LogLevel;
  private colors = {
    reset: '\x1b[0m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    gray: '\x1b[90m',
  };

  constructor() {
    // Obtener nivel de log desde variable de entorno o usar INFO por defecto
    const envLevel = process.env.LOG_LEVEL?.toUpperCase();
    this.level = LogLevel[envLevel as keyof typeof LogLevel] ?? LogLevel.INFO;
  }

  /**
   * Establecer nivel de log
   */
  setLevel(level: LogLevel): void {
    this.level = level;
  }

  /**
   * Obtener timestamp formateado
   */
  private getTimestamp(): string {
    const now = new Date();
    return now.toISOString();
  }

  /**
   * Formatear mensaje de log
   */
  private format(level: string, message: string, color: string): string {
    const timestamp = this.getTimestamp();
    return `${color}[${timestamp}] [${level}]${this.colors.reset} ${message}`;
  }

  /**
   * Log de error (siempre se muestra)
   */
  error(message: string, ...args: any[]): void {
    if (this.level >= LogLevel.ERROR) {
      console.error(this.format('ERROR', message, this.colors.red), ...args);
    }
  }

  /**
   * Log de advertencia
   */
  warn(message: string, ...args: any[]): void {
    if (this.level >= LogLevel.WARN) {
      console.warn(this.format('WARN', message, this.colors.yellow), ...args);
    }
  }

  /**
   * Log de información
   */
  info(message: string, ...args: any[]): void {
    if (this.level >= LogLevel.INFO) {
      console.log(this.format('INFO', message, this.colors.blue), ...args);
    }
  }

  /**
   * Log de depuración
   */
  debug(message: string, ...args: any[]): void {
    if (this.level >= LogLevel.DEBUG) {
      console.log(this.format('DEBUG', message, this.colors.gray), ...args);
    }
  }
}

// Exportar instancia singleton
export default new Logger();
