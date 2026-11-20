# Атрибуты нужны kotlinx.serialization (имена полей в дескрипторах) и Room
# (аннотации @Entity/@Dao/@Query считываются сгенерированным кодом).
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**

# Сгенерированные kotlinx.serialization-сериализаторы: сохраняем и сам класс,
# и его члены. includedescriptorclasses удерживает типы из сигнатур сериалайзера.
-keep, includedescriptorclasses class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Модели JSON-паков разбираются через Json.decodeFromString. Явно держим классы
# и их companion-объекты вместе с фабриками serializer().
-keep class com.iilmchat.chessproblem.data.content.PuzzlePackJson,
            com.iilmchat.chessproblem.data.content.PuzzleJson,
            com.iilmchat.chessproblem.data.content.TaskJson,
            com.iilmchat.chessproblem.data.content.LocalizedTextJson,
            com.iilmchat.chessproblem.data.content.HintJson,
            com.iilmchat.chessproblem.data.content.SourceJson,
            com.iilmchat.chessproblem.data.content.PuzzlePackHeader {
    *;
}

# chesslib использует рефлексию при преобразовании фигур/полей и SAN.
-keep class com.github.bhlangonijr.chesslib.** { *; }

# Room использует consumer-правила из AAR, но entity/DAO денормализованного
# прогресса держим явно: они участвуют в запросах и миграции contentVersion.
-keep class com.iilmchat.chessproblem.data.local.PuzzleProgressEntity,
            com.iilmchat.chessproblem.data.local.ChapterProgressEntity { *; }
-keep interface com.iilmchat.chessproblem.data.local.PuzzleProgressDao,
                com.iilmchat.chessproblem.data.local.ChapterProgressDao { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Снимок активной сессии пишется в SavedStateHandle как java.io.Serializable.
# Имена полей и констант перечислений должны быть стабильны между версиями.
-keep class com.iilmchat.chessproblem.core.session.PuzzleSessionSnapshot,
            com.iilmchat.chessproblem.core.session.PlayedMove { *; }
-keep enum com.iilmchat.chessproblem.core.chess.Side,
           com.iilmchat.chessproblem.core.model.HintTier,
           com.iilmchat.chessproblem.core.session.PuzzleState$Status { *; }
