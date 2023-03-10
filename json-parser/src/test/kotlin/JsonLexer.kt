object JsonLexer {

    fun parse(json: String): List<JsonToken> {
        return useForEachOverString(json)
    }

    private fun useForEachOverString(json: String): List<JsonToken> {
        val accumulatedCharacters: StringBuilder = StringBuilder()
        var mode = Mode.SingleCharacter
        val tokens = ArrayList<JsonToken>()
        json.forEach { character ->
            when (character) {
                '{' -> tokens.add(JsonToken.OpenBrace)
                '}' -> tokens.add(JsonToken.ClosedBrace)
                '"' -> when (mode) {
                    Mode.AccumulateStringCharacters -> {
                        tokens.add(JsonToken.JsonString(accumulatedCharacters.toString()))
                        accumulatedCharacters.clear()
                        mode = Mode.SingleCharacter
                    }
                    Mode.SingleCharacter -> mode = Mode.AccumulateStringCharacters
                    Mode.AccumulateNumberCharacters -> error("Cannot be in ${Mode.AccumulateNumberCharacters} with character '\"'")
                }
                ':' -> tokens.add(JsonToken.Colon)
                ' ' -> Unit // ignore whitespace
                else -> {
                    when (mode) {
                        Mode.AccumulateNumberCharacters -> accumulatedCharacters.append(character)
                        Mode.AccumulateStringCharacters -> accumulatedCharacters.append(character)
                        Mode.SingleCharacter -> Unit
                    }
                }
            }
        }

        return tokens
    }

    enum class Mode {
        SingleCharacter,
        AccumulateStringCharacters,
        AccumulateNumberCharacters
    }

    sealed interface JsonToken {
        object OpenBrace : JsonToken
        object ClosedBrace : JsonToken

        data class JsonString(val raw: String) : JsonToken
        data class JsonNumber(val raw: Number) : JsonToken
        object Colon : JsonToken
    }
}