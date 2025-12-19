<script setup>
import { onMounted, ref } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { reports, reportsStats } from '../../mocks/adminMockData'

const stats = ref([])
const reportList = ref([])

const loadReports = () => {
  stats.value = [
    { label: '대기중', value: `${reportsStats.pending}건`, sub: '빠른 초기 대응 필요', tone: 'warning' },
    { label: '처리중', value: `${reportsStats.inProgress}건`, sub: '담당자 배정 완료', tone: 'accent' },
    { label: '처리완료', value: `${reportsStats.resolved}건`, sub: '이번 달 92% 해결', tone: 'success' }
  ]
  reportList.value = reports
}

onMounted(loadReports)

const statusVariant = (status) => {
  if (status === '완료') return 'success'
  if (status === '처리중') return 'accent'
  if (status === '대기') return 'warning'
  return 'neutral'
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">신고 관리</h1>
        <p class="admin-subtitle">신고 접수 현황과 처리 단계를 추적합니다.</p>
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

    <AdminTableCard title="신고 목록">
      <table>
        <thead>
          <tr>
            <th>신고ID</th>
            <th>신고자</th>
            <th>피신고자</th>
            <th>신고 사유</th>
            <th>신고 일시</th>
            <th>처리 상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in reportList" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td>{{ item.reporter }}</td>
            <td>{{ item.target }}</td>
            <td>
              <div class="admin-col">
                <span class="admin-strong">{{ item.reason }}</span>
                <small class="admin-sub">{{ item.summary }}</small>
              </div>
            </td>
            <td>{{ item.createdAt }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>
              <button class="admin-btn-ghost" type="button">상세보기</button>
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

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
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
