import { Order, Courier } from '../types';

/**
 * Normalizes Somali telephone numbers for WhatsApp wa.me links
 * e.g. "0618890011" -> "252618890011"
 * e.g. "+252 61 889 0011" -> "252618890011"
 */
export function normalizeSomaliPhoneForWhatsapp(phone: string): string {
  const digits = phone.replace(/\D/g, '');
  if (digits.startsWith('252')) {
    return digits;
  }
  if (digits.startsWith('0')) {
    return `252${digits.slice(1)}`;
  }
  return `252${digits}`;
}

/**
 * Generate Customer to Driver / Merchant WhatsApp link
 */
export function buildCustomerToDriverWhatsappUrl(order: Order, driverPhone?: string): string {
  const targetPhone = normalizeSomaliPhoneForWhatsapp(driverPhone || order.courierPhone || '252613456789');
  const text = `Salaamu calaykum! Waxaan ahay macmiilka dalabka *${order.orderNumber}*.
Ciwaankayga Shiri Code: *${order.deliveryAddress.addressCode}* (${order.deliveryAddress.district}, ${order.deliveryAddress.neighborhood || ''}).
Calaamadda: ${order.deliveryAddress.landmark}.
Koodhka wareejinta (PIN): *${order.handoverPin}*.
Fadlan i soo wac markaad soo gaarto.`;

  return `https://wa.me/${targetPhone}?text=${encodeURIComponent(text)}`;
}

/**
 * Generate Driver to Customer WhatsApp link
 */
export function buildDriverToCustomerWhatsappUrl(order: Order, courierName: string): string {
  const targetPhone = normalizeSomaliPhoneForWhatsapp(order.customerWhatsapp || order.customerPhone || '252618890011');
  const text = `Salaamu calaykum ${order.customerName}! Waxaan ahay darawalkaaga Shiri Delivery (${courierName}).
Dalabkaagii *${order.orderNumber}* ee ka socday *${order.merchantName}* waan soo qaaday, waddada ayaan kusoo jiraa.
Ciwaanka: *${order.deliveryAddress.addressCode}* (${order.deliveryAddress.landmark}).
Fadlan diyaarso Handover PIN: *${order.handoverPin}*.`;

  return `https://wa.me/${targetPhone}?text=${encodeURIComponent(text)}`;
}

/**
 * Generate Merchant to Customer WhatsApp link
 */
export function buildMerchantToCustomerWhatsappUrl(order: Order): string {
  const targetPhone = normalizeSomaliPhoneForWhatsapp(order.customerWhatsapp || order.customerPhone || '252618890011');
  const text = `Salaamu calaykum ${order.customerName}! Dalabkaaga *${order.orderNumber}* ee ka socda *${order.merchantName}* waa la diyaariyay oo darawalka ayaa lagu wareejiyay.
Wadarta: *$${order.total.toFixed(2)}*. Waad ku mahadsan tahay isticmaalka Shiri Delivery!`;

  return `https://wa.me/${targetPhone}?text=${encodeURIComponent(text)}`;
}

/**
 * Generates simulated SMS text for EVC Plus & Shiri Notification
 */
export function generateSimulatedSms(order: Order) {
  const telecomReceipt = {
    provider: order.paymentMethod.toUpperCase(),
    sender: order.paymentMethod === 'evc_plus' ? 'EVCPlus' : order.paymentMethod === 'edahab' ? 'eDahab' : 'Telecom',
    body: `[- $${order.total.toFixed(2)}] Waxaad $${order.total.toFixed(2)} u wareejisay ${order.merchantName} (${order.merchantPhone}). Ref: ${order.paymentReference || 'TX-82910'}. Haraagaagu waa ku filan yahay.`,
    time: 'Hadda (Just now)',
  };

  const shiriSms = {
    sender: 'ShiriDelivery',
    body: `Dalabkaaga #${order.orderNumber} waa la xaqiijiyay! Ciwaanka: ${order.deliveryAddress.addressCode}. Handover PIN: ${order.handoverPin}. Ku raac live: shiri.so/t/${order.orderNumber.replace(/[^0-9]/g, '')}`,
    time: 'Hadda (Just now)',
  };

  return { telecomReceipt, shiriSms };
}
