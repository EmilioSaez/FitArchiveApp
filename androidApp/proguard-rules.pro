# Reglas R8 de FitArchive (release). Las librerías con consumer rules propias
# (Compose, ML Kit/Play Services, OkHttp, SQLDelight android-driver) no necesitan nada aquí.

# kotlinx.serialization: serializadores generados (Open-Meteo, copia de seguridad JSON).
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor usa SLF4J y detección de debug de JVM que no existen en Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.**

# Trazas legibles en Play Console (se sube mapping.txt con el bundle).
-keepattributes SourceFile, LineNumberTable
