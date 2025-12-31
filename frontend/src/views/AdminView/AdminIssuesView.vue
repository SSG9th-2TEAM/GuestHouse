<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { fetchAdminDashboardIssues } from '../../api/adminApi'

const router = useRouter()
const stats = ref([])
const issues = ref({
  pendingAccommodations: 0,
  openReports: 0,
  refundCount: 0,
  paymentFailureCount: 0,
  pendingAccommodationsList: [],
  openReportsList: []
})
const isLoading = ref(false)
const loadError = ref('')

const toDateString = (date) => date.toISOString().slice(0, 10)

const loadIssues = async () => {
  isLoading.value = true
  loadError.value = ''
  const today = new Date()
  const params = { from: toDateString(today), to: toDateString(today) }
  const response = await fetchAdminDashboardIssues(params)
  if (response.ok && response.data) {
    issues.value = response.data
    stats.value = [
      { label: '승인 대기 숙소', value: `${response.data.pendingAccommodations ?? 0}건`, sub: '심사 대기', tone: 'warning' },
      { label: '미처리 신고', value: `${response.data.openReports ?? 0}건`, sub: '처리 필요', tone: 'warning' },
      { label: '결제 실패', value: `${response.data.paymentFailureCount ?? 0}건`, sub: '실패/취소', tone: 'neutral' },
      { label: '환불 요청', value: `${response.data.refundCount ?? 0}건`, sub: '요청/완료', tone: 'accent' }
    ]
  } else {
    loadError.value = '이슈 센터 데이터를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const statusVariant = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'neutral'
}

const reportVariant = (status) => {
  if (status === 'WAIT') return 'warning'
  if (status === 'CHECKED') return 'success'
  return 'neutral'
}

const goTo = (target) => {
  if (target) router.push(target)
}

onMounted(loadIssues)
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">운영 이슈 센터</h1>
        <p class="admin-subtitle">오늘 확인해야 할 운영 이슈를 빠르게 점검합니다.</p>
      </div>
      <button class="admin-btn admin-btn--ghost" type="button" @click="loadIssues">새로고침</button>
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
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadIssues">다시 시도</button>
      </div>
    </div>

    <div class="admin-grid admin-grid--2">
      <AdminTableCard title="숙소 승인 대기">
        <template #actions>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/accommodations?status=PENDING')">
            바로가기
          </button>
        </template>
        <table class="admin-table--nowrap admin-table--tight">
          <thead>
            <tr>
              <th>ID</th>
              <th>숙소명</th>
              <th>호스트</th>
              <th>신청일</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in issues.pendingAccommodationsList" :key="item.accommodationsId">
              <td class="admin-strong">#{{ item.accommodationsId }}</td>
              <td class="admin-strong">{{ item.name }}</td>
              <td>{{ item.hostUserId }}</td>
              <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
              <td>
                <AdminBadge :text="item.approvalStatus" :variant="statusVariant(item.approvalStatus)" />
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!issues.pendingAccommodationsList?.length" class="admin-status">승인 대기 숙소가 없습니다.</div>
      </AdminTableCard>

      <AdminTableCard title="미처리 신고">
        <template #actions>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/reports?status=WAIT')">
            바로가기
          </button>
        </template>
        <table class="admin-table--nowrap admin-table--tight">
          <thead>
            <tr>
              <th>신고ID</th>
              <th>사유</th>
              <th>등록일</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in issues.openReportsList" :key="item.reportId">
              <td class="admin-strong">#{{ item.reportId }}</td>
              <td class="admin-strong">{{ item.title ?? '-' }}</td>
              <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
              <td>
                <AdminBadge :text="item.status" :variant="reportVariant(item.status)" />
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!issues.openReportsList?.length" class="admin-status">미처리 신고가 없습니다.</div>
      </AdminTableCard>
    </div>

    <div class="admin-grid admin-grid--2">
      <div class="admin-card">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">결제 실패/취소</p>
            <h3 class="admin-card__title">{{ issues.paymentFailureCount ?? 0 }}건</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/payments?status=FAILED')">
            바로가기
          </button>
        </div>
        <p class="admin-card__desc">최근 결제 실패/취소 건을 빠르게 확인하세요.</p>
      </div>

      <div class="admin-card">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">환불 요청/완료</p>
            <h3 class="admin-card__title">{{ issues.refundCount ?? 0 }}건</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/payments?status=REFUNDED')">
            바로가기
          </button>
        </div>
        <p class="admin-card__desc">환불 진행 내역을 확인할 수 있습니다.</p>
      </div>
    </div>
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

.admin-grid--2 {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: start;
}

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
}

.admin-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  padding: 16px;
}

.admin-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.admin-card__eyebrow {
  margin: 0;
  color: #0f766e;
  font-weight: 800;
  font-size: 0.9rem;
}

.admin-card__title {
  margin: 4px 0 0;
  font-size: 1.2rem;
  color: #0b3b32;
  font-weight: 900;
}

.admin-card__desc {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}
</style>
