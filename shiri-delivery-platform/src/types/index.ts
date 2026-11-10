export type AppRole = 'customer' | 'merchant' | 'courier' | 'admin';

export type Language = 'so' | 'en';

export type OrderStatus =
  | 'pending'
  | 'accepted'
  | 'preparing'
  | 'ready_for_pickup'
  | 'courier_assigned'
  | 'picked_up'
  | 'in_transit'
  | 'delivered'
  | 'cancelled'
  | 'failed_delivery';

export type PaymentMethod =
  | 'evc_plus'      // Hormuud EVC Plus (Somalia)
  | 'zaad'          // Telesom ZAAD (Somaliland)
  | 'edahab'        // Somtel eDahab
  | 'sahal'         // Golis Sahal (Puntland)
  | 'cash_on_delivery';

export type PaymentStatus = 'pending' | 'authorized' | 'paid' | 'failed' | 'refunded';

export interface Coordinates {
  lat: number;
  lng: number;
}

export interface ShiriAddress {
  id: string;
  addressCode: string; // e.g. "SHR-MG-48291"
  ownerId: string;
  ownerName: string;
  ownerPhone: string;
  label: 'Home' | 'Work' | 'Family' | 'Store' | 'Other';
  labelSomali: string;
  city: string; // "Mogadishu"
  district: string; // "Hodan", "Waberi", "Hamarweyne", "Wadajir", etc.
  neighborhood: string; // "Taleex", "Digfeer", "KM4", "Bulo Hubey"
  street: string; // "Waddo 21", "Maka Al-Mukarama"
  buildingOrFloor?: string; // "House 12, Floor 2"
  landmark: string; // "Agta Masaajidka Digfeer / Near Digfeer Hospital"
  coordinates: Coordinates;
  accuracyMeters: number;
  isVerified: boolean;
  notes?: string;
  createdAt: string;
  qrData: string;
}

export interface ProductOption {
  id: string;
  name: string;
  nameSomali: string;
  priceModifier: number;
}

export interface Product {
  id: string;
  merchantId: string;
  name: string;
  nameSomali: string;
  description: string;
  descriptionSomali: string;
  price: number;
  imageUrl: string;
  category: string;
  categorySomali: string;
  inStock: boolean;
  stockQuantity: number;
  prepTimeMinutes: number;
  options?: ProductOption[];
}

export interface SubscriptionInfo {
  status: 'active' | 'expiring_soon' | 'expired';
  planName: string;
  monthlyFee: number;
  nextBillingDate: string;
  commissionRate: number; // 0 for subscription model (0% commission)
  paymentMethod: PaymentMethod;
  paymentPhone?: string;
  payerPhone?: string; // Number the user paid from (Numberka laga soo diray)
  paymentReference?: string;
  isManualPayment?: boolean;
  manualPaymentStatus?: 'approved' | 'pending_verification' | 'unpaid';
  manualNotes?: string;
  submittedAt?: string;
  approvedAt?: string;
  adminReceiverPhone?: string;
  whatsappContact?: string;
}

export interface Merchant {
  id: string;
  name: string;
  slogan: string;
  category: string;
  categorySomali: string;
  logo: string;
  banner: string;
  rating: number;
  reviewCount: number;
  deliveryTimeMinutes: number;
  minOrder: number;
  deliveryFee: number;
  isOpen: boolean;
  addressCode: string;
  district: string;
  coordinates: Coordinates;
  phone: string;
  callPhone?: string;
  whatsappNumber?: string;
  paymentPhone?: string;
  paymentMethodType?: PaymentMethod;
  email: string;
  verificationStatus: 'verified' | 'pending' | 'rejected';
  salesToday: number;
  ordersTodayCount: number;
  ownerName: string;
  documentsUploaded: boolean;
  commissionRate: number; // 0 (Subscription model - 0% commission)
  subscription?: SubscriptionInfo;
  products: Product[];
}

export interface CartItem {
  product: Product;
  quantity: number;
  selectedOption?: ProductOption;
  instructions?: string;
}

export interface OrderItem {
  id: string;
  productId: string;
  name: string;
  nameSomali: string;
  price: number;
  quantity: number;
  selectedOptionName?: string;
  totalPrice: number;
}

export interface StatusHistoryEntry {
  status: OrderStatus;
  timestamp: string;
  actor: string;
  note?: string;
}

export interface VoiceNote {
  id: string;
  audioUrl?: string; // Blob URL or audio data
  durationSeconds: number;
  recordedAt: string;
  transcriptSomali?: string;
  isSimulated?: boolean;
}

export interface Order {
  id: string;
  orderNumber: string; // e.g. "#SHR-2026-000123"
  customerId: string;
  customerName: string;
  customerPhone: string;
  merchantId: string;
  merchantName: string;
  merchantPhone: string;
  merchantDistrict: string;
  merchantCoordinates: Coordinates;
  courierId?: string;
  courierName?: string;
  courierPhone?: string;
  courierVehicle?: string;
  courierPlate?: string;
  courierRating?: number;
  courierCoordinates?: Coordinates;
  items: OrderItem[];
  subtotal: number;
  deliveryFee: number;
  serviceFee: number;
  discount: number;
  total: number;
  status: OrderStatus;
  statusHistory: StatusHistoryEntry[];
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  paymentReference?: string;
  deliveryAddress: ShiriAddress;
  deliveryInstructions?: string;
  voiceNote?: VoiceNote;
  handoverPin: string; // 4-digit code e.g. "4792"
  createdAt: string;
  estimatedArrivalMinutes?: number;
  cancelledReason?: string;
  customerWhatsapp?: string;
  customerCallPhone?: string;
  payerPhone?: string; // Number customer paid from (Numberka laga soo diray)
  manualPaymentProofNote?: string;
  paymentApprovedAt?: string;
  paymentApprovedBy?: string;
}

export interface Courier {
  id: string;
  name: string;
  phone: string;
  callPhone?: string;
  whatsappNumber?: string;
  paymentPhone?: string;
  paymentMethodType?: PaymentMethod;
  avatar: string;
  vehicleType: 'Motorcycle' | 'Bajaj' | 'Car';
  plateNumber: string;
  isOnline: boolean;
  currentCoordinates: Coordinates;
  todayEarnings: number;
  completedDeliveriesToday: number;
  rating: number;
  verificationStatus: 'verified' | 'pending' | 'rejected';
  cashCollectedToday: number;
  activeOrderId?: string;
  subscription?: SubscriptionInfo;
}

export interface ServiceZone {
  id: string;
  name: string;
  district: string;
  baseDeliveryFee: number;
  perKmFee: number;
  isActive: boolean;
  coordinates: Coordinates;
}

export interface TelecomTransaction {
  id: string;
  orderId: string;
  provider: PaymentMethod;
  payerPhone: string;
  amount: number;
  currency: string; // USD
  status: PaymentStatus;
  providerReference: string;
  createdAt: string;
  verifiedAt?: string;
  idempotencyKey: string;
}

export interface PlatformConfig {
  baseDeliveryFee: number;
  serviceFee: number;
  platformCommissionRate: number;
  currency: string;
  smsOtpEnabled: boolean;
  autoDispatch: boolean;
}
