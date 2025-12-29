<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminBookings } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const bookingList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const channelFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()

const loadBookings = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminBookings({
    status: statusFilter.value,
    sort: statusFilter.value === 'checkin' ? 'checkin' : 'latest',
    page: page.value,
    size: size.value
  })
  if (response.ok && response.data) {
    const payload = response.data
    bookingList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
  } else {
    loadError.value = '예약 목록을 불러오지 못했습니다.'
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
  loadBookings()
})

const statusVariant = (status) => {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  if (status === 3) return 'success'
  if (status === 9) return 'danger'
  return 'neutral'
}

const paymentVariant = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 3) return 'warning'
  return 'neutral'
}

const mapStatusFilter = (filter) => {
  if (filter === 'confirmed') return '2'
  if (filter === 'pending') return '1'
  if (filter === 'canceled') return '9'
  return filter
}

const filteredBookings = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return bookingList.value.filter((item) => {
    const matchesQuery = !query ||
      String(item.reservationId ?? '').includes(query) ||
      String(item.accommodationsId ?? '').includes(query) ||
      String(item.userId ?? '').includes(query)
    const matchesStatus = statusFilter.value === 'all' || String(item.reservationStatus) === mapStatusFilter(statusFilter.value)
    const matchesChannel = channelFilter.value === 'all'
    return matchesQuery && matchesStatus && matchesChannel
  })
})

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({ status: statusFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadBookings()
})

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '예약 목록',
      columns: [
        { key: 'id', label: '예약번호' },
        { key: 'accommodation', label: '숙소' },
        { key: 'host', label: '호스트' },
        { key: 'guest', label: '게스트' },
        { key: 'checkIn', label: '체크인' },
        { key: 'checkOut', label: '체크아웃' },
        { key: 'people', label: '인원' },
        { key: 'price', label: '금액' },
        { key: 'status', label: '상태' },
        { key: 'paymentStatus', label: '결제' },
        { key: 'channel', label: '채널' },
        { key: 'createdAt', label: '등록일' }
      ],
      rows: filteredBookings.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-bookings-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-bookings-${today}.csv`, sheets })
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">예약 관리</h1>
        <p class="admin-subtitle">예약 상태와 체크인 현황을 확인합니다.</p>
      </div>
    </header>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="예약번호, 숙소, 호스트, 게스트"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="confirmed">확정</option>
          <option value="pending">대기</option>
          <option value="canceled">취소</option>
        </select>
        <select v-model="channelFilter" class="admin-select">
          <option value="all">전체 채널</option>
          <option value="웹">웹</option>
          <option value="모바일">모바일</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">작업</span>
        <button class="admin-btn admin-btn--ghost" type="button">일괄 알림</button>
        <button class="admin-btn admin-btn--primary" type="button">체크인 확인</button>
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

    <AdminTableCard title="예약 목록">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>예약번호</th>
            <th>숙소</th>
            <th>게스트</th>
            <th>체크인</th>
            <th>체크아웃</th>
            <th>인원</th>
            <th>금액</th>
            <th>상태</th>
            <th>결제</th>
            <th>등록일</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredBookings" :key="item.reservationId">
            <td class="admin-strong">#{{ item.reservationId }}</td>
            <td>{{ item.accommodationsId }}</td>
            <td>{{ item.userId }}</td>
            <td>{{ item.checkin?.slice?.(0, 10) ?? '-' }}</td>
            <td>{{ item.checkout?.slice?.(0, 10) ?? '-' }}</td>
            <td>-</td>
            <td>{{ item.finalPaymentAmount ?? '-' }}</td>
            <td>
              <AdminBadge :text="item.reservationStatus" :variant="statusVariant(item.reservationStatus)" />
            </td>
            <td>
              <AdminBadge :text="item.paymentStatus" :variant="paymentVariant(item.paymentStatus)" />
            </td>
            <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button class="admin-btn admin-btn--muted" type="button" disabled>변경</button>
                <button class="admin-btn admin-btn--danger" type="button" disabled>환불</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadBookings">다시 시도</button>
      </div>
      <div v-else-if="!filteredBookings.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadBookings()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadBookings()">
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

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
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
