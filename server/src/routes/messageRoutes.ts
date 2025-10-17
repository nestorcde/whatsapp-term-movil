import { Router } from 'express';
import multer from 'multer';
import * as messageController from '../controllers/messageController';

const router = Router();

// Configurar multer para subida de archivos
const upload = multer({ dest: 'uploads/' });

/**
 * @route   POST /api/messages/send
 * @desc    Enviar mensaje de texto
 * @access  Public
 */
router.post('/send', messageController.sendMessage);

/**
 * @route   POST /api/messages/send-image
 * @desc    Enviar imagen
 * @access  Public
 */
router.post('/send-image', upload.single('image'), messageController.sendImage);

/**
 * @route   POST /api/messages/send-file
 * @desc    Enviar archivo
 * @access  Public
 */
router.post('/send-file', upload.single('file'), messageController.sendFile);

/**
 * @route   GET /api/messages
 * @desc    Obtener mensajes recibidos
 * @access  Public
 */
router.get('/', messageController.getMessages);

/**
 * @route   DELETE /api/messages
 * @desc    Limpiar cola de mensajes
 * @access  Public
 */
router.delete('/', messageController.clearMessages);

/**
 * @route   GET /api/messages/unread
 * @desc    Obtener mensajes no leídos
 * @access  Public
 */
router.get('/unread', messageController.getUnreadMessages);

export default router;
