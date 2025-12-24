<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminReports } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const stats = ref([])
const reportList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const priorityFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()

const loadReports = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminReports({
    status: statusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    reportList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
    const pending = reportList.value.filter((r) => r.status === 'WAIT').length
    const checked = reportList.value.filter((r) => r.status === 'CHECKED').length
    const blinded = reportList.value.filter((r) => r.status === 'BLINDED').length
    stats.value = [
      { label: '대기중', value: `${pending}건`, sub: '현재 페이지 기준', tone: 'warning' },
      { label: '처리완료', value: `${checked}건`, sub: '현재 페이지 기준', tone: 'success' },
      { label: '블라인드', value: `${blinded}건`, sub: '현재 페이지 기준', tone: 'accent' }
    ]
  } else {
    loadError.value = '신고 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
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

onMounted(() => {
  statusFilter.value = route.query.status ?? 'all'
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadReports()
})

const statusVariant = (status) => {
  if (status === 'CHECKED') return 'success'
  if (status === 'BLINDED') return 'danger'
  if (status === 'WAIT') return 'warning'
  return 'neutral'
}

const priorityVariant = (priority) => {
  if (priority === '높음') return 'danger'
  if (priority === '중간') return 'warning'
  return 'success'
}

const filteredReports = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return reportList.value.filter((item) => {
    const matchesQuery = !query ||
      String(item.reportId ?? '').includes(query) ||
      (item.title ?? '').toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchesPriority = priorityFilter.value === 'all'
    return matchesQuery && matchesStatus && matchesPriority
  })
})

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({ status: statusFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadReports()
})

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '신고 목록',
      columns: [
        { key: 'id', label: '신고ID' },
        { key: 'reporter', label: '신고자' },
        { key: 'target', label: '피신고자' },
        { key: 'reason', label: '사유' },
        { key: 'summary', label: '요약' },
        { key: 'createdAt', label: '신고 일시' },
        { key: 'priority', label: '우선순위' },
        { key: 'status', label: '상태' },
        { key: 'assignee', label: '담당자' }
      ],
      rows: filteredReports.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-reports-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-reports-${today}.csv`, sheets })
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

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="신고ID, 신고자, 피신고자, 사유"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="WAIT">대기</option>
          <option value="CHECKED">처리완료</option>
          <option value="BLINDED">블라인드</option>
        </select>
        <select v-model="priorityFilter" class="admin-select">
          <option value="all">전체 우선순위</option>
          <option value="높음">높음</option>
          <option value="중간">중간</option>
          <option value="낮음">낮음</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">작업</span>
        <button class="admin-btn admin-btn--ghost" type="button">담당자 재배정</button>
        <button class="admin-btn admin-btn--primary" type="button">보고서 생성</button>
      </div>
      <div class="admin-filter-group">
        <details class="admin-dropdown">
          <summary class="admin-btn admin-btn--ghost">다운로드</summary>
          <div class="admin-dropdown__menu">
            <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">CSV</button>
            <button class="admin-btn admin-btn--primary admin-dropdown__item" type="button" @click="downloadReport('xlsx')">XLSX</button>
          </div>
        </details>
      </div>
    </div>

    <AdminTableCard title="신고 목록">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>신고ID</th>
            <th>신고 사유</th>
            <th>신고 일시</th>
            <th>처리 상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredReports" :key="item.reportId">
            <td class="admin-strong">#{{ item.reportId }}</td>
            <td class="admin-strong">{{ item.title }}</td>
            <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button class="admin-btn admin-btn--muted" type="button">메모</button>
                <button class="admin-btn admin-btn--primary" type="button">처리</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadReports">다시 시도</button>
      </div>
      <div v-else-if="!filteredReports.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadReports()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadReports()">
          다음
        </button>
      </div>
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

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
  margin-top: 12px;
}

.admin-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
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
