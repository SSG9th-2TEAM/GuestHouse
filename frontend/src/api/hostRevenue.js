import { hostGet } from './adminClient'

export async function fetchHostRevenueSummary({ year, month }) {
  const params = { year, month }
  return hostGet('/host/revenue/summary', params)
}

export async function fetchHostRevenueTrend({ year }) {
  const params = { year }
  return hostGet('/host/revenue/trend', params)
}

export async function fetchHostRevenueDetails({ from, to }) {
  const params = { from, to }
  return hostGet('/host/revenue/details', params)
}
