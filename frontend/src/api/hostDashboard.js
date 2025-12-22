import { hostGet } from './adminClient'

export async function fetchHostDashboardSummary({ year, month }) {
  const params = { year, month }
  return hostGet('/host/dashboard/summary', params)
}

export async function fetchHostTodaySchedule({ date }) {
  const params = { date }
  return hostGet('/host/dashboard/today-schedule', params)
}
