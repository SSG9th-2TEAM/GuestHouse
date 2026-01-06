<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { fetchAdminLogs } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'
import { formatDateTime } from '../../utils/formatters'

const logs = ref([])
const actionFilter = ref('all')
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()
const isSyncingDefaults = ref(false)

const toDateInput = (date) => {
  if (!date) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const initDefaultRange = () => {
  const today = new Date()
  const from = new Date()
  from.setDate(today.getDate() - 6)
  startDate.value = toDateInput(from)
  endDate.value = toDateInput(today)
}

const syncQuery = (next) => {
  const params = { ...route.query, ...next }
  const normalized = toQueryParams(params)
  const current = toQueryParams(route.query)
  const isSame = Object.keys({ ...normalized, ...current })
    .every((key) => String(normalized[key] ?? '') === String(current[key] ?? ''))
  if (!isSame) {
    router.replace({ query: normalized })
  }
}

const loadLogs = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminLogs({
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    actionType: actionFilter.value === 'all' ? undefined : actionFilter.value,
    keyword: keyword.value || undefined,
    page: page.value,
    size: size.value
  })
  if (response.ok && response.data) {
    const payload = response.data
    logs.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
  } else {
    loadError.value = '감사 로그를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const applyPreset = (days) => {
  const today = new Date()
  const start = new Date()
  start.setDate(today.getDate() - (days - 1))
  startDate.value = toDateInput(start)
  endDate.value = toDateInput(today)
  page.value = 0
  syncQuery({
    startDate: startDate.value,
    endDate: endDate.value,
    page: page.value
  })
}

const applyActionFilter = () => {
  page.value = 0
  syncQuery({
    actionType: actionFilter.value === 'all' ? undefined : actionFilter.value,
    page: page.value
  })
}

const applyKeywordSearch = () => {
  page.value = 0
  syncQuery({
    keyword: keyword.value || undefined,
    page: page.value
  })
}

const applyDateFilter = () => {
  page.value = 0
  syncQuery({
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    page: page.value
  })
}

const goToPage = (nextPage) => {
  page.value = nextPage
  syncQuery({
    page: page.value
  })
}

const formatReason = (value) => {
  if (!value || value === '-') return '없음'
  return value
}

const actionLabelMap = {
  APPROVE: '승인',
  REJECT: '반려',
  REFUND: '환불',
  BAN: '정지',
  UNBAN: '해제',
  RESOLVE: '처리'
}

const actionVariantMap = {
  APPROVE: 'success',
  REJECT: 'danger',
  REFUND: 'accent',
  BAN: 'warning',
  UNBAN: 'neutral',
  RESOLVE: 'primary'
}

const targetLabelMap = {
  ACC: '숙소',
  USER: '회원',
  PAY: '결제',
  REVIEW: '리뷰'
}

const resolveTargetLabel = (value) => targetLabelMap[value] ?? value ?? '-'

const resolveActionLabel = (value) => actionLabelMap[value] ?? value ?? '-'

const resolveActionVariant = (value) => actionVariantMap[value] ?? 'neutral'

const resolveTargetLink = (item) => {
  const id = item?.targetId
  if (!id) return null
  if (item?.targetType === 'ACC') return `/admin/accommodations?highlight=${id}`
  if (item?.targetType === 'USER') return `/admin/users?highlight=${id}`
  if (item?.targetType === 'PAY') return `/admin/payments?highlight=${id}`
  if (item?.targetType === 'REVIEW') return `/admin/reports?highlight=${id}`
  return null
}

const goToTarget = (item) => {
  const target = resolveTargetLink(item)
  if (target) router.push(target)
}

const syncStateFromQuery = (query) => {
  actionFilter.value = query.actionType ?? 'all'
  keyword.value = query.keyword ?? ''
  page.value = Number(query.page ?? 0)
  size.value = Number(query.size ?? 20)
  if (query.startDate) {
    startDate.value = query.startDate
  } else {
    initDefaultRange()
  }
  if (query.endDate) {
    endDate.value = query.endDate
  } else {
    endDate.value = endDate.value || toDateInput(new Date())
  }
}

onMounted(() => {
  initDefaultRange()
  if (!route.query.startDate || !route.query.endDate) {
    isSyncingDefaults.value = true
    syncQuery({
      startDate: startDate.value,
      endDate: endDate.value,
      page: page.value
    })
  }
})

watch(
  () => route.query,
  (query) => {
    if (isSyncingDefaults.value && (!query.startDate || !query.endDate)) {
      return
    }
    isSyncingDefaults.value = false
    syncStateFromQuery(query)
    loadLogs()
  },
  { immediate: true }
)
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">감사 로그</h1>
        <p class="admin-subtitle">관리자 주요 액션 기록을 조회합니다.</p>
      </div>
      <div class="admin-summary-chip">
        총 {{ totalElements.toLocaleString() }}건
      </div>
    </header>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">기간</span>
        <input v-model="startDate" class="admin-input" type="date" @change="applyDateFilter" />
        <span class="admin-divider">~</span>
        <input v-model="endDate" class="admin-input" type="date" @change="applyDateFilter" />
        <div class="admin-preset-group">
          <button class="admin-btn admin-btn--ghost" type="button" @click="applyPreset(7)">최근 7일(오늘 포함)</button>
          <button class="admin-btn admin-btn--ghost" type="button" @click="applyPreset(30)">최근 30일(오늘 포함)</button>
          <button class="admin-btn admin-btn--ghost" type="button" @click="applyPreset(90)">최근 90일(오늘 포함)</button>
        </div>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">액션 필터</span>
        <select v-model="actionFilter" class="admin-select" @change="applyActionFilter">
          <option value="all">전체</option>
          <option value="APPROVE">승인</option>
          <option value="REJECT">반려</option>
          <option value="REFUND">환불</option>
          <option value="BAN">정지</option>
          <option value="UNBAN">해제</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="keyword"
          class="admin-input"
          type="search"
          placeholder="대상ID, 사유, 대상 타입"
          @keydown.enter.prevent="applyKeywordSearch"
        />
        <button class="admin-btn admin-btn--ghost" type="button" @click="applyKeywordSearch">검색</button>
      </div>
    </div>

    <AdminTableCard title="감사 로그" class="admin-audit-card">
      <table class="admin-audit-table admin-table--nowrap">
        <colgroup>
          <col style="width: 180px" />
          <col style="width: 110px" />
          <col style="width: 90px" />
          <col style="width: 120px" />
          <col />
          <col style="width: 90px" />
        </colgroup>
        <thead>
          <tr>
            <th>시간</th>
            <th class="is-center">액션</th>
            <th class="is-center">대상</th>
            <th class="is-center">대상ID</th>
            <th class="col-reason">사유</th>
            <th class="is-center">관리자ID</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.logId">
            <td>{{ formatDateTime(item.createdAt, true) }}</td>
            <td class="is-center">
              <span class="admin-action-badge" :class="`admin-action-badge--${resolveActionVariant(item.actionType)}`">
                {{ resolveActionLabel(item.actionType) }}
              </span>
            </td>
            <td class="is-center">{{ resolveTargetLabel(item.targetType) }}</td>
            <td class="is-center">
              <button
                class="admin-link-button"
                type="button"
                :disabled="!resolveTargetLink(item)"
                @click="goToTarget(item)"
              >
                {{ item.targetId }}
              </button>
            </td>
            <td class="admin-audit-reason col-reason">
              <span
                class="reason-text"
                :class="{ 'reason-text--empty': formatReason(item.reason) === '없음' }"
                :title="formatReason(item.reason)"
              >
                <span class="reason-text__content clamp-2">
                  {{ formatReason(item.reason) }}
                </span>
              </span>
            </td>
            <td class="is-center">{{ item.adminId }}</td>
          </tr>
          <tr v-if="!logs.length && !isLoading && !loadError">
            <td class="admin-audit-empty" colspan="6">조건에 맞는 로그가 없습니다.</td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadLogs">다시 시도</button>
      </div>
      <div class="admin-pagination" v-if="totalElements > 0">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="goToPage(page - 1)">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="goToPage(page + 1)">
          다음
        </button>
      </div>
    </AdminTableCard>
  </section>
</template>

<style scoped>
.admin-summary-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: #f0fdfa;
  color: #0f766e;
  font-weight: 700;
  font-size: 0.85rem;
  border: 1px solid rgba(15, 118, 110, 0.2);
}

