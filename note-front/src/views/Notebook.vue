<template>
  <div class="main-container">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-left">
        <h2>我的笔记本</h2>
      </div>
      <div class="nav-right">
        <div class="user-info-nav">
          <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
            {{ userStore.userInfo?.username?.charAt(0) }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.phone }}</span>
          <el-dropdown @command="handleUserCommand">
            <el-icon class="user-menu"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- Tab页面 -->
    <div class="tab-container">
      <el-tabs v-model="activeTab" class="main-tabs">
        <!-- 笔记本Tab -->
        <el-tab-pane label="📝 笔记本" name="notebook">
          <div class="notebook-container">
            <div class="sidebar-wrapper">
              <!-- 左侧笔记本列表 -->
              <div class="sidebar" :class="{ collapsed: sidebarCollapsed }">
                <div class="sidebar-toggle" @click="toggleSidebar">
                  <el-icon>
                    <Fold v-if="!sidebarCollapsed" />
                    <Expand v-else />
                  </el-icon>
                </div>

                <div class="notebook-section">
                  <div class="section-header">
                    <span>笔记本</span>
                    <el-button
                        type="text"
                        size="small"
                        @click="showCreateNotebook = true"
                        title="新建笔记本"
                    >
                      <el-icon><Plus /></el-icon>
                    </el-button>
                  </div>

                  <div class="notebook-filter">
                    <div
                        class="filter-item"
                        :class="{ active: currentFilter === 'all' }"
                        @click="setFilter('all')"
                    >
                      <el-icon><Folder /></el-icon>
                      <span>全部</span>
                      <span class="count">{{ totalNoteCount }}</span>
                    </div>

                    <div
                        class="filter-item"
                        :class="{ active: currentFilter === 'recent' }"
                        @click="setFilter('recent')"
                    >
                      <el-icon><Clock /></el-icon>
                      <span>过去7天</span>
                      <span class="count">{{ recentNoteCount }}</span>
                    </div>
                  </div>

                  <div class="notebook-list">
                    <div
                        v-for="notebook in notebookStore.notebooks"
                        :key="notebook.id"
                        class="notebook-item"
                        :class="{ active: currentNotebook?.id === notebook.id }"
                        @click="selectNotebook(notebook)"
                    >
                      <div class="notebook-info">
                        <div class="notebook-name">{{ notebook.name }}</div>
                        <div class="notebook-count">{{ notebook.noteCount }} 篇笔记</div>
                      </div>
                      <el-dropdown @command="(command) => handleNotebookCommand(command, notebook)">
                        <el-icon class="notebook-menu"><MoreFilled /></el-icon>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="edit">编辑</el-dropdown-item>
                            <el-dropdown-item command="delete">删除</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- 中间笔记列表 -->
            <div class="note-list" :class="{ collapsed: noteListCollapsed }">
              <div class="note-list-toggle" @click="toggleNoteList">
                <el-icon>
                  <Fold v-if="!noteListCollapsed" />
                  <Expand v-else />
                </el-icon>
              </div>
              <div class="note-list-header">
                <div class="header-left">
                  <h3>{{ getListTitle() }}</h3>
                  <span class="note-count">{{ filteredNotes.length }} 篇笔记</span>
                </div>
                <div class="header-right">
                  <el-input
                      v-model="searchKeyword"
                      placeholder="搜索笔记..."
                      :prefix-icon="Search"
                      size="small"
                      clearable
                      style="width: 200px"
                  />
                  <el-button
                      type="primary"
                      @click="handleCreateNote"
                      size="small"
                  >
                    <el-icon><Plus /></el-icon>
                    新建笔记
                  </el-button>
                </div>
              </div>

              <div class="note-list-content">
                <div
                    v-for="note in filteredNotes"
                    :key="note.id"
                    class="note-item"
                    :class="{ active: currentNote?.id === note.id }"
                    @click="selectNote(note)"
                >
                  <div class="note-header">
                    <div class="note-title">
                      <el-icon v-if="note.isPinned" class="pin-icon"><Star /></el-icon>
                      {{ note.title || '无标题' }}
                    </div>
                    <div class="note-time">{{ formatTime(note.updateTime) }}</div>
                  </div>

                  <div class="note-preview">
                    {{ getPreview(note.contentMd, note.title) }}
                  </div>

                  <div class="note-footer">
                    <div class="note-tags">
                      <el-tag
                          v-for="tag in note.tags"
                          :key="tag"
                          size="small"
                          type="info"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                    <div class="note-stats">
                      <span>{{ note.wordCount }} 字</span>
                      <span>{{ note.viewCount }} 次阅读</span>
                    </div>
                  </div>
                </div>

                <div v-if="filteredNotes.length === 0" class="empty-state">
                  <el-icon size="64"><DocumentAdd /></el-icon>
                  <p>暂无笔记</p>
                  <p>点击"新建笔记"开始记录想法</p>
                </div>
              </div>
            </div>

            <!-- 右侧笔记编辑区域 -->
            <div class="note-editor" v-if="currentNote">
              <div class="editor-header">
                <el-input
                    v-model="currentNote.title"
                    placeholder="请输入笔记标题..."
                    size="large"
                    class="title-input"
                    @blur="autoSave"
                />
                <div class="editor-actions">
                  <el-button
                      @click="togglePin"
                      :type="currentNote.isPinned ? 'primary' : 'default'"
                      size="small"
                  >
                    <el-icon><Star /></el-icon>
                    {{ currentNote.isPinned ? '取消置顶' : '置顶' }}
                  </el-button>
                  <el-button @click="handleAddToKnowledgeBase" type="success" size="small">
                    <el-icon><DocumentAdd /></el-icon>
                    添加到知识库
                  </el-button>
                  <el-button @click="saveNote" type="primary" size="small">
                    保存
                  </el-button>
                  <el-button @click="deleteCurrentNote" type="danger" size="small">
                    删除
                  </el-button>
                </div>
              </div>

              <div class="editor-content">
                <v-md-editor
                    v-model="currentNote.contentMd"
                    height="500px"
                    placeholder="开始写作..."
                    @change="onEditorChange"
                    @blur="autoSave"
                />
              </div>

              <div class="editor-footer">
                <div class="tags-section">
                  <span>标签：</span>
                  <el-tag
                      v-for="tag in currentNote.tags"
                      :key="tag"
                      closable
                      @close="removeTag(tag)"
                  >
                    {{ tag }}
                  </el-tag>
                  <el-input
                      v-if="showTagInput"
                      v-model="newTag"
                      @keyup.enter="addTag"
                      @blur="addTag"
                      size="small"
                      style="width: 100px"
                  />
                  <el-button
                      v-else
                      @click="showTagInput = true"
                      size="small"
                      type="text"
                  >
                    + 添加标签
                  </el-button>
                </div>

                <div class="note-info">
                  <span>字数：{{ currentNote.wordCount }}</span>
                  <span>最后编辑：{{ formatTime(currentNote.updateTime) }}</span>
                </div>
              </div>
            </div>

            <div v-else class="empty-editor">
              <el-icon size="64"><Edit /></el-icon>
              <p>选择一篇笔记开始编辑</p>
            </div>
          </div>
        </el-tab-pane>

        <!-- 知识库Tab -->
        <el-tab-pane label="📁 知识库" name="documents">
          <DocumentManager />
        </el-tab-pane>

        <!-- AI对话Tab -->
        <el-tab-pane label="💬 AI对话" name="chat">
          <ChatDialog />
        </el-tab-pane>

        <!-- 共享知识库Tab -->
        <el-tab-pane label="🌐 共享知识库" name="shared">
          <SharedKnowledgeBase />
        </el-tab-pane>
        <el-tab-pane style="display: none" >
        </el-tab-pane>
      </el-tabs>

    </div>

    <!-- 创建笔记本对话框 -->
    <el-dialog
        v-model="showCreateNotebook"
        title="创建笔记本"
        width="400px"
    >
      <el-form :model="notebookForm" :rules="notebookRules" ref="notebookFormRef">
        <el-form-item label="笔记本名称" prop="name">
          <el-input v-model="notebookForm.name" placeholder="请输入笔记本名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
              v-model="notebookForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateNotebook = false">取消</el-button>
        <el-button type="primary" @click="handleCreateNotebook">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Plus,
  Folder,
  Clock,
  MoreFilled,
  Search,
  Star,
  DocumentAdd,
  Edit,
  Fold,
  Expand
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore, useNotebookStore } from '@/store'
import {
  getNotebooks,
  createNotebook,
  updateNotebook,
  deleteNotebook,
  getNotes,
  getNote,
  createNote as createNoteApi,
  updateNote,
  deleteNote,
  toggleNotePin,
  addNoteToKnowledgeBase,
  logout
} from '@/api'
import DocumentManager from '@/components/DocumentManager.vue'
import ChatDialog from '@/components/ChatDialog.vue'
import SharedKnowledgeBase from '@/components/SharedKnowledgeBase.vue'

