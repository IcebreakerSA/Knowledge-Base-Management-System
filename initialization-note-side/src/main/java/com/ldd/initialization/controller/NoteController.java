package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ldd.initialization.dto.NoteAutoSaveDTO;
import com.ldd.initialization.dto.NoteCreateDTO;
import com.ldd.initialization.dto.NoteUpdateDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.NoteService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.NoteBriefVO;
import com.ldd.initialization.vo.NoteDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 笔记Controller
 */
@RestController
@RequestMapping("/api/notes")
@SaCheckLogin
public class NoteController {

    @Autowired
    private NoteService noteService;

    /**
     * 创建笔记
     */
    @PostMapping
    public Result<NoteDetailVO> createNote(@Valid @RequestBody NoteCreateDTO createDTO) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        NoteDetailVO note = noteService.createNote(createDTO, userId);
        return Result.success(note);
    }

    /**
     * 分页查询笔记列表
     */
    @GetMapping
    public Result<PageResult<NoteBriefVO>> getNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long notebookId,
            @RequestParam(required = false) String keyword) {
        
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        IPage<NoteBriefVO> notePages = noteService.getNotesByPage(page, size, notebookId, userId, keyword);
        return Result.success(PageResult.convert(notePages));
    }

    /**
     * 获取笔记详情
     */
    @GetMapping("/{noteId}")
    public Result<NoteDetailVO> getNoteDetail(@PathVariable Long noteId) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        NoteDetailVO note = noteService.getNoteDetail(noteId, userId);
        return Result.success(note);
    }

    /**
     * 更新笔记
     */
    @PostMapping("/update")
    public Result<NoteDetailVO> updateNote(@Valid @RequestBody NoteUpdateDTO updateDTO) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        NoteDetailVO note = noteService.updateNote(updateDTO.getNoteId(), updateDTO, userId);
        return Result.success(note);
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        noteService.deleteNote(noteId, userId);
        return Result.success(null);
    }

    /**
     * 切换笔记置顶状态
     */
    @PostMapping("/{noteId}/pin")
    public Result<Void> togglePinNote(@PathVariable Long noteId) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        noteService.togglePinNote(noteId, userId);
        return Result.success(null);
    }

    /**
     * 获取用户的所有标签
     */
    @GetMapping("/tags")
    public Result<List<String>> getUserTags() {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        List<String> tags = noteService.getUserTags(userId);
        return Result.success(tags);
    }

    /**
     * 自动保存笔记内容（用于编辑器自动保存）
     */
    @PostMapping("/autosave")
    public Result<Void> autoSaveNote(@Valid @RequestBody NoteAutoSaveDTO autoSaveDTO) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        
        // 创建简单的更新DTO，只更新内容
        NoteUpdateDTO updateDTO = new NoteUpdateDTO();
        updateDTO.setNoteId(autoSaveDTO.getNoteId());
        updateDTO.setContentMd(autoSaveDTO.getContentMd());
        
        // 获取当前笔记信息，保留其他字段
        NoteDetailVO currentNote = noteService.getNoteDetail(autoSaveDTO.getNoteId(), userId);
        updateDTO.setTitle(currentNote.getTitle());
        updateDTO.setStatus(currentNote.getStatus());
        updateDTO.setIsPinned(currentNote.getIsPinned());
        updateDTO.setTags(currentNote.getTags());
        
        noteService.updateNote(autoSaveDTO.getNoteId(), updateDTO, userId);
        return Result.success(null);
    }

    /**
     * 将笔记添加到个人知识库
     */
    @PostMapping("/{noteId}/add-to-knowledge-base")
    public Result<Map<String, Object>> addNoteToPersonalKnowledgeBase(@PathVariable Long noteId) {
        Long userId = UserUtils.getCurrentUserId();
       return noteService.addNoteToPersonalKnowledgeBase(noteId, userId);
    }
} 