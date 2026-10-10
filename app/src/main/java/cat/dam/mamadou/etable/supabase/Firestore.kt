package cat.dam.mamadou.etable.supabase

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.Date
import java.util.UUID

/*
 * Capa de compatibilidad: imita la parte de la API de Cloud Firestore que usa eTable, guardando cada
 * documento como una fila de `public.documents` (collection, id, data jsonb) en Supabase.
 * Así los managers y pantallas originales conservan su lógica; solo cambia el import.
 *
 * Los whereEqualTo simples (texto, booleano, entero) se envían también a PostgREST para descargar menos filas;
 * el resto de filtros, el orden y el limit se aplican en el cliente (que repite los whereEqualTo como red de seguridad).
 */

private const val TABLE = "documents"
private const val TAG = "SupabaseStore"

internal val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
internal val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

@Serializable
internal data class DocRow(val collection: String, val id: String, val data: JsonObject)

// "users" y "user" eran dos colecciones con el mismo contenido en la app original
private fun canonical(collection: String) = if (collection == "users") "user" else collection

// ───────────────────────── Tipos de valor ─────────────────────────

class Timestamp(val seconds: Long, val nanoseconds: Int) : Comparable<Timestamp> {
    constructor(date: Date) : this(Math.floorDiv(date.time, 1000L), (Math.floorMod(date.time, 1000L) * 1_000_000).toInt())
    fun toDate() = Date(seconds * 1000 + nanoseconds / 1_000_000)
    internal fun millis() = toDate().time
    override fun compareTo(other: Timestamp) = compareValuesBy(this, other, { it.seconds }, { it.nanoseconds })
    override fun equals(other: Any?) = other is Timestamp && compareTo(other) == 0
    override fun hashCode() = millis().hashCode()
    override fun toString() = "Timestamp(${toDate()})"
    companion object { fun now() = Timestamp(Date()) }
}

enum class SetOptions { MERGE; companion object { fun merge() = MERGE } }

object FieldPath { internal const val ID = "__name__"; fun documentId(): Any = ID }

class FirebaseFirestoreException(message: String?, cause: Throwable? = null) : Exception(message, cause)

private val gson: Gson = GsonBuilder()
    .registerTypeAdapter(Date::class.java, JsonDeserializer { j, _, _ -> Date(j.asLong) })
    .create()

internal fun toJson(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Timestamp -> buildJsonObject { put("__ts", value.millis()) }
    is Date -> toJson(Timestamp(value))
    is Enum<*> -> JsonPrimitive(value.name)
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJson(v) })
    is Iterable<*> -> JsonArray(value.map(::toJson))
    is Array<*> -> JsonArray(value.map(::toJson))
    else -> Json.parseToJsonElement(gson.toJson(value)) // objetos (data classes) como hacía Firestore
}

internal fun fromJson(e: JsonElement): Any? = when (e) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        e.isString -> e.content
        e.booleanOrNull != null -> e.boolean
        e.longOrNull != null -> e.long          // Firestore devuelve enteros como Long
        else -> e.double
    }
    is JsonObject -> if (e.size == 1 && "__ts" in e) Timestamp(Date(e["__ts"]!!.jsonPrimitive.long))
                     else e.mapValuesTo(HashMap()) { fromJson(it.value) }
    is JsonArray -> e.mapTo(ArrayList(), ::fromJson)
}

@Suppress("UNCHECKED_CAST")
private fun toDataMap(o: JsonObject) = fromJson(o) as Map<String, Any>

private fun valueAt(data: Map<String, Any>?, path: String): Any? =
    path.split('.').fold(data as Any?) { acc, key -> (acc as? Map<*, *>)?.get(key) }

// Comparación al estilo Firestore: números entre sí, textos, fechas, booleanos
private fun normalize(v: Any?): Any? = when (v) {
    is Date -> Timestamp(v)
    is Number -> v.toDouble()
    is Enum<*> -> v.name
    else -> v
}

private fun compareValues(a: Any?, b: Any?): Int? {
    val x = normalize(a); val y = normalize(b)
    return when {
        x is Double && y is Double -> x.compareTo(y)
        x is String && y is String -> x.compareTo(y)
        x is Timestamp && y is Timestamp -> x.compareTo(y)
        x is Boolean && y is Boolean -> x.compareTo(y)
        else -> null
    }
}

