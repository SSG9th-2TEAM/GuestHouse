<script setup>
import TheHeader from './components/TheHeader.vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-layout">
    <TheHeader />
    <RouterView />
  </div>
</template>

<style>
@import './assets/style.css';
</style>
