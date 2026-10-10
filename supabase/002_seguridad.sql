-- eTable · 002 · Endurecimiento de seguridad (auditoría oct-2026)
-- Se ejecuta sobre una base de datos que ya tiene schema.sql aplicado. No borra datos.
--  · Superadmin real en el servidor (antes solo existía en la interfaz por el email admin@admin.cat)
--  · Usuarios creados fuera de la app quedan inactivos hasta que un admin los active
--  · Un camarero ya no puede borrar el historial, aprobarse descansos ni pedirlos en nombre de otro
--  · Validación de email y nombre en admin_create_user
--  · Políticas con (select …) para que Postgres evalúe is_staff()/is_admin() una vez por consulta

create or replace function public.is_superadmin() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from documents where collection = 'user' and id = auth.uid()::text
                 and data->>'role' = 'admin' and coalesce((data->>'superadmin')::boolean, false)
                 and coalesce((data->>'isActive')::boolean, true))
$$;

-- Altas desde el panel de Supabase u otros orígenes: inactivas hasta que un admin las active
create or replace function public.default_user_doc(p_id uuid, p_email text, p_full_name text) returns jsonb
language sql immutable as $$
  select jsonb_build_object(
    'id', p_id::text, 'email', p_email, 'username', split_part(p_email, '@', 1),
    'fullName', coalesce(p_full_name, ''), 'role', 'cambrer', 'isActive', false,
    'darkThemeEnabled', false, 'textSize', 16, 'language', 'es', 'textToSpeechEnabled', false,
    'accessibilityEnabled', false, 'canRequestBreak', true, 'canChangeTheme', true, 'canLogout', true,
    'canChangeLanguage', true, 'canUseTTS', true, 'canChangeTextSize', true,
    'allowedThemes', '[]'::jsonb, 'allowedLanguages', '[]'::jsonb)
$$;

create or replace function public.guard_user_doc() returns trigger
language plpgsql as $$
declare k text;
begin
  if auth.uid() is not null then
    if new.collection is distinct from old.collection or new.id is distinct from old.id then
      raise exception 'No se puede mover un documento a otra colección';
    end if;

    if new.collection = 'user' then
      if not public.is_admin() then
        foreach k in array array['role','isActive','email','username','superadmin','canRequestBreak','canChangeTheme',
                                 'canLogout','canChangeLanguage','canUseTTS','canChangeTextSize','allowedThemes','allowedLanguages'] loop
          if (new.data->k) is distinct from (old.data->k) then
            raise exception 'Solo un administrador puede cambiar %', k;
          end if;
        end loop;
      elsif not public.is_superadmin() and (
              coalesce((old.data->>'superadmin')::boolean, false)
              or (new.data->'superadmin') is distinct from (old.data->'superadmin')
              or ((new.data->'role') is distinct from (old.data->'role')
                  and 'admin' in (coalesce(new.data->>'role', ''), coalesce(old.data->>'role', '')))) then
        raise exception 'Solo el superadministrador puede gestionar administradores';
      end if;

    elsif new.collection = 'break_requests' and not public.is_admin()
          and ((new.data->'status') is distinct from (old.data->'status')
               or (new.data->'userId') is distinct from (old.data->'userId')) then
      raise exception 'Solo un administrador puede resolver solicitudes de descanso';
    end if;
  end if;
  new.updated_at = now();
  return new;
end $$;

create or replace function public.admin_create_user(p_email text, p_password text, p_full_name text, p_role text)
returns uuid language plpgsql security definer set search_path = public, extensions as $$
declare uid uuid := gen_random_uuid();
begin
  if not is_admin() then raise exception 'Solo un administrador puede crear usuarios'; end if;
  if p_role not in ('admin', 'cambrer') then raise exception 'Rol no válido'; end if;
  if p_role = 'admin' and not is_superadmin() then
    raise exception 'Solo el superadministrador puede crear administradores';
  end if;
  if p_email !~ '^[^@\s]+@[^@\s]+\.[^@\s]+$' then raise exception 'Email no válido'; end if;
  if length(p_password) < 6 then raise exception 'La contraseña debe tener al menos 6 caracteres'; end if;
  if length(coalesce(p_full_name, '')) > 100 then raise exception 'El nombre es demasiado largo'; end if;
  insert into auth.users (instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
                          raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                          confirmation_token, email_change, email_change_token_new, recovery_token)
  values ('00000000-0000-0000-0000-000000000000', uid, 'authenticated', 'authenticated', lower(p_email),
          crypt(p_password, gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}',
          jsonb_build_object('full_name', p_full_name), now(), now(), '', '', '', '');
  insert into auth.identities (id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
  values (gen_random_uuid(), uid, uid::text,
          jsonb_build_object('sub', uid::text, 'email', lower(p_email), 'email_verified', true),
          'email', now(), now(), now());
  update documents set data = data || jsonb_build_object('role', p_role, 'fullName', p_full_name, 'isActive', true)
   where collection = 'user' and id = uid::text;
  return uid;
end $$;

-- Borrar un administrador (por admin_delete_user o directamente) exige ser superadmin
create or replace function public.guard_user_removal() returns trigger
language plpgsql as $$
begin
  if old.collection = 'user' and auth.uid() is not null and old.data->>'role' = 'admin'
     and not public.is_superadmin() then
    raise exception 'Solo el superadministrador puede borrar administradores';
  end if;
  return old;
end $$;

create or replace trigger documents_guard_removal before delete on public.documents
  for each row execute function public.guard_user_removal();

grant execute on function public.is_superadmin() to authenticated;

alter policy "leer" on public.documents
  using ((select is_staff()) or (collection = 'user' and id = (select auth.uid())::text));

alter policy "crear" on public.documents
  with check ((select is_admin())
              or (collection not in ('user', 'menu_items') and (select is_staff())
                  and (collection <> 'break_requests' or data->>'userId' = (select auth.uid())::text)));

alter policy "editar" on public.documents
  using ((select is_admin()) or (collection not in ('user', 'menu_items') and (select is_staff()))
         or (collection = 'user' and id = (select auth.uid())::text))
  with check ((select is_admin()) or (collection not in ('user', 'menu_items') and (select is_staff()))
              or (collection = 'user' and id = (select auth.uid())::text));

-- El historial de ventas y los descansos solo los borra un admin
alter policy "borrar" on public.documents
  using ((select is_admin())
         or (collection not in ('user', 'menu_items', 'history_orders', 'break_requests') and (select is_staff())));

-- El propietario del proyecto es el superadministrador: cambia el email por el de tu cuenta admin
update public.documents set data = data || '{"superadmin": true}'
 where collection = 'user' and data->>'email' = 'YOUR_ADMIN_EMAIL@example.com';