// ───────────────────────── Task (equivalente a com.google.android.gms.tasks.Task) ─────────────────────────

class Task<T> internal constructor(block: suspend () -> T) {
    private val deferred = ioScope.async { block() }

    val isComplete get() = deferred.isCompleted
    val isSuccessful get() = deferred.isCompleted && deferred.getCompletionExceptionOrNull() == null
    val exception: Exception? get() = if (deferred.isCompleted) deferred.getCompletionExceptionOrNull()?.asException() else null
    @OptIn(ExperimentalCoroutinesApi::class)
    val result: T get() {
        check(deferred.isCompleted) { "Task.result leído antes de que la tarea termine; usa addOnSuccessListener o await()" }
        return deferred.getCompleted()
    }

    fun addOnSuccessListener(listener: (T) -> Unit): Task<T> = also {
        mainScope.launch { runCatching { deferred.await() }.onSuccess(listener) }
    }

    fun addOnFailureListener(listener: (Exception) -> Unit): Task<T> = also {
        mainScope.launch { runCatching { deferred.await() }.onFailure { listener(it.asException()) } }
    }

    fun addOnCompleteListener(listener: (Task<T>) -> Unit): Task<T> = also {
        mainScope.launch { runCatching { deferred.await() }; listener(this@Task) }
    }

    internal suspend fun awaitResult(): T = deferred.await()
}

private fun Throwable.asException() = this as? Exception ?: Exception(this)

/** Sustituye a kotlinx.coroutines.tasks.await */
suspend fun <T> Task<T>.await(): T = awaitResult()

// ───────────────────────── Acceso a datos ─────────────────────────

internal object Store {
    private val table get() = supabaseClient.from(TABLE)

    private const val PAGE = 1000 // = max-rows por defecto de PostgREST en Supabase

    /** [jsonEq]: pares (columna PostgREST, textos aceptados) que se filtran ya en el servidor. */
    suspend fun list(collection: String, jsonEq: List<Pair<String, List<String>>> = emptyList()): List<DocRow> {
        val rows = mutableListOf<DocRow>()
        while (true) {
            val from = rows.size.toLong()
            val page = table.select {
                filter {
                    eq("collection", collection)
                    jsonEq.forEach { (column, values) -> if (values.size == 1) eq(column, values[0]) else isIn(column, values) }
                }
                order("id", Order.ASCENDING) // orden estable para que las páginas no se solapen
                range(from, from + PAGE - 1)
            }.decodeList<DocRow>()
            rows += page
            if (page.size < PAGE) return rows
        }
    }

    suspend fun get(collection: String, id: String): DocRow? =
        table.select { filter { eq("collection", collection); eq("id", id) } }.decodeSingleOrNull()

    suspend fun set(collection: String, id: String, data: JsonObject) =
        table.upsert(DocRow(collection, id, data)) { onConflict = "collection,id" }

    suspend fun update(collection: String, id: String, patch: JsonObject) {
        supabaseClient.postgrest.rpc("doc_update", buildJsonObject {
            put("p_collection", collection); put("p_id", id); put("p_patch", patch)
        })
    }

    suspend fun delete(collection: String, id: String) =
        table.delete { filter { eq("collection", collection); eq("id", id) } }
}

/** Un único canal Realtime para toda la app; emite el nombre de la colección que ha cambiado. */
internal object Changes {
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 256)
    private var started = false

    @Synchronized
    fun flow(): SharedFlow<String> {
        if (!started) {
            started = true
            ioScope.launch { keepSubscribed() }
        }
        return events
    }

    /*
     * Si la suscripción falla o el canal se cae (y la librería no lo recupera sola), lo recrea con backoff
     * 1s, 2s, 4s… hasta 30s. `events` es siempre el mismo, así que los listeners existentes no se pierden;
     * al volver emite "*" para que re-descarguen lo que se hayan perdido mientras tanto.
     */
    private suspend fun keepSubscribed() {
        var backoff = 1_000L
        var reconnecting = false
        while (true) {
            val channel = supabaseClient.channel("documents-changes")
            val relay = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = TABLE }
                .onEach { action ->
                    val record = when (action) {
                        is PostgresAction.Insert -> action.record
                        is PostgresAction.Update -> action.record
                        is PostgresAction.Delete -> action.oldRecord
                        else -> null
                    }
                    events.emit(record?.get("collection")?.jsonPrimitive?.contentOrNull ?: "*")
                }
                .launchIn(ioScope)
            try {
                val subscribed = withTimeoutOrNull(15_000) { channel.subscribe(blockUntilSubscribed = true) } != null
                if (subscribed) {
                    if (reconnecting) events.emit("*")
                    backoff = 1_000L
                    // La librería re-une el canal sola tras cortes breves del websocket; solo actuamos si no vuelve
                    while (true) {
                        channel.status.first { it == RealtimeChannel.Status.UNSUBSCRIBED }
                        withTimeoutOrNull(10_000) { channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED } } ?: break
                        events.emit("*")
                    }
                    Log.w(TAG, "Canal Realtime caído; se vuelve a suscribir")
                } else {
                    Log.w(TAG, "Tiempo agotado suscribiendo a Realtime")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo suscribir a Realtime", e)
            }
            relay.cancel()
            runCatching { supabaseClient.realtime.removeChannel(channel) }
            reconnecting = true
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(30_000L)
        }
    }
}

