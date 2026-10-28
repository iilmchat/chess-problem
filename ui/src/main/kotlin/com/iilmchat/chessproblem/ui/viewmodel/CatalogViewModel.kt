package com.iilmchat.chessproblem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChapterUi(
    val id: String,
    val puzzles: List<Puzzle>,
    val solved: Int,
    val stars: Int,
)

data class CatalogUiState(
    val loading: Boolean = true,
    val puzzles: List<Puzzle> = emptyList(),
    val progress: Map<String, PuzzleProgress> = emptyMap(),
    val error: String? = null,
) {
    val total: Int
        get() = puzzles.size

    val solvedCount: Int
        get() = puzzles.count { progress[it.id]?.solved == true }

    val totalStars: Int
        get() = puzzles.sumOf { progress[it.id]?.stars ?: 0 }

    fun progressFor(puzzleId: String): PuzzleProgress? = progress[puzzleId]

    fun puzzlesFor(level: Level): List<Puzzle> = puzzles.filter { it.level == level }

    fun solvedInLevel(level: Level): Int =
        puzzlesFor(level).count { progress[it.id]?.solved == true }

    fun starsInLevel(level: Level): Int =
        puzzlesFor(level).sumOf { progress[it.id]?.stars ?: 0 }

    fun chaptersFor(level: Level): List<ChapterUi> =
        puzzlesFor(level)
            .groupBy { it.chapter }
            .map { (chapter, puzzlesInChapter) ->
                ChapterUi(
                    id = chapter,
                    puzzles = puzzlesInChapter,
                    solved = puzzlesInChapter.count { progress[it.id]?.solved == true },
                    stars = puzzlesInChapter.sumOf { progress[it.id]?.stars ?: 0 },
                )
            }

    fun chapter(level: Level, chapterId: String): ChapterUi? =
        chaptersFor(level).firstOrNull { it.id == chapterId }

    fun continuePuzzle(): Puzzle? {
        if (puzzles.isEmpty()) return null
        val byId = puzzles.associateBy { it.id }
        val recent = progress.values
            .asSequence()
            .filter { it.lastPlayedAt > 0 }
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { byId[it.puzzleId] }
            .firstOrNull { progress[it.id]?.completed != true }
        if (recent != null) return recent
        return puzzles.firstOrNull { progress[it.id]?.completed != true } ?: puzzles.first()
    }

    fun dailyPuzzle(): Puzzle? {
        if (puzzles.isEmpty()) return null
        val open = puzzles.filter { progress[it.id]?.completed != true }
        val pool = open.ifEmpty { puzzles }
        val offset = (LocalDate.now().dayOfYear - 1).coerceAtLeast(0) % pool.size
        return pool[offset]
    }

    fun nextPuzzleAfter(puzzle: Puzzle): Puzzle? {
        if (puzzles.isEmpty()) return null
        val index = puzzles.indexOfFirst { it.id == puzzle.id }
        if (index < 0) return puzzles.firstOrNull()
        return puzzles.getOrNull(index + 1) ?: puzzles.firstOrNull()
    }

    fun recentProgress(limit: Int = 5): List<Pair<Puzzle, PuzzleProgress>> =
        progress.values
            .asSequence()
            .filter { it.completed }
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { item -> puzzles.firstOrNull { it.id == item.puzzleId }?.let { it to item } }
            .take(limit)
            .toList()

    fun streakDays(): Int {
        val zone = java.time.ZoneId.systemDefault()
        val days = progress.values
            .asSequence()
            .filter { it.completed && it.lastPlayedAt > 0L }
            .map { item ->
                java.time.Instant.ofEpochMilli(item.lastPlayedAt)
                    .atZone(zone)
                    .toLocalDate()
            }
            .toSet()
        if (days.isEmpty()) return 0

        var cursor = LocalDate.now()
        if (cursor !in days) cursor = cursor.minusDays(1)
        var count = 0
        while (cursor in days) {
            count += 1
            cursor = cursor.minusDays(1)
        }
        return count
    }
}

class CatalogViewModel(
    private val puzzleRepository: PuzzleRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            progressRepository.observeAll().collect { items ->
                _state.update { it.copy(progress = items.associateBy(PuzzleProgress::puzzleId)) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { puzzleRepository.all() }
                .onSuccess { puzzles ->
                    _state.update {
                        it.copy(
                            loading = false,
                            puzzles = puzzles,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "content_error",
                        )
                    }
                }
        }
    }
}
