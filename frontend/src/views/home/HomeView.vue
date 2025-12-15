<script setup>
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import { useRouter } from 'vue-router'
import { guesthouses } from '../../data/guesthouses'

const router = useRouter()
const items = guesthouses
</script>

<template>
  <main class="container main-content">
    <div class="bannner">
      <h1>여행지에서 느끼는<br />특별한 하루</h1>
    </div>

    <div class="card-grid">
      <GuesthouseCard 
        v-for="item in items" 
        :key="item.id"
        :title="item.title"
        :location="item.location"
        :price="item.price"
        :image-url="item.imageUrl"
        @click="router.push(`/room/${item.id}`)"
        style="cursor: pointer;"
      />
    </div>
  </main>
</template>

<style scoped>
.main-content {
  padding-top: 2rem;
  padding-bottom: 4rem;
}

.bannner {
  margin-bottom: 2rem;
  padding: 3rem 0;
}

.bannner h1 {
  font-size: 2.5rem;
  font-weight: 800;
  line-height: 1.2;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 2rem;
}
</style>
