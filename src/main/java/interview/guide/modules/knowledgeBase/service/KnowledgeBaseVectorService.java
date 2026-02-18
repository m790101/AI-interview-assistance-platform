package interview.guide.modules.knowledgeBase.service;

import interview.guide.modules.knowledgeBase.repository.VectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseVectorService {
    /**
     *
     */
    private static final int MAX_BATCH_SIZE = 10;
    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;
    private final VectorRepository vectorRepository;

    public KnowledgeBaseVectorService(VectorStore vectorStore, VectorRepository vectorRepository){
        this.vectorStore = vectorStore;
        this.vectorRepository = vectorRepository;
        this.textSplitter = new TokenTextSplitter();
    }


    @Transactional
    public void vectorizeAndStore(Long knowledgeBaseId, String content){
        log.info("vectorize init kbId = {}, contentLength = {}", knowledgeBaseId,content.length());
        try{
            // 1. delete all existing data
            deleteByKnowledgeBaseId(knowledgeBaseId);

            // 2. chuck text
            List<Document> chunks = textSplitter.apply(
                    List.of(new Document(content))
            );

            log.info("chuck finished, chuck size: {}", chunks.size());

            // 3. add metadata to each chuck
            chunks.forEach(chunk -> chunk.getMetadata().put("kb_id", knowledgeBaseId.toString()) );

            // 4. batch vectorize and save data
            int totalChunks = chunks.size();
            int batchCount = (totalChunks + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;
            log.info("start batch vectorize: total {} chunks，total {} batches，max number for each one is {}",
                    totalChunks, batchCount, MAX_BATCH_SIZE);
            for(int i = 0; i < batchCount; i++){
                int start = i * MAX_BATCH_SIZE;
                int end = Math.min(start + MAX_BATCH_SIZE, totalChunks);
                List<Document> batch = chunks.subList(start, end);
                log.debug("handle number {}/{} batch: chunks {}-{}", i + 1, batchCount, start + 1, end);
                vectorStore.add(batch);
            }
            log.info("vectorize finished: kbId={}, chunks={}, batches={}",
                    knowledgeBaseId, totalChunks, batchCount);
        } catch (Exception e) {
            log.error("vectorize failed: kbId={}, error={}", knowledgeBaseId, e.getMessage(), e);
            throw new RuntimeException("vectorize failed: " + e.getMessage(), e);
        }
    }

    /**
     * 基于多个知识库进行相似度搜索
     *
     * @param query 查询文本
     * @param knowledgeBaseIds 知识库ID列表（如果为空则搜索所有）
     * @param topK 返回top K个结果
     * @return 相关文档列表
     */
    public List<Document> similaritySearch(String query, List<Long> knowledgeBaseIds, int topK) {
        log.info("similaritySearch: query={}, kbIds={}, topK={}", query, knowledgeBaseIds, topK);

        try {
            // 使用VectorStore的similaritySearch方法（只接受查询字符串）
            List<Document> allResults = vectorStore.similaritySearch(query);

            // 如果指定了知识库ID，进行过滤
            if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
                allResults = allResults.stream()
                        .filter(doc -> {
                            Object kbId = doc.getMetadata().get("kb_id");
                            if (kbId == null) return false;
                            // 支持 String 和 Long 两种格式（向后兼容）
                            try {
                                Long kbIdLong = kbId instanceof Long
                                        ? (Long) kbId
                                        : Long.parseLong(kbId.toString());
                                return knowledgeBaseIds.contains(kbIdLong);
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                        .toList();
                log.debug("使用metadata过滤，找到 {} 个相关文档", allResults.size());
            }

            // 限制返回数量
            List<Document> results = allResults.stream()
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("搜索完成: 找到 {} 个相关文档", results.size());
            return results;

        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage(), e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId){
        try {
            vectorRepository.deleteByKnowledgeBaseId(knowledgeBaseId);
        } catch (Exception e) {
            log.error("删除向量数据失败: kbId={}, error={}", knowledgeBaseId, e.getMessage(), e);
            // 不抛出异常，允许继续执行其他删除操作
            // 如果确实需要严格保证，可以取消下面的注释
            // throw new RuntimeException("删除向量数据失败: " + e.getMessage(), e);
        }

    }

}
