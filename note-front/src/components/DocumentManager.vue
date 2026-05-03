<template>
  <div class="document-manager">
    <div class="document-header">
      <h3>个人知识库</h3>
      <div class="header-actions">
        <el-input
            v-model="searchForm.keyword"
            placeholder="搜索文档..."
            size="default"
            clearable
            class="search-input"
            prefix-icon="Search"
            @change="searchDocuments"
        />
        <el-select
            v-model="searchForm.fileType"
            placeholder="文件类型"
            size="default"
            clearable
            class="type-select"
            @change="searchDocuments"
        >
          <el-option label="PDF" value="pdf" />
          <el-option label="Word" value="docx" />
          <el-option label="文本" value="txt" />
        </el-select>

        <!-- 新增的刷新按钮 -->
        <el-button
            size="default"
            circle
            :icon="Refresh"
            title="刷新列表"
            @click="loadDocuments"
        />

        <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            accept=".pdf,.docx,.txt"
        >
          <el-button type="primary" size="default" class="upload-btn">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
        </el-upload>
      </div>
    </div>

    <div class="document-content" v-loading="loading">
      <div v-if="documents.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无文档，点击上传按钮添加文档" />
      </div>

      <div v-else class="document-grid">
        <div
            v-for="doc in documents"
            :key="doc.id"
            class="document-card"
            @click="previewDocument(doc)"
        >
          <div class="card-header">
            <div class="file-icon">
              <el-icon size="32" :color="getFileTypeColor(doc.fileType)">
                <Document v-if="doc.fileType === 'pdf'" />
                <Edit v-else-if="doc.fileType === 'docx'" />
                <Memo v-else />
              </el-icon>
            </div>
            <div class="file-type-tag">
              <el-tag
                  :type="getFileTypeTagColor(doc.fileType)"
                  size="small"
                  round
              >
                {{ getFileTypeLabel(doc.mimeType) }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="file-name" :title="doc.originalFilename">
              {{ doc.originalFilename }}
            </div>
            <div class="file-meta">
              <span class="file-size">{{ formatFileSize(null, null, doc.fileSize) }}</span>
              <span class="file-date">{{ formatTime(null, null, doc.createTime) }}</span>
            </div>
          </div>

          <div class="card-actions">
            <el-button
                type="primary"
                size="small"
                text
                @click.stop="previewDocument(doc)"
            >
              预览
            </el-button>
            <el-button
                type="danger"
                size="small"
                text
                @click.stop="deleteDocument(doc)"
            >
              删除
            </el-button>
          </div>

          <div class="status-indicator">
            <el-tag
                :type="doc.processingStatus === 'SUCCESS' ? 'success' : 'warning'"
                size="small"
                round
            >
              {{ doc.processingStatus === 'SUCCESS' ? '已处理' : '已处理' }}
            </el-tag>
          </div>
        </div>
      </div>

      <div v-if="documents.length > 0" class="pagination-container">
        <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 文档预览对话框 -->
    <el-dialog
        v-model="previewDialog.visible"
        :title="previewDialog.title"
        width="80%"
        center
    >
      <div class="preview-content">
        <iframe
            v-if="previewDialog.fileUrl"
            :src="previewDialog.fileUrl"
            style="width: 100%; height: 500px; border: none; border-radius: 8px;"
        />
        <div v-else class="preview-placeholder">
          <el-empty description="文档预览暂不可用" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store'
import { getDocumentList, deleteDocument as deleteDocumentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入 Refresh 图标
import { Document, Edit, Memo, Upload, Search, Refresh } from '@element-plus/icons-vue'

const userStore = useUserStore()

// 数据
const documents = ref([])
const loading = ref(false)
const uploadUrl = ref('/api/documents/upload')
const uploadHeaders = ref({
  'Authorization': `Bearer ${userStore.token}`
})

// 搜索表单
const searchForm = reactive({
  keyword: '',
  fileType: '',
  sortBy: 'create_time',
  sortOrder: 'desc'
})

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 预览对话框
const previewDialog = reactive({
  visible: false,
  title: '',
  fileUrl: ''
})

// 获取文档列表
const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await getDocumentList({
      page: pagination.current,
      size: pagination.size,
      keyword: searchForm.keyword,
      fileType: searchForm.fileType,
      sortBy: searchForm.sortBy,
      sortOrder: searchForm.sortOrder
    })

    console.log('API响应数据:', response.data) // 调试信息

    // 处理不同的响应格式
    let responseData = response.data
    let documentList = []

    // 如果响应数据有code字段
    if (responseData.code !== undefined) {
      if (responseData.code === 0) {
        // 标准格式：{ code: 0, data: [...], message: "" }
        if (Array.isArray(responseData.data)) {
          documentList = responseData.data
          pagination.total = responseData.data.length
        } else if (responseData.data && Array.isArray(responseData.data.data)) {
          documentList = responseData.data.data
          pagination.total = responseData.data.total || responseData.data.data.length
        } else if (responseData.data && Array.isArray(responseData.data.records)) {
          documentList = responseData.data.records
          pagination.total = responseData.data.total || responseData.data.records.length
        } else if (responseData.data) {
          // 单个对象
          documentList = [responseData.data]
          pagination.total = 1
        }
      } else {
        ElMessage.error(responseData.message || '获取文档列表失败')
        return
      }
    } else {
      // 没有code字段，直接处理数据
      if (Array.isArray(responseData)) {
        // 直接是数组
        documentList = responseData
        pagination.total = responseData.length
      } else if (responseData && typeof responseData === 'object') {
        // 单个对象
        documentList = [responseData]
        pagination.total = 1
      }
    }

    // 处理文档数据，统一格式
    documents.value = documentList.map(doc => ({
      ...doc,
      // 处理文件类型映射
      fileType: getFileTypeFromExtension(doc.extension || doc.fileType),
      // 处理时间格式
      createTime: doc.createTime || doc.updateTime || new Date().toISOString(),
      updateTime: doc.updateTime || doc.createTime || new Date().toISOString()
    }))

    console.log('处理后的文档列表:', documents.value) // 调试信息
  } catch (error) {
    console.error('加载文档列表失败:', error)
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索文档
const searchDocuments = () => {
  pagination.current = 1
  loadDocuments()
}

// 上传前检查
const beforeUpload = (file) => {
  const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain']
  const isAllowed = allowedTypes.includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isAllowed) {
    ElMessage.error('只支持 PDF、Word 和文本文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response) => {
  console.log('上传响应:', response) // 调试信息
  if (response && (response.code === 0 || response.code === undefined)) {
    ElMessage.success('文档上传成功')
    loadDocuments()
  } else {
    ElMessage.error((response && response.message) || '上传失败')
  }
}

// 上传失败
const handleUploadError = (error) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败')
}

// 删除文档
const deleteDocument = async (row) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除文档 "${row.originalFilename}" 吗？`,
        '确认删除',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const response = await deleteDocumentApi(row.id)
    console.log('删除响应:', response.data) // 调试信息
    if (response.data && (response.data.code === 0 || response.data.code === undefined)) {
      ElMessage.success('删除成功')
      loadDocuments()
    } else {
      ElMessage.error((response.data && response.data.message) || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 预览文档
const previewDocument = (row) => {
  previewDialog.visible = true
  previewDialog.title = row.originalFilename
  previewDialog.fileUrl = row.fileUrl
}

// 格式化文件大小
const formatFileSize = (row, column, cellValue) => {
  if (cellValue === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(cellValue) / Math.log(k))
  return parseFloat((cellValue / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化时间
const formatTime = (row, column, cellValue) => {
  if (!cellValue) return '未知时间'

  try {
    const date = new Date(cellValue)
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      return '未知时间'
    }
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    console.error('时间格式化失败:', error)
    return '未知时间'
  }
}

// 根据文件扩展名获取文件类型
const getFileTypeFromExtension = (extension) => {
  if (!extension) return 'other'

  const ext = extension.toLowerCase().replace('.', '')
  const typeMap = {
    'pdf': 'pdf',
    'txt': 'txt',
    'docx': 'docx',
    'doc': 'docx',
    'other': 'txt' // 将OTHER映射为txt
  }

  return typeMap[ext] || 'other'
}

// 获取文件类型标签颜色
const getFileTypeTagColor = (fileType) => {
  const colorMap = {
    'pdf': 'danger',
    'docx': 'primary',
    'txt': 'info',
    'other': 'info'
  }
  return colorMap[fileType] || 'info'
}

// 获取文件类型标签文本
const getFileTypeLabel = (fileType) => {
  const labelMap = {
    'application/pdf': 'PDF',
    'application/docx': 'WORD',
    'text/plain': '文本',
    'other': '其他'
  }
  return labelMap[fileType] || '其他'
}

// 获取文件类型颜色
const getFileTypeColor = (fileType) => {
  const colorMap = {
    'pdf': '#f56565',
    'docx': '#4299e1',
    'txt': '#48bb78',
    'other': '#9f7aea'
  }
  return colorMap[fileType] || '#9f7aea'
}

// 排序处理
const handleSortChange = ({ column, prop, order }) => {
  searchForm.sortBy = prop
  searchForm.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  loadDocuments()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadDocuments()
}

const handleCurrentChange = (page) => {
  pagination.current = page
  loadDocuments()
}

// 测试API连接
const testApi = async () => {
  try {
    console.log('开始测试API连接...')

    // 直接测试API调用
    const response = await fetch('/api/documents/files/list', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify({
        page: 1,
        size: 10
      })
    })

    console.log('Fetch响应状态:', response.status)
    console.log('Fetch响应头:', response.headers)

    const data = await response.json()
    console.log('Fetch响应数据:', data)

  } catch (error) {
    console.error('Fetch测试失败:', error)
  }
}

// 生命周期
onMounted(() => {
  console.log('DocumentManager mounted, token:', userStore.token)
  testApi() // 先测试API
  loadDocuments()
})
</script>

<style scoped>
.document-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 20%, #f093fb 60%, #f5576c 90%, #4facfe 100%);
  border-radius: 24px;
  overflow: hidden;
  margin: 20px;
  box-shadow: 0 20px 60px rgba(139, 92, 246, 0.2);
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border-bottom: 1px solid rgba(139, 92, 246, 0.1);
  box-shadow: 0 10px 40px rgba(139, 92, 246, 0.1);
  backdrop-filter: blur(20px);
}

.document-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-input {
  width: 240px;
}

.type-select {
  width: 140px;
}

.upload-btn {
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(64, 153, 255, 0.2);
}

.upload-btn:hover {
  box-shadow: 0 4px 8px rgba(64, 153, 255, 0.3);
}

.document-content {
  flex: 1;
  padding: 30px;
  overflow: auto;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.9) 100%);
  backdrop-filter: blur(20px);
}

.empty-state {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 30px;
  padding: 0;
}

.document-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border: 1px solid rgba(139, 92, 246, 0.1);
  border-radius: 24px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 40px rgba(139, 92, 246, 0.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

.document-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: rgba(64, 153, 255, 0.1);
}

.file-type-tag {
  position: absolute;
  top: 12px;
  right: 12px;
}

.card-body {
  margin-bottom: 16px;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.file-size {
  font-weight: 500;
}

.file-date {
  color: #c0c4cc;
}

.card-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.document-card:hover .card-actions {
  opacity: 1;
}

.status-indicator {
  position: absolute;
  bottom: 12px;
  right: 12px;
}

.pagination-container {
  padding: 24px;
  display: flex;
  justify-content: center;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.preview-content {
  border-radius: 8px;
  overflow: hidden;
}

.preview-placeholder {
  height: 500px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
  border-radius: 8px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .document-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .document-header {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .search-input {
    width: 200px;
  }

  .type-select {
    width: 120px;
  }

  .document-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
  }

  .document-content {
    padding: 16px;
  }
}
</style>
