-- eTable · esquema Supabase
-- La app original guarda documentos estilo Firestore (colección + id + campos libres). Para portarla sin
-- cambiar su lógica, cada documento es una fila de `documents` con su contenido en `data` (jsonb).
-- La capa Kotlin `cat.dam.mamadou.etable.supabase` imita la API de Firestore encima de esta tabla.
-- Ejecutar una sola vez en Supabase → SQL Editor (no borra nada: si existen tablas de un intento anterior, quedan sin uso).

-- ───────── Documentos ─────────
create table if not exists public.documents (
  collection text not null,
  id         text not null default gen_random_uuid()::text,
  data       jsonb not null default '{}',
  updated_at timestamptz not null default now(),
  primary key (collection, id)
);
-- Realtime necesita la fila antigua completa para saber de qué colección era un DELETE
alter table public.documents replica identity full;

-- Usuarios: colección "user", id = auth.users.id, rol en data.role ('admin' | 'cambrer')
create or replace function public.is_staff() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from documents where collection = 'user' and id = auth.uid()::text
                 and coalesce((data->>'isActive')::boolean, true))
$$;

create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from documents where collection = 'user' and id = auth.uid()::text
                 and data->>'role' = 'admin' and coalesce((data->>'isActive')::boolean, true))
$$;

-- Documento de usuario por defecto (mismos campos que creaba FirebaseUserManager.createUser)
create or replace function public.default_user_doc(p_id uuid, p_email text, p_full_name text) returns jsonb
language sql immutable as $$
  select jsonb_build_object(
    'id', p_id::text, 'email', p_email, 'username', split_part(p_email, '@', 1),
    'fullName', coalesce(p_full_name, ''), 'role', 'cambrer', 'isActive', true,
    'darkThemeEnabled', false, 'textSize', 16, 'language', 'es', 'textToSpeechEnabled', false,
    'accessibilityEnabled', false, 'canRequestBreak', true, 'canChangeTheme', true, 'canLogout', true,
    'canChangeLanguage', true, 'canUseTTS', true, 'canChangeTextSize', true,
    'allowedThemes', '[]'::jsonb, 'allowedLanguages', '[]'::jsonb)
$$;

-- Al crear un usuario en Auth se crea su documento. Rol SIEMPRE 'cambrer' (nunca desde metadata).
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into documents (collection, id, data)
  values ('user', new.id::text, default_user_doc(new.id, new.email, new.raw_user_meta_data->>'full_name'))
  on conflict do nothing;
  return new;
end $$;

create or replace trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- Un camarero puede editar su propio documento (tema, idioma…) pero no rol, estado ni permisos.
-- Sin JWT (auth.uid() null) = SQL Editor / funciones internas.
create or replace function public.guard_user_doc() returns trigger
language plpgsql as $$
declare k text;
begin
  if new.collection = 'user' and auth.uid() is not null and not public.is_admin() then
    foreach k in array array['role','isActive','email','username','canRequestBreak','canChangeTheme','canLogout',
                             'canChangeLanguage','canUseTTS','canChangeTextSize','allowedThemes','allowedLanguages'] loop
      if (new.data->k) is distinct from (old.data->k) then
        raise exception 'Solo un administrador puede cambiar %', k;
      end if;
    end loop;
  end if;
  new.updated_at = now();
  return new;
end $$;

create or replace trigger documents_guard before update on public.documents
  for each row execute function public.guard_user_doc();

-- update() de Firestore: fusiona campos (admite rutas con puntos "stages.X.ready") y falla si no existe.
-- security invoker: respeta RLS.
create or replace function public.doc_update(p_collection text, p_id text, p_patch jsonb) returns void
language plpgsql set search_path = public as $$
declare
  d jsonb; k text; path text[]; i int;
