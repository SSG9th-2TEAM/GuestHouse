<script setup>
import TheHeader from './components/TheHeader.vue'
import TheFooter from './components/TheFooter.vue'
import ChatbotWidget from './components/chatbot/ChatbotWidget.vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-layout">
    <TheHeader />
    <RouterView />
    <TheFooter />
    <ChatbotWidget />
  </div>
</template>

<style>
@import './assets/style.css';
</style>
