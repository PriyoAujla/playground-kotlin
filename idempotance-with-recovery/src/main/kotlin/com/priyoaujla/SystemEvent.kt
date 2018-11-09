package com.priyoaujla

import com.fasterxml.jackson.databind.JsonNode

data class SystemEvent(val name: Name, val payload: JsonNode) {
    enum class Name(name: String) {
        SendingEmail("sending-email"),
        ErrorSendingEmail("error-sending-email")
    }
}