// ───────────────────────── API estilo Firestore ─────────────────────────

class FirebaseApp internal constructor(val applicationContext: android.content.Context)

class FirebaseFirestore private constructor() {
    val app get() = FirebaseApp(appContext)
    fun collection(path: String) = CollectionReference(canonical(path))
    fun batch() = WriteBatch()

    fun <T> runTransaction(function: (Transaction) -> T): Task<T> = Task {
        // shortcut: no es atómico; lecturas y escrituras van seguidas. Suficiente para crear una reserva.
        val tx = Transaction()
        val result = function(tx)
        tx.commit()
        result
    }

    companion object {
        private val instance = FirebaseFirestore()
        fun getInstance() = instance
    }
}

fun interface ListenerRegistration { fun remove() }

open class DocumentSnapshot internal constructor(
    val reference: DocumentReference,
    open val data: Map<String, Any>?,
) {
    val id get() = reference.id
    fun exists() = data != null
    operator fun get(field: String): Any? = valueAt(data, field)
    fun contains(field: String) = get(field) != null
    fun getString(field: String) = get(field) as? String
    fun getBoolean(field: String) = get(field) as? Boolean
    fun getLong(field: String) = (get(field) as? Number)?.toLong()
    fun getDouble(field: String) = (get(field) as? Number)?.toDouble()
    fun getTimestamp(field: String) = get(field) as? Timestamp
    fun getDate(field: String) = getTimestamp(field)?.toDate()

    fun <T> toObject(clazz: Class<T>): T? {
        val d = data ?: return null
        // Las fechas viajan como milisegundos para que Gson las convierta a Date
        fun plain(v: Any?): Any? = when (v) {
            is Timestamp -> v.millis()
            is Map<*, *> -> v.mapValues { plain(it.value) }
            is List<*> -> v.map(::plain)
            else -> v
        }
        return gson.fromJson(gson.toJsonTree(plain(d)), clazz)
    }
}

/** Documento que sale de una consulta: siempre existe, así que `data` no es nulo (como en Firestore). */
class QueryDocumentSnapshot internal constructor(reference: DocumentReference, override val data: Map<String, Any>) :
    DocumentSnapshot(reference, data)

class DocumentChange internal constructor(val type: Type, val document: QueryDocumentSnapshot) {
    enum class Type { ADDED, MODIFIED, REMOVED }
}

class QuerySnapshot internal constructor(
    val documents: List<QueryDocumentSnapshot>,
    val documentChanges: List<DocumentChange>,
) : Iterable<QueryDocumentSnapshot> {
    val isEmpty get() = documents.isEmpty()
    fun size() = documents.size
    override fun iterator() = documents.iterator()
}

private val simpleFieldPath = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")

/** Columna PostgREST para filtrar `field` en el servidor, o null si solo se puede filtrar en el cliente. */
private fun serverColumn(field: Any): String? {
    if (field == FieldPath.ID) return "id"
    val path = field.toString()
    if (!simpleFieldPath.matches(path)) return null
    val parts = path.split('.')
    return "data" + parts.dropLast(1).joinToString("") { "->$it" } + "->>" + parts.last()
}

