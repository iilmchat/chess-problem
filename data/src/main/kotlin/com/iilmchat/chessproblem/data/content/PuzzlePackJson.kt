package com.iilmchat.chessproblem.data.content

import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.core.model.LocalizedText
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleHint
import com.iilmchat.chessproblem.core.model.PuzzleSource
import com.iilmchat.chessproblem.core.model.PuzzleTask
import com.iilmchat.chessproblem.core.model.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class PuzzlePackJson(
    val schemaVersion: Int,
    val contentVersion: String,
    val level: String,
    val puzzles: List<PuzzleJson>,
)

@Serializable
data class PuzzleJson(
    val id: String,
    val chapter: String,
    val fen: String,
    val setupMove: String? = null,
    val solution: List<String>,
    val task: TaskJson,
    val rating: Int,
    val themes: List<String> = emptyList(),
    val timeLimitSec: Int,
    val title: LocalizedTextJson,
    val prompt: LocalizedTextJson,
    val hints: List<HintJson> = emptyList(),
    val explanation: LocalizedTextJson,
    val source: SourceJson? = null,
)

@Serializable
data class TaskJson(
    val type: String,
    val plies: Int,
)

@Serializable
data class LocalizedTextJson(
    val ru: String,
    val en: String,
)

@Serializable
data class HintJson(
    val tier: Int,
    val ru: String,
    val en: String,
)

@Serializable
data class SourceJson(
    val provider: String,
    val id: String? = null,
    val license: String,
)

fun PuzzlePackJson.toDomain(): List<Puzzle> {
    val domainLevel = Level.fromId(level)
    return puzzles.map { it.toDomain(domainLevel) }
}

private fun PuzzleJson.toDomain(level: Level): Puzzle = Puzzle(
    id = id,
    level = level,
    chapter = chapter,
    fen = fen,
    setupMove = setupMove,
    solution = solution,
    task = PuzzleTask(
        type = TaskType.fromId(task.type),
        plies = task.plies,
    ),
    rating = rating,
    themes = themes,
    timeLimitSec = timeLimitSec,
    title = title.toDomain(),
    prompt = prompt.toDomain(),
    hints = hints.map { it.toDomain() },
    explanation = explanation.toDomain(),
    source = source?.let {
        PuzzleSource(provider = it.provider, id = it.id, license = it.license)
    },
)

private fun LocalizedTextJson.toDomain() = LocalizedText(ru = ru, en = en)

private fun HintJson.toDomain() = PuzzleHint(
    tier = when (tier) {
        1 -> HintTier.IDEA
        2 -> HintTier.PIECE
        3 -> HintTier.MOVE
        else -> error("Unknown hint tier: $tier")
    },
    text = LocalizedText(ru = ru, en = en),
)