const router = useRouter()
const userStore = useUserStore()
const notebookStore = useNotebookStore()

// 新增Tab相关数据
const activeTab = ref('notebook')

// 响应式数据
const currentFilter = ref('all')
const currentNotebook = ref(null)
const currentNote = ref(null)
const searchKeyword = ref('')
const showCreateNotebook = ref(false)
const showTagInput = ref(false)
const newTag = ref('')
const sidebarCollapsed = ref(false)
const noteListCollapsed = ref(false)

// 表单数据
const notebookForm = reactive({
  name: '',
  description: ''
})

const notebookRules = {
  name: [
    { required: true, message: '请输入笔记本名称', trigger: 'blur' }
  ]
}

const notebookFormRef = ref()

// 计算属性
const totalNoteCount = computed(() => {
  return notebookStore.notes.length
})

const recentNoteCount = computed(() => {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  return notebookStore.notes.filter(note =>
      new Date(note.updateTime).getTime() > sevenDaysAgo
  ).length
})

const filteredNotes = computed(() => {
  let notes = notebookStore.notes

  // 根据当前过滤器过滤
  if (currentFilter.value === 'recent') {
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
    notes = notes.filter(note =>
        new Date(note.updateTime).getTime() > sevenDaysAgo
    )
  }

  // 根据搜索关键词过滤
  if (searchKeyword.value) {
    notes = notes.filter(note =>
        note.title.includes(searchKeyword.value) ||
        note.contentMd.includes(searchKeyword.value)
    )
  }

  // 置顶笔记排序
  return notes.sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1
    if (!a.isPinned && b.isPinned) return 1
    return new Date(b.updateTime) - new Date(a.updateTime)
  })
})

