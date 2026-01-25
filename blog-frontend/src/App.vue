<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- 这里是整个应用的出口，路由匹配到的组件（如 HomeView）会显示在这里 -->
  <RouterView />
</template>

<style scoped>
/* App.vue 不需要额外的全局样式，因为我们已经在 main.css 定义了 */
</style>