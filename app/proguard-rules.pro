# Shizuku user service is instantiated by class name via reflection in the shell process.
-keep class de.xitee.buttonmapper.shizuku.EventReaderService { <init>(...); *; }
-keep class de.xitee.buttonmapper.shizuku.** implements android.os.IInterface { *; }
-keep class * extends android.os.Binder { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
