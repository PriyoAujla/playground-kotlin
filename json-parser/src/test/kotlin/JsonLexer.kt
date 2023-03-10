object JsonLexer {

    fun parse(json: String): List<JsonToken> {
        return useForEachOverString(json)
    }

    private fun useForEachOverString(json: String): List<JsonToken> {
        val accumulatedCharacters: StringBuilder = StringBuilder()
        var mode = Mode.Normal
        val tokens = ArrayList<JsonToken>()
        json.forEach { character ->
            when(mode) {
                Mode.Normal -> {
                    val (newMode, token) = processInNormalMode(character)
                    if (token != null) {
                        tokens.add(token)
                    }
                    if(newMode == Mode.AccumulateNumberCharacters) {
                        accumulatedCharacters.append(character)
                    }
                    mode = newMode
                }
                Mode.AccumulateStringCharacters -> {
                    val (newMode, token) = processInAccumulateStringMode(character, accumulatedCharacters)
                    if (token != null) {
                        tokens.add(token)
                    }
                    if(newMode == Mode.Normal) {
                        accumulatedCharacters.clear()
                    }
                    mode = newMode
                }
                Mode.AccumulateNumberCharacters -> {
                    val (newMode, token) = processInAccumulateNumberMode(character, accumulatedCharacters)
                    if (token != null) {
                        tokens.add(token)
                    }
                    if(newMode == Mode.Normal) {
                        accumulatedCharacters.clear()
                    }
                    mode = newMode
                }
            }
        }

        return tokens
    }

    fun processInNormalMode(char: Char): Pair<Mode, JsonToken?> = when (char) {
        '{' -> Pair(Mode.Normal, JsonToken.OpenBrace)
        '}' -> Pair(Mode.Normal, JsonToken.ClosedBrace)
        ':' -> Pair(Mode.Normal, JsonToken.Colon)
        '"' -> Pair(Mode.AccumulateStringCharacters, null)
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9' -> Pair(Mode.AccumulateNumberCharacters, null)

        else -> Pair(Mode.Normal, null)
    }

    fun processInAccumulateStringMode(char: Char, accumulatedCharacters: StringBuilder): Pair<Mode, JsonToken?> =
        when (char) {
            '"' -> Pair(Mode.Normal, JsonToken.JsonString(accumulatedCharacters.toString()))
            else -> {
                accumulatedCharacters.append(char)
                Pair(Mode.AccumulateStringCharacters, null)
            }
        }

    fun processInAccumulateNumberMode(char: Char, accumulatedCharacters: StringBuilder): Pair<Mode, JsonToken?> =
        when (char) {
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9' -> {
                accumulatedCharacters.append(char)
                Pair(Mode.AccumulateNumberCharacters, null)
            }
            else -> {
                val asString = accumulatedCharacters.toString()
                val asNumber = asString.toLongOrNull() ?: asString.toDoubleOrNull() ?: error("Not a number $asString")
                Pair(Mode.Normal, JsonToken.JsonNumber(asNumber))
            }
        }

    fun addIfInNormalMode(mode: Mode, tokens: ArrayList<JsonToken>, token: JsonToken) {
        if (mode == Mode.Normal) {
            tokens.add(token)
        }
    }

    enum class Mode {
        Normal,
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