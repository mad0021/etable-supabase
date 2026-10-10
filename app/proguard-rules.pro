# El código de la app se conserva entero: Gson (toObject, NotificationHelper) y la capa supabase/
# leen los modelos por reflexión. R8 solo reduce las librerías.
-keep class cat.dam.mamadou.etable.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Los Log.d/v/i no llegan a la versión publicada (muchos imprimen emails y nombres de clientes)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# Ktor / supabase-kt
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
