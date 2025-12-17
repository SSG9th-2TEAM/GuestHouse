<script setup>
import { onMounted, ref } from 'vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { accommodations } from '../../mocks/adminMockData'

const accommodationList = ref([])

const loadAccommodations = () => {
  accommodationList.value = accommodations
}

onMounted(loadAccommodations)

const statusVariant = (status) => {
  if (status === '승인됨') return 'success'
  if (status === '검토중') return 'warning'
  return 'neutral'
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">숙소 관리</h1>
        <p class="admin-subtitle">등록 숙소 상태와 매출을 모니터링합니다.</p>
      </div>
    </header>

    <AdminTableCard title="숙소 목록">
      <table>
        <thead>
          <tr>
            <th>숙소명</th>
            <th>호스트</th>
            <th>위치</th>
            <th>평점</th>
            <th>예약</th>
            <th>총 매출</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in accommodationList" :key="item.name">
            <td>
              <div class="admin-col">
                <span class="admin-strong">{{ item.name }}</span>
                <small class="admin-sub">{{ item.registeredAt }} 등록</small>
              </div>
            </td>
            <td>{{ item.host }}</td>
            <td>{{ item.location }}</td>
            <td>{{ item.rating }}</td>
            <td>{{ item.bookings }}건</td>
            <td>{{ item.revenue }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>
              <button class="admin-btn-ghost" type="button">상세</button>
            </td>
          </tr>
        </tbody>
      </table>
    </AdminTableCard>
  </section>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-page__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.admin-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-subtitle {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-weight: 600;
}

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

.admin-sub {
  color: #6b7280;
}

.admin-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-btn-ghost {
  background: transparent;
  border: 1px solid #d1d5db;
  color: #0f766e;
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 800;
}

.admin-btn-ghost:hover {
  border-color: #0f766e;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
