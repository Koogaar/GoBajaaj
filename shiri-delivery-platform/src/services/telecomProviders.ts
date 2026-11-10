import { PaymentMethod, PaymentStatus, TelecomTransaction } from '../types';

export interface TelecomPaymentRequest {
  orderId: string;
  amount: number;
  provider: PaymentMethod;
  customerPhone: string;
  merchantAccount?: string;
  notes?: string;
}

export interface TelecomPaymentResult {
  success: boolean;
  transactionId: string;
  providerReference: string;
  status: PaymentStatus;
  ussdPrompt?: string;
  message: string;
  timestamp: string;
}

export class TelecomGatewayService {
  private static transactions: TelecomTransaction[] = [];

  /**
   * Initiate mobile money transaction for Hormuud EVC Plus, Somtel eDahab, Telesom ZAAD, Sahal, or Cash
   */
  static async processPayment(request: TelecomPaymentRequest): Promise<TelecomPaymentResult> {
    const timestamp = new Date().toISOString();
    const idempotencyKey = `IDEMP-${request.orderId}-${Date.now()}`;
    const txId = `TX-${Math.floor(100000 + Math.random() * 900000)}`;

    // Build realistic provider reference code
    let providerRef = '';
    let ussdPrompt = '';

    switch (request.provider) {
      case 'evc_plus':
        providerRef = `EVC-${Math.floor(10000000 + Math.random() * 90000000)}`;
        ussdPrompt = `*712*612345678*${request.amount}# (Hormuud EVC Plus)`;
        break;
      case 'edahab':
        providerRef = `EDH-${Math.floor(10000000 + Math.random() * 90000000)}`;
        ussdPrompt = `*789*623456789*${request.amount}# (Somtel eDahab)`;
        break;
      case 'zaad':
        providerRef = `ZAD-${Math.floor(10000000 + Math.random() * 90000000)}`;
        ussdPrompt = `*880*634567890*${request.amount}# (Telesom ZAAD)`;
        break;
      case 'sahal':
        providerRef = `SHL-${Math.floor(10000000 + Math.random() * 90000000)}`;
        ussdPrompt = `*550*681234567*${request.amount}# (Golis Sahal)`;
        break;
      case 'cash_on_delivery':
        providerRef = `COD-${request.orderId}`;
        ussdPrompt = 'Cash will be collected by courier upon physical package delivery';
        break;
    }

    const txRecord: TelecomTransaction = {
      id: txId,
      orderId: request.orderId,
      provider: request.provider,
      payerPhone: request.customerPhone,
      amount: request.amount,
      currency: 'USD',
      status: request.provider === 'cash_on_delivery' ? 'pending' : 'paid',
      providerReference: providerRef,
      createdAt: timestamp,
      verifiedAt: request.provider === 'cash_on_delivery' ? undefined : timestamp,
      idempotencyKey,
    };

    this.transactions.unshift(txRecord);

    return {
      success: true,
      transactionId: txId,
      providerReference: providerRef,
      status: txRecord.status,
      ussdPrompt,
      message:
        request.provider === 'cash_on_delivery'
          ? 'Cash on delivery registered. Courier will collect exact total.'
          : `Mobile money payment confirmed via ${request.provider.toUpperCase()} (Ref: ${providerRef})`,
      timestamp,
    };
  }

  static getTransactions(): TelecomTransaction[] {
    return [...this.transactions];
  }
}
