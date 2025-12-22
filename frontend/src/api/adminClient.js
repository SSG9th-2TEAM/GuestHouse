// Lightweight admin API client scaffold using the admin base URL.
// The base URL is configurable so user/admin servers can be split later.
const adminBaseURL = (import.meta.env.VITE_ADMIN_API_BASE_URL || '/admin-api').replace(/\/$/, '')

let adminAuthToken = ''

export function setAdminAuthToken(token) {
  adminAuthToken = token || ''
}

export async function adminRequest(path, options = {}) {
  const url = `${adminBaseURL}${path.startsWith('/') ? path : `/${path}`}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (adminAuthToken) {
    headers.Authorization = `Bearer ${adminAuthToken}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  // In V1 we keep this simple; consumers can handle non-OK states.
  const data = await response.json().catch(() => null)
  return { ok: response.ok, status: response.status, data }
}

// Convenience GET wrapper for future use (currently unused, ready for swap-in)
export async function adminGet(path, params = {}) {
  const query = new URLSearchParams(params).toString()
  const fullPath = query ? `${path}?${query}` : path
  return adminRequest(fullPath, { method: 'GET' })
}

// Host API client (shares same fetch wrapper style)
const hostBaseURL = (import.meta.env.VITE_HOST_API_BASE_URL || '/api').replace(/\/$/, '')

let hostAuthToken = ''

export function setHostAuthToken(token) {
  hostAuthToken = token || ''
}

export async function hostRequest(path, options = {}) {
  const url = `${hostBaseURL}${path.startsWith('/') ? path : `/${path}`}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (hostAuthToken) {
    headers.Authorization = `Bearer ${hostAuthToken}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  const data = await response.json().catch(() => null)
  return { ok: response.ok, status: response.status, data }
}

export async function hostGet(path, params = {}) {
  const query = new URLSearchParams(params).toString()
  const fullPath = query ? `${path}?${query}` : path
  return hostRequest(fullPath, { method: 'GET' })
}
