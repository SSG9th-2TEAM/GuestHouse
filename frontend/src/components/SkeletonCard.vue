<script setup>
// 스켈레톤 카드 컴포넌트 - 로딩 중 표시
</script>

<template>
  <div class="skeleton-card">
    <div class="skeleton-image shimmer"></div>
    <div class="skeleton-content">
      <div class="skeleton-title shimmer"></div>
      <div class="skeleton-location shimmer"></div>
      <div class="skeleton-rating shimmer"></div>
      <div class="skeleton-price shimmer"></div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-card {
  flex: 0 0 var(--card-width, 280px);
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.skeleton-image {
  width: 100%;
  height: 200px;
  background: #e5e7eb;
  border-radius: 16px 16px 0 0;
}

.skeleton-content {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.skeleton-title {
  height: 1.25rem;
  width: 70%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-location {
  height: 0.875rem;
  width: 50%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-rating {
  height: 0.875rem;
  width: 30%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-price {
  height: 1.125rem;
  width: 40%;
  background: #e5e7eb;
  border-radius: 4px;
  margin-top: 0.5rem;
}

/* 쉬머 애니메이션 효과 */
.shimmer {
  position: relative;
  overflow: hidden;
}

.shimmer::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 100%
  );
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
