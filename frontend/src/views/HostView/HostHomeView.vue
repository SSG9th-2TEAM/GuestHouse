<script setup>
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()

const kpis = ref([
  {label: '이번 달 예상 수익', value: 5200000, unit: '₩', trend: '+12%', tone: 'positive', target: '/host/revenue'},
  {label: '이번 달 예약 확정', value: 34, unit: '건', trend: '+4건', tone: 'positive', target: '/host/booking'},
  {label: '평균 평점', value: 4.8, unit: '/5.0', trend: '최근 30일', tone: 'neutral', target: '/host/review'},
  {label: '숙소 운영 현황', value: '5/6', unit: ' 운영중', trend: '1건 점검중', tone: 'warning', target: '/host/accommodation'}
])

const todayLabel = '2025년 12월 16일 (화)'
const tasks = ref([
  {id: 1, type: 'checkin', time: '15:00', accommodation: '강남 모던 게스트하우스 201호', guest: '김민수', phone: '010-1234-5678', email: 'minsu@example.com', memo: '바베큐 숯 추가 요청'},
  {id: 2, type: 'checkout', time: '11:00', accommodation: '제주 감성 숙소 별채', guest: '이서연', phone: '010-2345-6789', email: 'seoyeon@example.com', memo: '침구 교체 필요'},
  {id: 3, type: 'checkin', time: '18:00', accommodation: '해운대 오션뷰 802호', guest: '박지성', phone: '010-9876-5432', email: 'park@example.com', memo: ''}
])

const formatKpiValue = (value, unit) => {
  if (typeof value === 'number') {
    return `${unit === '₩' ? '₩' : ''}${value.toLocaleString()}${unit === '₩' ? '' : unit}`
  }
  return `${value}${unit ?? ''}`
}

const hasMemo = computed(() => tasks.value.some(t => t.memo))

const goTo = (path) => {
  if (path) router.push(path)
}

const selectedTask = ref(null)
const showTaskModal = ref(false)
const openTask = (task) => {
  selectedTask.value = task
  showTaskModal.value = true
}
const closeTask = () => {
  selectedTask.value = null
  showTaskModal.value = false
}
</script>

<template>
  <div class="dashboard-home">
    <header class="view-header">
      <div>
        <h2>대시보드</h2>
        <p class="subtitle">이번 달 운영 현황을 빠르게 확인하세요.</p>
      </div>
    </header>

    <!-- KPI grid -->
    <section class="kpi-grid">
      <article
          v-for="item in kpis"
          :key="item.label"
          class="kpi-card"
          role="button"
          tabindex="0"
          @click="goTo(item.target)"
          @keypress.enter="goTo(item.target)"
      >
        <div class="kpi-top">
          <p class="kpi-label">{{ item.label }}</p>
          <span class="kpi-trend" :class="item.tone">{{ item.trend }}</span>
        </div>
        <p class="kpi-value">{{ formatKpiValue(item.value, item.unit) }}</p>
      </article>
    </section>

    <!-- Today tasks -->
    <section class="task-panel">
      <div class="task-head">
        <div>
          <h3>오늘 일정</h3>
          <p class="task-date">{{ todayLabel }}</p>
        </div>
        <span class="task-chip">체크인/아웃 {{ tasks.length }}건</span>
      </div>

      <div class="task-list">
        <div v-for="task in tasks" :key="task.id" class="task-card" role="button" tabindex="0" @click="openTask(task)" @keypress.enter="openTask(task)">
          <div class="task-row">
            <span class="pill" :class="task.type === 'checkin' ? 'pill-green' : 'pill-gray'">
              {{ task.type === 'checkin' ? '체크인' : '체크아웃' }}
            </span>
            <span class="time">{{ task.time }}</span>
          </div>
          <p class="accommodation">{{ task.accommodation }}</p>
          <p class="guest">{{ task.guest }} 님</p>
          <p v-if="task.memo" class="memo">📝 {{ task.memo }}</p>
        </div>
      </div>

      <p v-if="!tasks.length" class="empty">오늘 예정된 일정이 없습니다.</p>
      <p v-else-if="hasMemo" class="footnote">메모가 있는 일정은 📝 로 표시됩니다.</p>
    </section>

    <div v-if="showTaskModal && selectedTask" class="modal-backdrop" @click.self="closeTask">
      <div class="modal">
        <header class="modal-head">
          <div>
            <p class="eyebrow small">오늘 일정</p>
            <h3>{{ selectedTask.accommodation }}</h3>
          </div>
          <button class="close-btn" @click="closeTask">×</button>
        </header>
        <div class="modal-body">
          <div class="modal-row"><span>유형</span><strong>{{ selectedTask.type === 'checkin' ? '체크인' : '체크아웃' }}</strong></div>
          <div class="modal-row"><span>시간</span><strong>{{ selectedTask.time }}</strong></div>
          <div class="modal-row"><span>게스트</span><strong>{{ selectedTask.guest }}</strong></div>
          <div class="modal-row"><span>연락처</span><strong>{{ selectedTask.phone || '미입력' }}</strong></div>
          <div class="modal-row"><span>이메일</span><strong>{{ selectedTask.email || '미입력' }}</strong></div>
          <div class="modal-row"><span>메모</span><strong>{{ selectedTask.memo || '메모 없음' }}</strong></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-home {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: #0b3b32;
  margin: 0.25rem 0;
}

