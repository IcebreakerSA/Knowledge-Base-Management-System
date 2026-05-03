import { defineStore } from 'pinia'
import { getUserInfo } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: localStorage.getItem('token') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo
    },
    async fetchUserInfo() {
      try {
        const response = await getUserInfo()
        const data = response.data
        this.setUserInfo(data.data)
        return data.data
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.logout()
        throw error
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})

export const useNotebookStore = defineStore('notebook', {
  state: () => ({
    notebooks: [],
    currentNotebook: null,
    notes: [],
    currentNote: null,
    tags: []
  }),
  actions: {
    setNotebooks(notebooks) {
      this.notebooks = notebooks
    },
    setCurrentNotebook(notebook) {
      this.currentNotebook = notebook
    },
    setNotes(notes) {
      this.notes = notes
    },
    setCurrentNote(note) {
      this.currentNote = note
    },
    setTags(tags) {
      this.tags = tags
    },
    addNotebook(notebook) {
      this.notebooks.push(notebook)
    },
    updateNotebook(id, notebook) {
      const index = this.notebooks.findIndex(n => n.id === id)
      if (index !== -1) {
        this.notebooks[index] = { ...this.notebooks[index], ...notebook }
      }
    },
    removeNotebook(id) {
      this.notebooks = this.notebooks.filter(n => n.id !== id)
    },
    addNote(note) {
      this.notes.unshift(note)
    },
    updateNote(id, note) {
      const index = this.notes.findIndex(n => n.id === id)
      if (index !== -1) {
        this.notes[index] = { ...this.notes[index], ...note }
      }
      if (this.currentNote && this.currentNote.id === id) {
        this.currentNote = { ...this.currentNote, ...note }
      }
    },
    removeNote(id) {
      this.notes = this.notes.filter(n => n.id !== id)
      if (this.currentNote && this.currentNote.id === id) {
        this.currentNote = null
      }
    }
  }
}) 