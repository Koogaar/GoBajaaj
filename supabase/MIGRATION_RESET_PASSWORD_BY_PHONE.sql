-- ==========================================================
-- GOBAJAAJ: RESET PASSWORD BY PHONE (SECURITY DEFINER RPC)
-- Dib u habaynta furaha sirta iyadoo la isticmaalayo taleefanka
-- ==========================================================

-- Enable pgcrypto if not already enabled (for password hashing)
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
    v_e164 text;
    v_user_id uuid;
    v_profile_name text;
    v_encrypted_pw text;
BEGIN
    -- 1. Check if input is email or phone
    IF p_phone LIKE '%@%' THEN
        -- Email lookup
        SELECT id, full_name INTO v_user_id, v_profile_name
        FROM public.profiles
        WHERE lower(phone_number) = lower(trim(p_phone))
        LIMIT 1;

        IF v_user_id IS NULL THEN
            SELECT id, email INTO v_user_id, v_profile_name
            FROM auth.users
            WHERE lower(email) = lower(trim(p_phone))
            LIMIT 1;
        END IF;
    ELSE
        -- Phone lookup
        v_clean_phone := regexp_replace(p_phone, '[^0-9]', '', 'g');
        IF length(v_clean_phone) >= 7 THEN
            IF length(v_clean_phone) >= 9 THEN
                v_e164 := '+252' || right(v_clean_phone, 9);
            ELSE
                v_e164 := '+252' || v_clean_phone;
            END IF;
        ELSE
            RETURN jsonb_build_object('success', false, 'message', 'Fadlan geli nambar taleefan ama Gmail sax ah.');
        END IF;

        SELECT id, full_name INTO v_user_id, v_profile_name
        FROM public.profiles
        WHERE phone_number = v_e164
           OR phone_number = right(v_clean_phone, 9)
           OR phone_number = '0' || right(v_clean_phone, 9)
           OR phone_number = '252' || right(v_clean_phone, 9)
           OR phone_number = v_clean_phone
           OR phone_number = p_phone
        LIMIT 1;
    END IF;

    -- 2. Validate password length
    IF length(p_new_password) < 4 THEN
        RETURN jsonb_build_object('success', false, 'message', 'Furaha sirta waa inuu ugu yaraan 4 xaraf yahay.');
    END IF;

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Ma jiro akoon ku diiwaangashan: ' || p_phone);
    END IF;

    -- 4. Fuzzy check full name if provided (security verification)
    IF trim(coalesce(p_full_name, '')) != '' THEN
        IF lower(trim(v_profile_name)) NOT LIKE '%' || lower(trim(p_full_name)) || '%' 
           AND lower(trim(p_full_name)) NOT LIKE '%' || lower(trim(v_profile_name)) || '%' THEN
            RETURN jsonb_build_object('success', false, 'message', 'Magaca aad gelisay kuma habboona xogta akoonka ku diiwaangashan.');
        END IF;
    END IF;

    -- 5. Hash password using bcrypt and update auth.users
    v_encrypted_pw := extensions.crypt(p_new_password, extensions.gen_salt('bf'));

    UPDATE auth.users
    SET encrypted_password = v_encrypted_pw,
        updated_at = now()
    WHERE id = v_user_id;

    RETURN jsonb_build_object(
        'success', true, 
        'message', 'Furaha sirta si guul leh ayaa loo beddelay! Hadda waad gali kartaa.'
    );
END;
$$;

-- Allow anon and authenticated users to call this reset RPC
REVOKE ALL ON FUNCTION public.reset_user_password_by_phone(text, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.reset_user_password_by_phone(text, text, text) TO anon, authenticated;
