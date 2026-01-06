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

const resolveReportTarget = (item) => {
  const type = String(item?.type ?? '').toUpperCase()
  if (type.includes('REVIEW')) return '리뷰'
  if (type.includes('ACCOM')) return '숙소'
  if (type.includes('USER')) return '회원'
  if (type.includes('RESERV')) return '예약'
  return '기타'
}

const resolveReportReason = (item) => item?.title ?? '-'

const resolveReportDetail = (item) => item?.content ?? item?.description ?? item?.detail ?? item?.message ?? ''

const formatElapsed = (value) => {
  if (!value) return '-'
  const created = new Date(value)
  if (Number.isNaN(created.getTime())) return '-'
  const diffMs = Date.now() - created.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  if (diffHours < 24) return `${diffHours}h`
  return `${Math.floor(diffHours / 24)}일`
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
        <table class="admin-table--tight admin-issue-table admin-issue-table--reports admin-issue-table--center">
          <colgroup>
            <col style="width: 90px" />
            <col style="width: 140px" />
            <col class="admin-issue-col--reason" />
            <col style="width: 110px" />
            <col style="width: 70px" />
            <col style="width: 90px" />
          </colgroup>
          <thead>
            <tr>
              <th>신고ID</th>
              <th>대상</th>
              <th>사유/내용</th>
              <th>등록일</th>
              <th>경과</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in issues.openReportsList?.slice(0, 2)" :key="item.reportId">
              <td class="admin-strong">{{ item.reportId ? `#${item.reportId}` : '-' }}</td>
              <td class="admin-issue-cell--center" :title="resolveReportTarget(item)">{{ resolveReportTarget(item) }}</td>
              <td class="admin-issue-cell--center">
                <div class="admin-issue-reason">
                  <span class="admin-issue-reason__text admin-issue-reason__title" :title="resolveReportReason(item)">
                    {{ resolveReportReason(item) }}
                  </span>
                  <span
                    v-if="resolveReportDetail(item)"
                    class="admin-issue-reason__text admin-issue-reason__detail"
                    :title="resolveReportDetail(item)"
                  >
                    {{ resolveReportDetail(item) }}
                  </span>
                </div>
              </td>
              <td class="admin-issue-cell--center" :title="item.createdAt?.slice?.(0, 10) ?? '-'">
                {{ item.createdAt?.slice?.(0, 10) ?? '-' }}
              </td>
              <td class="admin-issue-cell--center">{{ formatElapsed(item.createdAt) }}</td>
              <td class="admin-issue-cell--center">
                <AdminBadge :text="item.status" :variant="reportVariant(item.status)" />
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!issues.openReportsList?.length" class="admin-issue-empty">
          <AdminBadge text="정상" variant="success" />
          <span>기간 내 신규 없음</span>
        </div>
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
        <div class="admin-card__meta">
          <AdminBadge v-if="(issues.paymentFailureCount ?? 0) === 0" text="정상" variant="success" />
          <p class="admin-card__desc">
            {{ (issues.paymentFailureCount ?? 0) === 0 ? '기간 내 신규 없음' : '최근 결제 실패/취소 건을 빠르게 확인하세요.' }}
          </p>
        </div>
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
        <div class="admin-card__meta">
          <AdminBadge v-if="(issues.refundCount ?? 0) === 0" text="정상" variant="success" />
          <p class="admin-card__desc">
            {{ (issues.refundCount ?? 0) === 0 ? '기간 내 신규 없음' : '환불 진행 내역을 확인할 수 있습니다.' }}
          </p>
        </div>
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

.admin-issue-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
}

.admin-issue-table th,
.admin-issue-table td {
  padding: 8px 10px;
  line-height: 1.4;
}

.admin-issue-table--center th,
.admin-issue-table--center td {
  text-align: center;
}

.admin-issue-table th {
  overflow: visible;
  text-overflow: initial;
  white-space: nowrap;
}

.admin-issue-table td {
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-issue-table--reports td {
  white-space: nowrap;
}

.admin-issue-table--reports td:nth-child(3) {
  white-space: normal;
}

.admin-issue-col--reason {
  min-width: 240px;
}

.admin-issue-reason {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  align-items: center;
}

.admin-issue-reason__text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-issue-reason__title {
  font-weight: 700;
  color: #111827;
}

.admin-issue-reason__detail {
  font-size: 0.85rem;
  color: #6b7280;
}

.admin-issue-cell--center {
  text-align: center;
}

.admin-issue-empty {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
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
  color: #111827;
  font-weight: 800;
  font-size: 0.9rem;
}

.admin-card__title {
  margin: 4px 0 0;
  font-size: 1.2rem;
  color: #111827;
  font-weight: 900;
}

.admin-card__desc {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

.admin-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.admin-table-card__title) {
  color: #111827;
}
</style>
