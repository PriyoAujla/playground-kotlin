package com.priyoaujla

import com.google.gson.JsonElement

data class BusinessEvent(val name: Name, val payload: JsonElement) {
    enum class Name(val value: String) {
        Subscribed("subscribed"),
        CampaignCreated("campaign-created"),
        CampaignEmailSent("campaign-email-sent")
    }
}