.admin-divider {
  color: #94a3b8;
  font-size: 0.85rem;
}

.admin-preset-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-audit-card :deep(.admin-table-wrap) {
  width: 100%;
}

.admin-audit-table {
  width: 100%;
  min-width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

.admin-audit-table th,
.admin-audit-table td {
  padding: 12px 12px;
}

.admin-audit-table th.is-center,
.admin-audit-table td.is-center {
  text-align: center;
}

.admin-audit-table th.col-reason,
.admin-audit-table td.col-reason {
  text-align: center;
  vertical-align: middle;
}

.admin-audit-table tbody tr:nth-child(even) {
  background: #f8fafc;
}

.admin-audit-table tbody tr:hover {
  background: #eef8f6;
}

.admin-audit-reason {
  white-space: normal;
  line-height: 1.45;
}

.reason-text {
  display: inline-block;
  max-width: 90%;
  text-align: left;
}

.reason-text--empty {
  text-align: center;
}

.reason-text__content {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.clamp-2 {
  display: block;
}

.admin-action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
  border: 1px solid transparent;
  min-width: 64px;
}

.admin-action-badge--success {
  background: #ecfdf3;
  color: #047857;
  border-color: #a7f3d0;
}

.admin-action-badge--danger {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}

.admin-action-badge--accent {
  background: #eef2ff;
  color: #1d4ed8;
  border-color: #c7d2fe;
}

.admin-action-badge--warning {
  background: #fffbeb;
  color: #b45309;
  border-color: #fde68a;
}

.admin-action-badge--neutral {
  background: #f1f5f9;
  color: #475569;
  border-color: #e2e8f0;
}

.admin-action-badge--primary {
  background: #f0fdfa;
  color: #0f766e;
  border-color: rgba(15, 118, 110, 0.2);
}

.admin-link-button {
  background: none;
  border: none;
  padding: 0;
  color: #0f766e;
  font-weight: 700;
  cursor: pointer;
}

.admin-link-button:disabled {
  color: #9ca3af;
  cursor: default;
}

.admin-audit-empty {
  text-align: center;
  padding: 28px 12px;
  color: #64748b;
  font-weight: 700;
}

@media (max-width: 900px) {
  .admin-filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
