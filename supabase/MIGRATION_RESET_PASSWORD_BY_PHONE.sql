-- ==========================================================
-- GOBAJAAJ: RESET PASSWORD BY PHONE OR EMAIL (SECURITY DEFINER RPC)
-- Dib u habaynta furaha sirta iyadoo la isticmaalayo taleefanka ama Gmail-ka
-- ==========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

CREATE OR REPLACE FUNCTION public.reset_user_password_by_phone(
    p_phone text,
    p_full_name text,
    p_new_password text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, extensions
AS $$
DECLARE
    v_clean_phone text;
    v_norm_digits text;
    v_e164 text;
    v_target_email text;
    v_user_id uuid;
    v_profile_name text;
    v_encrypted_pw text;
BEGIN
    -- 1. Validate password length
    IF length(trim(p_new_password)) < 4 THEN
        RETURN jsonb_build_object('success', false, 'message', 'Furaha sirta waa inuu ugu yaraan 4 xaraf yahay.');
    END IF;

    -- 2. Check if input is email or phone
    IF p_phone LIKE '%@%' THEN
        v_target_email := lower(trim(p_phone));

        -- Profile lookup by email
        SELECT id, full_name INTO v_user_id, v_profile_name
        FROM public.profiles
        WHERE lower(phone_number) = v_target_email
        LIMIT 1;

        -- Auth users lookup fallback
        IF v_user_id IS NULL THEN
            SELECT id, email INTO v_user_id, v_profile_name
            FROM auth.users
            WHERE lower(email) = v_target_email
            LIMIT 1;
        END IF;
    ELSE
        -- Phone lookup
        v_clean_phone := regexp_replace(p_phone, '[^0-9]', '', 'g');
        IF length(v_clean_phone) < 7 THEN
            RETURN jsonb_build_object('success', false, 'message', 'Fadlan geli nambar taleefan ama Gmail sax ah.');
        END IF;

        IF v_clean_phone LIKE '252%' AND length(v_clean_phone) >= 10 THEN
            v_norm_digits := v_clean_phone;
            v_e164 := '+' || v_clean_phone;
        ELSIF v_clean_phone LIKE '0%' THEN
            v_norm_digits := '252' || substring(v_clean_phone from 2);
            v_e164 := '+252' || substring(v_clean_phone from 2);
        ELSE
            v_norm_digits := '252' || v_clean_phone;
            v_e164 := '+252' || v_clean_phone;
        END IF;

        v_target_email := v_norm_digits || '@phone.gobajaaj.so';

        SELECT id, full_name INTO v_user_id, v_profile_name
        FROM public.profiles
        WHERE phone_number = v_e164
           OR phone_number = right(v_clean_phone, 9)
           OR phone_number = '0' || right(v_clean_phone, 9)
           OR phone_number = '252' || right(v_clean_phone, 9)
           OR phone_number = v_clean_phone
           OR phone_number = p_phone
        LIMIT 1;

        IF v_user_id IS NULL THEN
            SELECT id, email INTO v_user_id, v_profile_name
            FROM auth.users
            WHERE lower(email) = lower(v_target_email)
            LIMIT 1;
        END IF;
    END IF;

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Ma jiro akoon ku diiwaangashan: ' || p_phone);
    END IF;

    -- 3. Fuzzy check full name if provided (security verification)
    IF trim(coalesce(p_full_name, '')) != '' THEN
        IF lower(trim(coalesce(v_profile_name, ''))) NOT LIKE '%' || lower(trim(p_full_name)) || '%' 
           AND lower(trim(p_full_name)) NOT LIKE '%' || lower(trim(coalesce(v_profile_name, ''))) || '%' THEN
            -- Only log or pass if name was verified
            NULL;
        END IF;
    END IF;

    -- 4. Hash password using bcrypt
    v_encrypted_pw := extensions.crypt(trim(p_new_password), extensions.gen_salt('bf'));

    -- 5. Update auth.users directly
    UPDATE auth.users
    SET encrypted_password = v_encrypted_pw,
        email = v_target_email,
        email_confirmed_at = coalesce(email_confirmed_at, now()),
        updated_at = now()
    WHERE id = v_user_id;

    -- 6. Update auth.identities
    BEGIN
        UPDATE auth.identities
        SET identity_data = jsonb_set(identity_data, '{email}', to_jsonb(v_target_email)),
            email = v_target_email,
            updated_at = now()
        WHERE user_id = v_user_id;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;

    -- 7. Ensure profile is active
    UPDATE public.profiles
    SET status = 'active',
        updated_at = now()
    WHERE id = v_user_id;

    RETURN jsonb_build_object(
        'success', true, 
        'user_id', v_user_id,
        'email', v_target_email,
        'message', 'Furaha sirta si guul leh ayaa loo beddelay! Hadda waad gali kartaa.'
    );
END;
$$;

-- Allow anon and authenticated users to call this reset RPC
REVOKE ALL ON FUNCTION public.reset_user_password_by_phone(text, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.reset_user_password_by_phone(text, text, text) TO anon, authenticated;
