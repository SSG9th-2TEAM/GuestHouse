<script setup>
import { onMounted, ref } from 'vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { bookings } from '../../mocks/adminMockData'

const bookingList = ref([])

const loadBookings = () => {
  bookingList.value = bookings
}

onMounted(loadBookings)

const statusVariant = (status) => {
  if (status === '확정' || status === '체크인') return 'success'
  if (status === '대기') return 'warning'
  return 'neutral'
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">예약 관리</h1>
        <p class="admin-subtitle">예약 상태와 체크인 현황을 확인합니다.</p>
      </div>
    </header>

    <AdminTableCard title="예약 목록">
      <table>
        <thead>
          <tr>
            <th>예약번호</th>
            <th>숙소</th>
            <th>호스트</th>
            <th>게스트</th>
            <th>체크인</th>
            <th>체크아웃</th>
            <th>인원</th>
            <th>금액</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in bookingList" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td>{{ item.accommodation }}</td>
            <td>{{ item.host }}</td>
            <td>{{ item.guest }}</td>
            <td>{{ item.checkIn }}</td>
            <td>{{ item.checkOut }}</td>
            <td>{{ item.people }}명</td>
            <td>{{ item.price }}</td>
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