/** Textos que puede devolver `data->>campo` para `value`; null si ese tipo no se filtra en el servidor. */
private fun serverTexts(value: Any?): List<String>? = when (value) {
    is String -> listOf(value)
    is Boolean -> listOf(value.toString())
    is Int, is Long, is Short, is Byte -> listOf("$value", "$value.0") // jsonb conserva el ".0" si se guardó como Double
    is Double, is Float -> value.toDouble().takeIf { it % 1.0 == 0.0 && kotlin.math.abs(it) < 1e15 }
        ?.toLong()?.let { listOf("$it", "$it.0") }
    else -> null
}

open class Query internal constructor(
    internal val collection: String,
    private val filters: List<(DocumentSnapshot) -> Boolean> = emptyList(),
    private val orders: List<Pair<String, Direction>> = emptyList(),
    private val limit: Long? = null,
    private val serverEq: List<Pair<String, List<String>>> = emptyList(),
) {
    enum class Direction { ASCENDING, DESCENDING }

    private fun field(doc: DocumentSnapshot, f: Any) = if (f == FieldPath.ID) doc.id else doc[f.toString()]
    private fun where(f: Any, server: Pair<String, List<String>>? = null, test: (Any?) -> Boolean) =
        Query(collection, filters + { test(field(it, f)) }, orders, limit, serverEq + listOfNotNull(server))

    fun whereEqualTo(field: Any, value: Any?): Query {
        val column = serverColumn(field)
        val texts = serverTexts(value)
        val server = if (column != null && texts != null) column to texts else null
        return where(field, server) { it == value || compareValues(it, value) == 0 }
    }
    fun whereNotEqualTo(field: Any, value: Any?) = where(field) { it != null && compareValues(it, value) != 0 && it != value }
    fun whereGreaterThan(field: Any, value: Any) = where(field) { (compareValues(it, value) ?: -1) > 0 }
    fun whereGreaterThanOrEqualTo(field: Any, value: Any) = where(field) { (compareValues(it, value) ?: -1) >= 0 }
    fun whereLessThan(field: Any, value: Any) = where(field) { (compareValues(it, value) ?: 1) < 0 }
    fun whereLessThanOrEqualTo(field: Any, value: Any) = where(field) { (compareValues(it, value) ?: 1) <= 0 }
    fun whereIn(field: Any, values: List<Any?>) = where(field) { v -> values.any { v == it || compareValues(v, it) == 0 } }
    fun whereArrayContains(field: Any, value: Any) = where(field) { (it as? List<*>)?.contains(value) == true }

    fun orderBy(field: String, direction: Direction = Direction.ASCENDING) =
        Query(collection, filters + { it[field] != null }, orders + (field to direction), limit, serverEq)

    fun limit(n: Long) = Query(collection, filters, orders, n, serverEq)

    private fun apply(rows: List<DocRow>): List<QueryDocumentSnapshot> {
        var docs = rows.map { QueryDocumentSnapshot(DocumentReference(collection, it.id), toDataMap(it.data)) }
            .filter { d -> filters.all { it(d) } }
        if (orders.isNotEmpty()) docs = docs.sortedWith { a, b ->
            orders.asSequence().map { (f, dir) ->
                val c = compareValues(a[f], b[f]) ?: 0
                if (dir == Direction.DESCENDING) -c else c
            }.firstOrNull { it != 0 } ?: 0
        }
        return limit?.let { docs.take(it.toInt()) } ?: docs
    }

    internal suspend fun fetch() = apply(Store.list(collection, serverEq))

    fun get(): Task<QuerySnapshot> = Task { fetch().let { QuerySnapshot(it, it.map { d -> DocumentChange(DocumentChange.Type.ADDED, d) }) } }

    fun addSnapshotListener(listener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit): ListenerRegistration {
        val job = ioScope.launch {
            var previous = emptyMap<String, QueryDocumentSnapshot>()
            suspend fun emit() {
                runCatching { fetch() }
                    .onSuccess { docs ->
                        val current = docs.associateBy { it.id }
                        val changes = docs.mapNotNull { d ->
                            when (val old = previous[d.id]) {
                                null -> DocumentChange(DocumentChange.Type.ADDED, d)
                                else -> if (old.data != d.data) DocumentChange(DocumentChange.Type.MODIFIED, d) else null
                            }
                        } + previous.values.filter { it.id !in current }.map { DocumentChange(DocumentChange.Type.REMOVED, it) }
                        val first = previous.isEmpty() && changes.isEmpty()
                        previous = current
                        if (changes.isNotEmpty() || first) withContext(Dispatchers.Main.immediate) { listener(QuerySnapshot(docs, changes), null) }
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        withContext(Dispatchers.Main.immediate) { listener(null, FirebaseFirestoreException(e.message, e)) }
                    }
            }
            val changes = Changes.flow()
            emit()
            @OptIn(FlowPreview::class)
            changes.filter { it == collection || it == "*" }.debounce(150).collect { emit() }
        }
        return ListenerRegistration { job.cancel() }
    }
}

