import { Router } from 'express';
import * as authController from '../controllers/authController';

const router = Router();

/**
 * @route   POST /api/auth/start
 * @desc    Iniciar sesión de WhatsApp
 * @access  Public
 */
router.post('/start', authController.startSession);

/**
 * @route   GET /api/auth/status
 * @desc    Obtener estado de la sesión
 * @access  Public
 */
router.get('/status', authController.getStatus);

/**
 * @route   GET /api/auth/qr
 * @desc    Obtener código QR para escanear
 * @access  Public
 */
router.get('/qr', authController.getQRCode);

/**
 * @route   GET /api/auth/link-code
 * @desc    Obtener código de vinculación (8 dígitos)
 * @access  Public
 */
router.get('/link-code', authController.getLinkCode);

/**
 * @route   POST /api/auth/close
 * @desc    Cerrar sesión de WhatsApp
 * @access  Public
 */
router.post('/close', authController.closeSession);

/**
 * @route   GET /api/auth/check
 * @desc    Verificar si está conectado
 * @access  Public
 */
router.get('/check', authController.checkConnection);

export default router;
