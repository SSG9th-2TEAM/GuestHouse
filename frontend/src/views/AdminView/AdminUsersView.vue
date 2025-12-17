<script setup>
import { onMounted, ref } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { adminUsers, usersStats } from '../../mocks/adminMockData'

const stats = ref([])
const users = ref([])

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
  return 'neutral'
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

    <AdminTableCard title="회원 목록">
      <table>
        <thead>
          <tr>
            <th>사용자</th>
            <th>이메일</th>
            <th>유형</th>
            <th>가입일</th>
            <th>활동</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.email">
            <td>
              <div class="admin-user">
                <div class="admin-avatar">{{ user.name.slice(0, 1) }}</div>
                <span class="admin-strong">{{ user.name }}</span>
              </div>
            </td>
            <td>{{ user.email }}</td>
            <td>{{ user.type }}</td>
            <td>{{ user.joinedAt }}</td>
            <td>{{ user.activity }}</td>
            <td>
              <AdminBadge :text="user.status" :variant="statusVariant(user.status)" />
            </td>
            <td>
              <button class="admin-btn-ghost" type="button" @click="console.log('open user', user.email)">
                상세
              </button>
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
