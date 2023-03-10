import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import java.time.Duration

class JsonTests {
    @Test
    fun compare() {
            val fileReadOnlyResult = benchmark(Benchmarked.Type.FileReadOnly) {
                jsonAsString
            }

            val objectMapper = ObjectMapper()
            val jacksonMapperResult = benchmark(Benchmarked.Type.FileReadAndParse) {
                objectMapper.readTree(jsonAsString)
            }

            val jsonLexerResult = benchmark(Benchmarked.Type.FileReadAndParse) {
                JsonLexer.parse(jsonAsString)
            }

            println("""
            |************************************
            | reading json file took ${fileReadOnlyResult.time.toMillis()}ms
            | reading and parsing with Jackson took ${jacksonMapperResult.time.toMillis()}ms
            | reading and lexing with JsonLexer took ${jsonLexerResult.time.toMillis()}ms
            | 
            | jackson mapper is ${safeDivision(jacksonMapperResult.time.toMillis(), fileReadOnlyResult.time.toMillis())} times slower
            |************************************ 
        """.trimMargin())

    }

    fun safeDivision(divide: Long, by: Long) = if(by == 0L) {
        divide
    } else {
        divide / by
    }

    private fun <T> benchmark(type: Benchmarked.Type, block: () -> T): Benchmarked<T> {
        val  start = System.currentTimeMillis()
        val result = block()
        val timeTaken = Duration.ofMillis(System.currentTimeMillis() - start)
        return Benchmarked(type,timeTaken, result)
    }

    data class Benchmarked<T>(val type: Type, val time: Duration, val data: T) {
        enum class Type {
            FileReadOnly, FileReadAndParse
        }
    }
    private val jsonAsString get() = this::class.java.classLoader.getResource("sample.json")!!.readText()
}