# O projeto não usa reflexão no caminho quente, mas Room e Hilt sim.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Modelos de dados imutáveis usados no Compose
-keep class com.afinu.app.core.model.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class com.afinu.app.data.db.** { *; }

# Mantém números de linha para stack traces legíveis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile