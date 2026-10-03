import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://lcigjbtprkloopvzpaxy.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
