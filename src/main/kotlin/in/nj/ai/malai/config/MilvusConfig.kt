package `in`.nj.ai.malai.config

import `in`.nj.ai.malai.TestService
import io.milvus.client.MilvusServiceClient
import io.milvus.param.ConnectParam
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.ollama.OllamaEmbeddingModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.ClientHttpRequestFactories
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Duration


@Configuration
class MilvusConfig {

    @Autowired
    lateinit var testService: TestService

    @Bean(name = ["milvusVectorStore"])
    fun vectorStore(milvusClient: MilvusServiceClient?,
                    @Autowired @Qualifier("ollamaEmbeddingModel_local") ollamaEmbeddingModel: EmbeddingModel): VectorStore? {
        testService.x = 123
       /*val config = MilvusVectorStoreConfig.builder()
                .withCollectionName("test_vector_store")
                .withDatabaseName("default")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withEmbeddingDimension(1536)
                .build()
        val milvusVectorStore = MilvusVectorStore(milvusClient, ollamaEmbeddingModel, config, true, null)
*/
        val milvusVectorStore = SimpleVectorStore(ollamaEmbeddingModel)
        if (load)
            init(milvusVectorStore)

        val results: List<Document> = milvusVectorStore.similaritySearch(SearchRequest.query("Thailand").withTopK(5))
        println(results)

        return milvusVectorStore
    }

    @Bean(name = ["ollamaEmbeddingModel_local"])
    @Primary
    fun ollamaEmbeddingModel_local(): EmbeddingModel {
        val ollamaApi = OllamaApi()

        return OllamaEmbeddingModel(ollamaApi,
                OllamaOptions.builder()
                        .withNumCtx(120000)
                        .withModel("mxbai-embed-large")
                        .build()
        )
    }

    @Bean
    fun milvusClient(): MilvusServiceClient? {
        return MilvusServiceClient(ConnectParam.newBuilder()
                .withAuthorization("minioadmin", "minioadmin")
                .withUri("http://localhost:19530")
                .build())
    }

    @Bean
    fun restClientCustomizer(): RestClientCustomizer? {
        return RestClientCustomizer { restClientBuilder: RestClient.Builder ->
            restClientBuilder
                    .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                            .withConnectTimeout(Duration.ofSeconds(10))
                            .withReadTimeout(Duration.ofSeconds(20))))
                    .requestInterceptor(LoggingInterceptor())

        }
    }

    val load = true

    fun init(vectorStore: VectorStore) {
        println("MaLai Application started")
        if (load) {
            val startTime = System.currentTimeMillis()
            try {
                PDDocument.load(File("/Users/niteshjain/Desktop/Thailand.pdf")).use { document ->
                    val pdfStripper = PDFTextStripper()
                    var text = pdfStripper.getText(document)
                    val textSplitter = TokenTextSplitter(400,400,1000,1000,true)

                    vectorStore.accept(textSplitter.apply(mutableListOf(Document(text))))
                }
            } catch (ex: Exception) {

            }
            val endTime = System.currentTimeMillis()
            println("Time taken to load vector store: ${endTime - startTime} ms")
            println("Vector store loaded")
        }

    }


    class LoggingInterceptor : ClientHttpRequestInterceptor {
        @Throws(IOException::class)
        override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
            // Log the request details (URL and body)
            logRequestDetails(request, body)

            // Continue the request execution
            return execution.execute(request, body)
        }

        @Throws(IOException::class)
        private fun logRequestDetails(request: HttpRequest, body: ByteArray) {
            // Get the URL of the request
            val url: String = request.getURI().toString()

            // Get the request body
            val requestBody = String(body, StandardCharsets.UTF_8)

            // Log the URL and body in JSON format
            val jsonOutput = String.format("{\"url\": \"%s\", \"body\": \"%s\"}", url, requestBody)
            println(jsonOutput)
        }
    }


}