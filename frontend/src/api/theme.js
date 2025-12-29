import { hostGet } from './adminClient'

export async function fetchThemes() {
  return hostGet('/themes')
}

export async function fetchUserThemes() {
  return hostGet('/themes/me')
}
