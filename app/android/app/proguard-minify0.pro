# Stage 0: Minify enabled but effectively no shrinking/obfuscation/optimization
-dontshrink
-dontoptimize
-dontobfuscate

# Keep method/field signatures and annotations commonly needed by reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

