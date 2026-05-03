package com.ldd.initialization.service.ai;

import com.ldd.initialization.service.ai.adapter.ConsultantService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import com.ldd.initialization.dto.SharedKnowledgeBaseChatDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户智能对话服务
 * 提供基于个人知识库和共享知识库的AI对话功能
 */
@Service
@Slf4j
public class UserConsultantService {
    
    @Autowired
    private ConsultantService consultantService;

    
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private SharedKnowledgeBaseVectorService sharedVectorService;
    
    private String systemPrompt;

    @Autowired
    private UserVectorService userVectorService;



    /**
     * 设置当前用户ID
     * 
     * @param userId 用户ID
     */
    public void setCurrentUserId(String userId) {
    }
    
    /**
     * 初始化系统提示词
     */
    public void initSystemPrompt() {
        if (systemPrompt == null) {
            try {
                ClassPathResource resource = new ClassPathResource("system.txt");
                systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("读取系统提示词失败: {}", e.getMessage());
                systemPrompt = "你是一个智能助手，可以帮助用户回答问题和处理任务,参考资料仅供参考，回答的内容不要和参考的一模一样，要有自己的回答的特色！";
            }
        }
    }


    /**
     * 为指定用户从个人知识库检索内容
     *
     * @param userId 用户ID
     * @param query 查询内容
     * @return 检索到的内容列表
     */
    public List<Content> retrieveForUser(String userId, String query) {
        log.info("为用户 {} 从个人知识库检索内容，查询: {}", userId, query);

        try {
            // 搜索用户的个人知识库
            List<EmbeddingMatch<TextSegment>> matches = userVectorService.searchForUser(
                    userId,
                    query,
                    10, // 最多返回10个结果
                    0.3 // 最小相似度阈值
            );

            // 转换为Content对象
            List<Content> contents = matches.stream()
                    .map(match -> {
                        TextSegment segment = match.embedded();
                        // 在LangChain4j 1.0.1-beta6中，直接使用TextSegment创建Content
                        return Content.from(segment);
                    })
                    .collect(Collectors.toList());

            log.info("为用户 {} 从个人知识库检索到 {} 个相关内容", userId, contents.size());
            return contents;

        } catch (Exception e) {
            log.error("为用户 {} 从个人知识库检索内容时发生错误: {}", userId, e.getMessage(), e);
            return List.of(); // 返回空列表
        }
    }
    
    /**
     * 构建增强的用户消息（包含个人知识库检索）
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @return 增强后的消息
     */
    public String buildEnhancedMessage(String userId, String message) {
        try {
            // 初始化系统提示词
            initSystemPrompt();

            
            // 检索用户个人知识库中的相关内容
            List<Content> relevantContents = retrieveForUser(userId, message);
            
            // 构建上下文
            String context = buildContext(relevantContents);
            
            // 构建完整的用户消息
            String enhancedMessage = buildUserMessage(message, context);
            
            // 返回包含系统提示词的完整消息
            return systemPrompt + "\n\n" + enhancedMessage;
            
        } catch (Exception e) {
            log.error("构建增强消息失败: {}", e.getMessage(), e);
            return systemPrompt + "\n\n用户问题：" + message;
        }
    }


    /**
     * 构建上下文信息
     */
    private String buildContext(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        
        String context = contents.stream()
                .map(content -> {
                    try {
                        // 在LangChain4j 1.0.1-beta6中，Content可能有textSegment()方法
                        return content.textSegment().text();
                    } catch (Exception e) {
                        // 如果没有textSegment()方法，直接返回toString()内容
                        return content.toString();
                    }
                })
                .limit(5) // 限制上下文长度
                .collect(Collectors.joining("\n\n"));
        
        return "参考资料：\n" + context + "\n\n";
    }
    