.eyebrow {
  font-size: 0.85rem;
  color: #0f766e;
  font-weight: 700;
  margin: 0;
}

.subtitle {
  color: #6b7280;
  margin: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 1.1rem 1.25rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;
  cursor: pointer;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.kpi-label {
  font-size: 0.95rem;
  color: #4b5563;
  margin: 0;
}

.kpi-trend {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.kpi-trend.positive {
  color: #0f766e;
  background: #e0f2f1;
  border-color: #c0e6df;
}

.kpi-trend.warning {
  color: #b45309;
  background: #fff7ed;
  border-color: #fde68a;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.kpi-card:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  border-color: #d1e9e3;
}

.task-panel {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.task-head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #0f172a;
}

.task-date {
  margin: 0.15rem 0 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.task-chip {
  background: #e0f2f1;
  color: #0f766e;
  font-weight: 700;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  border: 1px solid #c0e6df;
  font-size: 0.9rem;
}

.task-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

.task-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1rem;
  background: #f9fafb;
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;
}

.task-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.35rem;
}

.pill {
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 700;
}

.pill-green {
  background: #e0f2f1;
  color: #0f766e;
}

.pill-gray {
  background: #e5e7eb;
  color: #4b5563;
}

.time {
  font-weight: 700;
  color: #111827;
}

.accommodation {
  font-weight: 700;
  color: #111827;
  margin: 0.1rem 0;
}

.guest {
  margin: 0;
  color: #374151;
  font-size: 0.95rem;
}

.memo {
  margin: 0.35rem 0 0;
  color: #b45309;
  font-size: 0.9rem;
}

.task-card:hover {
  border-color: #0f766e;
  background: #f0fcf9;
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.07);
}

.empty {
  text-align: center;
  color: #9ca3af;
  margin: 1rem 0 0;
}

.footnote {
  margin: 0.75rem 0 0;
  font-size: 0.85rem;
  color: #6b7280;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 80;
}

.modal {
  background: white;
  border-radius: 14px;
  padding: 1.25rem;
  width: min(420px, 100%);
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.2);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.close-btn {
  border: none;
  background: #f5f5f5;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1.1rem;
  cursor: pointer;
}

.modal-body {
  margin-top: 1rem;
  display: grid;
  gap: 0.55rem;
}

.modal-row {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.modal-row span:first-child {
  color: #6b7280;
}

.eyebrow.small {
  font-size: 0.8rem;
  margin: 0;
}

@media (min-width: 768px) {
  .task-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1024px) {
  .view-header h2 {
    font-size: 2rem;
  }

  .task-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
