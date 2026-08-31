package com.altusix.slate.widgets.games

data class TicTacToeState(val board: IntArray = IntArray(9) { 0 }, val currentTurn: Int = 1, val winner: Int = 0, val isVsRobot: Boolean = true) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TicTacToeState
        return board.contentEquals(other.board) && currentTurn == other.currentTurn && winner == other.winner && isVsRobot == other.isVsRobot
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + currentTurn
        result = 31 * result + winner
        result = 31 * result + isVsRobot.hashCode()
        return result
    }
}