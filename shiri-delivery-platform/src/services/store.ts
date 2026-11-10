import {
  Order,
  Merchant,
  Courier,
  ShiriAddress,
  Product,
  OrderStatus,
  CartItem,
  Language,
  AppRole,
  PaymentMethod,
  ServiceZone,
  Coordinates,
  VoiceNote,
} from '../types';
import {
  INITIAL_ADDRESSES,
  INITIAL_MERCHANTS,
  INITIAL_COURIERS,
  INITIAL_ORDERS,
  SERVICE_ZONES,
} from './mockData';
import { TelecomGatewayService } from './telecomProviders';
import { soundEffects } from '../utils/soundEffects';

type Listener = () => void;

class ShiriPlatformStore {
  private merchants: Merchant[] = [];
  private couriers: Courier[] = [];
  private orders: Order[] = [];
  private addresses: ShiriAddress[] = [];
  private cart: CartItem[] = [];
  private currentLanguage: Language = 'so';
  private currentRole: AppRole = 'customer';
  private activeOrderId: string | null = null;
  private listeners: Set<Listener> = new Set();
  private auditLogs: { id: string; timestamp: string; action: string; actor: string; details: string }[] = [];
  private courierSimulationInterval: number | null = null;
  private customerAuth = { isLoggedIn: false, phone: '', name: '' };
  private merchantAuth = { isLoggedIn: false, merchantId: '', merchantName: '' };
  private courierAuth = { isLoggedIn: false, courierId: '', courierName: '' };
  private adminAuth = { isLoggedIn: false, username: '' };

  constructor() {
    this.loadInitialData();
  }

  private loadInitialData() {
    try {
      const storedOrders = localStorage.getItem('shiri_orders');
      const storedAddresses = localStorage.getItem('shiri_addresses');
      const storedMerchants = localStorage.getItem('shiri_merchants');
      const storedCouriers = localStorage.getItem('shiri_couriers');

      this.orders = storedOrders ? JSON.parse(storedOrders) : INITIAL_ORDERS;
      this.addresses = storedAddresses ? JSON.parse(storedAddresses) : INITIAL_ADDRESSES;
      this.merchants = storedMerchants ? JSON.parse(storedMerchants) : INITIAL_MERCHANTS;
      this.couriers = storedCouriers ? JSON.parse(storedCouriers) : INITIAL_COURIERS;
      this.activeOrderId = this.orders[0]?.id || null;

      const custAuth = localStorage.getItem('shiri_auth_customer');
      if (custAuth) this.customerAuth = JSON.parse(custAuth);
      const merchAuth = localStorage.getItem('shiri_auth_merchant');
      if (merchAuth) this.merchantAuth = JSON.parse(merchAuth);
      const courAuth = localStorage.getItem('shiri_auth_courier');
      if (courAuth) this.courierAuth = JSON.parse(courAuth);
      const admAuth = localStorage.getItem('shiri_auth_admin');
      if (admAuth) this.adminAuth = JSON.parse(admAuth);
    } catch {
      this.orders = [...INITIAL_ORDERS];
      this.addresses = [...INITIAL_ADDRESSES];
      this.merchants = [...INITIAL_MERCHANTS];
      this.couriers = [...INITIAL_COURIERS];
      this.activeOrderId = this.orders[0]?.id || null;
    }
  }

