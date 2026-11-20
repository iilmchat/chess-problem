package com.iilmchat.chessproblem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iilmchat.chessproblem.core.chess.ChessRules
import com.iilmchat.chessproblem.core.chess.ChesslibRules
import com.iilmchat.chessproblem.core.chess.Side
import com.iilmchat.chessproblem.core.model.HintTier
import com.iilmchat.chessproblem.core.model.Puzzle
import com.iilmchat.chessproblem.core.model.PuzzleProgress
import com.iilmchat.chessproblem.core.model.UserSettings
import com.iilmchat.chessproblem.core.repository.HintRepository
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.PuzzleRepository
import com.iilmchat.chessproblem.core.repository.SettingsRepository
import com.iilmchat.chessproblem.core.session.Feedback
import com.iilmchat.chessproblem.core.session.PuzzleEvent
import com.iilmchat.chessproblem.core.session.PuzzleSession
import com.iilmchat.chessproblem.core.session.PuzzleState
import com.iilmchat.chessproblem.core.session.toOutcome
import com.iilmchat.chessproblem.ui.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class PuzzleUiState(
    val loading: Boolean = true,
    val puzzle: Puzzle? = null,
    val session: PuzzleState? = null,
    val settings: UserSettings = UserSettings(),
    val hintBalance: Int = 3,
    val showHints: Boolean = false,
    val showPause: Boolean = false,
    val showExit: Boolean = false,
    val resultVisible: Boolean = false,
    val reviewBoard: Boolean = false,
    val messageRes: Int? = null,
    val error: String? = null,
)

