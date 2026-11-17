package ai.vorion.hologram

/** Visual states exposed to the assistant orchestration layer. */
enum class HologramState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING,
    SUCCESS,
    WARNING,
    ERROR
}