// 方法
const setFilter = (filter) => {
  currentFilter.value = filter
  currentNotebook.value = null
  loadNotes()
}

const selectNotebook = (notebook) => {
  currentNotebook.value = notebook
  currentFilter.value = 'notebook'
  notebookStore.setCurrentNotebook(notebook)
  loadNotes()
}

const selectNote = async (note) => {
  if (currentNote.value && currentNote.value.id === note.id) return

  try {
    // 获取完整的笔记详情
    const response = await getNote(note.id)
    const data = response.data

    // 设置当前笔记为完整数据
    // 根据API返回结构调整数据访问
    const noteData = data.data || data
    currentNote.value = { ...noteData }
    notebookStore.setCurrentNote(noteData)

    // 增加阅读次数（静默更新，不影响用户体验）
    try {
      await updateNote({
        noteId: note.id,
        title: noteData.title,
        contentMd: noteData.contentMd,
        status: noteData.status,
        isPinned: noteData.isPinned,
        tags: noteData.tags,
        viewCount: (noteData.viewCount || 0) + 1
      })
    } catch (error) {
      console.error('更新阅读次数失败:', error)
    }

  } catch (error) {
    console.error('获取笔记详情失败:', error)
    // 如果获取详情失败，使用列表中的基本信息
    currentNote.value = { ...note }
    notebookStore.setCurrentNote(note)
    ElMessage.error('获取笔记详情失败')
  }
}

const getListTitle = () => {
  if (currentFilter.value === 'all') return '全部笔记'
  if (currentFilter.value === 'recent') return '最近笔记'
  if (currentNotebook.value) return currentNotebook.value.name
  return '笔记列表'
}

const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString()
}

