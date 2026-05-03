package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ldd.initialization.domain.KnowledgeBaseMember;
import com.ldd.initialization.dto.*;
import com.ldd.initialization.mapper.KnowledgeBaseMemberMapper;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.KnowledgeBaseFileService;
import com.ldd.initialization.service.SharedKnowledgeBaseService;
import com.ldd.initialization.service.ai.UserConsultantService;
import com.ldd.initialization.vo.KnowledgeBaseFileVO;
import com.ldd.initialization.vo.KnowledgeBaseMemberVO;
import com.ldd.initialization.vo.KnowledgeBaseSquareVO;
import com.ldd.initialization.vo.SharedKnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 共享知识库控制器
 * 提供共享知识库的创建、管理、文件上传、AI对话等功能
 */
@RestController
@RequestMapping("/api/shared-knowledge-base")
@Slf4j
public class SharedKnowledgeBaseController {

    @Autowired
    private SharedKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeBaseFileService fileService;

    @Autowired
    private UserConsultantService consultantService;
    @Autowired
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    /**
     * 创建共享知识库
     * 用户可以创建自己的共享知识库，支持设置名称、描述、封面、密码等
     *
     * @param createDTO 创建知识库的请求参数
     * @return 创建成功的知识库信息
     */
    @PostMapping
    //@SaCheckLogin
    public Result<SharedKnowledgeBaseVO> createKnowledgeBase(@Valid @RequestBody SharedKnowledgeBaseCreateDTO createDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        SharedKnowledgeBaseVO result = knowledgeBaseService.createKnowledgeBase(createDTO, userId);
        return Result.success(result);
    }

