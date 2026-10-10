// Autotest del esquema: node check.mjs schema.sql  (requiere: npm i @electric-sql/pglite@0.3)
// Simula lo mínimo de Supabase (auth, roles, Realtime) y comprueba reglas de seguridad y doc_update.
import { PGlite } from '@electric-sql/pglite';
import { pgcrypto } from '@electric-sql/pglite/contrib/pgcrypto';
import fs from 'fs';
import assert from 'assert/strict';

const db = new PGlite({ extensions: { pgcrypto } });
await db.exec(`
create role anon; create role authenticated; create schema auth; create schema extensions;
create extension pgcrypto schema extensions;
create table auth.users(id uuid primary key default gen_random_uuid(), instance_id uuid, aud text, role text,
  email text, encrypted_password text, email_confirmed_at timestamptz, raw_app_meta_data jsonb,
  raw_user_meta_data jsonb default '{}', created_at timestamptz, updated_at timestamptz,
  confirmation_token text, email_change text, email_change_token_new text, recovery_token text);
create table auth.identities(id uuid, user_id uuid references auth.users on delete cascade, provider_id text,
  identity_data jsonb, provider text, last_sign_in_at timestamptz, created_at timestamptz, updated_at timestamptz);
create function auth.uid() returns uuid language sql stable as
  $$ select nullif(current_setting('test.uid', true), '')::uuid $$;
create publication supabase_realtime;`);
for (const f of process.argv.slice(2)) await db.exec(fs.readFileSync(f, 'utf8'));  // schema.sql + migraciones
await db.exec(`grant usage on schema public, auth to authenticated, anon; grant all on public.documents to authenticated;`);

const q = async (s, p) => (await db.query(s, p)).rows;
const as = async (uid, fn) => {  // ejecuta como usuario autenticado (RLS activo)
  await db.exec(`set test.uid = '${uid}'; set role authenticated`);
  try { return await fn(); } finally { await db.exec(`reset role; set test.uid = ''`); }
};
const fails = async (p, msg) => { await assert.rejects(p); console.log('OK bloqueado:', msg); };

const [a] = await q(`insert into auth.users(email, raw_user_meta_data) values ('jefe@x.com','{"role":"admin"}') returning id`);
const [w] = await q(`insert into auth.users(email) values ('cam@x.com') returning id`);
assert.equal((await q(`select data->>'role' r from documents where id = $1`, [a.id]))[0].r, 'cambrer', 'metadata no da admin');
assert.equal((await q(`select data->>'isActive' v from documents where id = $1`, [w.id]))[0].v, 'false', 'altas externas inactivas');
// Desde el SQL Editor (sin JWT): jefe = superadmin, camarero activado
await q(`update documents set data = data || '{"role":"admin","superadmin":true,"isActive":true}' where collection='user' and id = $1`, [a.id]);
await q(`update documents set data = data || '{"isActive":true}' where collection='user' and id = $1`, [w.id]);

// Camarero
await as(w.id, async () => {
  assert.equal((await q(`select count(*)::int n from documents where collection='tables'`))[0].n, 12);
  await q(`select doc_update('tables', '3', '{"status":"OCCUPIED","clientes":4}')`);
  await q(`update documents set data = data || '{"language":"ca"}' where collection='user' and id = $1`, [w.id]);
  await fails(q(`update documents set data = data || '{"role":"admin"}' where collection='user' and id = $1`, [w.id]), 'camarero se hace admin');
  await fails(q(`select admin_create_user('x@x.com','123456','X','admin')`), 'camarero crea usuarios');
  const n = (await q(`delete from documents where collection='menu_items' returning id`)).length;
  assert.equal(n, 0, 'camarero no borra la carta'); console.log('OK bloqueado: camarero borra carta');
  await q(`insert into documents(collection, id, data) values ('history_orders', 'h1', '{"total":10}')`);
  assert.equal((await q(`delete from documents where collection='history_orders' returning id`)).length, 0);
  console.log('OK bloqueado: camarero borra historial');
  await fails(q(`insert into documents(collection, data) values ('break_requests', '{"userId":"otro","status":"pending"}')`), 'camarero pide descanso por otro');
  await q(`insert into documents(collection, id, data) values ('break_requests', 'b1', $1)`, [JSON.stringify({ userId: w.id, status: 'pending' })]);
  await fails(q(`select doc_update('break_requests', 'b1', '{"status":"approved"}')`), 'camarero se aprueba el descanso');
});
assert.equal((await q(`select data->>'status' s from documents where collection='tables' and id='3'`))[0].s, 'OCCUPIED');

// Admin
await as(a.id, async () => {
  const [o] = await q(`insert into documents(collection, data) values ('orders', '{"tableId":3}') returning id`);
  await q(`select doc_update('orders', $1, '{"stages.SEGONS.ready": true}')`, [o.id]);
  assert.deepEqual((await q(`select data->'stages' s from documents where id = $1`, [o.id]))[0].s, { SEGONS: { ready: true } });
  await fails(q(`select doc_update('orders', 'no-existe', '{"a":1}')`), 'update de documento inexistente');
  const [{ admin_create_user: nu }] = await q(`select admin_create_user('Nuevo@X.com','secreto1','Nuevo','cambrer')`);
  assert.equal((await q(`select data->>'fullName' f from documents where id = $1`, [nu]))[0].f, 'Nuevo');
  assert.equal((await q(`select data->>'isActive' v from documents where id = $1`, [nu]))[0].v, 'true', 'altas del admin activas');
  await fails(q(`select admin_create_user('mal-email','secreto1','X','cambrer')`), 'email no válido');
  await q(`select admin_delete_user($1)`, [nu]);
  await q(`select doc_update('break_requests', 'b1', '{"status":"approved"}')`);
});

// Admin normal (no superadmin): no gestiona administradores
const [{ admin_create_user: adm2 }] = await as(a.id, () => q(`select admin_create_user('admin2@x.com','secreto1','Admin 2','admin')`));
await as(adm2, async () => {
  await fails(q(`select admin_create_user('admin3@x.com','secreto1','A3','admin')`), 'admin normal crea admins');
  await fails(q(`update documents set data = data || '{"role":"admin"}' where collection='user' and id = $1`, [w.id]), 'admin normal asciende a admin');
  await fails(q(`update documents set data = data || '{"isActive":false}' where collection='user' and id = $1`, [a.id]), 'admin normal desactiva al superadmin');
  await fails(q(`select admin_delete_user($1)`, [a.id]), 'admin normal borra al superadmin');
  await q(`update documents set data = data || '{"fullName":"Camarero"}' where collection='user' and id = $1`, [w.id]);
});
// Sin sesión no se ve nada
await db.exec(`set role anon`);
await fails(q(`select * from documents`), 'anon lee documentos');
console.log('Todo OK');
