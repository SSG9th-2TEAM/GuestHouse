<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminUsers } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'
import { getUserStatusLabel, getUserStatusVariant } from '../../constants/adminUserStatus'

const stats = ref([])
const users = ref([])
const searchQuery = ref('')
const typeFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()

const loadUsers = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminUsers({
    role: typeFilter.value === 'all' ? undefined : typeFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    users.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
    const hostCount = users.value.filter((item) => item.role === 'HOST' || item.role === 'ROLE_HOST').length
    const guestCount = users.value.filter((item) => item.role === 'USER' || item.role === 'ROLE_USER').length
    stats.value = [
      { label: '전체 사용자', value: `${totalElements.value}명`, sub: '전체 계정 기준', tone: 'primary' },
      { label: '게스트', value: `${guestCount}명`, sub: '현재 페이지 기준', tone: 'success' },
      { label: '호스트', value: `${hostCount}명`, sub: '현재 페이지 기준', tone: 'accent' }
    ]
  } else {
    loadError.value = '회원 목록을 불러오지 못했습니다.'
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
  typeFilter.value = route.query.role ?? 'all'
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadUsers()
})

const riskVariant = (risk) => {
  if (risk === '높음') return 'danger'
  if (risk === '중간') return 'warning'
  return 'success'
}

const filteredUsers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return users.value.filter((user) => {
    const matchesQuery = !query ||
      String(user.userId ?? '').includes(query) ||
      (user.email ?? '').toLowerCase().includes(query) ||
      (user.phone ?? '').includes(query)
    const matchesType = typeFilter.value === 'all' || user.role === typeFilter.value || user.role === `ROLE_${typeFilter.value}`
    return matchesQuery && matchesType
  })
})

watch([searchQuery, typeFilter], () => {
  page.value = 0
  syncQuery({ role: typeFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadUsers()
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

const handleUserAction = () => {
  alert('준비중입니다.')
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
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadUsers">다시 시도</button>
      </div>
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
          <option value="USER">게스트</option>
          <option value="HOST">호스트</option>
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
      <table class="admin-table--nowrap admin-table--tight admin-table--stretch">
        <colgroup>
          <col style="width:90px"/>
          <col style="width:320px"/>
          <col style="width:160px"/>
          <col style="width:110px"/>
          <col style="width:140px"/>
          <col style="width:140px"/>
          <col style="width:120px"/>
          <col style="width:160px"/>
        </colgroup>
        <thead>
          <tr>
            <th>ID</th>
            <th>이메일</th>
            <th>연락처</th>
            <th>유형</th>
            <th>가입일</th>
            <th>호스트 승인</th>
            <th>계정 상태</th>
            <th class="admin-align-right">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.userId">
            <td class="admin-strong">#{{ user.userId }}</td>
            <td class="admin-strong admin-ellipsis" :title="user.email">{{ user.email }}</td>
            <td>{{ user.phone }}</td>
            <td class="admin-align-center">{{ user.role }}</td>
            <td class="admin-align-center">{{ user.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td>
              <AdminBadge :text="user.hostApproved ? '승인' : '미승인'" :variant="user.hostApproved ? 'success' : 'warning'" />
            </td>
            <td>
              <AdminBadge :text="getUserStatusLabel(user.accountStatus)" :variant="getUserStatusVariant(user.accountStatus)" />
            </td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap admin-actions-right">
                <button class="admin-btn admin-btn--muted admin-action-disabled" type="button" @click="handleUserAction">정지</button>
                <button class="admin-btn admin-btn--ghost admin-action-disabled" type="button" @click="handleUserAction">해제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!isLoading && !loadError && !filteredUsers.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadUsers()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadUsers()">
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

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
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

.admin-table--stretch {
  width: 100%;
  table-layout: fixed;
}

.admin-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-align-center {
  text-align: center;
}

.admin-actions-right {
  justify-content: flex-end;
}

.admin-action-disabled {
  opacity: 0.6;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
