<script setup>
// 主应用组件
</script>

<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1e293b;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 10%, #f093fb 50%, #f5576c 90%, #4facfe 100%);
  display: flex;
  flex-direction: column;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  max-height: 100vh;
  overflow: hidden;
}

body {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 10%, #f093fb 50%, #f5576c 90%, #4facfe 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  line-height: 1.6;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #2563eb 100%);
}
</style>
