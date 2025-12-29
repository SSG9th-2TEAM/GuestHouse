<script setup>
import { onMounted, ref, computed } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { reports, reportsStats } from '../../mocks/adminMockData'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const stats = ref([])
const reportList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const priorityFilter = ref('all')

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

const priorityVariant = (priority) => {
  if (priority === '높음') return 'danger'
  if (priority === '중간') return 'warning'
  return 'success'
}

const filteredReports = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return reportList.value.filter((item) => {
    const matchesQuery = !query ||
      item.id.toLowerCase().includes(query) ||
      item.reporter.toLowerCase().includes(query) ||
      item.target.toLowerCase().includes(query) ||
      item.reason.toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchesPriority = priorityFilter.value === 'all' || item.priority === priorityFilter.value
    return matchesQuery && matchesStatus && matchesPriority
  })
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
          <option value="대기">대기</option>
          <option value="처리중">처리중</option>
          <option value="완료">완료</option>
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
            <th>신고자</th>
            <th>피신고자</th>
            <th>신고 사유</th>
            <th>신고 일시</th>
            <th>우선순위</th>
            <th>처리 상태</th>
            <th>담당자</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredReports" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td>{{ item.reporter }}</td>
            <td>{{ item.target }}</td>
            <td class="admin-strong">{{ item.reason }} · {{ item.summary }}</td>
            <td>{{ item.createdAt }}</td>
            <td>
              <AdminBadge :text="item.priority" :variant="priorityVariant(item.priority)" />
            </td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>{{ item.assignee }}</td>
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