const getPreview = (content, title) => {
  if (!content || content.trim() === '') {
    return title ? `${title}...` : '点击开始编辑'
  }

  // 清理markdown语法并提取纯文本
  const cleanContent = content
      .replace(/[#*`_~\[\]()]/g, '') // 移除markdown标记
      .replace(/\n+/g, ' ') // 换行符替换为空格
      .replace(/\s+/g, ' ') // 多个空格合并为一个
      .trim()

  if (!cleanContent) {
    return title ? `${title}...` : '点击开始编辑'
  }

  return cleanContent.length > 100 ?
      cleanContent.substring(0, 100) + '...' :
      cleanContent
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const toggleNoteList = () => {
  noteListCollapsed.value = !noteListCollapsed.value
}

const onEditorChange = (text, html) => {
  if (currentNote.value) {
    currentNote.value.contentMd = text
    currentNote.value.contentHtml = html
    debouncedAutoSave()
  }
}

const handleCreateNote = async () => {
  // 如果没有选择笔记本，自动创建一个默认笔记本
  if (!currentNotebook.value && notebookStore.notebooks.length === 0) {
    try {
      const defaultNotebook = {
        name: '我的笔记本',
        description: '默认笔记本'
      }
      const response = await createNotebook(defaultNotebook)
      const data = response.data
      const notebook = data.data || data
      notebookStore.addNotebook(notebook)
      currentNotebook.value = notebook
      notebookStore.setCurrentNotebook(notebook)
    } catch (error) {
      ElMessage.error('创建默认笔记本失败')
      return
    }
  } else if (!currentNotebook.value && notebookStore.notebooks.length > 0) {
    // 如果有笔记本但没有选择，选择第一个
    currentNotebook.value = notebookStore.notebooks[0]
    notebookStore.setCurrentNotebook(currentNotebook.value)
  }

  try {
    const noteData = {
      notebookId: currentNotebook.value.id,
      title: '新建笔记',
      contentMd: '# 新建笔记\n\n开始记录你的想法...',
      status: 1,
      isPinned: false,
      tags: []
    }

    const response = await createNoteApi(noteData)
    const data = response.data

    // 添加到store并设置为当前笔记
    const createdNote = data.data || data
    notebookStore.addNote(createdNote)
    currentNote.value = { ...createdNote }
    notebookStore.setCurrentNote(createdNote)

    // 重新加载笔记列表以确保同步
    await loadNotes()

    ElMessage.success('笔记创建成功')

  } catch (error) {
    console.error('创建笔记失败:', error)
    ElMessage.error('创建笔记失败')
  }
}

const saveNote = async () => {
  if (!currentNote.value) return

  try {
    const noteData = {
      noteId: currentNote.value.id,
      title: currentNote.value.title || '无标题',
      contentMd: currentNote.value.contentMd || '',
      tags: currentNote.value.tags || [],
      status: 1,
      isPinned: currentNote.value.isPinned || false
    }

    const response = await updateNote(noteData)
    const data = response.data

    // 更新当前笔记和store中的数据
    const updatedNote = data.data || data
    Object.assign(currentNote.value, updatedNote)
    notebookStore.updateNote(currentNote.value.id, updatedNote)

    // 重新加载笔记列表以确保同步
    await loadNotes()

    ElMessage.success('保存成功')

  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const autoSave = async () => {
  if (!currentNote.value || !currentNote.value.id) return

  try {
    const noteData = {
      noteId: currentNote.value.id,
      title: currentNote.value.title || '无标题',
      contentMd: currentNote.value.contentMd || '',
      tags: currentNote.value.tags || [],
      status: 1,
      isPinned: currentNote.value.isPinned || false
    }

    const response = await updateNote(noteData)
    const data = response.data

    // 更新当前笔记的数据
    const updatedNote = data.data || data
    Object.assign(currentNote.value, updatedNote)
    notebookStore.updateNote(currentNote.value.id, updatedNote)

    console.log('自动保存成功')
  } catch (error) {
    console.error('自动保存失败:', error)
  }
}

const togglePin = async () => {
  if (!currentNote.value) return

  try {
    await toggleNotePin(currentNote.value.id)
    currentNote.value.isPinned = !currentNote.value.isPinned

    // 更新笔记的置顶状态
    await updateNote({
      noteId: currentNote.value.id,
      title: currentNote.value.title,
      contentMd: currentNote.value.contentMd,
      status: currentNote.value.status,
      isPinned: currentNote.value.isPinned,
      tags: currentNote.value.tags
    })

    notebookStore.updateNote(currentNote.value.id, { isPinned: currentNote.value.isPinned })

    // 重新加载笔记列表以确保同步
    await loadNotes()

    ElMessage.success(currentNote.value.isPinned ? '置顶成功' : '取消置顶成功')

  } catch (error) {
    console.error('置顶操作失败:', error)
    ElMessage.error('置顶操作失败')
  }
}

const deleteCurrentNote = async () => {
  if (!currentNote.value) return

  try {
    await ElMessageBox.confirm('确定删除这篇笔记吗？', '提示', {
      type: 'warning'
    })

    await deleteNote(currentNote.value.id)

    notebookStore.removeNote(currentNote.value.id)
    currentNote.value = null

    ElMessage.success('删除成功')

  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const addTag = () => {
  if (!newTag.value.trim()) {
    showTagInput.value = false
    return
  }

  if (!currentNote.value.tags.includes(newTag.value)) {
    currentNote.value.tags.push(newTag.value)
  }

  newTag.value = ''
  showTagInput.value = false
}

const removeTag = (tag) => {
  const index = currentNote.value.tags.indexOf(tag)
  if (index > -1) {
    currentNote.value.tags.splice(index, 1)
  }
}

const handleAddToKnowledgeBase = async () => {
  if (!currentNote.value || !currentNote.value.id) {
    ElMessage.warning('请先选择一篇笔记')
    return
  }

  try {
    const response = await addNoteToKnowledgeBase(currentNote.value.id)
    const data = response.data

    if (data.code === 0) {
      const result = data.data
      ElMessage.success(`笔记"${result.noteTitle}"已成功添加到个人知识库`)

      // 显示处理结果的详细信息
      setTimeout(() => {
        ElMessage.info(`文件名: ${result.fileName}，文件大小: ${(result.fileSize / 1024 / 1024).toFixed(2)}MB，处理文档数: ${result.processResult.documentCount}`)
      }, 1000)
    } else {
      ElMessage.error(data.message || '添加到知识库失败')
    }

  } catch (error) {
    console.error('添加到知识库失败:', error)
    ElMessage.error('添加到知识库失败')
  }
}

const handleCreateNotebook = async () => {
  if (!notebookFormRef.value) return

  try {
    await notebookFormRef.value.validate()

    const response = await createNotebook(notebookForm)
    const data = response.data

    // 添加到store并选择新创建的笔记本
    const notebookData = data.data || data
    notebookStore.addNotebook(notebookData)
    currentNotebook.value = notebookData
    notebookStore.setCurrentNotebook(notebookData)
    currentFilter.value = 'notebook'

    // 重新加载笔记列表
    await loadNotes()

    // 重置表单
    notebookForm.name = ''
    notebookForm.description = ''
    showCreateNotebook.value = false

    ElMessage.success('笔记本创建成功')

  } catch (error) {
    console.error('创建笔记本失败:', error)
    ElMessage.error('创建笔记本失败')
  }
}

const handleNotebookCommand = async (command, notebook) => {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除这个笔记本吗？', '提示', {
        type: 'warning'
      })

      await deleteNotebook(notebook.id)

      notebookStore.removeNotebook(notebook.id)

      if (currentNotebook.value?.id === notebook.id) {
        currentNotebook.value = null
        currentNote.value = null
      }

      ElMessage.success('删除成功')

    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }
}

const handleUserCommand = async (command) => {
  if (command === 'logout') {
    try {
      await logout()
      userStore.logout()
      router.push('/login')
      ElMessage.success('退出成功')
    } catch (error) {
      console.error('退出失败:', error)
    }
  }
}

const loadNotebooks = async () => {
  try {
    const response = await getNotebooks()
    const data = response.data
    // notebooks API返回的是 {data: [...], code: 0, message: "成功"}
    notebookStore.setNotebooks(data.data)
  } catch (error) {
    console.error('加载笔记本失败:', error)
  }
}

const loadNotes = async () => {
  try {
    const params = {}
    if (currentNotebook.value && currentFilter.value === 'notebook') {
      params.notebookId = currentNotebook.value.id
    }

    const response = await getNotes(params)
    const data = response.data
    console.log('获取笔记数据:', data) // 添加调试日志

    // notes API返回的是 {data: {data: [...], total: 2}, code: 0, message: "成功"}
    let notes = []
    if (data && data.data && Array.isArray(data.data.data)) {
      notes = data.data.data
    }

    // 确保每个笔记都有必要的字段
    notes = notes.map(note => {
      const processedNote = {
        ...note,
        tags: note.tags || [],
        contentMd: note.contentMd || note.content || '', // 尝试多个可能的字段名
        title: note.title || '无标题',
        isPinned: note.isPinned || false,
        viewCount: note.viewCount || 0,
        wordCount: note.wordCount || (note.contentMd ? note.contentMd.length : 0)
      }

      console.log('处理后的笔记:', processedNote) // 添加调试日志
      return processedNote
    })

    notebookStore.setNotes(notes)
  } catch (error) {
    console.error('加载笔记失败:', error)
    notebookStore.setNotes([])
  }
}

// 防抖自动保存
let autoSaveTimer = null

const debouncedAutoSave = () => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  autoSaveTimer = setTimeout(() => {
    autoSave()
  }, 2000) // 2秒后自动保存
}

// 监听
watch(currentNote, (newNote) => {
  if (newNote) {
    // 计算字数
    newNote.wordCount = (newNote.contentMd || '').length
  }
}, { deep: true })

// 监听笔记内容变化，触发自动保存
watch(() => currentNote.value?.contentMd, (newContent, oldContent) => {
  if (currentNote.value && newContent !== oldContent && currentNote.value.id) {
    debouncedAutoSave()
  }
})

// 监听笔记标题变化，触发自动保存
watch(() => currentNote.value?.title, (newTitle, oldTitle) => {
  if (currentNote.value && newTitle !== oldTitle && currentNote.value.id) {
    debouncedAutoSave()
  }
})

onMounted(async () => {
  try {
    // 先加载笔记本
    await loadNotebooks()

    // 如果有笔记本，默认选择第一个
    if (notebookStore.notebooks.length > 0 && !currentNotebook.value) {
      currentNotebook.value = notebookStore.notebooks[0]
      notebookStore.setCurrentNotebook(currentNotebook.value)
      currentFilter.value = 'notebook'
    }

    // 再加载笔记
    await loadNotes()
  } catch (error) {
    console.error('初始化数据失败:', error)
    ElMessage.error('加载数据失败')
  }
})

onUnmounted(() => {
  // 清理自动保存定时器
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
})
</script>

<style scoped>
.main-container {
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0f0c29 0%, #24243e 25%, #313862 50%, #4a5568 75%, #2d3748 100%);
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(139, 92, 246, 0.2);
  flex-shrink: 0;
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.1);
}

.nav-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.nav-right {
  display: flex;
  align-items: center;
}

.user-info-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.user-menu {
  cursor: pointer;
  color: #64748b;
  transition: all 0.3s ease;
  padding: 4px;
  border-radius: 8px;
}

.user-menu:hover {
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.1);
}

.tab-container {
  flex: 1;
  overflow: visible;
  display: flex;
  flex-direction: column;
  margin: 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.main-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.9) 100%);
  border-bottom: 1px solid rgba(139, 92, 246, 0.2);
  backdrop-filter: blur(10px);
  flex-shrink: 0;
  border-radius: 16px 16px 0 0;
}

