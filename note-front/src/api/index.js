import request from './request'

// 获取验证码
export const getCaptcha = () => {
  return request({
    url: '/common/captcha',
    method: 'get'
  })
}

// 注册
export const register = (data) => {
  return request({
    url: '/common/register',
    method: 'post',
    data
  })
}

// 登录
export const login = (data) => {
  return request({
    url: '/common/login',
    method: 'post',
    data
  })
}

// 获取用户信息
export const getUserInfo = () => {
  return request({
    url: '/common/user/info',
    method: 'get'
  })
}

// 更新用户个人信息
export const updateUserProfile = (data) => {
  return request({
    url: '/common/user/update',
    method: 'put',
    data
  })
}

// 退出登录
export const logout = () => {
  return request({
    url: '/common/logout',
    method: 'post'
  })
}

// 文件上传
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/common/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 笔记本管理
export const createNotebook = (data) => {
  return request({
    url: '/notebooks',
    method: 'post',
    data
  })
}

export const getNotebooks = () => {
  return request({
    url: '/notebooks',
    method: 'get'
  })
}

export const getNotebook = (id) => {
  return request({
    url: `/notebooks/${id}`,
    method: 'get'
  })
}

export const updateNotebook = (id, data) => {
  return request({
    url: `/notebooks/${id}`,
    method: 'put',
    data
  })
}

export const deleteNotebook = (id) => {
  return request({
    url: `/notebooks/${id}`,
    method: 'delete'
  })
}

// 笔记管理
export const createNote = (data) => {
  return request({
    url: '/notes',
    method: 'post',
    data
  })
}

export const getNotes = (params) => {
  return request({
    url: '/notes',
    method: 'get',
    params
  })
}

export const getNote = (id) => {
  return request({
    url: `/notes/${id}`,
    method: 'get'
  })
}

export const updateNote = (data) => {
  return request({
    url: `/notes/update`,
    method: 'post',
    data
  })
}

export const deleteNote = (id) => {
  return request({
    url: `/notes/${id}`,
    method: 'delete'
  })
}

export const toggleNotePin = (id) => {
  return request({
    url: `/notes/${id}/pin`,
    method: 'post'
  })
}

export const autoSaveNote = (data) => {
  return request({
    url: '/notes/autosave',
    method: 'post',
    data
  })
}

export const getTags = () => {
  return request({
    url: '/notes/tags',
    method: 'get'
  })
}

export const addNoteToKnowledgeBase = (noteId) => {
  return request({
    url: `/notes/${noteId}/add-to-knowledge-base`,
    method: 'post'
  })
}

// 文档管理接口
export const uploadDocument = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/documents/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getDocumentList = (data) => {
  return request({
    url: '/documents/files/list',
    method: 'post',
    data
  })
}

export const deleteDocument = (fileId) => {
  return request({
    url: `/documents/files/${fileId}`,
    method: 'delete'
  })
}

// 聊天对话接口
export const chatStream = (data) => {
  return request({
    url: '/chat/stream',
    method: 'post',
    data,
    headers: {
      'Accept': 'text/event-stream'
    },
    responseType: 'stream'
  })
}

// 共享知识库管理接口
export const createSharedKnowledgeBase = (data) => {
  return request({
    url: '/shared-knowledge-base',
    method: 'post',
    data
  })
}

export const updateSharedKnowledgeBase = (id, data) => {
  return request({
    url: `/shared-knowledge-base/${id}`,
    method: 'put',
    data
  })
}

export const deleteSharedKnowledgeBase = (id) => {
  return request({
    url: `/shared-knowledge-base/${id}`,
    method: 'delete'
  })
}

export const getSharedKnowledgeBaseDetail = (id) => {
  return request({
    url: `/shared-knowledge-base/${id}`,
    method: 'get'
  })
}

export const getMyCreatedKnowledgeBase = () => {
  return request({
    url: '/shared-knowledge-base/my-created',
    method: 'get'
  })
}

export const getMyJoinedKnowledgeBase = () => {
  return request({
    url: '/shared-knowledge-base/my-joined',
    method: 'get'
  })
}

export const getKnowledgeBaseSquare = (params) => {
  return request({
    url: '/shared-knowledge-base/square',
    method: 'get',
    params
  })
}

