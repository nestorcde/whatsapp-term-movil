import { Request, Response } from 'express';
import whatsAppService from '../services/WhatsAppService';
import { ApiResponse, SendMessageRequest } from '../types';

/**
 * Enviar mensaje de texto
 */
export const sendMessage = async (req: Request, res: Response) => {
  try {
    const { phone, message } = req.body as SendMessageRequest;

    if (!phone || !message) {
      return res.status(400).json({
        success: false,
        error: 'Se requieren los campos: phone y message',
      } as ApiResponse);
    }

    const result = await whatsAppService.sendText(phone, message);

    res.json({
      success: true,
      message: 'Mensaje enviado exitosamente',
      data: result,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al enviar mensaje',
    } as ApiResponse);
  }
};

/**
 * Enviar imagen
 */
export const sendImage = async (req: Request, res: Response) => {
  try {
    const { phone, caption } = req.body;
    const file = req.file;

    if (!phone || !file) {
      return res.status(400).json({
        success: false,
        error: 'Se requieren los campos: phone y el archivo de imagen',
      } as ApiResponse);
    }

    const result = await whatsAppService.sendImage(phone, file.path, caption);

    res.json({
      success: true,
      message: 'Imagen enviada exitosamente',
      data: result,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al enviar imagen',
    } as ApiResponse);
  }
};

/**
 * Enviar archivo
 */
export const sendFile = async (req: Request, res: Response) => {
  try {
    const { phone, caption } = req.body;
    const file = req.file;

    if (!phone || !file) {
      return res.status(400).json({
        success: false,
        error: 'Se requieren los campos: phone y el archivo',
      } as ApiResponse);
    }

    const result = await whatsAppService.sendFile(
      phone,
      file.path,
      file.originalname,
      caption
    );

    res.json({
      success: true,
      message: 'Archivo enviado exitosamente',
      data: result,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al enviar archivo',
    } as ApiResponse);
  }
};

/**
 * Obtener mensajes recibidos
 */
export const getMessages = async (req: Request, res: Response) => {
  try {
    const messages = whatsAppService.getMessageQueue();

    res.json({
      success: true,
      data: messages,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener mensajes',
    } as ApiResponse);
  }
};

/**
 * Limpiar cola de mensajes
 */
export const clearMessages = async (req: Request, res: Response) => {
  try {
    whatsAppService.clearMessageQueue();

    res.json({
      success: true,
      message: 'Cola de mensajes limpiada',
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al limpiar mensajes',
    } as ApiResponse);
  }
};

/**
 * Obtener mensajes no leídos de WhatsApp
 */
export const getUnreadMessages = async (req: Request, res: Response) => {
  try {
    const messages = await whatsAppService.getUnreadMessages();

    res.json({
      success: true,
      data: messages,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener mensajes no leídos',
    } as ApiResponse);
  }
};
