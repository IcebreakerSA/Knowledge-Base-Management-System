<template>
  <div class="admin-container">
    <!-- 顶部导航 -->
    <div class="top-nav">
      <div class="nav-left">
        <el-button @click="router.push('/notebook')" :icon="ArrowLeft" text>
          返回笔记本
        </el-button>
        <h2>管理后台</h2>
      </div>
      <div class="nav-right">
        <div class="user-info-nav">
          <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
            {{ userStore.userInfo?.username?.charAt(0) }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.phone }}</span>
        </div>
      </div>
    </div>

    <!-- Tab内容 -->
    <div class="tab-container">
      <el-tabs v-model="activeTab">
        <!-- 仪表盘 -->
        <el-tab-pane label="仪表盘" name="dashboard">
          <div class="dashboard">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card class="stat-card total">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="48"><User /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.totalUsers }}</div>
                      <div class="stat-label">总用户数</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="stat-card enabled">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="48"><CircleCheck /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.enabledUsers }}</div>
                      <div class="stat-label">启用用户</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card class="stat-card disabled">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="48"><CircleClose /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.disabledUsers }}</div>
                      <div class="stat-label">禁用用户</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20" style="margin-top: 20px">
              <el-col :span="6">
                <el-card class="stat-card notes">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="40"><Document /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.totalNotes }}</div>
                      <div class="stat-label">总笔记数(全局)</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card notebooks">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="40"><Notebook /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.totalNotebooks }}</div>
                      <div class="stat-label">总笔记本数(全局)</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card kb">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="40"><Share /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.totalKnowledgeBases }}</div>
                      <div class="stat-label">知识库数</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card recent">
                  <div class="stat-content">
                    <div class="stat-icon"><el-icon :size="40"><Plus /></el-icon></div>
                    <div class="stat-info">
                      <div class="stat-value">{{ dashboard.recentRegistrations }}</div>
                      <div class="stat-label">7日新增用户</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <div class="search-bar">
            <el-input v-model="userSearch.keyword" placeholder="搜索用户名或手机号..."
              :prefix-icon="Search" clearable style="width: 240px; margin-right: 12px"
              @keyup.enter="loadUsers" />
            <el-select v-model="userSearch.roleType" placeholder="角色筛选" clearable style="width: 140px; margin-right: 12px">
              <el-option label="管理员" :value="1" />
              <el-option label="普通用户" :value="2" />
            </el-select>
            <el-select v-model="userSearch.status" placeholder="状态筛选" clearable style="width: 140px; margin-right: 12px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadUsers" :icon="Search">搜索</el-button>
            <el-button @click="resetUserSearch">重置</el-button>
          </div>

          <el-table :data="userTableData" v-loading="userLoading" border stripe
            style="width: 100%; margin-top: 16px">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column prop="roleType" label="角色" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.roleType === 1 ? 'danger' : 'info'" size="small">
                  {{ row.roleType === 1 ? '管理员' : '普通用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="lastLoginTime" label="最后登录" min-width="170">
              <template #default="{ row }">{{ row.lastLoginTime ? formatDateTime(row.lastLoginTime) : '从未登录' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="handleEditUser(row)" :icon="Edit">编辑</el-button>
                <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'"
                  @click="handleToggleUserStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button v-if="row.roleType !== 1" size="small" type="danger"
                  @click="handleDeleteUser(row)" :icon="Delete">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="userPage.current" v-model:page-size="userPage.size"
              :page-sizes="[10, 20, 50, 100]" :total="userPage.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadUsers" @current-change="loadUsers" />
          </div>
        </el-tab-pane>

        <!-- 知识库管理 -->
        <el-tab-pane label="知识库管理" name="knowledgeBases">
          <div class="search-bar">
            <el-input v-model="kbSearch.keyword" placeholder="搜索知识库名称..."
              :prefix-icon="Search" clearable style="width: 240px; margin-right: 12px"
              @keyup.enter="loadKnowledgeBases" />
            <el-select v-model="kbSearch.status" placeholder="状态筛选" clearable style="width: 140px; margin-right: 12px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadKnowledgeBases" :icon="Search">搜索</el-button>
            <el-button @click="resetKbSearch">重置</el-button>
          </div>

          <el-table :data="kbTableData" v-loading="kbLoading" border stripe
            style="width: 100%; margin-top: 16px">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="name" label="知识库名称" min-width="150" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="creatorId" label="创建者ID" width="90" align="center" />
            <el-table-column prop="memberCount" label="成员数" width="80" align="center" />
            <el-table-column prop="fileCount" label="文件数" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="info" @click="handleViewMembers(row)" :icon="User">成员</el-button>
                <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'"
                  @click="handleToggleKbStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDeleteKnowledgeBase(row)" :icon="Delete">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="kbPage.current" v-model:page-size="kbPage.size"
              :page-sizes="[10, 20, 50]" :total="kbPage.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadKnowledgeBases" @current-change="loadKnowledgeBases" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.roleType" style="width: 100%">
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 知识库成员对话框 -->
    <el-dialog v-model="memberDialogVisible" title="知识库成员管理" width="700px">
      <div v-if="currentKb" style="margin-bottom: 16px">
        <strong>{{ currentKb.name }}</strong> - 成员列表
      </div>

      <!-- 添加成员 -->
      <div style="display: flex; gap: 12px; margin-bottom: 16px">
        <el-input v-model="addMemberForm.userId" placeholder="输入用户ID" style="width: 160px" />
        <el-button type="primary" @click="handleAddMember">添加成员</el-button>
      </div>

      <el-table :data="memberTableData" v-loading="memberLoading" border stripe>
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinTime" label="加入时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.joinTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="row.role !== 1" size="small" type="danger"
              @click="handleRemoveMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, User, CircleCheck, CircleClose, Document, Notebook,
  Share, Plus, Search, Edit, Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store'
import {
  getAdminDashboard,
  getAdminUsers,
  updateUserStatus,
  deleteAdminUser,
  updateAdminUser,
  getAdminKnowledgeBases,
  updateKnowledgeBaseStatus,
  deleteAdminKnowledgeBase,
  getAdminKnowledgeBaseMembers,
  removeKnowledgeBaseMember,
  addKnowledgeBaseMember
} from '@/api'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('dashboard')

// ============ 仪表盘 ============
const dashboard = reactive({
  totalUsers: 0, enabledUsers: 0, disabledUsers: 0,
  totalNotes: 0, totalNotebooks: 0, totalKnowledgeBases: 0, recentRegistrations: 0
})

const loadDashboard = async () => {
  try {
    const res = await getAdminDashboard()
    if (res.data.code === 0) Object.assign(dashboard, res.data.data)
  } catch (e) {
    ElMessage.error('加载仪表盘失败')
  }
}

// ============ 用户管理 ============
const userSearch = reactive({ keyword: '', roleType: null, status: null })
const userLoading = ref(false)
const userTableData = ref([])
const userPage = reactive({ current: 1, size: 10, total: 0 })

const loadUsers = async () => {
  userLoading.value = true
  try {
    const params = { page: userPage.current, size: userPage.size }
    if (userSearch.roleType != null && userSearch.roleType !== '') params.roleType = userSearch.roleType
    if (userSearch.status != null && userSearch.status !== '') params.status = userSearch.status
    if (userSearch.keyword) params.keyword = userSearch.keyword

    const res = await getAdminUsers(params)
    if (res.data.code === 0) {
      userTableData.value = res.data.data.data
      userPage.total = res.data.data.total
    }
  } catch (e) {
    ElMessage.error('加载用户列表失败')
  } finally {
    userLoading.value = false
  }
}

const resetUserSearch = () => {
  userSearch.keyword = ''
  userSearch.roleType = null
  userSearch.status = null
  userPage.current = 1
  loadUsers()
}

const handleEditUser = (row) => {
  editForm.id = row.id
  editForm.username = row.username
  editForm.phone = row.phone
  editForm.roleType = row.roleType
  editForm.status = row.status
  editDialogVisible.value = true
}

const editDialogVisible = ref(false)
const editForm = reactive({ id: null, username: '', phone: '', roleType: 2, status: 1 })

const handleSaveEdit = async () => {
  try {
    await updateAdminUser(editForm.id, {
      username: editForm.username, phone: editForm.phone,
      roleType: editForm.roleType, status: editForm.status
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadUsers()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleToggleUserStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户 "${row.username}"？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`${action}失败`)
  }
}

const handleDeleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除用户 "${row.username}" (${row.phone})？此操作不可恢复！`,
      '危险操作', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
    )
    await deleteAdminUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
    loadDashboard()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ============ 知识库管理 ============
const kbSearch = reactive({ keyword: '', status: null })
const kbLoading = ref(false)
const kbTableData = ref([])
const kbPage = reactive({ current: 1, size: 10, total: 0 })

const loadKnowledgeBases = async () => {
  kbLoading.value = true
  try {
    const params = { page: kbPage.current, size: kbPage.size }
    if (kbSearch.status != null && kbSearch.status !== '') params.status = kbSearch.status
    if (kbSearch.keyword) params.keyword = kbSearch.keyword

    const res = await getAdminKnowledgeBases(params)
    if (res.data.code === 0) {
      kbTableData.value = res.data.data.data
      kbPage.total = res.data.data.total
    }
  } catch (e) {
    ElMessage.error('加载知识库列表失败')
  } finally {
    kbLoading.value = false
  }
}

const resetKbSearch = () => {
  kbSearch.keyword = ''
  kbSearch.status = null
  kbPage.current = 1
  loadKnowledgeBases()
}

const handleToggleKbStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}知识库 "${row.name}"？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await updateKnowledgeBaseStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadKnowledgeBases()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`${action}失败`)
  }
}

const handleDeleteKnowledgeBase = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除知识库 "${row.name}"？此操作将删除所有成员和文件关联，不可恢复！`,
      '危险操作', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
    )
    await deleteAdminKnowledgeBase(row.id)
    ElMessage.success('删除成功')
    loadKnowledgeBases()
    loadDashboard()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ============ 成员管理 ============
const memberDialogVisible = ref(false)
const memberLoading = ref(false)
const memberTableData = ref([])
const currentKb = ref(null)
const addMemberForm = reactive({ userId: '' })

const handleViewMembers = async (kb) => {
  currentKb.value = kb
  memberDialogVisible.value = true
  await loadMembers()
}

const loadMembers = async () => {
  if (!currentKb.value) return
  memberLoading.value = true
  try {
    const res = await getAdminKnowledgeBaseMembers(currentKb.value.id)
    if (res.data.code === 0) {
      memberTableData.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('加载成员列表失败')
  } finally {
    memberLoading.value = false
  }
}

const handleAddMember = async () => {
  const uid = parseInt(addMemberForm.userId)
  if (!uid) { ElMessage.warning('请输入有效的用户ID'); return }
  try {
    await addKnowledgeBaseMember(currentKb.value.id, uid)
    ElMessage.success('添加成功')
    addMemberForm.userId = ''
    loadMembers()
  } catch (e) {
    ElMessage.error('添加失败，请检查用户ID是否有效且未加入该知识库')
  }
}

const handleRemoveMember = async (row) => {
  try {
    await ElMessageBox.confirm(`确认移除成员 "${row.username}"？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await removeKnowledgeBaseMember(currentKb.value.id, row.userId)
    ElMessage.success('移除成功')
    loadMembers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('移除失败')
  }
}

// ============ 工具函数 ============
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

onMounted(() => {
  loadDashboard()
  loadUsers()
  loadKnowledgeBases()
})
</script>

<style scoped>
.admin-container { height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }
.top-nav { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; height: 60px; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); z-index: 10; }
.nav-left { display: flex; align-items: center; gap: 16px; }
.nav-left h2 { font-size: 18px; font-weight: 600; color: #303133; }
.nav-right { display: flex; align-items: center; }
.user-info-nav { display: flex; align-items: center; gap: 8px; }
.username { font-size: 14px; color: #606266; }
.tab-container { flex: 1; overflow: auto; padding: 20px 24px; }
.tab-container :deep(.el-tabs) { background: #fff; border-radius: 8px; padding: 8px 20px 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.dashboard { padding: 10px 0; }
.stat-card { cursor: default; border-radius: 10px; transition: transform 0.2s, box-shadow 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.stat-content { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 64px; height: 64px; display: flex; align-items: center; justify-content: center; border-radius: 12px; }
.stat-card.total .stat-icon { background: #e8f4fd; color: #409eff; }
.stat-card.enabled .stat-icon { background: #e8f8e8; color: #67c23a; }
.stat-card.disabled .stat-icon { background: #fde8e8; color: #f56c6c; }
.stat-card.notes .stat-icon { background: #fdf6ec; color: #e6a23c; }
.stat-card.notebooks .stat-icon { background: #f4e8fd; color: #9c27b0; }
.stat-card.kb .stat-icon { background: #e8faf0; color: #26a69a; }
.stat-card.recent .stat-icon { background: #e8f0fd; color: #5c6bc0; }
.stat-info { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.search-bar { display: flex; align-items: center; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
