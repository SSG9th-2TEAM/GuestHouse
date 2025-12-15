<script setup>
defineProps({
  title: String,
  location: String,
  price: Number,
  imageUrl: String
})
</script>

<template>
  <article class="card">
    <div class="image-container">
      <img :src="imageUrl" :alt="title" class="card-image" />
      <button class="favorite-btn">♡</button>
    </div>
    <div class="card-content">
      <div class="header-row">
        <h3 class="title">{{ title }}</h3>
        <span class="rating">★ 4.8</span>
      </div>
      <p class="location">{{ location }}</p>
      <div class="footer-row">
        <span class="price">
          <strong>₩{{ price.toLocaleString() }}</strong> / 박
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
  height: 200px;
  background: #e5e7eb;
  position: relative;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
}

.card-content {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 0.25rem;
}

.location {
  font-size: 0.875rem;
  color: var(--text-sub);
}

.footer-row {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.75rem;
}

.book-btn {
  background-color: var(--primary);
  color: #004d40;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.875rem;
  transition: opacity 0.2s;
}

.book-btn:hover {
  opacity: 0.9;
}
</style>
