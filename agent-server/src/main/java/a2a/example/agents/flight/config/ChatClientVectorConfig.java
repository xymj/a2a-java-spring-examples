package a2a.example.agents.flight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ChatClientVectorConfig {
    private static final Logger logger = LoggerFactory.getLogger(ChatClientVectorConfig.class);
    @Bean
    CommandLineRunner ingestTermOfServiceToVectorStore(
        VectorStore vectorStore,
        @Value("classpath:rag/terms-of-service.txt") org.springframework.core.io.Resource termsOfServiceDocs
    ) {

        return args -> {
            // Ingest the document into the vector store
            /*
             * 1、文档读取TextReader 读取 resources/rag/terms-of-service.txt 文件内容
             * 2、TokenTextSplitter 按token长度切分文本（避免大文本超出模型限制）
             * 3、向量化存储 通过 VectorStore.write() 将文本向量存入内存（后续可用于RAG检索）
             */
            vectorStore.write(new TokenTextSplitter().transform(new TextReader(termsOfServiceDocs).read()));

            // 相似性搜索检测
            vectorStore.similaritySearch("Cancelling Bookings").forEach(doc -> {
                logger.info("Similar Document: {}", doc.getText());
            });
        };
    }

    /**
     * 提供基于内存的向量存储（SimpleVectorStore）
     * <p>
     * 依赖 EmbeddingModel（自动注入，Alibaba的嵌入模型）
     * @param embeddingModel
     * @return
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }


}