class CollectionReference internal constructor(collection: String) : Query(collection) {
    val id get() = collection
    val path get() = collection
    fun document(id: String) = DocumentReference(collection, id)
    fun document() = DocumentReference(collection, UUID.randomUUID().toString())
    fun add(data: Any): Task<DocumentReference> = document().let { ref -> ref.set(data).continueWithRef(ref) }
}

private fun Task<Unit>.continueWithRef(ref: DocumentReference) = Task { awaitResult(); ref }

class DocumentReference internal constructor(internal val collection: String, val id: String) {
    val path get() = "$collection/$id"

    internal suspend fun fetch() = DocumentSnapshot(this, Store.get(collection, id)?.let { toDataMap(it.data) })

    fun get(): Task<DocumentSnapshot> = Task { fetch() }

    fun set(data: Any): Task<Unit> = Task { Store.set(collection, id, toJson(data).jsonObject) }

    fun set(data: Any, options: SetOptions): Task<Unit> = Task {
        val existing = Store.get(collection, id)
        if (existing == null) Store.set(collection, id, toJson(data).jsonObject)
        else Store.update(collection, id, toJson(data).jsonObject)
    }

    fun update(data: Map<String, Any?>): Task<Unit> = Task { Store.update(collection, id, toJson(data).jsonObject) }

    fun update(field: String, value: Any?, vararg more: Any?): Task<Unit> =
        update(mapOf(field to value) + more.toList().chunked(2).associate { (k, v) -> k.toString() to v })

    fun delete(): Task<Unit> = Task { Store.delete(collection, id) }

    fun addSnapshotListener(listener: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit): ListenerRegistration {
        val job = ioScope.launch {
            var emitted = false
            var lastData: Map<String, Any>? = null
            suspend fun emit() {
                runCatching { fetch() }
                    .onSuccess {
                        // Cualquier cambio en la colección dispara el evento: solo avisar si este documento cambió
                        if (emitted && it.data == lastData) return@onSuccess
                        emitted = true
                        lastData = it.data
                        withContext(Dispatchers.Main.immediate) { listener(it, null) }
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        withContext(Dispatchers.Main.immediate) { listener(null, FirebaseFirestoreException(e.message, e)) }
                    }
            }
            val changes = Changes.flow()
            emit()
            @OptIn(FlowPreview::class)
            changes.filter { it == collection || it == "*" }.debounce(150).collect { emit() }
        }
        return ListenerRegistration { job.cancel() }
    }
}

class WriteBatch internal constructor() {
    private val ops = mutableListOf<suspend () -> Unit>()
    fun set(ref: DocumentReference, data: Any) = also { ops += { ref.set(data).awaitResult() } }
    fun update(ref: DocumentReference, data: Map<String, Any?>) = also { ops += { ref.update(data).awaitResult() } }
    fun update(ref: DocumentReference, field: String, value: Any?) = also { ops += { ref.update(field, value).awaitResult() } }
    fun delete(ref: DocumentReference) = also { ops += { ref.delete().awaitResult() } }
    // shortcut: las operaciones se aplican en orden, no de forma atómica
    fun commit(): Task<Unit> = Task { ops.forEach { it() } }
}

class Transaction internal constructor() {
    private val batch = WriteBatch()
    fun get(ref: DocumentReference): DocumentSnapshot = runBlocking { ref.fetch() }
    fun set(ref: DocumentReference, data: Any) = also { batch.set(ref, data) }
    fun update(ref: DocumentReference, data: Map<String, Any?>) = also { batch.update(ref, data) }
    fun update(ref: DocumentReference, field: String, value: Any?) = also { batch.update(ref, field, value) }
    fun delete(ref: DocumentReference) = also { batch.delete(ref) }
    internal suspend fun commit() = batch.commit().awaitResult()
}
