package `in`.nj.ai.malai.controller

import `in`.nj.ai.malai.TestService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.ollama.api.OllamaModel
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import reactor.core.publisher.Flux
import java.io.OutputStream
import java.util.concurrent.Flow.Publisher
import java.util.stream.Collectors
import java.util.stream.Stream


@RestController
@RequestMapping("/malai/")
class MaLAIController(chatClientBuilder: ChatClient.Builder) {
    @Autowired
    @Qualifier("milvusVectorStore")
    lateinit var vectorStore: VectorStore

    private final lateinit var chatClient: ChatClient

    @Autowired
    lateinit var testService: TestService

    init {
        this.chatClient = chatClientBuilder
                .defaultOptions(OllamaOptions.builder().withModel(OllamaModel.LLAMA3_1).build()).build()
    }

    @GetMapping("message")
    private fun handleMessage(@RequestParam("text") message: String): Flux<String>? {
        val documents = vectorStore.similaritySearch(message)
        val information = documents.stream().map { it.content }.collect(Collectors.joining(System.lineSeparator()))
        var systemPromptTemplate = PromptTemplate("""
            You are a travel planner assistant MaLAI. I can provide you with information about different destinations.
            If you do not know, simply answer I do not know.
            
            {information}
        """.trimIndent())

        println("x is = ${this.testService.x}")

        var systemMessage = systemPromptTemplate.createMessage(mapOf(Pair("information", information)))
        val promptTemplate = PromptTemplate(message)
        val userMessage = promptTemplate.createMessage(mapOf(Pair("query", message)))
        val prompt = Prompt(listOf(systemMessage, userMessage), OllamaOptions.builder().withModel(OllamaModel.LLAMA3_1).build())
        val responseSpec: ChatClient.ChatClientPromptRequestSpec? = chatClient.prompt(prompt)
        return responseSpec?.stream()?.chatResponse()?.map { response -> response.result.output.content }
    }

    @GetMapping("/ai")
    fun generation(text: String?): Flux<String> {
        return chatClient.prompt(Prompt(text)).stream().chatResponse().map { response -> response.result.output.content }
    }


}