.main-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: visible;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 50%, #e2e8f0 100%);
  border-radius: 0 0 16px 16px;
}

.main-tabs :deep(.el-tab-pane) {
  height: 100%;
  padding: 0;
  overflow: visible;
}

.main-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  transition: all 0.3s ease;
  border-radius: 12px 12px 0 0;
  margin-right: 8px;
  padding: 14px 20px;
}

.main-tabs :deep(.el-tabs__item.is-active) {
  color: #8b5cf6;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(59, 130, 246, 0.1) 100%);
  border-bottom: 3px solid #8b5cf6;
}

.main-tabs :deep(.el-tabs__item:hover) {
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.05);
}

.notebook-container {
  height: 100%;
  display: flex;
  overflow: visible;
  gap: 20px;
  padding: 20px 30px;
}

.sidebar-wrapper {
  position: relative;
  overflow: visible;
  flex-shrink: 0;
}

.sidebar {
  width: 280px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.1);
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
  flex-shrink: 0;
  overflow: visible;
  display: flex;
  flex-direction: column;
  position: relative;
  max-height: calc(100vh - 200px);
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-toggle {
  position: absolute;
  top: 20px;
  right: -25px;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  z-index: 200;
  box-shadow: 0 6px 20px rgba(139, 92, 246, 0.4);
  transition: all 0.3s ease;
  border: 2px solid rgba(255, 255, 255, 0.9);
}

