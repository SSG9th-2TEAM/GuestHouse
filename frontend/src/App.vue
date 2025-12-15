<script setup>
import AppHeader from "./components/common/AppHeader.vue";
import AppFooter from "./components/common/AppFooter.vue";
</script>

<template>
  <AppHeader />
  <main>
    <div class="content-wrapper">
      <RouterView />
    </div>
  </main>
  <AppFooter />
</template>

<style scoped>
main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.content-wrapper {
  flex: 1;
}
</style>
