<template>
  <div class="forum-layout app-wrapper">
    <ForumHeader />
    <router-view />
    <ForumFooter />
  </div>
</template>

<script>
import ForumHeader from './ForumHeader.vue'
import ForumFooter from './ForumFooter.vue'

export default {
  name: 'ForumLayout',
  components: {
    ForumHeader,
    ForumFooter
  }
}
</script>

<style scoped>
.forum-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--bg-body, #eceef1);
}
</style>