.sidebar-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.4);
}

.notebook-section {
  padding: 20px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.notebook-filter {
  margin-bottom: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
}

.filter-item:hover {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.filter-item.active {
  background: linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.count {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.notebook-list {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 400px);
}

.notebook-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.notebook-item:hover {
  background: rgba(139, 92, 246, 0.1);
  border-color: rgba(139, 92, 246, 0.2);
  transform: translateX(4px);
}

.notebook-item.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2) 0%, rgba(59, 130, 246, 0.2) 100%);
  border-color: rgba(139, 92, 246, 0.3);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.2);
}

.notebook-info {
  flex: 1;
}

.notebook-name {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
  font-size: 14px;
}

.notebook-count {
  font-size: 12px;
  color: #64748b;
}

.notebook-menu {
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.notebook-menu:hover {
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.1);
}

.note-list {
  width: 320px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.1);
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
  flex-shrink: 0;
  overflow: visible;
  display: flex;
  flex-direction: column;
  position: relative;
  max-height: calc(100vh - 200px);
}

.note-list.collapsed {
  width: 60px;
}

.note-list-toggle {
  position: absolute;
  top: 20px;
  right: -25px;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  z-index: 200;
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
  transition: all 0.3s ease;
  border: 2px solid rgba(255, 255, 255, 0.9);
}

