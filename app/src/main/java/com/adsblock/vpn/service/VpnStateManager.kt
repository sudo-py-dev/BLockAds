package com.adsblock.vpn.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VpnState {
    DISCONNECTED,
    CONNECTED,
    PAUSED,
}

object VpnStateManager {
    private val _state = MutableStateFlow(VpnState.DISCONNECTED)
    val state: StateFlow<VpnState> = _state

    fun updateState(newState: VpnState) {
        _state.value = newState
    }
}
