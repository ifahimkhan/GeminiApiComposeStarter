package com.fahim.geminiApiComposeStarter.ui.mascot

/**
 * What the assistant character is doing. Owned by [com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel]
 * and carried in `ChatUiState`, so business logic drives the mascot and the composable stays a
 * pure function of state.
 *
 * Only one piece of mascot artwork was supplied, so these states are expressed through motion --
 * timing, tilt, bob, scale and glow -- rather than through different drawings. See
 * `MascotMotion` for the per-state motion spec.
 */
enum class MascotState {
    /** Nothing happening: the slow breathing loop from the supplied idle animation. */
    IDLE,

    /** A prompt is in flight. Leaning forward, faster, brighter glow. */
    THINKING,

    /** A response just arrived. */
    HAPPY,

    /** A long response arrived, or the conversation just started. */
    EXCITED,

    /** The request failed. */
    CONFUSED,

    /** Something unexpected -- reserved for future use by callers. */
    SURPRISED,

    /** No interaction for a while. Sinks, dims and slows down. */
    SLEEPY,
}
