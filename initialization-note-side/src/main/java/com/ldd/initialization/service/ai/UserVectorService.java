package com.ldd.initialization.service.ai;

import com.ldd.initialization.config.ai.PgVectorConnectionPool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserVectorService implements EmbeddingStore<TextSegment> {

    @Autowired
    private PgVectorConnectionPool connectionPool;
    
    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 添加文档到指定用户的个人知识库（带文件ID）
     * 
     * @param userId 用户ID，将作为个人知识库的knowledge_base_id
     * @param documents 要添加的文档列表
     * @param fileId 文件ID，用于关联文件记录
     */
    public void addDocumentsForUser(String userId, List<Document> documents, Long fileId) {
        // 验证用户ID
        if (userId == null || userId.trim().isEmpty() || "default".equals(userId)) {
            log.warn("无效的用户ID: {}, 无法添加文档到个人知识库", userId);
            throw new IllegalArgumentException("无效的用户ID");
        }
        
        // 验证用户ID是否为有效的数字
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式无效: {}, 无法解析为数字，无法添加文档到个人知识库", userId);
            throw new IllegalArgumentException("用户ID格式无效");
        }
        
        try (Connection connection = connectionPool.getConnection()) {
            // 使用新的向量表结构：knowledge_base_id, knowledge_base_type, file_id
            String sql = "INSERT INTO documents (text, metadata, embedding, file_id, knowledge_base_id, knowledge_base_type) VALUES (?, ?::jsonb, CAST(? AS vector), ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (Document doc : documents) {
                    // 生成embedding
                    Embedding embedding = embeddingModel.embed(doc.text()).content();
                    
                    // 准备元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("userId", userId);
                    metadata.put("knowledgeBaseId", userId); // 个人知识库ID就是用户ID
                    metadata.put("knowledgeBaseType", 1); // 1表示个人知识库
                    metadata.put("fileId", fileId);
                    metadata.put("contentLength", doc.text().length());
                    metadata.put("uploadTime", System.currentTimeMillis());
                    
                    // 如果文档有原始元数据，合并它们
                    if (doc.metadata() != null) {
                        try {
                            // 尝试直接使用toMap()方法
                            metadata.putAll(doc.metadata().toMap());
                        } catch (Exception e) {
                            // 如果没有toMap()方法，添加一个原始元数据字段
                            metadata.put("originalMetadata", doc.metadata().toString());
                        }
                    }
                    
                    // 将embedding转换为PostgreSQL向量格式的字符串
                    List<Float> vectorList = embedding.vectorAsList();
                    String vectorString = "[" + vectorList.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("") + "]";
                    
                    stmt.setString(1, doc.text());
                    stmt.setString(2, toJsonString(metadata));
                    stmt.setString(3, vectorString);
                    if (fileId != null) {
                        stmt.setLong(4, fileId);
                    } else {
                        stmt.setNull(4, java.sql.Types.BIGINT);
                    }
                    stmt.setLong(5, userIdLong); // 使用验证后的用户ID
                    stmt.setInt(6, 1); // knowledge_base_type = 1 (个人知识库)
                    
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                log.info("成功为用户 {} 添加 {} 个文档到个人知识库，文件ID: {}", userId, results.length, fileId);
            }
        } catch (SQLException e) {
            log.error("为用户 {} 添加文档到个人知识库失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("添加文档到个人知识库失败", e);
        }
    }

    /**
     * 添加文档到指定用户的个人知识库（兼容旧版本，无文件ID）
     * 
     * @param userId 用户ID
     * @param documents 要添加的文档列表
     */
    public void addDocumentsForUser(String userId, List<Document> documents) {
        addDocumentsForUser(userId, documents, null);
    }

    /**
     * 根据文件ID删除向量数据
     * 
     * @param fileId 文件ID
     * @return 删除的记录数
     */
    public int deleteDocumentsByFileId(Long fileId) {
        try (Connection connection = connectionPool.getConnection()) {
            String sql = "DELETE FROM documents WHERE file_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, fileId);
                int deletedCount = stmt.executeUpdate();
                
                log.info("成功删除文件ID {} 对应的 {} 个向量记录", fileId, deletedCount);
                return deletedCount;
            }
        } catch (SQLException e) {
            log.error("删除文件ID {} 对应的向量数据失败: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("删除向量数据失败", e);
        }
    }

    /**
     * 根据用户ID删除个人知识库的向量数据
     * 
     * @param userId 用户ID
     * @return 删除的记录数
     */
    public int deleteDocumentsByUserId(String userId) {
        // 验证用户ID
        if (userId == null || userId.trim().isEmpty() || "default".equals(userId)) {
            log.warn("无效的用户ID: {}, 无法删除个人知识库数据", userId);
            return 0;
        }
        
        // 验证用户ID是否为有效的数字
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式无效: {}, 无法解析为数字，无法删除个人知识库数据", userId);
            return 0;
        }
        
        try (Connection connection = connectionPool.getConnection()) {
            // 只删除个人知识库的数据
            String sql = "DELETE FROM documents WHERE knowledge_base_id = ? AND knowledge_base_type = 1";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userIdLong);
                int deletedCount = stmt.executeUpdate();
                
                log.info("成功删除用户 {} 个人知识库的 {} 个向量记录", userId, deletedCount);
                return deletedCount;
            }
        } catch (SQLException e) {
            log.error("删除用户 {} 个人知识库的向量数据失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("删除向量数据失败", e);
        }
    }

    /**
     * 根据文件ID和用户ID删除个人知识库的向量数据（双重验证）
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 删除的记录数
     */
    public int deleteDocumentsByFileIdAndUserId(Long fileId, String userId) {
        // 验证用户ID
        if (userId == null || userId.trim().isEmpty() || "default".equals(userId)) {
            log.warn("无效的用户ID: {}, 无法删除个人知识库文件数据", userId);
            return 0;
        }
        
        // 验证用户ID是否为有效的数字
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式无效: {}, 无法解析为数字，无法删除个人知识库文件数据", userId);
            return 0;
        }
        
        try (Connection connection = connectionPool.getConnection()) {
            // 只删除个人知识库中该用户的指定文件数据
            String sql = "DELETE FROM documents WHERE file_id = ? AND knowledge_base_id = ? AND knowledge_base_type = 1";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, fileId);
                stmt.setLong(2, userIdLong);
                int deletedCount = stmt.executeUpdate();
                
                log.info("成功删除文件ID {} 用户 {} 个人知识库的 {} 个向量记录", fileId, userId, deletedCount);
                return deletedCount;
            }
        } catch (SQLException e) {
            log.error("删除文件ID {} 用户 {} 个人知识库的向量数据失败: {}", fileId, userId, e.getMessage(), e);
            throw new RuntimeException("删除向量数据失败", e);
        }
    }

    /**
     * 获取文件的向量数据统计
     * 
     * @param fileId 文件ID
     * @return 统计信息
     */
    public Map<String, Object> getFileVectorStats(Long fileId) {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection connection = connectionPool.getConnection()) {
            String sql = "SELECT COUNT(*) as count, AVG(length(text)) as avg_length FROM documents WHERE file_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, fileId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("fileId", fileId);
                        stats.put("vectorCount", rs.getInt("count"));
                        stats.put("avgLength", rs.getDouble("avg_length"));
                        stats.put("success", true);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取文件 {} 的向量统计信息失败: {}", fileId, e.getMessage());
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    /**
     * 为指定用户在个人知识库中搜索相似文档
     * 
     * @param userId 用户ID
     * @param query 查询内容
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度阈值
     * @return 匹配的文档列表
     */
    public List<EmbeddingMatch<TextSegment>> searchForUser(String userId, String query, int maxResults, double minScore) {
        // 验证用户ID
        if (userId == null || userId.trim().isEmpty() || "default".equals(userId)) {
            log.warn("无效的用户ID: {}, 无法搜索个人知识库", userId);
            return new ArrayList<>(); // 返回空列表
        }
        
        // 验证用户ID是否为有效的数字
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式无效: {}, 无法解析为数字，无法搜索个人知识库", userId);
            return new ArrayList<>(); // 返回空列表
        }
        
        try {
            // 生成查询embedding
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 构造搜索请求
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(minScore)
                    .build();
            
            return searchForUser(userId, request);
            
        } catch (Exception e) {
            log.error("为用户 {} 搜索个人知识库文档失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("搜索文档失败", e);
        }
    }

    /**
     * 为指定用户在个人知识库中搜索相似文档（使用EmbeddingSearchRequest）
     * 
     * @param userId 用户ID
     * @param request 搜索请求
     * @return 匹配的文档列表
     */
    public List<EmbeddingMatch<TextSegment>> searchForUser(String userId, EmbeddingSearchRequest request) {
        List<EmbeddingMatch<TextSegment>> results = new ArrayList<>();
        
        // 验证用户ID
        if (userId == null || userId.trim().isEmpty() || "default".equals(userId)) {
            log.warn("无效的用户ID: {}, 无法搜索个人知识库", userId);
            return results; // 返回空列表
        }
        
        // 验证用户ID是否为有效的数字
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式无效: {}, 无法解析为数字，无法搜索个人知识库", userId);
            return results; // 返回空列表
        }
        
        try (Connection connection = connectionPool.getConnection()) {
            // 修改查询条件，使用knowledge_base_id和knowledge_base_type
            String sql = """
                SELECT embedding_id, text, metadata, embedding <=> CAST(? AS vector) as distance
                FROM documents
                WHERE knowledge_base_id = ? AND knowledge_base_type = 1
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // 将Float[]转换为PostgreSQL向量格式的字符串
                List<Float> vectorList = request.queryEmbedding().vectorAsList();
                String vectorString = "[" + vectorList.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("") + "]";
                
                log.debug("为用户 {} 搜索个人知识库，查询向量格式: {}", userId, vectorString.substring(0, Math.min(50, vectorString.length())) + "...");
                
                stmt.setString(1, vectorString);
                stmt.setLong(2, userIdLong); // 使用验证后的用户ID
                stmt.setString(3, vectorString);
                stmt.setInt(4, request.maxResults());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        double distance = rs.getDouble("distance");
                        double score = 1.0 - distance; // 转换为相似度分数
                        
                        if (score >= request.minScore()) {
                            String embeddingId = rs.getString("embedding_id");
                            String text = rs.getString("text");
                            String metadataJson = rs.getString("metadata");
                            
                            // 解析元数据
                            Map<String, Object> metadataMap = parseJsonString(metadataJson);
                            Metadata metadata = Metadata.from(metadataMap);
                            
                            TextSegment segment = TextSegment.from(text, metadata);
                            EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(score, embeddingId, null, segment);
                            results.add(match);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("为用户 {} 搜索个人知识库文档失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("搜索文档失败", e);
        }
        
        log.info("为用户 {} 在个人知识库中搜索到 {} 个匹配的文档", userId, results.size());
        return results;
    }

    /**
     * 获取用户个人知识库统计信息
     * 
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getUserStats(String userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 验证用户ID
        if (userId == null || userId.trim().isEmpty() || "default".equals(userId)) {
            log.warn("无效的用户ID: {}, 无法获取个人知识库统计信息", userId);
            stats.put("success", false);
            stats.put("error", "无效的用户ID");
            return stats;
        }
        
        // 验证用户ID是否为有效的数字
        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式无效: {}, 无法解析为数字，无法获取个人知识库统计信息", userId);
            stats.put("success", false);
            stats.put("error", "用户ID格式无效");
            return stats;
        }
        
        try (Connection connection = connectionPool.getConnection()) {
            String sql = """
                SELECT 
                    COUNT(*) as total_documents,
                    COUNT(DISTINCT file_id) as unique_files,
                    AVG(length(text)) as avg_length,
                    MIN(created_at) as first_upload,
                    MAX(created_at) as last_upload
                FROM documents 
                WHERE knowledge_base_id = ? AND knowledge_base_type = 1
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userIdLong);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("userId", userId);
                        stats.put("totalDocuments", rs.getInt("total_documents"));
                        stats.put("uniqueFiles", rs.getInt("unique_files"));
                        stats.put("avgLength", rs.getDouble("avg_length"));
                        stats.put("firstUpload", rs.getTimestamp("first_upload"));
                        stats.put("lastUpload", rs.getTimestamp("last_upload"));
                        stats.put("success", true);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取用户 {} 的个人知识库统计信息失败: {}", userId, e.getMessage());
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    // 实现EmbeddingStore接口的必须方法（使用默认实现）
    @Override
    public String add(Embedding embedding) {
        throw new UnsupportedOperationException("请使用addDocumentsForUser方法");
    }

    @Override
    public void add(String id, Embedding embedding) {
        throw new UnsupportedOperationException("请使用addDocumentsForUser方法");
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        throw new UnsupportedOperationException("请使用addDocumentsForUser方法");
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        throw new UnsupportedOperationException("请使用addDocumentsForUser方法");
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        throw new UnsupportedOperationException("请使用addDocumentsForUser方法");
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest embeddingSearchRequest) {
        throw new UnsupportedOperationException("请使用searchForUser方法");
    }

    // 辅助方法
    private String toJsonString(Map<String, Object> map) {
        // 简单的JSON序列化（生产环境建议使用Jackson或Gson）
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseJsonString(String json) {
        // 简单的JSON解析（生产环境建议使用Jackson或Gson）
        Map<String, Object> map = new HashMap<>();
        if (json != null && !json.trim().isEmpty()) {
            // 这里简化处理，实际应用中建议使用专业的JSON库
            map.put("raw", json);
        }
        return map;
    }

    /**
     * 测试向量数据库连接和插入功能
     */
    public boolean testVectorConnection() {
        try (Connection connection = connectionPool.getConnection()) {
            // 测试基本连接
            String testSql = "SELECT 1";
            try (PreparedStatement stmt = connection.prepareStatement(testSql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        log.info("向量数据库连接测试成功");
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("向量数据库连接测试失败: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 测试向量插入功能
     */
    public boolean testVectorInsert(String userId) {
        try {
            // 创建测试文档
            Document testDoc = Document.from("这是一个测试文档，用于验证向量插入功能是否正常工作。");
            List<Document> testDocs = List.of(testDoc);
            
            // 尝试插入
            addDocumentsForUser(userId, testDocs, null);
            
            log.info("向量插入测试成功");
            return true;
        } catch (Exception e) {
            log.error("向量插入测试失败: {}", e.getMessage(), e);
            return false;
        }
    }
} 