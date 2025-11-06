import { Router } from 'express';
import * as contactController from '../controllers/contactController';

const router = Router();

/**
 * @route   GET /api/contacts/chats
 * @desc    Obtener todos los chats
 * @access  Public
 */
router.get('/chats', contactController.getAllChats);

/**
 * @route   GET /api/contacts
 * @desc    Obtener todos los contactos
 * @access  Public
 */
router.get('/', contactController.getAllContacts);

/**
 * @route   GET /api/contacts/battery
 * @desc    Obtener nivel de batería del teléfono
 * @access  Public
 */
router.get('/battery', contactController.getBatteryLevel);

export default router;
