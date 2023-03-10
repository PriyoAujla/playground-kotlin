import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JsonLexerTests {

    @Test
    fun `can parse empty object`() {
        val json = """
            |{
            |
            |}
        """.trimMargin()

        val tokens = JsonLexer.parse(json)
        tokens shouldBe listOf(
            JsonLexer.JsonToken.OpenBrace,
            JsonLexer.JsonToken.ClosedBrace
        )
    }

    @Test
    fun `can parse object with string property`() {
        val json = """
            |{
            |   "propertyKey": "propertyValue" 
            |}
        """.trimMargin()

        val tokens = JsonLexer.parse(json)
        tokens shouldContainExactly listOf(
            JsonLexer.JsonToken.OpenBrace,
            JsonLexer.JsonToken.JsonString(raw = "propertyKey"),
            JsonLexer.JsonToken.Colon,
            JsonLexer.JsonToken.JsonString(raw = "propertyValue"),
            JsonLexer.JsonToken.ClosedBrace
        )
    }

    @Test
    fun `can parse object with number property`() {
        val json = """
            |{
            |   "propertyKey": 1234 
            |}
        """.trimMargin()

        val tokens = JsonLexer.parse(json)
        tokens shouldContainExactly listOf(
            JsonLexer.JsonToken.OpenBrace,
            JsonLexer.JsonToken.JsonString(raw = "propertyKey"),
            JsonLexer.JsonToken.Colon,
            JsonLexer.JsonToken.JsonNumber(raw = 1234L),
            JsonLexer.JsonToken.ClosedBrace
        )
    }
}

/*

input := `{
		"glossary": {
			"title": "example glossary",
			"GlossDiv": {
				"title": "S",
				"GlossList": {
			                "GlossEntry": {
						"GlossTerm": "Standard Generalized Markup Language",
						"Abbrev": "ISO 8879:1986",
						"GlossDef": {
				                        "para": "A meta-markup language, used to create markup languages such as DocBook.",
							"GlossSeeAlso": ["GML", "XML"]
				                },
						"GlossSee": "markup"
				                }
				        },
				        "Nums": 5245243
				}
			}
		}`

		*/

