import { Request, Response } from 'express';
import whatsAppService from '../services/WhatsAppService';
import { ApiResponse, StartSessionRequest } from '../types';
import logger from '../utils/logger';

/**
 * Iniciar sesión de WhatsApp
 * Soporta inicio con QR (sin phoneNumber) o con código de vinculación (con phoneNumber)
 */
export const startSession = async (req: Request, res: Response) => {
  try {
    const { phoneNumber } = req.body as StartSessionRequest;
    const status = whatsAppService.getSessionStatus();

    if (status.status === 'connected' || status.status === 'connecting') {
      return res.status(400).json({
        success: false,
        error: 'Ya existe una sesión activa o en proceso de conexión',
        data: status,
      } as ApiResponse);
    }

    // Iniciar sesión en segundo plano
    whatsAppService.startSession(phoneNumber).catch((error) => {
      logger.error('Error en startSession', error);
    });

    const message = phoneNumber
      ? 'Iniciando sesión con número de teléfono. Use /auth/link-code para obtener el código de vinculación.'
      : 'Iniciando sesión con QR. Use /auth/qr para obtener el código QR.';

    res.json({
      success: true,
      message,
      data: whatsAppService.getSessionStatus(),
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al iniciar sesión',
    } as ApiResponse);
  }
};

/**
 * Obtener estado de la sesión
 */
export const getStatus = async (req: Request, res: Response) => {
  try {
    const status = whatsAppService.getSessionStatus();
    const isConnected = await whatsAppService.isConnected();

    res.json({
      success: true,
      data: {
        ...status,
        isConnected,
      },
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener estado',
    } as ApiResponse);
  }
};

/**
 * Obtener código QR
 */
export const getQRCode = async (req: Request, res: Response) => {
  try {
    const status = whatsAppService.getSessionStatus();

    if (!status.qrCodeBase64) {
      return res.status(404).json({
        success: false,
        error: 'No hay código QR disponible. Inicie una sesión primero.',
      } as ApiResponse);
    }

    res.json({
      success: true,
      data: {
        qrCode: status.qrCodeBase64,
        qrCodeAscii: status.qrCode,
        status: status.status,
      },
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener código QR',
    } as ApiResponse);
  }
};

/**
 * Cerrar sesión
 */
export const closeSession = async (req: Request, res: Response) => {
  try {
    await whatsAppService.closeSession();

    res.json({
      success: true,
      message: 'Sesión cerrada exitosamente',
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al cerrar sesión',
    } as ApiResponse);
  }
};

/**
 * Verificar si está conectado
 */
export const checkConnection = async (req: Request, res: Response) => {
  try {
    const isConnected = await whatsAppService.isConnected();

    res.json({
      success: true,
      data: { isConnected },
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al verificar conexión',
    } as ApiResponse);
  }
};

/**
 * Obtener código de vinculación (Link Code)
 * Se usa cuando se inicia sesión con número de teléfono
 */
export const getLinkCode = async (req: Request, res: Response) => {
  try {
    const status = whatsAppService.getSessionStatus();

    if (!status.linkCode) {
      return res.status(404).json({
        success: false,
        error: 'No hay código de vinculación disponible. Asegúrese de iniciar sesión con phoneNumber.',
      } as ApiResponse);
    }

    res.json({
      success: true,
      data: {
        linkCode: status.linkCode,
        status: status.status,
        phoneNumber: status.usePhoneNumber,
      },
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener código de vinculación',
    } as ApiResponse);
  }
};
