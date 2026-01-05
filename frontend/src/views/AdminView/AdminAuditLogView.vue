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

onMounted(() => {
  initDefaultRange()
  actionFilter.value = route.query.actionType ?? 'all'
  keyword.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  if (route.query.startDate) startDate.value = route.query.startDate
  if (route.query.endDate) endDate.value = route.query.endDate
  loadLogs()
})

watch([actionFilter, keyword, startDate, endDate], () => {
  page.value = 0
  syncQuery({
    actionType: actionFilter.value === 'all' ? undefined : actionFilter.value,
    keyword: keyword.value || undefined,
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    page: page.value
  })
  loadLogs()
})
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
        <input v-model="startDate" class="admin-input" type="date" />
        <span class="admin-divider">~</span>
        <input v-model="endDate" class="admin-input" type="date" />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">액션</span>
        <select v-model="actionFilter" class="admin-select">
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
        />
      </div>
    </div>

    <AdminTableCard title="감사 로그">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>시간</th>
            <th>액션</th>
            <th>대상</th>
            <th>대상ID</th>
            <th>사유</th>
            <th>관리자ID</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.logId">
            <td>{{ formatDateTime(item.createdAt, true) }}</td>
            <td>{{ item.actionType }}</td>
            <td>{{ item.targetType }}</td>
            <td>{{ item.targetId }}</td>
            <td class="admin-table__reason">{{ item.reason || '-' }}</td>
            <td>{{ item.adminId }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadLogs">다시 시도</button>
      </div>
      <div v-else-if="!logs.length" class="admin-status">조건에 맞는 로그가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadLogs()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadLogs()">
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

.admin-table__reason {
  max-width: 360px;
  white-space: normal;
  line-height: 1.4;
}

@media (max-width: 900px) {
  .admin-filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
