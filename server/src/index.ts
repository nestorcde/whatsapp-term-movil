import * as wppconnect from '@wppconnect-team/wppconnect';

/**
 * Cliente de WhatsApp para Termux
 * Configurado para funcionar en terminal sin interfaz gráfica
 */
async function main() {
  console.log('Iniciando cliente de WhatsApp para Termux...');
  console.log('');

  try {
    // Crear cliente con configuración optimizada para Termux
    const client = await wppconnect.create({
      session: 'whatsapp-termux', // Nombre de la sesión
      headless: true, // Modo headless (sin GUI)
      devtools: false, // Sin DevTools
      useChrome: false, // Usar Chromium de Puppeteer
      logQR: true, // Mostrar QR en la terminal
      browserArgs: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-accelerated-2d-canvas',
        '--no-first-run',
        '--no-zygote',
        '--disable-gpu',
      ],
      // Callbacks para eventos
      catchQR: (base64Qr, asciiQR) => {
        console.log('Código QR recibido:');
        console.log('');
        console.log(asciiQR); // QR en ASCII para la terminal
        console.log('');
        console.log('Escanea el código QR con tu WhatsApp para conectar');
      },
      statusFind: (statusSession, session) => {
        console.log(`Estado de sesión: ${statusSession}`);
        if (statusSession === 'qrReadSuccess') {
          console.log('QR escaneado exitosamente!');
        }
        if (statusSession === 'inChat') {
          console.log('Sesión conectada y lista!');
        }
      },
    });

    console.log('Cliente creado exitosamente!');
    console.log('');

    // Configurar manejador de mensajes
    client.onMessage(async (message) => {
      try {
        console.log('Nuevo mensaje recibido:');
        console.log(`De: ${message.from}`);
        console.log(`Mensaje: ${message.body ?? '[sin contenido]'}`);
        console.log(`Es grupo: ${message.isGroupMsg}`);
        console.log('---');

        // Verificar que el mensaje tenga contenido
        if (!message.body) return;

        // Ejemplo: responder a mensajes privados con "hola"
        if (message.body.toLowerCase() === 'hola' && !message.isGroupMsg) {
          await client.sendText(
            message.from,
            'Hola! Soy un bot de WhatsApp corriendo en Termux 🤖'
          );
          console.log('Respuesta enviada!');
        }

        // Ejemplo: comando para obtener información del bot
        if (message.body.toLowerCase() === '/info' && !message.isGroupMsg) {
          const info = await client.getHostDevice();
          await client.sendText(
            message.from,
            `Bot activo en:\nDispositivo: ${info.phone.device_manufacturer} ${info.phone.device_model}\nVersión WhatsApp: ${info.phone.wa_version}`
          );
        }

        // Ejemplo: comando para obtener estado de batería
        if (message.body.toLowerCase() === '/bateria' && !message.isGroupMsg) {
          const battery = await client.getBatteryLevel();
          await client.sendText(
            message.from,
            `Nivel de batería: ${battery}%`
          );
        }
      } catch (error) {
        console.error('Error al procesar mensaje:', error);
      }
    });

    // Eventos de estado
    client.onStateChange((state) => {
      console.log(`Estado del cliente: ${state}`);
    });

    // Mantener el proceso activo
    console.log('Bot activo. Escuchando mensajes...');
    console.log('Presiona Ctrl+C para detener el bot');
    console.log('');
    console.log('Comandos disponibles:');
    console.log('- hola: Saludo del bot');
    console.log('- /info: Información del dispositivo');
    console.log('- /bateria: Nivel de batería');
    console.log('');

    // Información adicional del cliente
    const isConnected = await client.isConnected();
    console.log(`Conectado: ${isConnected}`);

  } catch (error) {
    console.error('Error al iniciar el cliente:', error);
    process.exit(1);
  }
}

// Manejar cierre graceful
process.on('SIGINT', () => {
  console.log('\n\nCerrando cliente de WhatsApp...');
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\n\nCerrando cliente de WhatsApp...');
  process.exit(0);
});

// Iniciar la aplicación
main().catch((error) => {
  console.error('Error fatal:', error);
  process.exit(1);
});