    /**
     * 更新共享知识库信息
     * 只有知识库创建者可以操作，支持修改名称、描述、封面、密码、公开状态等
     *
     * @param knowledgeBaseId 知识库ID
     * @param updateDTO       更新参数
     * @return 更新后的知识库信息
     */
    @PutMapping("/{knowledgeBaseId}")
    @SaCheckLogin
    public Result<SharedKnowledgeBaseVO> updateKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                                             @Valid @RequestBody SharedKnowledgeBaseUpdateDTO updateDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        SharedKnowledgeBaseVO result = knowledgeBaseService.updateKnowledgeBase(knowledgeBaseId, updateDTO, userId);
        return Result.success(result);
    }

    /**
     * 删除共享知识库
     * 只有知识库创建者可以操作，删除后将清理相关的文件和向量数据
     *
     * @param knowledgeBaseId 知识库ID
     * @return 操作结果
     */
    @DeleteMapping("/{knowledgeBaseId}")
    @SaCheckLogin
    public Result<Void> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId, userId);
        return Result.success();
    }

    /**
     * 获取知识库详情
     * 获取指定知识库的详细信息，包括基本信息、成员信息、用户权限等
     *
     * @param knowledgeBaseId 知识库ID
     * @return 知识库详细信息
     */
    @GetMapping("/{knowledgeBaseId}")
    @SaCheckLogin
    public Result<SharedKnowledgeBaseVO> getKnowledgeBaseDetail(@PathVariable Long knowledgeBaseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        SharedKnowledgeBaseVO result = knowledgeBaseService.getKnowledgeBaseDetail(knowledgeBaseId, userId);
        return Result.success(result);
    }

    /**
     * 获取我创建的知识库列表
     * 返回当前用户作为创建者的所有知识库
     *
     * @return 知识库列表
     */
    @GetMapping("/my-created")
    @SaCheckLogin
    public Result<List<SharedKnowledgeBaseVO>> getMyCreatedKnowledgeBases() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SharedKnowledgeBaseVO> result = knowledgeBaseService.getCreatedKnowledgeBases(userId);
        return Result.success(result);
    }

    /**
     * 获取我加入的知识库列表
     * 返回当前用户作为成员加入的所有知识库
     *
     * @return 知识库列表
     */
    @GetMapping("/my-joined")
    @SaCheckLogin
    public Result<List<SharedKnowledgeBaseVO>> getMyJoinedKnowledgeBases() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SharedKnowledgeBaseVO> result = knowledgeBaseService.getJoinedKnowledgeBases(userId);
        return Result.success(result);
    }

    /**
     * 知识库广场
     * 搜索和浏览公开的知识库，支持关键词搜索、排序、分页
     *
     * @param keyword   搜索关键词，可选
     * @param sortBy    排序字段，默认为创建时间
     * @param sortOrder 排序顺序，默认为降序
     * @param page      页码，默认为1
     * @param size      每页大小，默认为10
     * @return 分页的知识库列表
     */
    @GetMapping("/square")
    @SaCheckLogin
    public Result<PageResult<KnowledgeBaseSquareVO>> getKnowledgeBaseSquare(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "create_time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Long userId = StpUtil.getLoginIdAsLong();
        KnowledgeBaseSearchDTO searchDTO = new KnowledgeBaseSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setSortBy(sortBy);
        searchDTO.setSortOrder(sortOrder);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        searchDTO.setOnlyPublic(true);

        IPage<KnowledgeBaseSquareVO> result = knowledgeBaseService.searchKnowledgeBaseSquare(searchDTO, userId);
        return Result.success(PageResult.convert(result));
    }

    /**
     * 加入知识库
     * 用户申请加入指定的知识库，如果知识库设置了密码则需要提供正确密码
     *
     * @param joinDTO 加入请求参数，包含知识库ID和密码
     * @return 操作结果
     */
    @PostMapping("/join")
    @SaCheckLogin
    public Result<Void> joinKnowledgeBase(@Valid @RequestBody KnowledgeBaseJoinDTO joinDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.joinKnowledgeBase(joinDTO, userId);
        return Result.success();
    }

    /**
     * 退出知识库
     * 用户主动退出指定的知识库，创建者不能退出自己的知识库
     *
     * @param knowledgeBaseId 知识库ID
     * @return 操作结果
     */
    @PostMapping("/{knowledgeBaseId}/leave")
    @SaCheckLogin
    public Result<Void> leaveKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.leaveKnowledgeBase(knowledgeBaseId, userId);
        return Result.success();
    }

    /**
     * 移除成员
     * 知识库创建者可以移除指定的成员，被移除的成员将失去访问权限
     *
     * @param knowledgeBaseId 知识库ID
     * @param memberId        要移除的成员ID
     * @return 操作结果
     */
    @DeleteMapping("/{knowledgeBaseId}/members/{memberId}")
    @SaCheckLogin
    public Result<Void> removeMember(@PathVariable Long knowledgeBaseId,
                                     @PathVariable Long memberId) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.removeMember(knowledgeBaseId, memberId, userId);
        return Result.success();
    }

    // ==================== 文件管理相关接口 ====================

    /**
     * 上传文件到知识库
     * 将本地文件上传到指定的知识库中，文件会自动进行向量化处理以支持AI检索
     *
     * @param knowledgeBaseId 知识库ID
     * @param file            要上传的文件
     * @return 上传成功的文件信息
     */
    @PostMapping("/{knowledgeBaseId}/files/upload")
    @SaCheckLogin
    public Result<KnowledgeBaseFileVO> uploadFile(@PathVariable Long knowledgeBaseId,
                                                  @RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        KnowledgeBaseFileVO result = fileService.uploadFile(knowledgeBaseId, file, userId);
        return Result.success(result);
    }

    /**
     * 复制文件到知识库
     * 从用户的个人知识库复制已有文件到共享知识库中
     *
     * @param knowledgeBaseId 知识库ID
     * @param uploadDTO       复制请求参数，包含要复制的文件ID列表
     * @return 复制操作结果，包含成功和失败的文件统计
     */
    @PostMapping("/{knowledgeBaseId}/files/copy")
    @SaCheckLogin
    public Result<Map<String, Object>> copyFiles(@PathVariable Long knowledgeBaseId,
                                                 @Valid @RequestBody KnowledgeBaseFileUploadDTO uploadDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        uploadDTO.setKnowledgeBaseId(knowledgeBaseId);
        Map<String, Object> result = fileService.copyFilesToKnowledgeBase(uploadDTO, userId);
        return Result.success(result);
    }

    /**
     * 删除知识库文件
     * 从知识库中删除指定的文件，同时会清理相关的向量数据
     * 创建者可以删除任何文件，普通成员只能删除自己上传的文件
     *
     * @param knowledgeBaseId 知识库ID
     * @param fileId          文件ID
     * @return 操作结果
     */
    @DeleteMapping("/{knowledgeBaseId}/files/{fileId}")
    @SaCheckLogin
    public Result<Void> deleteFile(@PathVariable Long knowledgeBaseId,
                                   @PathVariable Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        fileService.deleteFile(knowledgeBaseId, fileId, userId);
        return Result.success();
    }

    /**
     * 获取知识库文件列表
     * 分页获取知识库中的所有文件，支持按文件名搜索
     *
     * @param knowledgeBaseId 知识库ID
     * @param page            页码，默认为1
     * @param size            每页大小，默认为10
     * @param keyword         搜索关键词，可选
     * @return 分页的文件列表
     */
    @GetMapping("/{knowledgeBaseId}/files")
    @SaCheckLogin
    public Result<PageResult<KnowledgeBaseFileVO>> getKnowledgeBaseFiles(
            @PathVariable Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<KnowledgeBaseFileVO> result = fileService.getKnowledgeBaseFiles(knowledgeBaseId, page, size, keyword, userId);
        return Result.success(PageResult.convert(result));
    }

    /**
     * 获取个人文件列表
     * 获取用户个人知识库中的所有文件，用于复制到共享知识库时选择
     *
     * @return 用户的个人文件列表
     */
    @GetMapping("/personal-files")
    @SaCheckLogin
    public Result<List<KnowledgeBaseFileVO>> getPersonalFiles() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<KnowledgeBaseFileVO> result = fileService.getPersonalFiles(userId);
        return Result.success(result);
    }

    // ==================== AI 对话相关接口 ====================

    /**
     * 共享知识库AI对话（流式）
     * 基于指定共享知识库进行AI对话，支持流式响应以提供更好的用户体验
     *
     * @param chatDTO 对话请求参数
     * @return 流式AI回复
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckLogin
    public Flux<String> chatWithKnowledgeBaseStream(
            @Valid @RequestBody SharedKnowledgeBaseChatDTO chatDTO) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证用户是否有权限访问该知识库
        if (!knowledgeBaseService.hasPermission(chatDTO.getKnowledgeBaseId(), userId)) {
            return Flux.error(new RuntimeException("您没有权限访问该知识库"));
        }

        chatDTO.setKnowledgeBaseId(chatDTO.getKnowledgeBaseId());
        return consultantService.chatWithSharedKnowledgeBaseStream(chatDTO, userId);
    }

    /**
     * 查询知识库的完整成员列表
     * 对应 Mapper 的 selectMembersByKnowledgeBaseId 方法
     * 权限：通常知识库的所有成员都可以查看
     *
     * @param knowledgeBaseId 知识库ID (从URL路径中获取)
     * @return 成员VO列表
     */
    @GetMapping("/members/{knowledgeBaseId}")
    @SaCheckLogin
    public Result<List<KnowledgeBaseMemberVO>> listMembers(@PathVariable Long knowledgeBaseId) {
        List<KnowledgeBaseMemberVO> members = knowledgeBaseMemberMapper.selectMembersByKnowledgeBaseId(knowledgeBaseId);
        return Result.success(members);
    }

    /**
     * 获取指定的单个成员信息
     * 对应 Mapper 的 selectMemberInfo 方法
     * 权限：通常知识库的所有成员都可以查看
     *
     * @param knowledgeBaseId 知识库ID (从URL路径中获取)
     * @param userId          要查询的成员用户ID (从URL路径中获取)
     * @return 指定成员的VO信息
     */
    @GetMapping("/member/{userId}/{knowledgeBaseId}")
    // @SaCheckLogin
    public Result<KnowledgeBaseMemberVO> getMemberInfo(@PathVariable Long knowledgeBaseId, @PathVariable Long userId) {
        KnowledgeBaseMemberVO memberInfo = knowledgeBaseMemberMapper.selectMemberInfo(knowledgeBaseId, userId);
        return Result.success(memberInfo);
    }

    /**
     * 删除指定成员
     */
    @DeleteMapping("/members/{deleteUserId}/{knowledgeBaseId}")
    // @SaCheckLogin
    public Result<Void> deleteMember(@PathVariable Long knowledgeBaseId, @PathVariable Long deleteUserId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证用户是否有权限访问该知识库
        if (!knowledgeBaseService.isCreator(knowledgeBaseId, userId)) {
            return Result.error(new RuntimeException("您没有权限访问该知识库"));
        }
        LambdaQueryWrapper<KnowledgeBaseMember> query = new LambdaQueryWrapper<KnowledgeBaseMember>().eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId).eq(KnowledgeBaseMember::getUserId, deleteUserId);
         knowledgeBaseMemberMapper.delete(query);
        return Result.success();
    }

} 