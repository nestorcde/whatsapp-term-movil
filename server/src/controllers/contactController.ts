import { Request, Response } from 'express';
import whatsAppService from '../services/WhatsAppService';
import { ApiResponse } from '../types';

/**
 * Obtener todos los chats
 */
export const getAllChats = async (req: Request, res: Response) => {
  try {
    const chats = await whatsAppService.getAllChats();

    res.json({
      success: true,
      data: chats,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener chats',
    } as ApiResponse);
  }
};

/**
 * Obtener todos los contactos
 */
export const getAllContacts = async (req: Request, res: Response) => {
  try {
    const contacts = await whatsAppService.getAllContacts();

    res.json({
      success: true,
      data: contacts,
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener contactos',
    } as ApiResponse);
  }
};

/**
 * Obtener nivel de batería
 */
export const getBatteryLevel = async (req: Request, res: Response) => {
  try {
    const batteryLevel = await whatsAppService.getBatteryLevel();

    res.json({
      success: true,
      data: { batteryLevel },
    } as ApiResponse);
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error.message || 'Error al obtener nivel de batería',
    } as ApiResponse);
  }
};