    /**
     * 构建用户消息
     */
    private String buildUserMessage(String originalMessage, String context) {
        if (context.isEmpty()) {
            return "用户问题：" + originalMessage;
        }
        
        return context + "用户问题：" + originalMessage;
    }


    // ==================== 共享知识库相关方法 ====================


    /**
     * 基于共享知识库进行流式对话
     */
    public Flux<String> chatWithSharedKnowledgeBaseStream(SharedKnowledgeBaseChatDTO chatDTO, Long userId) {
        log.info("共享知识库流式对话，用户: {}, 知识库: {}", userId, chatDTO.getKnowledgeBaseId());
        
        try {
            // 设置当前用户ID到内容检索器
           // setCurrentUserId(String.valueOf(userId));
            
            // 构建增强消息（基于共享知识库）
            String enhancedMessage = buildSharedKnowledgeBaseMessage(chatDTO.getKnowledgeBaseId(), chatDTO.getMessage());
            
            // 构建会话ID
            String memoryId = chatDTO.getMemoryId() != null ? chatDTO.getMemoryId() : 
                            "shared_kb_" + chatDTO.getKnowledgeBaseId() + "_" + userId;
            
            log.info("调用流式AI模型，共享知识库: {}, 用户: {}, 会话: {}", 
                    chatDTO.getKnowledgeBaseId(), userId, memoryId);
            
            // 调用流式聊天服务
            return consultantService.streamChat(memoryId, enhancedMessage)
                .doOnNext(chunk -> log.debug("共享知识库流式响应块: {}", chunk))
                .doOnComplete(() -> log.info("共享知识库流式对话完成，知识库: {}, 用户: {}", 
                             chatDTO.getKnowledgeBaseId(), userId))
                .doOnError(error -> log.error("共享知识库流式对话失败: {}", error.getMessage()));
            
        } catch (Exception e) {
            log.error("共享知识库流式对话处理失败: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("共享知识库对话服务暂时不可用，请稍后再试"));
        }
    }

    /**
     * 构建基于共享知识库的增强消息
     */
    private String buildSharedKnowledgeBaseMessage(Long knowledgeBaseId, String message) {
        try {
            // 初始化系统提示词
            initSystemPrompt();
            
            // 为用户消息生成embedding
            Embedding queryEmbedding = embeddingModel.embed(message).content();
            
            // 创建搜索请求
            //EmbeddingSearchRequest.builder() ：创建一个搜索请求，
            // 其中包含了查询向量、期望返回的最大结果数 ( maxResults=5 )
            // 和最低相似度分数阈值 ( minScore=0.7 )。
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5)
                    .minScore(0.7)
                    .build();
            
            // 从共享知识库搜索相关内容
            List<EmbeddingMatch<TextSegment>> matches = sharedVectorService.searchInSharedKnowledgeBase(
                    knowledgeBaseId, searchRequest);
            
            // 构建上下文
            String context = buildSharedKnowledgeBaseContext(matches);
            
            // 构建完整的用户消息
            String enhancedMessage = buildUserMessage(message, context);
            
            // 返回包含系统提示词的完整消息
            return systemPrompt + "\n\n" + enhancedMessage;
            
        } catch (Exception e) {
            log.error("构建共享知识库增强消息失败: {}", e.getMessage(), e);
            return systemPrompt + "\n\n用户问题：" + message;
        }
    }

    /**
     * 构建共享知识库上下文信息
     */
    private String buildSharedKnowledgeBaseContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }
        
        String context = matches.stream()
                .map(match -> {
                    try {
                        return match.embedded().text();
                    } catch (Exception e) {
                        log.warn("获取匹配文本失败: {}", e.getMessage());
                        return "";
                    }
                })
                .filter(text -> !text.isEmpty())
                .limit(5) // 限制上下文长度
                .collect(Collectors.joining("\n\n"));
        
        if (context.isEmpty()) {
            return "";
        }
        
        return "共享知识库参考资料：\n" + context + "\n\n";
    }
} 