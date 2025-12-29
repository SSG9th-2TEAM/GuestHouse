<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminAccommodations, approveAccommodation, rejectAccommodation } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const accommodationList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()

const loadAccommodations = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminAccommodations({
    status: statusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    accommodationList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
  } else {
    loadError.value = '숙소 목록을 불러오지 못했습니다.'
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
  loadAccommodations()
})

const statusVariant = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'neutral'
}

const filteredAccommodations = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return accommodationList.value.filter((item) => {
    const matchesQuery = !query ||
      (item.name ?? '').toLowerCase().includes(query) ||
      String(item.hostUserId ?? '').includes(query) ||
      `${item.city ?? ''} ${item.district ?? ''}`.toLowerCase().includes(query) ||
      String(item.accommodationsId ?? '').includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.approvalStatus === statusFilter.value
    return matchesQuery && matchesStatus
  })
})

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({ status: statusFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadAccommodations()
})

const handleApprove = async (item) => {
  if (!item?.accommodationsId) return
  if (!confirm('이 숙소를 승인하시겠습니까?')) return
  await approveAccommodation(item.accommodationsId)
  loadAccommodations()
}

const handleReject = async (item) => {
  if (!item?.accommodationsId) return
  const reason = prompt('반려 사유를 입력해주세요')
  if (!reason) return
  await rejectAccommodation(item.accommodationsId, reason)
  loadAccommodations()
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '숙소 목록',
      columns: [
        { key: 'accommodationsId', label: 'ID' },
        { key: 'name', label: '숙소명' },
        { key: 'hostUserId', label: '호스트' },
        { key: 'city', label: '도시' },
        { key: 'district', label: '지역' },
        { key: 'category', label: '유형' },
        { key: 'createdAt', label: '등록일' },
        { key: 'approvalStatus', label: '상태' }
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
          <option value="PENDING">승인 대기</option>
          <option value="APPROVED">승인</option>
          <option value="REJECTED">반려</option>
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
            <td class="admin-strong">#{{ item.accommodationsId }}</td>
            <td class="admin-strong">{{ item.name }}</td>
            <td>{{ item.hostUserId }}</td>
            <td>{{ item.city }} {{ item.district }}</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td>
              <AdminBadge :text="item.approvalStatus" :variant="statusVariant(item.approvalStatus)" />
            </td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button class="admin-btn admin-btn--primary" type="button" @click="handleApprove(item)">승인</button>
                <button class="admin-btn admin-btn--muted" type="button" @click="handleReject(item)">반려</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadAccommodations">다시 시도</button>
      </div>
      <div v-else-if="!filteredAccommodations.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadAccommodations()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadAccommodations()">
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