.note-list-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.note-list-header {
  padding: 20px;
  border-bottom: 1px solid rgba(139, 92, 246, 0.1);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.note-count {
  color: #64748b;
  font-size: 12px;
  background: rgba(139, 92, 246, 0.1);
  padding: 4px 8px;
  border-radius: 8px;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.note-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-height: calc(100vh - 400px);
}

.note-item {
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.5);
}

.note-item:hover {
  background: rgba(139, 92, 246, 0.1);
  border-color: rgba(139, 92, 246, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.1);
}

.note-item.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2) 0%, rgba(59, 130, 246, 0.2) 100%);
  border-color: rgba(139, 92, 246, 0.3);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.2);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.note-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.pin-icon {
  color: #f59e0b;
  font-size: 12px;
}

.note-time {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

.note-preview {
  color: #64748b;
  font-size: 13px;
  line-height: 1.4;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.note-stats {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;
}

.note-editor {
  flex: 1;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.1);
  backdrop-filter: blur(20px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-header {
  padding: 20px;
  border-bottom: 1px solid rgba(139, 92, 246, 0.1);
  flex-shrink: 0;
}

.title-input {
  margin-bottom: 16px;
}

.title-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 2px solid rgba(139, 92, 246, 0.1);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.05);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
}

.title-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(139, 92, 246, 0.3);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.1);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 4px rgba(139, 92, 246, 0.1);
}

.editor-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.editor-content {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.editor-content :deep(.v-md-editor) {
  border-radius: 12px;
  border: 2px solid rgba(139, 92, 246, 0.1);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.05);
  overflow: hidden;
}

.editor-footer {
  padding: 20px;
  border-top: 1px solid rgba(139, 92, 246, 0.1);
  flex-shrink: 0;
}

.tags-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.note-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #64748b;
}

.empty-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.1);
  backdrop-filter: blur(20px);
  color: #64748b;
  text-align: center;
}

.empty-editor .el-icon {
  color: #94a3b8;
  margin-bottom: 16px;
}

.empty-editor p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  color: #64748b;
  padding: 40px;
}

.empty-state .el-icon {
  color: #94a3b8;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 8px 0;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .notebook-container {
    gap: 16px;
    padding: 16px 25px;
  }

  .sidebar {
    width: 240px;
  }

  .note-list {
    width: 280px;
  }
}

@media (max-width: 768px) {
  .top-nav {
    flex-direction: column;
    gap: 12px;
    padding: 12px;
  }

  .tab-container {
    margin: 10px;
  }

  .notebook-container {
    flex-direction: column;
    gap: 12px;
    padding: 12px 20px;
  }

  .sidebar,
  .note-list {
    width: 100%;
    height: 200px;
    position: relative;
    overflow: visible;
  }

  .sidebar-toggle,
  .note-list-toggle {
    right: -20px;
    z-index: 300;
  }

  .note-editor,
  .empty-editor {
    min-height: 300px;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notebook-item, .note-item {
  animation: fadeIn 0.3s ease;
}

/* 按钮悬停效果 */
.el-button {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style> 