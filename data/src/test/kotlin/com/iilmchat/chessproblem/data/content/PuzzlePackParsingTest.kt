package com.iilmchat.chessproblem.data.content

import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.TaskType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Читает паки с диска, чтобы тестировать загрузчик без Android AssetManager. */
private class FilePuzzlePackReader(
    private val directory: File,
) : PuzzlePackReader {
    override fun names(): List<String> =
        directory.listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            ?.map { it.name }
            ?.sorted()
            .orEmpty()

    override fun read(name: String): String = File(directory, name).readText()
}

class PuzzlePackParsingTest {
    private val packsDirectory = File("../content/puzzles").canonicalFile
    private val reader = FilePuzzlePackReader(packsDirectory)
    private val source = AssetPuzzleSource(reader)
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `загружаются все 360 задач трёх уровней`() = runTest {
        val puzzles = source.load()

        assertEquals(360, puzzles.size)
        assertEquals(
            setOf(Level.NOVICE, Level.PROFESSIONAL, Level.GRANDMASTER),
            puzzles.map { it.level }.toSet(),
        )
        Level.entries.forEach { level ->
            assertEquals(120, puzzles.count { it.level == level }, "уровень $level")
        }
        assertTrue(puzzles.all { it.id.matches(Regex("(nov|pro|grm)-[0-9]{3}")) })
        assertEquals(360, puzzles.map { it.id }.toSet().size)
    }

    @Test
    fun `загружаются 36 глав по 12 на уровень`() = runTest {
        val puzzles = source.load()

        // Два ключа глав (mate-in-2 и mixed-review) повторяются на разных уровнях,
        // поэтому 36 глав — это 36 пар (уровень, глава), а не 36 уникальных строк.
        assertEquals(36, puzzles.map { it.level to it.chapter }.toSet().size)
        Level.entries.forEach { level ->
            assertEquals(
                12,
                puzzles.filter { it.level == level }.map { it.chapter }.toSet().size,
                "уровень $level",
            )
        }
        assertTrue(puzzles.all { it.chapter.isNotBlank() })
    }

    @Test
    fun `все типы задач MATE WIN DRAW разбираются из паков`() = runTest {
        val puzzles = source.load()

        assertEquals(setOf(TaskType.MATE, TaskType.WIN, TaskType.DRAW), puzzles.map { it.task.type }.toSet())
        TaskType.entries.forEach { type ->
            assertTrue(puzzles.count { it.task.type == type } > 0, "тип $type")
        }
        assertTrue(puzzles.all { it.task.plies > 0 })
        assertTrue(puzzles.all { it.solution.isNotEmpty() })
    }

    @Test
    fun `локализация RU и EN заполнена у каждой задачи`() = runTest {
        val puzzles = source.load()

        assertTrue(puzzles.all { it.title.ru.isNotBlank() && it.title.en.isNotBlank() })
        assertTrue(puzzles.all { it.prompt.ru.isNotBlank() && it.prompt.en.isNotBlank() })
        assertTrue(puzzles.all { it.explanation.ru.isNotBlank() && it.explanation.en.isNotBlank() })
        assertTrue(puzzles.all { it.hints.size == 3 })
        assertTrue(
            puzzles.all { puzzle ->
                puzzle.hints.all { hint -> hint.text.ru.isNotBlank() && hint.text.en.isNotBlank() }
            },
        )
    }

    @Test
    fun `PuzzlePackJson читает версию контента из каждого пака`() {
        assertTrue(packsDirectory.isDirectory, "каталог ${packsDirectory.path} не найден")
        val packs = reader.names().map { json.decodeFromString<PuzzlePackJson>(reader.read(it)) }

        assertEquals(3, packs.size)
        assertEquals(setOf(1), packs.map { it.schemaVersion }.toSet())
        assertEquals(setOf("2026.10.4"), packs.map { it.contentVersion }.toSet())
        assertEquals("2026.10.4", AssetContentVersionProvider(reader).current())
    }
}
