<script setup>
import TheHeader from './components/TheHeader.vue'
import TheFooter from './components/TheFooter.vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-layout">
    <TheHeader />
    <RouterView />
    <TheFooter />
  </div>
</template>

<style>
@import './assets/style.css';
</style>
