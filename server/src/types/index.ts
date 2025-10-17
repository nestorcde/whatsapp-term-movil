import { Whatsapp } from '@wppconnect-team/wppconnect';

export interface SessionData {
  client: Whatsapp | null;
  status: SessionStatus;
  qrCode: string | null;
  qrCodeBase64: string | null;
  linkCode: string | null;
  connectedPhone: string | null;
  usePhoneNumber: boolean;
}

export enum SessionStatus {
  DISCONNECTED = 'disconnected',
  CONNECTING = 'connecting',
  QR_READY = 'qr_ready',
  LINK_CODE_READY = 'link_code_ready',
  CONNECTED = 'connected',
  FAILED = 'failed',
}

export interface StartSessionRequest {
  phoneNumber?: string;
}

export interface SendMessageRequest {
  phone: string;
  message: string;
}

export interface SendImageRequest {
  phone: string;
  image: string; // base64 o path
  caption?: string;
}

export interface SendFileRequest {
  phone: string;
  file: string; // base64 o path
  filename: string;
  caption?: string;
}

export interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
}

export interface MessageReceived {
  id: string;
  from: string;
  body: string;
  timestamp: number;
  isGroupMsg: boolean;
  type: string;
}
