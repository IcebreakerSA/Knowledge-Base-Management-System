import axios from 'axios'
import { ElMessage } from 'element-plus'

// 模块级token，由store设置
let currentToken = ''

export function setRequestToken(token) {
  currentToken = token || ''
}

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    if (currentToken) {
      config.headers.Authorization = `Bearer ${currentToken}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('API响应:', response) // 调试信息
    const res = response.data

    // 如果响应没有code字段，直接返回整个response对象
    if (res.code === undefined) {
      return response
    }

    // 如果有code字段且为0，返回整个response对象
    if (res.code === 0) {
      return response
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('网络错误详情:', error) // 调试信息
    ElMessage.error('网络错误: ' + (error.message || '未知错误'))
    return Promise.reject(error)
  }
)

export default request