export const joinKnowledgeBase = (data) => {
  return request({
    url: '/shared-knowledge-base/join',
    method: 'post',
    data
  })
}

export const leaveKnowledgeBase = (id) => {
  return request({
    url: `/shared-knowledge-base/${id}/leave`,
    method: 'post'
  })
}

export const removeMember = (knowledgeBaseId, memberId) => {
  return request({
    url: `/shared-knowledge-base/${knowledgeBaseId}/members/${memberId}`,
    method: 'delete'
  })
}

// 获取知识库成员列表
export const getKnowledgeBaseMembers = (knowledgeBaseId) => {
  return request({
    url: `/shared-knowledge-base/members/${knowledgeBaseId}`,
    method: 'get'
  })
}

// 删除知识库成员
export const deleteKnowledgeBaseMember = (deleteUserId, knowledgeBaseId) => {
  return request({
    url: `/shared-knowledge-base/members/${deleteUserId}/${knowledgeBaseId}`,
    method: 'delete'
  })
}

// 共享知识库文件管理接口
export const uploadToSharedKnowledgeBase = (knowledgeBaseId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/shared-knowledge-base/${knowledgeBaseId}/files/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const copyFilesToSharedKnowledgeBase = (knowledgeBaseId, data) => {
  return request({
    url: `/shared-knowledge-base/${knowledgeBaseId}/files/copy`,
    method: 'post',
    data
  })
}

export const deleteSharedKnowledgeBaseFile = (knowledgeBaseId, fileId) => {
  return request({
    url: `/shared-knowledge-base/${knowledgeBaseId}/files/${fileId}`,
    method: 'delete'
  })
}

export const getSharedKnowledgeBaseFiles = (knowledgeBaseId, params) => {
  return request({
    url: `/shared-knowledge-base/${knowledgeBaseId}/files`,
    method: 'get',
    params
  })
}

export const getPersonalFileList = () => {
  return request({
    url: '/shared-knowledge-base/personal-files',
    method: 'get'
  })
}

// 管理员接口
export const getAdminDashboard = () => {
  return request({
    url: '/admin/dashboard',
    method: 'get'
  })
}

export const getAdminUsers = (params) => {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export const getAdminUserDetail = (id) => {
  return request({
    url: `/admin/users/${id}`,
    method: 'get'
  })
}

export const updateUserStatus = (id, status) => {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

export const updateUserRole = (id, roleType) => {
  return request({
    url: `/admin/users/${id}/role`,
    method: 'put',
    data: { roleType }
  })
}

export const deleteAdminUser = (id) => {
  return request({
    url: `/admin/users/${id}`,
    method: 'delete'
  })
}

export const updateAdminUser = (id, data) => {
  return request({
    url: `/admin/users/${id}`,
    method: 'put',
    data
  })
}

// 管理员知识库管理接口
export const getAdminKnowledgeBases = (params) => {
  return request({
    url: '/admin/knowledge-bases',
    method: 'get',
    params
  })
}

export const updateKnowledgeBaseStatus = (id, status) => {
  return request({
    url: `/admin/knowledge-bases/${id}/status`,
    method: 'put',
    data: { status }
  })
}

export const deleteAdminKnowledgeBase = (id) => {
  return request({
    url: `/admin/knowledge-bases/${id}`,
    method: 'delete'
  })
}

export const getAdminKnowledgeBaseMembers = (id) => {
  return request({
    url: `/admin/knowledge-bases/${id}/members`,
    method: 'get'
  })
}

export const removeKnowledgeBaseMember = (kbId, userId) => {
  return request({
    url: `/admin/knowledge-bases/${kbId}/members/${userId}`,
    method: 'delete'
  })
}

export const addKnowledgeBaseMember = (id, userId) => {
  return request({
    url: `/admin/knowledge-bases/${id}/members`,
    method: 'post',
    data: { userId }
  })
}

// 共享知识库AI对话接口
export const sharedKnowledgeBaseChatStream = (data) => {
  return request({
    url: '/shared-knowledge-base/chat/stream',
    method: 'post',
    data,
    headers: {
      'Accept': 'text/event-stream'
    },
    responseType: 'stream',
    timeout: 60000 // 60秒超时，适合流式响应
  })
}