begin
  select data into d from documents where collection = p_collection and id = p_id for update;
  if not found then raise exception 'NOT_FOUND: %/%', p_collection, p_id; end if;
  for k in select jsonb_object_keys(p_patch) loop
    path := string_to_array(k, '.');
    for i in 1 .. array_length(path, 1) - 1 loop  -- crea los objetos intermedios que falten
      if jsonb_typeof(d #> path[1:i]) is distinct from 'object' then
        d := jsonb_set(d, path[1:i], '{}'::jsonb, true);
      end if;
    end loop;
    d := jsonb_set(d, path, p_patch->k, true);
  end loop;
  update documents set data = d where collection = p_collection and id = p_id;
end $$;

-- Alta de usuarios desde la pantalla de admin (en Firebase era createUserWithEmailAndPassword).
create or replace function public.admin_create_user(p_email text, p_password text, p_full_name text, p_role text)
returns uuid language plpgsql security definer set search_path = public, extensions as $$
declare uid uuid := gen_random_uuid();
begin
  if not is_admin() then raise exception 'Solo un administrador puede crear usuarios'; end if;
  if p_role not in ('admin', 'cambrer') then raise exception 'Rol no válido'; end if;
  if length(p_password) < 6 then raise exception 'La contraseña debe tener al menos 6 caracteres'; end if;
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
  update documents set data = data || jsonb_build_object('role', p_role, 'fullName', p_full_name)
   where collection = 'user' and id = uid::text;
  return uid;
end $$;

create or replace function public.admin_delete_user(p_user_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not is_admin() then raise exception 'Solo un administrador puede borrar usuarios'; end if;
  if p_user_id = auth.uid() then raise exception 'No puedes borrarte a ti mismo'; end if;
  delete from documents where collection = 'user' and id = p_user_id::text;
  delete from auth.users where id = p_user_id;
end $$;

revoke execute on function public.admin_create_user, public.admin_delete_user from anon, public;
grant execute on function public.admin_create_user, public.admin_delete_user, public.doc_update to authenticated;

-- ───────── Seguridad (RLS) ─────────
-- Personal activo: lee todo y opera mesas, pedidos, reservas, descansos.
-- Carta y usuarios: solo admin (cada usuario puede editar su propio documento, con el trigger de arriba).
alter table public.documents enable row level security;

create policy "leer" on public.documents for select to authenticated
  using (is_staff() or (collection = 'user' and id = auth.uid()::text));
create policy "crear" on public.documents for insert to authenticated
  with check (is_admin() or (collection not in ('user', 'menu_items') and is_staff()));
create policy "editar" on public.documents for update to authenticated
  using (is_admin() or (collection not in ('user', 'menu_items') and is_staff())
         or (collection = 'user' and id = auth.uid()::text));
create policy "borrar" on public.documents for delete to authenticated
  using (is_admin() or (collection not in ('user', 'menu_items') and is_staff()));

alter publication supabase_realtime add table public.documents;

-- ───────── Datos iniciales, en el mismo formato que usaba Firestore ─────────
insert into public.documents (collection, id, data)
-- name vacío: la tarjeta de mesa muestra el número en grande, como en la app original
select 'tables', n::text, jsonb_build_object('id', n, 'name', '', 'status', 'FREE',
       'customers', 0, 'clientes', 0, 'isOccupied', false, 'orders', '[]'::jsonb)
from generate_series(1, 12) n;

insert into public.documents (collection, data)
-- Fotos incluidas en la app (app/src/main/assets/menu, créditos en CREDITS.txt)
select 'menu_items', jsonb_build_object('name', name, 'category', category, 'isAvailable', true,
       'requiresCookingPoint', cooking, 'price', price, 'imageUrl', 'file:///android_asset/menu/' || img || '.jpg')
from (values ('Ensalada César', 'PRIMERS', false, 9.50, 'ensalada-cesar'), ('Croquetas caseras', 'PRIMERS', false, 7.00, 'croquetas'),
             ('Entrecot', 'SEGONS', true, 19.90, 'entrecot'), ('Ceviche', 'SEGONS', false, 15.50, 'ceviche'),
             ('Crema catalana', 'POSTRES', false, 5.50, 'crema-catalana'), ('Agua', 'BEGUDES', false, 2.00, 'agua'),
             ('Refresco', 'BEGUDES', false, 2.80, 'refresco')) m(name, category, cooking, price, img);

-- Usuarios que ya existían en Auth antes de este esquema
insert into public.documents (collection, id, data)
select 'user', u.id::text, default_user_doc(u.id, u.email, u.raw_user_meta_data->>'full_name')
from auth.users u on conflict do nothing;
