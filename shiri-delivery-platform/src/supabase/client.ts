import { createClient, SupabaseClient } from '@supabase/supabase-js';

// Retrieve credentials from environment variables or localStorage (allows configuration directly in UI)
const envUrl = import.meta.env.VITE_SUPABASE_URL || '';
const envKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

const storedUrl = typeof window !== 'undefined' ? localStorage.getItem('shiri_supabase_url') || '' : '';
const storedKey = typeof window !== 'undefined' ? localStorage.getItem('shiri_supabase_key') || '' : '';

export const supabaseUrl = envUrl || storedUrl;
export const supabaseAnonKey = envKey || storedKey;

export const isSupabaseConfigured = (): boolean => {
  return Boolean(
    supabaseUrl &&
    supabaseUrl.startsWith('https://') &&
    supabaseUrl.includes('supabase.co') &&
    supabaseAnonKey &&
    supabaseAnonKey.length > 20
  );
};

export const setSupabaseCredentials = (url: string, key: string) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem('shiri_supabase_url', url.trim());
    localStorage.setItem('shiri_supabase_key', key.trim());
  }
};

let clientInstance: SupabaseClient | null = null;

export const getSupabaseClient = (): SupabaseClient | null => {
  if (!isSupabaseConfigured()) {
    return null;
  }
  if (!clientInstance) {
    try {
      clientInstance = createClient(supabaseUrl, supabaseAnonKey, {
        auth: {
          persistSession: true,
          autoRefreshToken: true,
        },
      });
    } catch (err) {
      console.warn('Failed to initialize Supabase client:', err);
      return null;
    }
  }
  return clientInstance;
};

export const supabase = getSupabaseClient();
