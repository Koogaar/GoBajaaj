import React, { useState, useEffect } from 'react';
import { CreditCard, CheckCircle2, XCircle, RefreshCw, AlertCircle, DollarSign, Filter, ShieldAlert, Search, Smartphone, Copy, Check, Info, Sparkles, Clock, Zap, Volume2, VolumeX, Bell } from 'lucide-react';
import { PaymentTransaction, Subscription } from '../types';
import { supabase } from '../supabase';
import { ConfirmationModal } from './ConfirmationModal';
import { soundAlert } from '../utils/soundAlert';

export const isWithinMinutes = (dateStr?: string, minutes: number = 10) => {
  if (!dateStr) return false;
  const diff = Date.now() - new Date(dateStr).getTime();
  return diff >= 0 && diff <= minutes * 60 * 1000;
};

export const formatTimeAgoSomali = (dateStr?: string) => {
  if (!dateStr) return 'Lama yaqaan';
  const diffSec = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
  if (diffSec < 60) return 'Hadda (dhowr ilbiriqsi)';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin} daqiiqo kahor`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours} saac kahor`;
  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays} maalmood kahor`;
};

export const formatExactDateTime = (dateStr?: string) => {
  if (!dateStr) return '';
  try {
    const d = new Date(dateStr);
    return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  } catch {
    return dateStr;
  }
};

interface PaymentManagementProps {
  actorName: string;
  canManage: boolean;
}

export const PaymentManagement: React.FC<PaymentManagementProps> = ({ actorName, canManage }) => {
  const [payments, setPayments] = useState<PaymentTransaction[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState<'all' | 'recent10' | 'today' | 'pending' | 'completed' | 'rejected'>('all');
  const [processingId, setProcessingId] = useState<string | null>(null);

  // Confirmation Modal
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    paymentId: string;
    action: 'approve' | 'reject';
    amount: number;
    phone: string;
    userId: string;
    subId?: string;
  }>({ isOpen: false, paymentId: '', action: 'approve', amount: 0.25, phone: '', userId: '' });

  const [rejectionReason, setRejectionReason] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [soundEnabled, setSoundEnabled] = useState(soundAlert.isEnabled());

  const handleCopyPhone = (phone: string) => {
    if (!phone) return;
    navigator.clipboard.writeText(phone);
    setCopiedPhone(phone);
    setTimeout(() => setCopiedPhone(null), 2000);
  };

  // Detects a missing server RPC so the legacy path is only used in that case.
  const isMissingRpc = (error: any): boolean => {
    if (!error) return false;
    const code = String(error.code || '');
    const msg = String(error.message || '').toLowerCase();
    return (
      code === 'PGRST202' ||
      code === '42883' ||
      msg.includes('could not find the function') ||
      msg.includes('does not exist')
    );
  };

  // Reusable core approval engine with strict concurrency & error handling
  const approveTransactionCore = async (p: PaymentTransaction) => {
    // Preferred path: secure server-side RPC. The server sets the trusted
    // NOW() + 24h window and enforces admin permission via auth.uid().
    if (p.id) {
      const { data, error } = await supabase.rpc('approve_driver_pass_payment', {
        p_payment_id: p.id
      });
      if (!error) {
        const res = (data || {}) as any;
        if (res.ok === true) {
          return; // audit log is written inside the RPC
        }
        if (res.error === 'forbidden') {
          throw new Error('Ma lehid fasax aad ku ansixiso Driver Pass-ka.');
        }
        if (res.error === 'not_pending') {
          throw new Error(`Bixintan mar hore ayaa la maareeyay (Xaaladdu hadda waa: ${res.status || 'lama yaqaan'}).`);
        }
        if (res.error === 'already_processed') {
          throw new Error('Bixintan mar hore ayaa la maareeyay.');
        }
        if (res.error !== 'payment_not_found') {
          throw new Error(`Ansixinta way fashilantay (${res.error || 'khalad lama garanayn'}).`);
        }
        // payment_not_found => older RPC variant; fall through to legacy path.
      } else if (!isMissingRpc(error)) {
        throw new Error(`Payment-ka lama ansixin karin: ${error.message}`);
      }
    }

    const now = new Date();
    let startDate = now;
    let expiry = new Date(now.getTime() + 24 * 60 * 60 * 1000);

    // 1. Concurrency / Duplicate check: verify and update transaction to completed
    if (p.id) {
      const { data: updatedTx, error: txErr } = await supabase
        .from('payment_transactions')
        .update({ status: 'completed' })
        .eq('id', p.id)
        .eq('status', 'pending')
        .select();

      if (txErr) {
        throw new Error(`Payment-ka lama ansixin karin: ${txErr.message}`);
      }

      if (!updatedTx || updatedTx.length === 0) {
        const { data: currentTx } = await supabase
          .from('payment_transactions')
          .select('status')
          .eq('id', p.id)
          .maybeSingle();

        if (currentTx && currentTx.status !== 'pending') {
          throw new Error(`Bixintan mar hore ayaa la maareeyay (Xaaladdu hadda waa: ${currentTx.status}).`);
        }
      }
    }

    // 2. Check if the driver has an active unexpired subscription to extend from its end_date
    if (p.user_id) {
      const { data: activeSub } = await supabase
        .from('subscriptions')
        .select('end_date')
        .eq('user_id', p.user_id)
        .eq('status', 'active')
        .order('end_date', { ascending: false })
        .limit(1)
        .maybeSingle();

      if (activeSub && activeSub.end_date) {
        const activeEnd = new Date(activeSub.end_date);
        if (activeEnd.getTime() > now.getTime()) {
          expiry = new Date(activeEnd.getTime() + 24 * 60 * 60 * 1000);
        }
      }
    }

    // 3. Activate the exact subscription attached to the payment
    if (p.subscription_id) {
      const { error: subErr } = await supabase
        .from('subscriptions')
        .update({
          status: 'active',
          plan_name: 'Driver Pass (Maalinle 05:00 AM)',
          amount: Number(p.amount || 0.25),
          payment_method: p.payment_method || 'EVC Plus',
          payment_reference: p.reference_number || null,
          sender_phone: p.phone_number || null,
          start_date: startDate.toISOString(),
          end_date: expiry.toISOString(),
          rejection_reason: null,
          updated_at: new Date().toISOString()
        })
        .eq('id', p.subscription_id);

      if (subErr) {
        throw new Error(`Subscription-ka lama active-gareyn karin: ${subErr.message}`);
      }
    } else if (p.user_id) {
      // Find latest pending subscription for driver, or create one
      const { data: existingSub, error: findErr } = await supabase
        .from('subscriptions')
        .select('id')
        .eq('user_id', p.user_id)
        .eq('status', 'pending')
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();

      if (findErr) {
        throw new Error(`Subscription-ka lama raadin karin: ${findErr.message}`);
      }

      if (existingSub) {
        const { error: subErr } = await supabase
          .from('subscriptions')
          .update({
            status: 'active',
            plan_name: 'Driver Pass (Maalinle 05:00 AM)',
            amount: Number(p.amount || 0.25),
            payment_method: p.payment_method || 'EVC Plus',
            payment_reference: p.reference_number || null,
            sender_phone: p.phone_number || null,
            start_date: startDate.toISOString(),
            end_date: expiry.toISOString(),
            rejection_reason: null,
            updated_at: new Date().toISOString()
          })
          .eq('id', existingSub.id);

        if (subErr) {
          throw new Error(`Subscription-ka lama active-gareyn karin: ${subErr.message}`);
        }
      } else {
        const { error: insertErr } = await supabase.from('subscriptions').insert([{
          user_id: p.user_id,
          plan_name: 'Driver Pass (Maalinle 05:00 AM)',
          amount: Number(p.amount || 0.25),
          currency: p.currency || 'USD',
          status: 'active',
          payment_method: p.payment_method || 'EVC Plus',
          payment_reference: p.reference_number || `PASS_${Date.now() % 100000}`,
          sender_phone: p.phone_number || null,
          start_date: startDate.toISOString(),
          end_date: expiry.toISOString(),
          auto_renew: false
        }]);

        if (insertErr) {
          throw new Error(`Subscription cusub lama abuuri karin: ${insertErr.message}`);
        }
      }
    }

    // 4. Update the driver's profile status and subscription_status
    if (p.user_id) {
      const { error: profileErr } = await supabase
        .from('profiles')
        .update({
          status: 'active',
          subscription_status: 'active',
          subscription_expiry: expiry.toISOString(),
          updated_at: new Date().toISOString()
        })
        .eq('id', p.user_id);

      if (profileErr) {
        throw new Error(`Profile-ka darawalka lama active-gareyn karin: ${profileErr.message}`);
      }
    }

    if (p.phone_number) {
      const { error: phoneProfileErr } = await supabase
        .from('profiles')
        .update({
          status: 'active',
          subscription_status: 'active',
          subscription_expiry: expiry.toISOString(),
          updated_at: new Date().toISOString()
        })
        .eq('phone_number', p.phone_number);

      if (phoneProfileErr) {
        console.warn(`Profile-ka lambarka ${p.phone_number} lama cusboonaysiin: ${phoneProfileErr.message}`);
      }
    }

    if (p.account_phone && p.account_phone !== p.phone_number) {
      const { error: accErr } = await supabase
        .from('profiles')
        .update({
          status: 'active',
          subscription_status: 'active',
          subscription_expiry: expiry.toISOString(),
          updated_at: new Date().toISOString()
        })
        .eq('phone_number', p.account_phone);

      if (accErr) {
        console.warn(`Profile-ka account-ka ${p.account_phone} lama cusboonaysiin: ${accErr.message}`);
      }
    }

    // 5. Record Audit Log
    await supabase.from('audit_logs').insert([{
      actor_name: actorName,
      action: 'PAYMENT_APPROVED',
      target_type: 'payment_transaction',
      target_id: p.id,
      metadata: {
        amount: Number(p.amount || 0.25),
        phone: p.phone_number,
        user_id: p.user_id,
        subscription_id: p.subscription_id,
        expiry: expiry.toISOString()
      }
    }]);
  };

  // Instant 1-Click Single Approve
  const handleQuickSingleApprove = async (p: PaymentTransaction) => {
    if (!canManage) {
      alert('Ma lehid fasax aad ku ansixiso.');
      return;
    }
    setIsLoading(true);
    setProcessingId(p.id);
    setErrorMsg(null);
    try {
      await approveTransactionCore(p);
      soundAlert.playPaymentSound();
      await fetchPaymentsAndSubscriptions(true);
    } catch (err: any) {
      console.error('Quick approve error:', err);
      const usefulMessage = err?.message || 'Khalad ayaa dhacay';
      setErrorMsg(`Khalad ansixinta: ${usefulMessage}`);
      alert(`Khalad: ${usefulMessage}`);
    } finally {
      setIsLoading(false);
      setProcessingId(null);
    }
  };

  // Bulk Approve All Pending
  const handleBulkApproveAll = async () => {
    if (!canManage) {
      alert('Ma lehid fasax.');
      return;
    }
    const pendings = payments.filter(p => p.status === 'pending');
    if (pendings.length === 0) {
      alert('Ma jiraan lacago sugaya ansixin xilligan.');
      return;
    }

    if (!window.confirm(`Ma hubtaa inaad hal mar wada ansixiso dhammaan (${pendings.length}) darawal ee hadda sugaya?`)) {
      return;
    }

    setIsLoading(true);
    setErrorMsg(null);
    let successCount = 0;
    const errors: string[] = [];

    try {
      for (const p of pendings) {
        try {
          await approveTransactionCore(p);
          successCount++;
        } catch (e: any) {
          console.error('Error in bulk item:', p.id, e);
          errors.push(`${p.phone_number || p.reference_number}: ${e.message}`);
        }
      }

      if (successCount > 0) {
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'BULK_PAYMENTS_APPROVED',
          target_type: 'bulk_payment',
          metadata: { count: successCount, totalAmount: successCount * 0.25 }
        }]);
        soundAlert.playPaymentSound();
      }

      await fetchPaymentsAndSubscriptions(true);

      if (errors.length > 0) {
        const msg = `${successCount} ayaa la ansixiyay, laakiin ${errors.length} ayaa ku fashilmay:\n` + errors.slice(0, 5).join('\n');
        setErrorMsg(msg);
        alert(msg);
      } else {
        alert(`Guul! Dhammaan ${successCount} darawal ee sugayay ayaa la wada ansixiyay!`);
      }
    } catch (err: any) {
      const usefulMessage = err?.message || 'Khalad ayaa dhacay';
      setErrorMsg(`Khalad ansixinta guud: ${usefulMessage}`);
      alert(`Khalad baa dhacay: ${usefulMessage}`);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchPaymentsAndSubscriptions = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    setErrorMsg(null);
    try {
      let list: PaymentTransaction[] = [];

      // 1. Fetch real payment_transactions table directly from Supabase
      const { data: payData, error: payErr } = await supabase
        .from('payment_transactions')
        .select('*')
        .order('created_at', { ascending: false });

      if (payErr) {
        throw new Error(`Bixinnada lama soo qaadi karin: ${payErr.message}`);
      }

      // 2. Fetch subscriptions to cross-reference sender_phone and rejection_reason
      const subsMap = new Map<string, any>();
      const { data: subsData, error: subsErr } = await supabase
        .from('subscriptions')
        .select('*')
        .order('created_at', { ascending: false });

      if (subsErr) {
        console.warn('Subscriptions fetch notice:', subsErr.message);
      } else if (subsData) {
        setSubscriptions(subsData as Subscription[]);
        subsData.forEach((s: any) => {
          subsMap.set(s.id, s);
        });
      }

      // 3. Fetch driver profiles from profiles table to build name/phone/plate lookup
      const profilesMap = new Map<string, any>();
      const { data: profilesData, error: profilesErr } = await supabase
        .from('profiles')
        .select('*')
        .order('created_at', { ascending: false });

      if (profilesErr) {
        console.warn('Profiles fetch notice:', profilesErr.message);
      } else if (profilesData && profilesData.length > 0) {
        profilesData.forEach((prof: any) => {
          profilesMap.set(prof.id, prof);
          if (prof.phone_number) {
            profilesMap.set(prof.phone_number, prof);
          }
        });
      }

      // 4. Map payment_transactions to view models (No mock data)
      if (payData && payData.length > 0) {
        list = payData.map((p: any) => {
          const linkedSub = p.subscription_id ? subsMap.get(p.subscription_id) : null;
          const prof = profilesMap.get(p.user_id) || (p.phone_number ? profilesMap.get(p.phone_number) : null);
          const effectivePhone = p.phone_number || linkedSub?.sender_phone || prof?.phone_number || '';
          const driverName = prof?.full_name || (effectivePhone ? `Darawal (${effectivePhone.slice(-6)})` : `Darawal (${(p.user_id || '').slice(0, 6)})`);
          const accountPhone = prof?.phone_number || '';
          const senderPhone = p.phone_number || linkedSub?.sender_phone || '';

          return {
            id: p.id,
            subscription_id: p.subscription_id,
            user_id: p.user_id,
            user_name: driverName,
            amount: Number(p.amount || 0.25),
            currency: p.currency || 'USD',
            payment_method: p.payment_method || linkedSub?.payment_method || 'EVC Plus',
            reference_number: p.reference_number || (linkedSub?.payment_reference) || `REF_${p.id.slice(0, 8)}`,
            phone_number: effectivePhone,
            account_phone: accountPhone,
            sender_phone: senderPhone,
            status: p.status || 'pending',
            rejection_reason: p.rejection_reason || linkedSub?.rejection_reason,
            created_at: p.created_at
          };
        });
      }

      // 5. Also merge any real pending subscription that might have been created without payment_transaction
      if (subsData && subsData.length > 0) {
        subsData.forEach((s: any) => {
          if (s.status === 'pending') {
            const alreadyExists = list.some(p => (p.subscription_id && p.subscription_id === s.id) || p.id === s.id);
            if (!alreadyExists) {
              const prof = profilesMap.get(s.user_id);
              const phoneMatch = s.payment_reference?.match(/\d{7,12}/)?.[0] || '';
              const sender = s.sender_phone || phoneMatch || '';
              const account = prof?.phone_number || '';
              const phone = sender || account || '';
              list.push({
                id: s.id,
                subscription_id: s.id,
                user_id: s.user_id,
                user_name: prof?.full_name || `Darawal (${(phone || s.user_id || '').slice(-6)})`,
                amount: Number(s.amount || 0.25),
                currency: s.currency || 'USD',
                payment_method: s.payment_method || 'EVC Plus',
                reference_number: s.payment_reference || `PASS_${s.id.slice(0, 8)}`,
                phone_number: phone,
                account_phone: account,
                sender_phone: sender,
                status: 'pending',
                rejection_reason: s.rejection_reason,
                created_at: s.created_at
              });
            }
          }
        });
      }

      // Sort by created_at descending
      list.sort((a, b) => {
        const tA = a.created_at ? new Date(a.created_at).getTime() : 0;
        const tB = b.created_at ? new Date(b.created_at).getTime() : 0;
        return tB - tA;
      });

      setPayments(list);

    } catch (err: any) {
      console.error('fetchPaymentsAndSubscriptions catch:', err);
      const usefulMessage = err?.message || 'Khalad aan la garanayn';
      setErrorMsg(`Cilad ayaa ka dhacday soo qaadista xogta: ${usefulMessage}`);
    } finally {
      if (!isSilent) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPaymentsAndSubscriptions(false);

    // Auto-poll silently every 10 seconds so new payments appear smoothly without screen blinking
    const pollInterval = setInterval(() => {
      fetchPaymentsAndSubscriptions(true);
    }, 10000);

    // Supabase Realtime channel
    const channel = supabase
      .channel('realtime_payments')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'payment_transactions' }, (payload: any) => {
        if (payload?.eventType === 'INSERT') {
          soundAlert.playPaymentSound();
        }
        fetchPaymentsAndSubscriptions(true);
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'subscriptions' }, (payload: any) => {
        if (payload?.eventType === 'INSERT') {
          soundAlert.playPaymentSound();
        }
        fetchPaymentsAndSubscriptions(true);
      })
      .subscribe();

    return () => {
      clearInterval(pollInterval);
      supabase.removeChannel(channel);
    };
  }, []);

  const handleExecuteApproval = async () => {
    if (!canManage) {
      alert('Ma lehid fasax aad ku ansixiso ama ku diido lacagaha.');
      return;
    }

    const { paymentId, action, userId, subId, phone } = confirmModal;
    setIsLoading(true);
    setProcessingId(paymentId);
    setErrorMsg(null);

    try {
      if (action === 'approve') {
        const targetPayment = payments.find(p => p.id === paymentId) || {
          id: paymentId,
          subscription_id: subId,
          user_id: userId,
          phone_number: phone,
          amount: confirmModal.amount || 0.25,
          currency: 'USD',
          payment_method: 'EVC Plus',
          reference_number: '',
          status: 'pending' as const,
          created_at: new Date().toISOString()
        };
        await approveTransactionCore(targetPayment);
        soundAlert.playPaymentSound();
      } else {
        const trimmedReason = rejectionReason.trim() || 'Ansixinta waa la diiday';

        // Preferred path: secure server-side RPC (writes the audit log itself).
        let rejectedViaRpc = false;
        if (paymentId) {
          const { data, error } = await supabase.rpc('reject_driver_pass_payment', {
            p_payment_id: paymentId,
            p_reason: trimmedReason
          });
          if (!error) {
            const res = (data || {}) as any;
            if (res.ok === true) {
              rejectedViaRpc = true;
            } else if (res.error === 'forbidden') {
              throw new Error('Ma lehid fasax aad ku diido Driver Pass-ka.');
            } else if (res.error === 'not_pending') {
              throw new Error(`Bixintan mar hore ayaa la maareeyay (Xaaladdu hadda waa: ${res.status || 'lama yaqaan'}).`);
            } else if (res.error !== 'payment_not_found') {
              throw new Error(`Diidmada way fashilantay (${res.error || 'khalad lama garanayn'}).`);
            }
          } else if (!isMissingRpc(error)) {
            throw new Error(`Payment-ka lama diidi karin: ${error.message}`);
          }
        }

        if (!rejectedViaRpc) {
        // Legacy fallback: only used when the RPC is not installed yet.
        // 1. Verify transaction status if real payment_transaction
        if (paymentId && !paymentId.startsWith('sub-')) {
          const { data: currentTx, error: checkErr } = await supabase
            .from('payment_transactions')
            .select('status')
            .eq('id', paymentId)
            .maybeSingle();

          if (checkErr) {
            throw new Error(`Xaqiijinta xaaladda payment-ka ayaa khaldantay: ${checkErr.message}`);
          }

          if (currentTx && currentTx.status !== 'pending') {
            throw new Error(`Bixintan mar hore ayaa la maareeyay (Xaaladdu hadda waa: ${currentTx.status}).`);
          }

          const { error: txRejectErr } = await supabase
            .from('payment_transactions')
            .update({
              status: 'rejected',
              rejection_reason: trimmedReason
            })
            .eq('id', paymentId);

          if (txRejectErr) {
            throw new Error(`Payment-ka lama diidi karin: ${txRejectErr.message}`);
          }
        }

        // 2. Update subscription status & rejection reason
        if (subId) {
          const { error: subRejectErr } = await supabase
            .from('subscriptions')
            .update({
              status: 'rejected',
              rejection_reason: trimmedReason,
              updated_at: new Date().toISOString()
            })
            .eq('id', subId);

          if (subRejectErr) {
            throw new Error(`Subscription-ka lama diidi karin: ${subRejectErr.message}`);
          }
        } else if (userId) {
          const { error: subRejectErr } = await supabase
            .from('subscriptions')
            .update({
              status: 'rejected',
              rejection_reason: trimmedReason,
              updated_at: new Date().toISOString()
            })
            .eq('user_id', userId)
            .eq('status', 'pending');

          if (subRejectErr) {
            console.warn(`Pending subscription rejection note: ${subRejectErr.message}`);
          }
        }

        // 3. Update profile subscription status
        if (userId) {
          const { error: profRejectErr } = await supabase
            .from('profiles')
            .update({
              subscription_status: 'rejected',
              updated_at: new Date().toISOString()
            })
            .eq('id', userId);

          if (profRejectErr) {
            console.warn(`Profile reject note: ${profRejectErr.message}`);
          }
        }

        if (phone) {
          const { error: phoneProfErr } = await supabase
            .from('profiles')
            .update({
              subscription_status: 'rejected',
              updated_at: new Date().toISOString()
            })
            .eq('phone_number', phone);

          if (phoneProfErr) {
            console.warn(`Profile phone reject note: ${phoneProfErr.message}`);
          }
        }

        // 4. Record Audit Log
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'PAYMENT_REJECTED',
          target_type: 'payment_transaction',
          target_id: paymentId,
          metadata: { reason: trimmedReason, user_id: userId, phone }
        }]);
        }
      }

      setConfirmModal({ isOpen: false, paymentId: '', action: 'approve', amount: 0.25, phone: '', userId: '' });
      setRejectionReason('');
      await fetchPaymentsAndSubscriptions(true);
    } catch (err: any) {
      console.error('handleExecuteApproval error:', err);
      const usefulMessage = err?.message || 'Khalad aan la filayn ayaa dhacay.';
      setErrorMsg(`Khalad maamulka bixinta: ${usefulMessage}`);
      alert(`Khalad: ${usefulMessage}`);
    } finally {
      setIsLoading(false);
      setProcessingId(null);
    }
  };

  const recent10Count = payments.filter((p) => isWithinMinutes(p.created_at, 10)).length;

  const filteredPayments = [...payments]
    .sort((a, b) => {
      // 1. Pending always on top so admin can take action
      if (a.status === 'pending' && b.status !== 'pending') return -1;
      if (b.status === 'pending' && a.status !== 'pending') return 1;
      // 2. Newest created_at first
      const timeA = a.created_at ? new Date(a.created_at).getTime() : 0;
      const timeB = b.created_at ? new Date(b.created_at).getTime() : 0;
      return timeB - timeA;
    })
    .filter((p) => {
      if (filterStatus === 'today') {
        if (!p.created_at) return false;
        try {
          if (new Date(p.created_at).toLocaleDateString('en-CA') !== todayDateStr) return false;
        } catch {
          return false;
        }
      } else if (filterStatus === 'recent10') {
        if (!isWithinMinutes(p.created_at, 10)) return false;
      } else if (filterStatus !== 'all' && p.status !== filterStatus) {
        return false;
      }
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase().trim();
      return (
        (p.phone_number && p.phone_number.toLowerCase().includes(q)) ||
        (p.user_name && p.user_name.toLowerCase().includes(q)) ||
        (p.reference_number && p.reference_number.toLowerCase().includes(q))
      );
    });

  const now = new Date();
  const todayDateStr = now.toLocaleDateString('en-CA'); // YYYY-MM-DD

  const totalRevenue = payments
    .filter(p => p.status === 'completed')
    .reduce((sum, p) => sum + Number(p.amount || 0.25), 0);

  const dailyPayments = payments.filter(p => {
    if (p.status !== 'completed' || !p.created_at) return false;
    try {
      return new Date(p.created_at).toLocaleDateString('en-CA') === todayDateStr;
    } catch {
      return false;
    }
  });
  const dailyRevenue = dailyPayments.reduce((sum, p) => sum + Number(p.amount || 0.25), 0);
  const dailyCount = dailyPayments.length;

  const pendingCount = payments.filter(p => p.status === 'pending').length;

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <CreditCard className="w-6 h-6 text-[#E50914] shrink-0" />
            <h2 className="text-lg sm:text-xl font-black text-black">Maamulka Lacagaha & Driver Pass ($0.25/Shift)</h2>
          </div>
          <p className="text-xs text-neutral-600 mt-1">
            Eeg oo ansixi/diid bixinta darawallada ee EVC Plus, ZAAD, ama Sahal ($0.25 maalinle 05:00 AM).
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Bulk Approve All Pending Button (20k scale) */}
          {pendingCount > 0 && canManage && (
            <button
              onClick={handleBulkApproveAll}
              disabled={isLoading}
              className="flex items-center space-x-1.5 px-3.5 py-2 rounded-xl text-xs font-black bg-emerald-600 hover:bg-emerald-700 text-white shadow-md transition cursor-pointer shrink-0"
              title="Hal mar wada ansixi dhammaan darawallada sugaya"
            >
              <Zap className="w-4 h-4 fill-white" />
              <span>Ansixi Dhammaan ({pendingCount})</span>
            </button>
          )}

          {/* Single Unified Cash Chime Button for Pending Payments */}
          <button
            onClick={() => {
              const next = !soundEnabled;
              setSoundEnabled(next);
              soundAlert.setEnabled(next);
              if (next) {
                soundAlert.testSound();
              }
            }}
            className={`flex items-center space-x-2 px-3 py-2 rounded-xl text-xs font-black transition cursor-pointer border shadow-xs ${
              soundEnabled
                ? 'bg-emerald-50 text-emerald-800 border-emerald-300 hover:bg-emerald-100'
                : 'bg-neutral-100 text-neutral-500 border-neutral-300 hover:bg-neutral-200'
            }`}
            title={soundEnabled ? "Dhawaaqa lacagta waa shidan yahay (Guji si aad u damiso ama u tijaabiso)" : "Dhawaaqa waa damisan yahay (Guji si aad u daarto)"}
          >
            {soundEnabled ? <Volume2 className="w-4 h-4 text-emerald-600 shrink-0" /> : <VolumeX className="w-4 h-4 text-neutral-500 shrink-0" />}
            <span>{soundEnabled ? 'Dhawaaqa: DAARAN' : 'Dhawaaqa: DAMISAN'}</span>
            {soundEnabled && <Bell className="w-3.5 h-3.5 text-amber-500 shrink-0 animate-pulse" />}
          </button>

          <button 
            onClick={() => fetchPaymentsAndSubscriptions(false)}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>
        </div>
      </div>

      {/* Practical Guidance Note for Mobile Money in Somalia */}
      <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-start space-x-3 text-emerald-950">
        <div className="p-2 bg-emerald-100 rounded-xl text-emerald-700 shrink-0 mt-0.5">
          <Info className="w-5 h-5" />
        </div>
        <div className="text-xs leading-relaxed">
          <p className="font-bold text-emerald-900 text-sm">Xaqiijinta Lacagaha EVC Plus & ZAAD (Taleefan Gacmeed / Nokia Yar)</p>
          <p className="mt-1 text-emerald-800">
            Darawallada badankoodu waxay lacagta ka soo diraan taleefan kale oo baabaco ah (Nokia yar), ma haysan karaan tixraac qoraal ah. Sidaas darteed, 
            <strong> nidaamku wuxuu si buuxda u muujinayaa Lambarka Lacagta laga soo diray</strong>. 
            Kaliya ka hubi taleefankaaga ganacsiga fariinta EVC Plus ee sheegaysa: <em>"Waxaad $0.25 ka heshay lambarka..."</em> kadibna riix <strong>Ansixi</strong>.
          </p>
        </div>
      </div>

      {/* 10-Minute Alert Notification */}
      {recent10Count > 0 && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-300 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-emerald-950">
          <div className="flex items-center space-x-3">
            <span className="relative flex h-3 w-3 shrink-0">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-600"></span>
            </span>
            <p className="text-xs sm:text-sm font-bold">
              Waxaa jira <span className="underline font-black">{recent10Count} bixin cusub</span> oo soo gashay 10-kii daqiiqo ee ugu dambeysay!
            </p>
          </div>
          <button
            onClick={() => setFilterStatus(filterStatus === 'recent10' ? 'all' : 'recent10')}
            className={`px-3 py-1.5 text-xs font-bold rounded-xl transition shrink-0 cursor-pointer ${
              filterStatus === 'recent10'
                ? 'bg-neutral-800 text-white'
                : 'bg-emerald-600 text-white hover:bg-emerald-700 shadow'
            }`}
          >
            {filterStatus === 'recent10' ? 'Muuji Dhammaan' : 'Kaliya Eeg Kuwa 10-ka Daqiiqo'}
          </button>
        </div>
      )}

      {!canManage && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs font-semibold flex items-center space-x-2">
          <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0" />
          <span>Fiiro gaar ah: Keliya PAYMENT ADMIN ama SUPER ADMIN ayaa ansixin kara ama diidi kara lacagaha.</span>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4">
        {/* Dakhliga Guud (Total Revenue) */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Dakhliga Guud</p>
            <span className="p-1 rounded-lg bg-emerald-50 text-emerald-600">
              <DollarSign className="w-4 h-4" />
            </span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-emerald-600 mt-2">${totalRevenue.toFixed(2)} USD</p>
          <p className="text-[11px] text-neutral-400 mt-1">Wadarta lacagaha la ansixiyay</p>
        </div>

        {/* Dakhliga Maalinlaha ah (Daily Revenue / Maanta) */}
        <div className="bg-gradient-to-br from-emerald-50 to-white p-5 rounded-2xl border-2 border-emerald-500/40 shadow-xs">
          <div className="flex items-center justify-between">
            <p className="text-xs font-black text-emerald-800 uppercase tracking-wider">Dakhliga Maanta</p>
            <span className="px-1.5 py-0.5 rounded-md bg-emerald-600 text-white text-[10px] font-black">
              Maalinle
            </span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-emerald-700 mt-2">${dailyRevenue.toFixed(2)} USD</p>
          <p className="text-[11px] text-emerald-600 font-semibold mt-1">🟢 {dailyCount} bixino maanta la ansixiyay</p>
        </div>

        {/* Sugaya Ansixinta (Pending) */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Sugaya Ansixinta</p>
          <p className="text-2xl sm:text-3xl font-black text-[#E50914] mt-2">{pendingCount}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Bixino cusub oo u baahan fiirin</p>
        </div>

        {/* 10-Daqiiqo Cusub */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">10-Daqiiqo Cusub</p>
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-emerald-800 mt-2">{recent10Count}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Bixino soo dhacay 10 min gudahood</p>
        </div>

        {/* Wadarta Bixinnada (Total Count) */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Wadarta Bixinnada</p>
          <p className="text-2xl sm:text-3xl font-black text-black mt-2">{payments.length}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Completed, Pending, Rejected</p>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Search & Filter Controls */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Filter Tabs */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1">
          <Filter className="w-4 h-4 text-neutral-400 shrink-0" />
          
          <button
            onClick={() => setFilterStatus('all')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'all'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            Dhammaan ({payments.length})
          </button>

          <button
            onClick={() => setFilterStatus('today')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shrink-0 ${
              filterStatus === 'today'
                ? 'bg-emerald-600 text-white shadow'
                : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>Maanta Shushubtay ({dailyCount})</span>
          </button>

          <button
            onClick={() => setFilterStatus('recent10')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shrink-0 ${
              filterStatus === 'recent10'
                ? 'bg-emerald-600 text-white shadow'
                : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200'
            }`}
          >
            <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
            <span>10-Kii Daqiiqo ee Ugu Dambeeyay ({recent10Count})</span>
          </button>

          <button
            onClick={() => setFilterStatus('pending')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'pending'
                ? 'bg-[#E50914] text-white shadow'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            Pending ({pendingCount})
          </button>

          <button
            onClick={() => setFilterStatus('completed')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'completed'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            Ansixsan
          </button>

          <button
            onClick={() => setFilterStatus('rejected')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'rejected'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            La Diaday
          </button>
        </div>

        {/* Search by phone or name */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Ka raadi lambar, tixraac, ama magac..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-white border border-neutral-200 rounded-xl text-xs font-medium text-black focus:outline-none focus:border-black"
          />
        </div>
      </div>

      {/* Payments Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
            <p className="text-sm font-semibold">Soo qaadaya bixinnada...</p>
          </div>
        ) : filteredPayments.length === 0 ? (
          <div className="p-12 text-center">
            <CreditCard className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Xog lama helin</h3>
            <p className="text-xs text-neutral-500 mt-1 mb-4">Ma jiro bixin ku jira shuruudahan hadda.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[760px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Lambarka Lacagta Laga Soo Diray</th>
                  <th className="p-4 font-bold">Darawalka / Account</th>
                  <th className="p-4 font-bold">Tixraaca (Ref / TRX)</th>
                  <th className="p-4 font-bold">Lacagta</th>
                  <th className="p-4 font-bold">Habka Bixinta</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  <th className="p-4 font-bold">Waqtiga</th>
                  {canManage && <th className="p-4 font-bold text-right">Ficil (Approval)</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredPayments.map((p) => {
                  const isRecent = isWithinMinutes(p.created_at, 10);
                  return (
                    <tr 
                      key={p.id} 
                      className={`transition ${
                        isRecent 
                          ? 'bg-emerald-50/80 hover:bg-emerald-100/70 border-l-4 border-l-emerald-500' 
                          : 'hover:bg-neutral-50'
                      }`}
                    >
                      <td className="p-4">
                        <div className="flex items-center space-x-2.5">
                          <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 border ${
                            isRecent ? 'bg-emerald-100 border-emerald-300' : 'bg-emerald-50 border-emerald-200'
                          }`}>
                            <Smartphone className="w-4 h-4 text-emerald-600" />
                          </div>
                          <div>
                            <div className="flex items-center space-x-1.5">
                              <span className="font-mono font-black text-black text-xs sm:text-sm">
                                {p.sender_phone || p.phone_number || 'Lama gelin'}
                              </span>
                              {(p.sender_phone || p.phone_number) && (
                                <button
                                  onClick={() => handleCopyPhone(p.sender_phone || p.phone_number)}
                                  className="p-1 hover:bg-neutral-100 rounded text-neutral-400 hover:text-neutral-700 transition cursor-pointer"
                                  title="Nuuxi lambarka lacagta laga soo diray"
                                >
                                  {copiedPhone === (p.sender_phone || p.phone_number) ? (
                                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                                  ) : (
                                    <Copy className="w-3.5 h-3.5" />
                                  )}
                                </button>
                              )}
                            </div>
                            <div className="flex items-center space-x-1.5 mt-0.5">
                              {isRecent ? (
                                <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-emerald-200/80 text-emerald-900 text-[10px] font-black border border-emerald-400 animate-pulse">
                                  <Sparkles className="w-3 h-3 text-emerald-700" />
                                  CUSUB (10 min)
                                </span>
                              ) : (
                                <p className="text-[10px] text-emerald-700 font-semibold">💳 Lacagta ka timid</p>
                              )}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="p-4">
                        <p className="font-bold text-black text-xs">{p.user_name || `Darawal ${p.user_id.slice(0, 6)}`}</p>
                        {p.account_phone ? (
                          <div className="flex items-center space-x-1 mt-1">
                            <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-blue-50 text-blue-800 text-[11px] font-mono font-bold border border-blue-200">
                              📱 Akoon: {p.account_phone}
                            </span>
                            <button
                              onClick={() => handleCopyPhone(p.account_phone!)}
                              className="p-1 hover:bg-neutral-100 rounded text-neutral-400 hover:text-neutral-700 transition cursor-pointer"
                              title="Nuuxi lambarka akoonka"
                            >
                              {copiedPhone === p.account_phone ? (
                                <Check className="w-3 h-3 text-emerald-600" />
                              ) : (
                                <Copy className="w-3 h-3" />
                              )}
                            </button>
                          </div>
                        ) : (
                          <p className="text-[10px] font-mono text-neutral-400 mt-0.5">ID: {p.user_id.slice(0, 8)}</p>
                        )}
                      </td>
                      <td className="p-4">
                        {p.reference_number?.startsWith('TG-') ? (
                          <div>
                            <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 text-[10px] font-bold border border-blue-200">
                              📱 Tel-Gacmeed (Toos)
                            </span>
                            <p className="text-[10px] font-mono text-neutral-400 mt-0.5">{p.reference_number}</p>
                          </div>
                        ) : (
                          <div>
                            <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-neutral-100 text-neutral-800 text-[10px] font-bold border border-neutral-200">
                              🧾 SMS: {p.reference_number || 'Toos'}
                            </span>
                          </div>
                        )}
                      </td>
                      <td className="p-4 font-black text-emerald-600">${Number(p.amount || 0.25).toFixed(2)} USD</td>
                      <td className="p-4 text-xs font-bold text-neutral-800">{p.payment_method}</td>
                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                          p.status === 'completed' ? 'bg-emerald-100 text-emerald-800' :
                          p.status === 'pending' ? 'bg-amber-100 text-amber-800' :
                          'bg-red-100 text-red-800'
                        }`}>
                          {p.status === 'completed' ? '🟢 ANSIXSAN' : p.status === 'pending' ? '🟡 SUGAYA' : '🔴 LA DIADAY'}
                        </span>
                        {p.status === 'rejected' && p.rejection_reason && (
                          <div className="mt-1.5 flex items-start space-x-1 text-[11px] text-red-700 bg-red-50 p-1.5 rounded-lg border border-red-200 max-w-[220px]">
                            <AlertCircle className="w-3.5 h-3.5 text-red-500 shrink-0 mt-0.5" />
                            <span className="leading-tight"><strong className="font-bold">Sabab:</strong> {p.rejection_reason}</span>
                          </div>
                        )}
                      </td>
                      <td className="p-4">
                        <p className={`text-xs font-mono font-bold ${isRecent ? 'text-emerald-700' : 'text-neutral-700'}`}>
                          {formatTimeAgoSomali(p.created_at)}
                        </p>
                        <p className="text-[10px] font-mono text-neutral-400 mt-0.5">
                          {formatExactDateTime(p.created_at)}
                        </p>
                      </td>
                      {canManage && (
                        <td className="p-4 text-right space-x-1.5 whitespace-nowrap">
                          {p.status === 'pending' ? (
                            <>
                              <button
                                onClick={() => handleQuickSingleApprove(p)}
                                disabled={isLoading || processingId === p.id}
                                title="Ansixi degdeg ah (1-click)"
                                className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white text-xs font-black rounded-xl shadow transition cursor-pointer inline-flex items-center space-x-1"
                              >
                                {processingId === p.id ? (
                                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <Check className="w-3.5 h-3.5" />
                                )}
                                <span>{processingId === p.id ? 'Socda...' : '✔ Ansixi Hadda'}</span>
                              </button>
                              <button
                                onClick={() => setConfirmModal({
                                  isOpen: true,
                                  paymentId: p.id,
                                  action: 'approve',
                                  amount: Number(p.amount || 0.25),
                                  phone: p.phone_number,
                                  userId: p.user_id,
                                  subId: p.subscription_id
                                })}
                                disabled={isLoading || processingId === p.id}
                                className="px-2.5 py-1.5 bg-neutral-100 hover:bg-neutral-200 disabled:opacity-50 text-neutral-800 text-xs font-bold rounded-xl border border-neutral-300 transition cursor-pointer"
                              >
                                Fiiri
                              </button>
                              <button
                                onClick={() => {
                                  setRejectionReason('');
                                  setConfirmModal({
                                    isOpen: true,
                                    paymentId: p.id,
                                    action: 'reject',
                                    amount: Number(p.amount || 0.25),
                                    phone: p.phone_number,
                                    userId: p.user_id,
                                    subId: p.subscription_id
                                  });
                                }}
                                disabled={isLoading || processingId === p.id}
                                className="px-2.5 py-1.5 bg-red-100 hover:bg-red-200 disabled:opacity-50 text-red-700 text-xs font-bold rounded-xl transition cursor-pointer"
                              >
                                Diid
                              </button>
                            </>
                          ) : p.status === 'completed' ? (
                            <span className="text-xs text-emerald-700 font-bold bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                              Dhammaystiran
                            </span>
                          ) : (
                            <span className="text-xs text-red-700 font-bold bg-red-50 px-2.5 py-1 rounded-lg border border-red-200">
                              Waa La Diiday
                            </span>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      <ConfirmationModal
        isOpen={confirmModal.isOpen}
        title={confirmModal.action === 'approve' ? 'Ansixinta Bixinta Driver Pass' : 'Diidmada Bixinta Driver Pass'}
        message={
          confirmModal.action === 'approve'
            ? `Fadlan taleefankaaga ganacsiga ee EVC Plus/ZAAD ka hubi inaad heshay $${confirmModal.amount.toFixed(2)} USD oo ka timid lambarka: ${confirmModal.phone || 'Lama gelin'}.\n\nMa xaqiijinaysaa inaad lacagta heshay oo aad darawalka u furto Driver Pass-ka?`
            : `Ma hubtaa inaad diido bixintan $${confirmModal.amount.toFixed(2)} USD ee taleefanka: ${confirmModal.phone || 'Lama gelin'}?`
        }
        confirmLabel={confirmModal.action === 'approve' ? 'Haa, Waan Helay (Ansixi)' : 'Diid Bixinta'}
        cancelLabel="Jooji"
        isDanger={confirmModal.action === 'reject'}
        isLoading={isLoading}
        onConfirm={handleExecuteApproval}
        onCancel={() => {
          setConfirmModal({ isOpen: false, paymentId: '', action: 'approve', amount: 0.25, phone: '', userId: '' });
          setRejectionReason('');
        }}
      >
        {confirmModal.action === 'reject' && (
          <div className="space-y-3 pt-2">
            <label className="block text-xs font-bold text-neutral-800">
              Sababta loo diiday (Rejection Reason):
            </label>
            <textarea
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              placeholder="Qor sababta diidmada (tusaale: Lacagta lama helin, Lambarka ma saxna...)"
              className="w-full p-2.5 bg-neutral-50 border border-neutral-300 rounded-xl text-xs font-medium text-black focus:outline-none focus:border-red-500 focus:bg-white resize-none"
              rows={3}
            />
            <div className="space-y-1">
              <span className="text-[11px] text-neutral-500 font-medium">Xulo sabab degdeg ah:</span>
              <div className="flex flex-wrap gap-1.5">
                {[
                  'Lacagta lama helin SMS-ka',
                  'Lambarka ma saxna',
                  'Lacagtu waa dhiman tahay ($0.25 kayar)',
                  'Tixraac/TRX ID khaldan'
                ].map((reason) => (
                  <button
                    key={reason}
                    type="button"
                    onClick={() => setRejectionReason(reason)}
                    className="text-[10px] px-2 py-1 rounded-lg bg-neutral-100 hover:bg-neutral-200 text-neutral-700 font-semibold transition border border-neutral-200 cursor-pointer"
                  >
                    {reason}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}
      </ConfirmationModal>
    </div>
  );
};
