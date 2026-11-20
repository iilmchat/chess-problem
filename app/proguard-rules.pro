# chesslib uses reflection for some board/piece conversions.
-keep class com.github.bhlangonijr.chesslib.** { *; }

# kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room приходит со своими consumer-правилами в AAR, отдельные keep-строки не нужны.
# Снимок активной сессии сериализуется в SavedStateHandle через java.io.Serializable:
# поля должны сохранять имена между версиями приложения.
-keepclassmembers class com.iilmchat.chessproblem.core.session.PuzzleSessionSnapshot,
                        com.iilmchat.chessproblem.core.session.PlayedMove {
    *;
}
