# Stage 1: Shrink only (no obfuscation, no optimization)
-dontoptimize
-dontobfuscate

# Preserve attributes used by Retrofit/Gson
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Rely on global proguard-rules.pro for coroutines keep
