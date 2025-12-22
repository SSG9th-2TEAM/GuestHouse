<script setup>
import { onMounted, ref, computed } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { adminUsers, usersStats } from '../../mocks/adminMockData'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const stats = ref([])
const users = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')

const loadUsers = () => {
  stats.value = [
    { label: '전체 사용자', value: usersStats.total, sub: '지난 30일 +2.1%', tone: 'primary' },
    { label: '게스트', value: usersStats.guests, sub: '활성 비율 83%', tone: 'success' },
    { label: '호스트', value: usersStats.hosts, sub: '신규 호스트 +38', tone: 'accent' }
  ]
  users.value = adminUsers
}

onMounted(loadUsers)

const statusVariant = (status) => {
  if (status === '활성') return 'success'
  if (status === '휴면') return 'warning'
  if (status === '정지') return 'danger'
  return 'neutral'
}

const riskVariant = (risk) => {
  if (risk === '높음') return 'danger'
  if (risk === '중간') return 'warning'
  return 'success'
}

const filteredUsers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return users.value.filter((user) => {
    const matchesQuery = !query ||
      user.name.toLowerCase().includes(query) ||
      user.email.toLowerCase().includes(query) ||
      user.phone.includes(query) ||
      user.id.toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || user.status === statusFilter.value
    const matchesType = typeFilter.value === 'all' || user.type === typeFilter.value
    return matchesQuery && matchesStatus && matchesType
  })
})

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '회원 목록',
      columns: [
        { key: 'id', label: 'ID' },
        { key: 'name', label: '이름' },
        { key: 'email', label: '이메일' },
        { key: 'phone', label: '연락처' },
        { key: 'type', label: '유형' },
        { key: 'joinedAt', label: '가입일' },
        { key: 'lastLogin', label: '최근 접속' },
        { key: 'activity', label: '활동' },
        { key: 'risk', label: '리스크' },
        { key: 'status', label: '상태' }
      ],
      rows: filteredUsers.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-users-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-users-${today}.csv`, sheets })
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">회원 관리</h1>
        <p class="admin-subtitle">게스트/호스트 현황과 상태를 확인합니다.</p>
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
          placeholder="이름, 이메일, 연락처, ID"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="typeFilter" class="admin-select">
          <option value="all">전체 유형</option>
          <option value="게스트">게스트</option>
          <option value="호스트">호스트</option>
        </select>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="활성">활성</option>
          <option value="휴면">휴면</option>
          <option value="정지">정지</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <details class="admin-dropdown">
          <summary class="admin-btn admin-btn--ghost">다운로드</summary>
          <div class="admin-dropdown__menu">
            <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">
              CSV
            </button>
            <button class="admin-btn admin-btn--primary admin-dropdown__item" type="button" @click="downloadReport('xlsx')">
              XLSX
            </button>
          </div>
        </details>
      </div>
    </div>

    <AdminTableCard title="회원 목록">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>ID</th>
            <th>사용자</th>
            <th>연락처</th>
            <th>이메일</th>
            <th>유형</th>
            <th>가입일</th>
            <th>최근 접속</th>
            <th>활동</th>
            <th>리스크</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td class="admin-strong">{{ user.id }}</td>
            <td>
              <div class="admin-user">
                <div class="admin-avatar">{{ user.name.slice(0, 1) }}</div>
                <span class="admin-strong">{{ user.name }} · {{ user.email }}</span>
              </div>
            </td>
            <td>{{ user.phone }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.type }}</td>
            <td>{{ user.joinedAt }}</td>
            <td>{{ user.lastLogin }}</td>
            <td>{{ user.activity }}</td>
            <td>
              <AdminBadge :text="user.risk" :variant="riskVariant(user.risk)" />
            </td>
            <td>
              <AdminBadge :text="user.status" :variant="statusVariant(user.status)" />
            </td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button
                  v-if="user.status === '정지'"
                  class="admin-btn admin-btn--primary"
                  type="button"
                >
                  복구
                </button>
                <button
                  v-else
                  class="admin-btn admin-btn--danger"
                  type="button"
                >
                  정지
                </button>
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

.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: #e5f3ef;
  color: #0f766e;
  display: grid;
  place-items: center;
  font-weight: 900;
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
