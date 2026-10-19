# ---------------------------------------------------------------------------
# Regras do R8 para o release.
#
# Estas regras não são decorativas: sem elas o build de debug passa e o
# release **quebra em runtime**, no aparelho do usuário. Foi o que aconteceu com
# apps que usaram Room sem manter as entidades, e com widgets sem manter o
# receiver.
#
# Android Gradle Plugin já gera regras de consumidor para Compose, Hilt,
# coroutines, Room e lifecycle. O que está abaixo cobre o que falta ou o que
# merece ficar explícito para documentar a intenção.
# ---------------------------------------------------------------------------

# --- Room -------------------------------------------------------------------
# O Room gera `AppDatabase_Impl` e as implementações de DAO por geração de
# código, e as referencia por nome em tempo de execução. Se as entidades forem
# removidas, o Room falha ao abrir o banco — e só no release, porque o build de
# debug não minifica.
-keep class com.afinu.app.data.db.** { *; }

# --- Widget -----------------------------------------------------------------
# Widget e `BroadcastReceiver` são instanciados pelo sistema a partir do
# manifesto, por nome. O AGP já preserva componentes do manifesto, mas manter
# explicitamente documenta a dependência e protege contra o caso em que alguém
# registra o receiver por código.
-keep class com.afinu.app.widget.TunerWidgetProvider { *; }
-keep class com.afinu.app.widget.ReferenceToneReceiver { *; }

# --- Hilt / Dagger ----------------------------------------------------------
# O Hilt resolve a dependência em tempo de compilação, mas as classes geradas
# usam reflexão em alguns pontos do framework.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# --- kotlinx.serialization --------------------------------------------------
# O plugin do Kotlin gera o serializer e o referencia estaticamente em quem
# usa `Foo.serializer()`. Estas regras cobrem o caso de alguém desserializar
# por `::class` ou por `Class.serializer()`, que resolve por reflexão.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations
-keepclassmembers class com.afinu.app.data.repository.FavoritesExport$Item {
    *** Companion;
    <fields>;
}
-keep,includedescriptorclasses class com.afinu.app.data.repository.FavoritesExport$$serializer { *; }

# --- Modelos usados em telas e na UI ---------------------------------------
# Não há reflexão sobre eles, mas manter evita que uma refatoração renomeie um
# campo e quebre um `copy()` gerado em outro lugar sem erro de compilação.
-keep class com.afinu.app.core.model.** { *; }

# --- Enum com `entries` ----------------------------------------------------
# `entries` é sintetizado pelo compilador e o R8 às vezes o descarta por não ter
# referência estática visível no bytecode.
-keepclassmembers enum com.afinu.app.** {
    public static **[] values();
    public static ** ENTRY$*;
}

# --- Stack traces legíveis -------------------------------------------------
# Sem isto, um crashReport no Play Console mostra "SourceFile:1" e a linha,
# que é inútil para depurar em campo.
-keepattributes SourceFile,LineNumberTable,RuntimeVisibleAnnotations
-renamesourcefileattribute SourceFile