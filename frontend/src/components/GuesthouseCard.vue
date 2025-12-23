<script setup>
defineProps({
  title: String,
  description: String,
  location: String,
  rating: [Number, String],
  price: Number,
  imageUrl: String
})

const formatRating = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '-'
  return numeric.toFixed(1)
}
</script>

<template>
  <article class="card">
    <div class="image-container">
      <img :src="imageUrl" :alt="title" class="card-image" />
      <button class="favorite-btn">&#9825;</button>
    </div>
    <div class="card-content">
      <div class="header-row">
        <h3 class="title">{{ title }}</h3>
        <span class="rating">&#9733; {{ formatRating(rating) }}</span>
      </div>
      <p v-if="description" class="description">{{ description }}</p>
      <p class="location">{{ location }}</p>
      <div class="footer-row">
        <span class="price">
          <strong>&#8361;{{ price.toLocaleString() }}</strong> / 박
        </span>
        <button class="book-btn">예약하기</button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.card {
  background: var(--bg-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.image-container {
  aspect-ratio: 1 / 1;
  background: #e5e7eb;
  position: relative;
  overflow: hidden;
  width: 100%;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.favorite-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255,255,255,0.8);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  cursor: pointer;
  border: none;
}

.card-content {
  padding: 0.4rem 0.7rem;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  flex: 1;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 0.1rem;
  flex: 1;
  min-width: 0;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.rating {
  flex-shrink: 0;
}

.description {
  font-size: 0.82rem;
  color: var(--text-sub);
  line-height: 1.3;
}

.location {
  font-size: 0.86rem;
  color: var(--text-sub);
}

.footer-row {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.2rem;
}

.book-btn {
  background-color: var(--primary);
  color: #004d40;
  padding: 0.35rem 0.75rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  transition: opacity 0.2s;
  border: none;
}

.book-btn:hover {
  opacity: 0.9;
}
</style>
