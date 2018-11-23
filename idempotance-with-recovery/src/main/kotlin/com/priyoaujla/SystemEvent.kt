package com.priyoaujla

import com.google.gson.JsonObject

data class SystemEvent(val name: Name, val payload: JsonObject) {
    enum class Name(name: String) {
        SendingEmail("sending-email"),
        ErrorSendingEmail("error-sending-email")
    }
}


