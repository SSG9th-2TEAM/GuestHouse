<script setup>
import { onMounted, ref } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { payments, paymentsStats } from '../../mocks/adminMockData'

const stats = ref([])
const paymentList = ref([])

const loadPayments = () => {
  stats.value = [
    { label: '총 거래액', value: paymentsStats.totalVolume, sub: '주간 +5.2%', tone: 'primary' },
    { label: '플랫폼 수수료', value: paymentsStats.platformFee, sub: '수수료율 7%', tone: 'accent' },
    { label: '거래 건수', value: paymentsStats.transactions, sub: '성공률 98.2%', tone: 'success' }
  ]
  paymentList.value = payments
}

onMounted(loadPayments)

const statusVariant = (status) => {
  if (status === '완료') return 'success'
  if (status === '환불') return 'warning'
  return 'neutral'
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">결제 관리</h1>
        <p class="admin-subtitle">거래 흐름과 수수료를 모니터링합니다.</p>
      </div>
    </header>

    <div class="admin-grid">
      <AdminStatCard
        v-for="card in stats"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :sub="card.sub"
        :tone="card.tone"
      />
    </div>

    <AdminTableCard title="결제 내역">
      <table>
        <thead>
          <tr>
            <th>거래ID</th>
            <th>호스트</th>
            <th>게스트</th>
            <th>숙소</th>
            <th>거래액</th>
            <th>수수료</th>
            <th>유형</th>
            <th>상태</th>
            <th>날짜</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in paymentList" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td>{{ item.host }}</td>
            <td>{{ item.guest }}</td>
            <td>{{ item.accommodation }}</td>
            <td>{{ item.amount }}</td>
            <td>{{ item.fee }}</td>
            <td>{{ item.type }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>{{ item.date }}</td>
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

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
