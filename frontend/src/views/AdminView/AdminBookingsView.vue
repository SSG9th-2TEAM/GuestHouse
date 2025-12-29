<script setup>
import { onMounted, ref, computed } from 'vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { bookings } from '../../mocks/adminMockData'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const bookingList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const channelFilter = ref('all')

const loadBookings = () => {
  bookingList.value = bookings
}

onMounted(loadBookings)

const statusVariant = (status) => {
  if (status === '확정' || status === '체크인') return 'success'
  if (status === '대기') return 'warning'
  if (status === '취소') return 'danger'
  return 'neutral'
}

const paymentVariant = (status) => {
  if (status === '완료') return 'success'
  if (status === '보류') return 'warning'
  if (status === '환불') return 'danger'
  return 'neutral'
}

const filteredBookings = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return bookingList.value.filter((item) => {
    const matchesQuery = !query ||
      item.id.toLowerCase().includes(query) ||
      item.accommodation.toLowerCase().includes(query) ||
      item.host.toLowerCase().includes(query) ||
      item.guest.toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchesChannel = channelFilter.value === 'all' || item.channel === channelFilter.value
    return matchesQuery && matchesStatus && matchesChannel
  })
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
          <option value="확정">확정</option>
          <option value="대기">대기</option>
          <option value="체크인">체크인</option>
          <option value="취소">취소</option>
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
            <th>호스트</th>
            <th>게스트</th>
            <th>체크인</th>
            <th>체크아웃</th>
            <th>인원</th>
            <th>금액</th>
            <th>상태</th>
            <th>결제</th>
            <th>채널</th>
            <th>등록일</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredBookings" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td>{{ item.accommodation }}</td>
            <td>{{ item.host }}</td>
            <td>{{ item.guest }}</td>
            <td>{{ item.checkIn }}</td>
            <td>{{ item.checkOut }}</td>
            <td>{{ item.people }}명</td>
            <td>{{ item.price }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>
              <AdminBadge :text="item.paymentStatus" :variant="paymentVariant(item.paymentStatus)" />
            </td>
            <td>{{ item.channel }}</td>
            <td>{{ item.createdAt }}</td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button class="admin-btn admin-btn--muted" type="button">변경</button>
                <button class="admin-btn admin-btn--danger" type="button">환불</button>
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