  private persist() {
    try {
      localStorage.setItem('shiri_orders', JSON.stringify(this.orders));
      localStorage.setItem('shiri_addresses', JSON.stringify(this.addresses));
      localStorage.setItem('shiri_merchants', JSON.stringify(this.merchants));
      localStorage.setItem('shiri_couriers', JSON.stringify(this.couriers));
    } catch {
      // storage unavailable
    }
    this.notify();
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private notify() {
    this.listeners.forEach((fn) => fn());
  }

  logAudit(action: string, actor: string, details: string) {
    this.auditLogs.unshift({
      id: `LOG-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      timestamp: new Date().toISOString(),
      action,
      actor,
      details,
    });
    this.notify();
  }

  // --- GETTERS ---
  getMerchants(): Merchant[] {
    return this.merchants;
  }

  getCouriers(): Courier[] {
    return this.couriers;
  }

  getOrders(): Order[] {
    return this.orders;
  }

  getProducts(): Product[] {
    return this.merchants.flatMap((m) => m.products);
  }

  // --- ROLE AUTHENTICATION & SESSIONS ---
  getCustomerAuth() {
    return this.customerAuth;
  }

  loginCustomer(phone: string, name: string = 'Macmiil') {
    this.customerAuth = { isLoggedIn: true, phone, name };
    try {
      localStorage.setItem('shiri_auth_customer', JSON.stringify(this.customerAuth));
    } catch {}
    this.notify();
  }

  getMerchantAuth() {
    return this.merchantAuth;
  }

  loginMerchant(merchantId: string) {
    const merch = this.merchants.find((m) => m.id === merchantId) || this.merchants[0];
    this.merchantAuth = {
      isLoggedIn: true,
      merchantId: merch?.id || merchantId,
      merchantName: merch?.name || 'Meherad',
    };
    try {
      localStorage.setItem('shiri_auth_merchant', JSON.stringify(this.merchantAuth));
    } catch {}
    this.notify();
  }

  getActiveMerchantId(): string {
    if (this.merchantAuth.isLoggedIn && this.merchantAuth.merchantId) {
      return this.merchantAuth.merchantId;
    }
    return this.merchants[0]?.id || 'merch-1';
  }

  getCourierAuth() {
    return this.courierAuth;
  }

  loginCourier(courierId: string) {
    const cour = this.couriers.find((c) => c.id === courierId) || this.couriers[0];
    this.courierAuth = {
      isLoggedIn: true,
      courierId: cour?.id || courierId,
      courierName: cour?.name || 'Darawal',
    };
    try {
      localStorage.setItem('shiri_auth_courier', JSON.stringify(this.courierAuth));
    } catch {}
    this.notify();
  }

  getActiveCourierId(): string {
    if (this.courierAuth.isLoggedIn && this.courierAuth.courierId) {
      return this.courierAuth.courierId;
    }
    return this.couriers[0]?.id || 'cour-1';
  }

  getAdminAuth() {
    return this.adminAuth;
  }

  loginAdmin() {
    this.adminAuth = { isLoggedIn: true, username: 'admin' };
    try {
      localStorage.setItem('shiri_auth_admin', JSON.stringify(this.adminAuth));
    } catch {}
    this.notify();
  }

  isRoleLoggedIn(role: AppRole): boolean {
    switch (role) {
      case 'customer':
        return this.customerAuth.isLoggedIn;
      case 'merchant':
        return this.merchantAuth.isLoggedIn;
      case 'courier':
        return this.courierAuth.isLoggedIn;
      case 'admin':
        return this.adminAuth.isLoggedIn;
      default:
        return false;
    }
  }

  logoutRole(role: AppRole) {
    switch (role) {
      case 'customer':
        this.customerAuth = { isLoggedIn: false, phone: '', name: '' };
        try {
          localStorage.removeItem('shiri_auth_customer');
        } catch {}
        break;
      case 'merchant':
        this.merchantAuth = { isLoggedIn: false, merchantId: '', merchantName: '' };
        try {
          localStorage.removeItem('shiri_auth_merchant');
        } catch {}
        break;
      case 'courier':
        this.courierAuth = { isLoggedIn: false, courierId: '', courierName: '' };
        try {
          localStorage.removeItem('shiri_auth_courier');
        } catch {}
        break;
      case 'admin':
        this.adminAuth = { isLoggedIn: false, username: '' };
        try {
          localStorage.removeItem('shiri_auth_admin');
        } catch {}
        break;
    }
    this.notify();
  }

  getAddresses(): ShiriAddress[] {
    return this.addresses;
  }

  getCart(): CartItem[] {
    return this.cart;
  }

  getLanguage(): Language {
    return this.currentLanguage;
  }

  getCurrentRole(): AppRole {
    return this.currentRole;
  }

  getActiveOrder(): Order | null {
    if (!this.activeOrderId) return null;
    return this.orders.find((o) => o.id === this.activeOrderId) || null;
  }

  getAuditLogs() {
    return this.auditLogs;
  }

  getServiceZones(): ServiceZone[] {
    return SERVICE_ZONES;
  }

  // --- APP LEVEL CONTROLS ---
  setLanguage(lang: Language) {
    this.currentLanguage = lang;
    this.notify();
  }

  setRole(role: AppRole) {
    this.currentRole = role;
    this.notify();
  }

  setActiveOrderId(orderId: string | null) {
    this.activeOrderId = orderId;
    this.notify();
  }

  // --- CART MANAGEMENT ---
  addToCart(product: Product, quantity = 1, selectedOptionId?: string, instructions?: string) {
    const selectedOption = product.options?.find((o) => o.id === selectedOptionId);
    const existingIndex = this.cart.findIndex(
      (item) => item.product.id === product.id && item.selectedOption?.id === selectedOptionId
    );

    if (existingIndex > -1) {
      this.cart[existingIndex].quantity += quantity;
      if (instructions) this.cart[existingIndex].instructions = instructions;
    } else {
      // If items from different merchant, reset or append? Enforce single merchant per delivery
      if (this.cart.length > 0 && this.cart[0].product.merchantId !== product.merchantId) {
        this.cart = []; // reset cart for new merchant
      }
      this.cart.push({ product, quantity, selectedOption, instructions });
    }
    this.notify();
  }

  updateCartQuantity(index: number, quantity: number) {
    if (quantity <= 0) {
      this.cart.splice(index, 1);
    } else {
      this.cart[index].quantity = quantity;
    }
    this.notify();
  }

  clearCart() {
    this.cart = [];
    this.notify();
  }

  // --- SHIRI ADDRESS MANAGEMENT ---
  createAddress(data: Omit<ShiriAddress, 'id' | 'addressCode' | 'createdAt' | 'qrData' | 'isVerified'>): ShiriAddress {
    // Generate unique Shiri digital address code e.g. "SHR-MG-48291"
    const randomCode = Math.floor(10000 + Math.random() * 90000);
    const districtCode = data.district.substring(0, 2).toUpperCase() || 'MG';
    const addressCode = `SHR-${districtCode}-${randomCode}`;
    const id = `addr-${Date.now()}`;
    const createdAt = new Date().toISOString();
    const qrData = `SHIRI-ADDRESS:${addressCode}:${data.coordinates.lat.toFixed(4)},${data.coordinates.lng.toFixed(4)}`;

    const newAddress: ShiriAddress = {
      ...data,
      id,
      addressCode,
      createdAt,
      qrData,
      isVerified: true,
    };

    this.addresses.unshift(newAddress);
    this.logAudit('CREATE_SHIRI_ADDRESS', data.ownerName, `Created ${addressCode} in ${data.district}`);
    this.persist();
    return newAddress;
  }

  // --- ORDER LIFECYCLE ---
  async placeOrder(params: {
    merchantId: string;
    deliveryAddress: ShiriAddress;
    paymentMethod: PaymentMethod;
    payerPhone?: string;
    paymentReference?: string;
    deliveryInstructions?: string;
    customerWhatsapp?: string;
    customerCallPhone?: string;
    manualPaymentProofNote?: string;
    voiceNote?: VoiceNote;
  }): Promise<Order> {
    const merchant = this.merchants.find((m) => m.id === params.merchantId);
    if (!merchant) throw new Error('Merchant not found');

    const subtotal = this.cart.reduce((sum, item) => {
      const optionPrice = item.selectedOption?.priceModifier || 0;
      return sum + (item.product.price + optionPrice) * item.quantity;
    }, 0);

    const deliveryFee = merchant.deliveryFee || 2.0;
    const serviceFee = 0.5;
    const total = subtotal + deliveryFee + serviceFee;

    const orderId = `order-${Date.now()}`;
    const orderNumber = `#SHR-2026-${Math.floor(100000 + Math.random() * 900000)}`;
    const now = new Date().toISOString();
    const handoverPin = `${Math.floor(1000 + Math.random() * 9000)}`;

    const orderItems = this.cart.map((item, idx) => {
      const optMod = item.selectedOption?.priceModifier || 0;
      const unitPrice = item.product.price + optMod;
      return {
        id: `item-${Date.now()}-${idx}`,
        productId: item.product.id,
        name: item.product.name,
        nameSomali: item.product.nameSomali,
        price: unitPrice,
        quantity: item.quantity,
        selectedOptionName: item.selectedOption?.name,
        totalPrice: unitPrice * item.quantity,
      };
    });

    const payerPhone = params.payerPhone || params.deliveryAddress.ownerPhone;
    const paymentRef =
      params.paymentReference ||
      `${params.paymentMethod.toUpperCase().slice(0, 3)}-${Math.floor(100000 + Math.random() * 900000)}`;

    // Order created with pending payment approval so merchant approves customer payment
    const newOrder: Order = {
      id: orderId,
      orderNumber,
      customerId: params.deliveryAddress.ownerId,
      customerName: params.deliveryAddress.ownerName,
      customerPhone: params.deliveryAddress.ownerPhone,
      customerWhatsapp: params.customerWhatsapp || params.deliveryAddress.ownerPhone.replace(/\D/g, ''),
      customerCallPhone: params.customerCallPhone || params.deliveryAddress.ownerPhone,
      payerPhone,
      manualPaymentProofNote: params.manualPaymentProofNote,
      merchantId: merchant.id,
      merchantName: merchant.name,
      merchantPhone: merchant.phone,
      merchantDistrict: merchant.district,
      merchantCoordinates: merchant.coordinates,
      items: orderItems,
      subtotal,
      deliveryFee,
      serviceFee,
      discount: 0,
      total,
      status: 'pending',
      statusHistory: [
        {
          status: 'pending',
          timestamp: now,
          actor: 'Customer',
          note: `Dalabka waxaa laga dalbaday app-ka. Nambarka lacagta laga soo diray: ${payerPhone} (${params.paymentMethod}). Sugaya oggolaanshaha meheradda.`,
        },
      ],
      paymentMethod: params.paymentMethod,
      paymentStatus: 'pending', // Awaiting merchant approval
      paymentReference: paymentRef,
      deliveryAddress: params.deliveryAddress,
      deliveryInstructions: params.deliveryInstructions,
      voiceNote: params.voiceNote,
      handoverPin,
      createdAt: now,
      estimatedArrivalMinutes: merchant.deliveryTimeMinutes || 25,
    };

    this.orders.unshift(newOrder);
    this.activeOrderId = orderId;
    this.clearCart();

    // Update merchant today sales counters
    merchant.salesToday += subtotal;
    merchant.ordersTodayCount += 1;

    this.logAudit('ORDER_PLACED', params.deliveryAddress.ownerName, `Placed ${orderNumber} - $${total.toFixed(2)} (${params.paymentMethod})`);
    this.persist();

    // Trigger Merchant loud kitchen chime alert
    try {
      soundEffects.playOrderAlert();
    } catch {}

    return newOrder;
  }

  // --- MERCHANT ACTIONS ---
  approveCustomerPayment(orderId: string, approvedBy?: string): { success: boolean; message: string } {
    const order = this.orders.find((o) => o.id === orderId);
    if (!order) return { success: false, message: 'Dalabka lama helin' };

    const now = new Date().toISOString();
    order.paymentStatus = 'paid';
    order.paymentApprovedAt = now;
    order.paymentApprovedBy = approvedBy || order.merchantName;

    // If order is still pending, automatically move it to accepted
    if (order.status === 'pending') {
      order.status = 'accepted';
      order.statusHistory.push({
        status: 'accepted',
        timestamp: now,
        actor: approvedBy || order.merchantName,
        note: `Lacagta ($${order.total.toFixed(2)}) laga soo diray ${order.payerPhone || order.customerPhone} waa la xaqiijiyay oo la oggolaaday (Payment Approved).`,
      });
    } else {
      order.statusHistory.push({
        status: order.status,
        timestamp: now,
        actor: approvedBy || order.merchantName,
        note: `Lacagta ($${order.total.toFixed(2)}) laga soo diray ${order.payerPhone || order.customerPhone} waa la xaqiijiyay (Payment Approved).`,
      });
    }

    try {
      soundEffects.playSuccessChime();
    } catch {}

    this.logAudit(
      'PAYMENT_APPROVED',
      approvedBy || order.merchantName,
      `Merchant approved payment of $${order.total.toFixed(2)} for ${order.orderNumber} sent from ${order.payerPhone || order.customerPhone}`
    );
    this.persist();
    return { success: true, message: 'Lacagta macmiilka si guul leh ayaa loo oggolaaday!' };
  }

  rejectCustomerPayment(orderId: string, reason?: string, actor?: string) {
    const order = this.orders.find((o) => o.id === orderId);
    if (!order) return;
    order.paymentStatus = 'failed';
    order.status = 'cancelled';
    order.cancelledReason = reason || 'Lacag-bixinta lama xaqiijin / Payment rejected';
    order.statusHistory.push({
      status: 'cancelled',
      timestamp: new Date().toISOString(),
      actor: actor || order.merchantName,
      note: `Lacag bixinta waa la diiday: ${order.cancelledReason}`,
    });
    this.logAudit('PAYMENT_REJECTED', actor || order.merchantName, `Rejected payment for ${order.orderNumber}`);
    this.persist();
  }

  updateOrderStatus(orderId: string, newStatus: OrderStatus, actor: string, note?: string) {
    const order = this.orders.find((o) => o.id === orderId);
    if (!order) return;

    order.status = newStatus;
    order.statusHistory.push({
      status: newStatus,
      timestamp: new Date().toISOString(),
      actor,
      note,
    });

    if (newStatus === 'ready_for_pickup' || newStatus === 'courier_assigned') {
      try {
        soundEffects.playDispatchAlert();
      } catch {}
    } else if (newStatus === 'delivered') {
      try {
        soundEffects.playSuccessChime();
      } catch {}
    }

    this.logAudit('ORDER_STATUS_UPDATE', actor, `Order ${order.orderNumber} changed to ${newStatus}`);
    this.persist();
  }

  toggleMerchantOpen(merchantId: string) {
    const merchant = this.merchants.find((m) => m.id === merchantId);
    if (merchant) {
      merchant.isOpen = !merchant.isOpen;
      this.logAudit('MERCHANT_STATUS', merchant.name, `Store set to ${merchant.isOpen ? 'OPEN' : 'CLOSED'}`);
      this.persist();
    }
  }

  addProduct(merchantId: string, productData: Omit<Product, 'id' | 'merchantId'>) {
    const merchant = this.merchants.find((m) => m.id === merchantId);
    if (!merchant) return;

    const newProd: Product = {
      ...productData,
      id: `prod-${Date.now()}`,
      merchantId,
    };

    merchant.products.unshift(newProd);
    this.logAudit('ADD_PRODUCT', merchant.name, `Added product: ${newProd.name}`);
    this.persist();
  }

  toggleProductStock(productId: string) {
    for (const merchant of this.merchants) {
      const prod = merchant.products.find((p) => p.id === productId);
      if (prod) {
        prod.inStock = !prod.inStock;
        this.logAudit('PRODUCT_STOCK', merchant.name, `${prod.name} stock changed to ${prod.inStock ? 'IN STOCK' : 'OUT OF STOCK'}`);
        this.persist();
        break;
      }
    }
  }

  // --- COURIER ACTIONS ---
  toggleCourierOnline(courierId: string) {
    const courier = this.couriers.find((c) => c.id === courierId);
    if (courier) {
      courier.isOnline = !courier.isOnline;
      this.logAudit('COURIER_STATUS', courier.name, `Courier went ${courier.isOnline ? 'ONLINE' : 'OFFLINE'}`);
      this.persist();
    }
  }

  assignNearestCourier(order: Order) {
    const available = this.couriers.find((c) => c.isOnline && !c.activeOrderId);
    if (available) {
      order.courierId = available.id;
      order.courierName = available.name;
      order.courierPhone = available.phone;
      order.courierVehicle = `${available.vehicleType} (${available.plateNumber})`;
      order.courierPlate = available.plateNumber;
      order.courierRating = available.rating;
      order.courierCoordinates = { ...available.currentCoordinates };
      available.activeOrderId = order.id;

      this.updateOrderStatus(order.id, 'courier_assigned', 'Dispatch System', `Assigned to ${available.name}`);
    }
  }

  courierAcceptTrip(courierId: string, orderId: string) {
    const courier = this.couriers.find((c) => c.id === courierId);
    const order = this.orders.find((o) => o.id === orderId);
    if (!courier || !order) return;

    courier.activeOrderId = order.id;
    order.courierId = courier.id;
    order.courierName = courier.name;
    order.courierPhone = courier.phone;
    order.courierVehicle = `${courier.vehicleType} (${courier.plateNumber})`;
    order.courierPlate = courier.plateNumber;
    order.courierRating = courier.rating;
    order.courierCoordinates = { ...courier.currentCoordinates };

    this.updateOrderStatus(order.id, 'courier_assigned', courier.name, 'Courier accepted trip');
  }

  courierConfirmPickup(orderId: string) {
    const order = this.orders.find((o) => o.id === orderId);
    if (!order) return;
    this.updateOrderStatus(order.id, 'picked_up', order.courierName || 'Courier', 'Picked up package from merchant');
    setTimeout(() => {
      this.updateOrderStatus(order.id, 'in_transit', order.courierName || 'Courier', 'Out for delivery to customer');
    }, 1200);
  }

  courierCompleteDelivery(orderId: string, enteredPin: string): { success: boolean; message: string } {
    const order = this.orders.find((o) => o.id === orderId);
    if (!order) return { success: false, message: 'Order not found' };

    // Validate 4-digit handover PIN from customer
    if (order.handoverPin !== enteredPin.trim()) {
      return { success: false, message: `Incorrect PIN. Customer PIN does not match ${order.handoverPin}` };
    }

    const courier = this.couriers.find((c) => c.id === order.courierId);
    if (courier) {
      courier.todayEarnings += order.deliveryFee + 4.5; // trip pay
      courier.completedDeliveriesToday += 1;
      courier.activeOrderId = undefined;
      if (order.paymentMethod === 'cash_on_delivery') {
        courier.cashCollectedToday += order.total;
        order.paymentStatus = 'paid';
      }
    }

    this.updateOrderStatus(order.id, 'delivered', order.courierName || 'Courier', 'Handover PIN verified. Delivered successfully!');
    return { success: true, message: 'Delivery successfully verified and completed!' };
  }

  // Incoming orders feed for drivers
  getUnassignedOrders(): Order[] {
    return this.orders.filter(
      (o) => !o.courierId && o.status !== 'delivered' && o.status !== 'cancelled'
    );
  }

  simulateIncomingOrder(): Order {
    const merchants = this.merchants;
    const merchant = merchants[Math.floor(Math.random() * merchants.length)] || merchants[0];
    const addresses = this.addresses;
    const address = addresses[Math.floor(Math.random() * addresses.length)] || addresses[0];
    const prod = merchant.products[0] || {
      id: 'prod-fallback',
      merchantId: merchant.id,
      name: 'Special Meal Combo',
      nameSomali: 'Cunto Gaar ah',
      description: 'Fresh Somali meal',
      descriptionSomali: 'Cunto cusub oo macaan',
      price: 8.5,
      imageUrl: '',
      category: 'Food',
      categorySomali: 'Cunto',
      inStock: true,
      stockQuantity: 20,
      prepTimeMinutes: 10,
    };

    const orderId = `order-${Date.now()}`;
    const orderNumber = `#SHR-2026-${Math.floor(100000 + Math.random() * 900000)}`;
    const now = new Date().toISOString();
    const handoverPin = `${Math.floor(1000 + Math.random() * 9000)}`;
    const deliveryFee = merchant.deliveryFee || 2.5;
    const subtotal = prod.price * 2;
    const total = subtotal + deliveryFee + 0.5;

    const newOrder: Order = {
      id: orderId,
      orderNumber,
      customerId: `cust-${Date.now()}`,
      customerName: address.ownerName || 'Khadar Maxamed',
      customerPhone: address.ownerPhone || '+252 61 889 0011',
      customerCallPhone: address.ownerPhone || '+252 61 889 0011',
      customerWhatsapp: address.ownerPhone ? address.ownerPhone.replace(/\D/g, '') : '252618890011',
      merchantId: merchant.id,
      merchantName: merchant.name,
      merchantPhone: merchant.phone,
      merchantDistrict: merchant.district,
      merchantCoordinates: merchant.coordinates,
      items: [
        {
          id: `item-${Date.now()}`,
          productId: prod.id,
          name: prod.name,
          nameSomali: prod.nameSomali,
          price: prod.price,
          quantity: 2,
          totalPrice: subtotal,
        },
      ],
      subtotal,
      deliveryFee,
      serviceFee: 0.5,
      discount: 0,
      total,
      status: 'ready_for_pickup',
      statusHistory: [
        { status: 'pending', timestamp: now, actor: 'Customer' },
        { status: 'accepted', timestamp: now, actor: merchant.name },
        { status: 'ready_for_pickup', timestamp: now, actor: merchant.name, note: 'Diyaar u ah inuu darawalku soo qaado' },
      ],
      paymentMethod: 'evc_plus',
      paymentStatus: 'paid',
      paymentReference: `EVC-${Math.floor(100000 + Math.random() * 900000)}`,
      deliveryAddress: address,
      deliveryInstructions: 'Agta masaajidka weyn, wac markaad soo gaarto albaabka.',
      handoverPin,
      createdAt: now,
      estimatedArrivalMinutes: 15,
    };

    this.orders.unshift(newOrder);
    this.logAudit('ORDER_SIMULATED', 'System Simulator', `Created incoming test dispatch order ${orderNumber} for drivers`);
    this.persist();
    return newOrder;
  }

  // --- REGISTRATION & PROFILES WITH MANUAL PAYMENT & SOCIAL CONTACTS ---
  registerCourier(data: {
    name: string;
    phone: string;
    callPhone?: string;
    whatsappNumber?: string;
    paymentPhone?: string;
    vehicleType: 'Motorcycle' | 'Bajaj' | 'Car';
    plateNumber: string;
  }): Courier {
    const newCourier: Courier = {
      id: `cour-${Date.now()}`,
      name: data.name,
      phone: data.phone,
      callPhone: data.callPhone || data.phone.replace(/\s+/g, ''),
      whatsappNumber: data.whatsappNumber || data.phone.replace(/\D/g, ''),
      paymentPhone: data.paymentPhone || data.phone,
      paymentMethodType: 'evc_plus',
      avatar: data.vehicleType === 'Bajaj' ? '🛵' : data.vehicleType === 'Car' ? '🚗' : '🏍️',
      vehicleType: data.vehicleType,
      plateNumber: data.plateNumber,
      isOnline: true,
      currentCoordinates: { lat: 2.045, lng: 45.32 },
      todayEarnings: 0,
      completedDeliveriesToday: 0,
      rating: 5.0,
      verificationStatus: 'verified',
      cashCollectedToday: 0,
      subscription: {
        status: 'active',
        planName: 'Courier Freedom Monthly',
        monthlyFee: 15,
        nextBillingDate: '2026-11-01',
        commissionRate: 0,
        paymentMethod: 'evc_plus',
        paymentPhone: data.paymentPhone || data.phone,
        isManualPayment: true,
        manualPaymentStatus: 'approved',
        whatsappContact: data.whatsappNumber || data.phone.replace(/\D/g, ''),
      },
    };

    this.couriers.unshift(newCourier);
    this.logAudit('COURIER_REGISTERED', data.name, `New driver registered: ${data.name} (${data.vehicleType})`);
    this.persist();
    return newCourier;
  }

  updateCourierProfile(courierId: string, updates: Partial<Courier>) {
    const courier = this.couriers.find((c) => c.id === courierId);
    if (!courier) return;
    Object.assign(courier, updates);
    this.logAudit('COURIER_PROFILE_UPDATED', courier.name, `Updated contact/payment info`);
    this.persist();
  }

  registerMerchant(data: {
    name: string;
    slogan?: string;
    category?: string;
    district: string;
    addressCode: string;
    phone: string;
    callPhone?: string;
    whatsappNumber?: string;
    paymentPhone?: string;
    ownerName: string;
  }): Merchant {
    const newMerchant: Merchant = {
      id: `merch-${Date.now()}`,
      name: data.name,
      slogan: data.slogan || 'Adeeg Hufan & Degdeg ah',
      category: data.category || 'Food & Restaurant',
      categorySomali: data.category || 'Cunto & Restaurant',
      logo: '🏪',
      banner: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop&q=80',
      rating: 5.0,
      reviewCount: 1,
      deliveryTimeMinutes: 25,
      minOrder: 5.0,
      deliveryFee: 2.0,
      isOpen: true,
      addressCode: data.addressCode,
      district: data.district,
      coordinates: { lat: 2.0435, lng: 45.3211 },
      phone: data.phone,
      callPhone: data.callPhone || data.phone.replace(/\s+/g, ''),
      whatsappNumber: data.whatsappNumber || data.phone.replace(/\D/g, ''),
      paymentPhone: data.paymentPhone || data.phone,
      paymentMethodType: 'evc_plus',
      email: `${data.name.toLowerCase().replace(/\s+/g, '')}@shiri.so`,
      verificationStatus: 'verified',
      salesToday: 0,
      ordersTodayCount: 0,
      ownerName: data.ownerName,
      documentsUploaded: true,
      commissionRate: 0,
      subscription: {
        status: 'active',
        planName: 'Merchant Business Partner',
        monthlyFee: 30,
        nextBillingDate: '2026-11-01',
        commissionRate: 0,
        paymentMethod: 'evc_plus',
        paymentPhone: data.paymentPhone || data.phone,
        isManualPayment: true,
        manualPaymentStatus: 'approved',
        whatsappContact: data.whatsappNumber || data.phone.replace(/\D/g, ''),
      },
      products: [],
    };

    this.merchants.unshift(newMerchant);
    this.logAudit('MERCHANT_REGISTERED', data.name, `New store registered: ${data.name} in ${data.district}`);
    this.persist();
    return newMerchant;
  }

  updateMerchantProfile(merchantId: string, updates: Partial<Merchant>) {
    const merchant = this.merchants.find((m) => m.id === merchantId);
    if (!merchant) return;
    Object.assign(merchant, updates);
    this.logAudit('MERCHANT_PROFILE_UPDATED', merchant.name, `Updated store contact/payment info`);
    this.persist();
  }

  submitSubscriptionPayment(
    type: 'merchant' | 'courier',
    id: string,
    data: {
      payerPhone: string;
      paymentMethod: PaymentMethod;
      reference?: string;
      notes?: string;
    }
  ): { success: boolean; message: string } {
    const now = new Date().toISOString();
    const ref = data.reference || `SUB-${Math.floor(100000 + Math.random() * 900000)}`;

    if (type === 'merchant') {
      const merchant = this.merchants.find((m) => m.id === id);
      if (!merchant) return { success: false, message: 'Meheradda lama helin' };
      if (!merchant.subscription) {
        merchant.subscription = {
          status: 'active',
          planName: 'Merchant Business Partner',
          monthlyFee: 30,
          nextBillingDate: '2026-11-01',
          commissionRate: 0,
          paymentMethod: data.paymentMethod,
        };
      }
      merchant.subscription.payerPhone = data.payerPhone;
      merchant.subscription.paymentMethod = data.paymentMethod;
      merchant.subscription.paymentReference = ref;
      merchant.subscription.isManualPayment = true;
      merchant.subscription.manualPaymentStatus = 'pending_verification';
      merchant.subscription.submittedAt = now;
      merchant.subscription.manualNotes = data.notes || `Bixinta $30 laga soo diray ${data.payerPhone} (${data.paymentMethod})`;
      this.logAudit('SUBSCRIPTION_SUBMITTED', merchant.name, `Merchant submitted $30 from ${data.payerPhone}`);
    } else {
      const courier = this.couriers.find((c) => c.id === id);
      if (!courier) return { success: false, message: 'Darawalka lama helin' };
      if (!courier.subscription) {
        courier.subscription = {
          status: 'active',
          planName: 'Courier Freedom Monthly',
          monthlyFee: 15,
          nextBillingDate: '2026-11-01',
          commissionRate: 0,
          paymentMethod: data.paymentMethod,
        };
      }
      courier.subscription.payerPhone = data.payerPhone;
      courier.subscription.paymentMethod = data.paymentMethod;
      courier.subscription.paymentReference = ref;
      courier.subscription.isManualPayment = true;
      courier.subscription.manualPaymentStatus = 'pending_verification';
      courier.subscription.submittedAt = now;
      courier.subscription.manualNotes = data.notes || `Bixinta $15 laga soo diray ${data.payerPhone} (${data.paymentMethod})`;
      this.logAudit('SUBSCRIPTION_SUBMITTED', courier.name, `Courier submitted $15 from ${data.payerPhone}`);
    }

    this.persist();
    return {
      success: true,
      message: 'Lacag-bixinta bisha si guul leh ayaa loo gudbiyay! Maamulka ayaa hadda eegaya oo ansixinaya.',
    };
  }

  verifySubscriptionPayment(type: 'merchant' | 'courier', id: string, note?: string) {
    const now = new Date();
    const nextMonth = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    const nowIso = now.toISOString();

    if (type === 'merchant') {
      const merchant = this.merchants.find((m) => m.id === id);
      if (merchant && merchant.subscription) {
        merchant.subscription.status = 'active';
        merchant.subscription.manualPaymentStatus = 'approved';
        merchant.subscription.approvedAt = nowIso;
        merchant.subscription.nextBillingDate = nextMonth;
        merchant.subscription.manualNotes = note || 'Maamulka ayaa xaqiijiyay lacagta bisha ($30)';
        this.logAudit('SUBSCRIPTION_APPROVED', 'Admin', `Merchant ${merchant.name} subscription approved`);
      }
    } else {
      const courier = this.couriers.find((c) => c.id === id);
      if (courier && courier.subscription) {
        courier.subscription.status = 'active';
        courier.subscription.manualPaymentStatus = 'approved';
        courier.subscription.approvedAt = nowIso;
        courier.subscription.nextBillingDate = nextMonth;
        courier.subscription.manualNotes = note || 'Maamulka ayaa xaqiijiyay lacagta bisha ($15)';
        this.logAudit('SUBSCRIPTION_APPROVED', 'Admin', `Courier ${courier.name} subscription approved`);
      }
    }
    this.persist();
  }

  rejectSubscriptionPayment(type: 'merchant' | 'courier', id: string, reason?: string) {
    if (type === 'merchant') {
      const merchant = this.merchants.find((m) => m.id === id);
      if (merchant && merchant.subscription) {
        merchant.subscription.manualPaymentStatus = 'unpaid';
        merchant.subscription.manualNotes = reason || 'Lacag-bixinta waxaa diiday maamulka';
        this.logAudit('SUBSCRIPTION_REJECTED', 'Admin', `Merchant ${merchant.name} subscription payment rejected`);
      }
    } else {
      const courier = this.couriers.find((c) => c.id === id);
      if (courier && courier.subscription) {
        courier.subscription.manualPaymentStatus = 'unpaid';
        courier.subscription.manualNotes = reason || 'Lacag-bixinta waxaa diiday maamulka';
        this.logAudit('SUBSCRIPTION_REJECTED', 'Admin', `Courier ${courier.name} subscription payment rejected`);
      }
    }
    this.persist();
  }

  // --- ADMIN ACTIONS ---
  verifyMerchant(merchantId: string, status: 'verified' | 'rejected') {
    const merchant = this.merchants.find((m) => m.id === merchantId);
    if (merchant) {
      merchant.verificationStatus = status;
      this.logAudit('MERCHANT_VERIFICATION', 'Admin', `${merchant.name} set to ${status}`);
      this.persist();
    }
  }

  verifyCourier(courierId: string, status: 'verified' | 'rejected') {
    const courier = this.couriers.find((c) => c.id === courierId);
    if (courier) {
      courier.verificationStatus = status;
      this.logAudit('COURIER_VERIFICATION', 'Admin', `${courier.name} set to ${status}`);
      this.persist();
    }
  }

  resetDemoData() {
    localStorage.removeItem('shiri_orders');
    localStorage.removeItem('shiri_addresses');
    localStorage.removeItem('shiri_merchants');
    localStorage.removeItem('shiri_couriers');
    this.orders = [...INITIAL_ORDERS];
    this.addresses = [...INITIAL_ADDRESSES];
    this.merchants = [...INITIAL_MERCHANTS];
    this.couriers = [...INITIAL_COURIERS];
    this.activeOrderId = this.orders[0]?.id || null;
    this.logAudit('SYSTEM_RESET', 'System', 'Demo state re-initialized to default seed');
    this.persist();
  }

  updateCourierLocation(courierId: string, coords: Coordinates) {
    const courier = this.couriers.find((c) => c.id === courierId);
    if (!courier) return;

    courier.currentCoordinates = coords;

    // If courier has an active order, update its courier coordinates synchronously
    if (courier.activeOrderId) {
      const activeOrder = this.orders.find((o) => o.id === courier.activeOrderId);
      if (activeOrder) {
        activeOrder.courierCoordinates = coords;
      }
    }

    this.persist();
  }

  getGoogleMapsApiKey(): string {
    const userKey = localStorage.getItem('shiri_gmaps_key');
    if (userKey && userKey.trim()) return userKey.trim();
    const envKey = (import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string) || '';
    if (envKey && envKey.trim() && envKey !== 'YOUR_GOOGLE_MAPS_API_KEY') {
      return envKey.trim();
    }
    return '';
  }

  setGoogleMapsApiKey(key: string) {
    if (key && key.trim()) {
      localStorage.setItem('shiri_gmaps_key', key.trim());
    } else {
      localStorage.removeItem('shiri_gmaps_key');
    }
    this.notify();
  }
}

export const platformStore = new ShiriPlatformStore();