class PuzzleViewModel(
    private val puzzleId: String,
    private val puzzleRepository: PuzzleRepository,
    private val progressRepository: ProgressRepository,
    private val hintRepository: HintRepository,
    private val settingsRepository: SettingsRepository,
    private val rules: ChessRules = ChesslibRules(),
) : ViewModel() {

    private val _state = MutableStateFlow(PuzzleUiState())
    val state: StateFlow<PuzzleUiState> = _state.asStateFlow()

    private var session: PuzzleSession? = null
    private var tickerJob: Job? = null
    private var opponentJob: Job? = null
    private var feedbackJob: Job? = null
    private var hintJob: Job? = null
    private var scheduledFeedback: Feedback? = null
    private var scheduledHint: HintTier? = null
    private var savedOutcome = false

    init {
        viewModelScope.launch {
            val puzzle = puzzleRepository.byId(puzzleId)
            if (puzzle == null) {
                _state.update {
                    it.copy(loading = false, error = "puzzle_not_found")
                }
                return@launch
            }

            val settings = runCatching { settingsRepository.settings.first() }
                .getOrDefault(UserSettings())
            val puzzleSession = PuzzleSession(puzzle, rules).also { session = it }
            val initial = puzzleSession.initialState().let { state ->
                if (settings.autoFlip && state.board.sideToMove == Side.BLACK) {
                    state.copy(flipped = true)
                } else {
                    state
                }
            }

            _state.update {
                it.copy(
                    loading = false,
                    puzzle = puzzle,
                    session = initial,
                    settings = settings,
                )
            }
            observeSettings()
            observeHints()
            hintRepository.refillIfNeeded()
            startTicker()
        }
    }

    fun onTapSquare(square: com.iilmchat.chessproblem.core.chess.Square) {
        withSession { current -> session?.onEvent(current, PuzzleEvent.TapSquare(square)) }
    }

    fun onMakeMove(
        from: com.iilmchat.chessproblem.core.chess.Square,
        to: com.iilmchat.chessproblem.core.chess.Square,
        promotion: com.iilmchat.chessproblem.core.chess.PieceType?,
    ) {
        withSession { current ->
            session?.onEvent(current, PuzzleEvent.MakeMove(from, to, promotion))
        }
    }

    fun flipBoard() {
        withSession { current -> session?.onEvent(current, PuzzleEvent.FlipBoard) }
    }

    fun clearPromotion() {
        withSession { current -> session?.onEvent(current, PuzzleEvent.ClearPromotion) }
    }

    fun openHints() {
        val current = _state.value.session ?: return
        if (current.finished || current.status != PuzzleState.Status.SOLVING) return
        val paused = session?.onEvent(current, PuzzleEvent.Pause) ?: return
        _state.update { it.copy(session = paused, showHints = true) }
    }

    fun dismissHints() {
        val current = _state.value.session ?: return
        val resumed = if (current.finished) {
            current
        } else {
            session?.onEvent(current, PuzzleEvent.Resume) ?: current
        }
        _state.update { it.copy(session = resumed, showHints = false) }
    }

    fun useHint(tier: HintTier) {
        val current = _state.value.session ?: return
        if (current.finished || current.status != PuzzleState.Status.SOLVING) return
        if (tier == HintTier.IDEA && !current.ideaAvailable) return

        viewModelScope.launch {
            val cost = tier.tokenCost
            val allowed = cost == 0 || hintRepository.spend(cost)
            if (!allowed) {
                _state.update { it.copy(messageRes = R.string.puzzle_hint_not_enough) }
                return@launch
            }
            val latest = _state.value.session ?: return@launch
            val next = session?.onEvent(latest, PuzzleEvent.UseHint(tier)) ?: return@launch
            _state.update { it.copy(showHints = false) }
            applySession(next)
        }
    }

    fun pause() {
        val current = _state.value.session ?: return
        if (current.finished) return
        val paused = session?.onEvent(current, PuzzleEvent.Pause) ?: return
        _state.update { it.copy(session = paused, showPause = true) }
    }

    fun resume() {
        val current = _state.value.session ?: return
        val resumed = session?.onEvent(current, PuzzleEvent.Resume) ?: return
        _state.update { it.copy(session = resumed, showPause = false) }
    }

    fun requestExit() {
        val current = _state.value.session ?: return
        val paused = if (current.finished) {
            current
        } else {
            session?.onEvent(current, PuzzleEvent.Pause) ?: current
        }
        if (!current.finished) saveVisit(current)
        _state.update {
            it.copy(
                session = paused,
                showExit = true,
                showPause = false,
                showHints = false,
            )
        }
    }

    fun dismissExit() {
        val current = _state.value.session ?: return
        val resumed = if (current.finished) {
            current
        } else {
            session?.onEvent(current, PuzzleEvent.Resume) ?: current
        }
        _state.update { it.copy(session = resumed, showExit = false) }
    }

    fun onLifecycleStop() {
        val current = _state.value.session ?: return
        if (current.finished) return
        saveVisit(current)
        if (current.paused) return
        val paused = session?.onEvent(current, PuzzleEvent.Pause) ?: return
        _state.update { it.copy(session = paused, showPause = true) }
    }

    fun continueWithTime() {
        val current = _state.value.session ?: return
        val next = session?.onEvent(current, PuzzleEvent.ContinueWithTime) ?: return
        savedOutcome = false
        _state.update { it.copy(resultVisible = false, reviewBoard = false) }
        applySession(next)
        startTicker()
    }

    fun restartAfterFail() {
        val current = _state.value.session ?: return
        val next = session?.onEvent(current, PuzzleEvent.Restart) ?: return
        savedOutcome = false
        _state.update { it.copy(resultVisible = false, reviewBoard = false) }
        applySession(next)
        startTicker()
    }

    fun retry() {
        val puzzleSession = session ?: return
        val current = _state.value.session ?: return
        val fresh = puzzleSession.initialState().copy(flipped = current.flipped)
        savedOutcome = false
        _state.update { it.copy(resultVisible = false, reviewBoard = false) }
        applySession(fresh)
        startTicker()
    }

    fun reveal() {
        val current = _state.value.session ?: return
        val next = session?.onEvent(current, PuzzleEvent.Reveal) ?: return
        _state.update { it.copy(resultVisible = true) }
        applySession(next)
    }

    fun toggleReviewBoard() {
        _state.update { it.copy(reviewBoard = !it.reviewBoard) }
    }

    fun clearMessage() {
        _state.update { it.copy(messageRes = null) }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeHints() {
        viewModelScope.launch {
            hintRepository.balance.collect { balance ->
                _state.update { it.copy(hintBalance = balance) }
            }
        }
    }

    private fun withSession(transform: (PuzzleState) -> PuzzleState?) {
        val current = _state.value.session ?: return
        val next = transform(current) ?: return
        applySession(next)
    }

    private fun applySession(next: PuzzleState) {
        _state.update {
            it.copy(
                session = next,
                resultVisible = next.finished,
            )
        }

        if (next.status == PuzzleState.Status.OPPONENT) {
            scheduleOpponentReply()
        }
        if (next.finished) {
            tickerJob?.cancel()
            if (!savedOutcome) {
                savedOutcome = true
                saveOutcome(next)
            }
        } else if (tickerJob?.isActive != true) {
            startTicker()
        }

        next.feedback?.let(::scheduleFeedbackClear)
        if (next.activeHint == HintTier.PIECE || next.activeHint == HintTier.MOVE) {
            scheduleHintClear()
        } else if (next.activeHint == null) {
            hintJob?.cancel()
            scheduledHint = null
        }
    }

    private fun scheduleOpponentReply() {
        if (opponentJob?.isActive == true) return
        opponentJob = viewModelScope.launch {
            delay(480)
            val current = _state.value.session ?: return@launch
            if (current.status != PuzzleState.Status.OPPONENT) return@launch
            val next = session?.onEvent(current, PuzzleEvent.OpponentReply) ?: return@launch
            applySession(next)
        }
    }

    private fun scheduleFeedbackClear(feedback: Feedback) {
        if (scheduledFeedback == feedback && feedbackJob?.isActive == true) return
        scheduledFeedback = feedback
        feedbackJob?.cancel()
        val wait = if (feedback is Feedback.Correct) 900L else 1_500L
        feedbackJob = viewModelScope.launch {
            delay(wait)
            val current = _state.value.session ?: return@launch
            if (current.feedback == feedback) {
                _state.update {
                    it.copy(session = session?.onEvent(current, PuzzleEvent.ClearFeedback) ?: current)
                }
            }
            scheduledFeedback = null
        }
    }

    private fun scheduleHintClear() {
        val tier = _state.value.session?.activeHint ?: return
        if (scheduledHint == tier && hintJob?.isActive == true) return
        scheduledHint = tier
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(2_800L)
            val current = _state.value.session ?: return@launch
            _state.update {
                it.copy(session = session?.onEvent(current, PuzzleEvent.ClearHint) ?: current)
            }
            scheduledHint = null
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(250L)
                val current = _state.value.session ?: continue
                if (current.paused || current.finished) continue
                val next = session?.onEvent(current, PuzzleEvent.Tick(current.elapsedMs + 250L))
                    ?: continue
                applySession(next)
                if (next.finished) break
            }
        }
    }

    private fun saveOutcome(state: PuzzleState) {
        val outcome = state.toOutcome()
        viewModelScope.launch {
            val existing = progressRepository.forPuzzle(outcome.puzzleId)
            val solved = existing?.solved == true || outcome.solved
            val reviewed = existing?.reviewed == true || outcome.reviewed
            val stars = max(existing?.stars ?: 0, outcome.stars)
            val bestTime = if (outcome.solved) {
                min(existing?.bestTimeMs ?: Long.MAX_VALUE, outcome.elapsedMs)
            } else {
                existing?.bestTimeMs
            }.takeIf { it != Long.MAX_VALUE }

            progressRepository.save(
                PuzzleProgress(
                    puzzleId = outcome.puzzleId,
                    chapterId = state.puzzle.chapter,
                    solved = solved,
                    reviewed = reviewed,
                    stars = stars,
                    bestTimeMs = bestTime,
                    attempts = outcome.mistakes,
                    hintsUsed = outcome.hintsUsed,
                    lastPlayedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun saveVisit(state: PuzzleState) {
        viewModelScope.launch {
            val puzzleId = state.puzzle.id
            val existing = progressRepository.forPuzzle(puzzleId)
            progressRepository.save(
                PuzzleProgress(
                    puzzleId = puzzleId,
                    chapterId = state.puzzle.chapter,
                    solved = existing?.solved == true,
                    reviewed = existing?.reviewed == true,
                    stars = existing?.stars ?: 0,
                    bestTimeMs = existing?.bestTimeMs,
                    attempts = state.mistakes,
                    hintsUsed = state.hintsUsed.size,
                    lastPlayedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        opponentJob?.cancel()
        feedbackJob?.cancel()
        hintJob?.cancel()
        super.onCleared()
    }
}
