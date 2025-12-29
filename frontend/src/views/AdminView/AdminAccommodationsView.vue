<script setup>
import { onMounted, ref, computed } from 'vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { accommodations } from '../../mocks/adminMockData'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const accommodationList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')

const loadAccommodations = () => {
  accommodationList.value = accommodations
}

onMounted(loadAccommodations)

const statusVariant = (status) => {
  if (status === '승인됨') return 'success'
  if (status === '검토중') return 'warning'
  if (status === '보류') return 'accent'
  return 'neutral'
}

const filteredAccommodations = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return accommodationList.value.filter((item) => {
    const matchesQuery = !query ||
      item.name.toLowerCase().includes(query) ||
      item.host.toLowerCase().includes(query) ||
      item.location.toLowerCase().includes(query) ||
      item.id.toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    return matchesQuery && matchesStatus
  })
})

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '숙소 목록',
      columns: [
        { key: 'id', label: 'ID' },
        { key: 'name', label: '숙소명' },
        { key: 'host', label: '호스트' },
        { key: 'location', label: '위치' },
        { key: 'rating', label: '평점' },
        { key: 'reviewCount', label: '리뷰' },
        { key: 'bookings', label: '예약' },
        { key: 'occupancy', label: '가동률' },
        { key: 'cancellationRate', label: '취소율' },
        { key: 'revenue', label: '총 매출' },
        { key: 'registeredAt', label: '등록일' },
        { key: 'status', label: '상태' }
      ],
      rows: filteredAccommodations.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-accommodations-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-accommodations-${today}.csv`, sheets })
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

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="숙소명, 호스트, 지역, ID"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">상태</span>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="승인됨">승인됨</option>
          <option value="검토중">검토중</option>
          <option value="보류">보류</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">작업</span>
        <button class="admin-btn admin-btn--primary" type="button">대기 숙소 검토</button>
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

    <AdminTableCard title="숙소 목록">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>ID</th>
            <th>숙소명</th>
            <th>호스트</th>
            <th>위치</th>
            <th>평점</th>
            <th>리뷰</th>
            <th>예약</th>
            <th>가동률</th>
            <th>취소율</th>
            <th>총 매출</th>
            <th>등록일</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredAccommodations" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td class="admin-strong">{{ item.name }}</td>
            <td>{{ item.host }}</td>
            <td>{{ item.location }}</td>
            <td>{{ item.rating }}</td>
            <td>{{ item.reviewCount }}건</td>
            <td>{{ item.bookings }}건</td>
            <td>{{ item.occupancy }}</td>
            <td>{{ item.cancellationRate }}</td>
            <td>{{ item.revenue }}</td>
            <td>{{ item.registeredAt }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button class="admin-btn admin-btn--primary" type="button">승인</button>
                <button class="admin-btn admin-btn--muted" type="button">보류</button>
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
