# =====================================================
# 1. PROTECCIÓN ABSOLUTA DE TU CÓDIGO FUENTE
# Impide que R8 borre ViewModels, Repositorios o Resultados
# =====================================================
-keep class com.example.myapplication.** { *; }
-keep interface com.example.myapplication.** { *; }
-keep enum com.example.myapplication.** { *; }

# =====================================================
# 2. BARRERA PARA ODOO (APACHE XML-RPC)
# XML-RPC usa fábricas dinámicas. R8 no debe tocar NADA aquí.
# Esto soluciona el cierre automático al presionar LOGIN.
# =====================================================
-keep class org.apache.xmlrpc.** { *; }
-keep class org.apache.ws.** { *; }
-keep class org.apache.commons.** { *; }
-keepclassmembers class org.apache.xmlrpc.** { *; }
-dontwarn org.apache.xmlrpc.**
-dontwarn org.apache.ws.**
-dontwarn org.apache.commons.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.naming.**
-dontwarn javax.management.**

# =====================================================
# 3. BARRERA PARA SEGURIDAD (ENCRYPTED SHARED PREFERENCES)
# Tink crashea instantáneamente si se ofusca.
# =====================================================
-keep class androidx.security.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn androidx.security.**
-dontwarn com.google.crypto.tink.**

# =====================================================
# 4. BARRERA PARA RED (RETROFIT, OKHTTP, GSON)
# =====================================================
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.google.gson.**
-dontwarn sun.misc.Unsafe

# =====================================================
# 5. BARRERA PARA CORRUTINAS Y LIFECYCLE
# =====================================================
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.lifecycle.**

# =====================================================
# REGLAS GENERALES DE ATRIBUTOS
# =====================================================
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions