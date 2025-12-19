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
