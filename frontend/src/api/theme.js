import { hostGet } from './adminClient'

export async function fetchThemes() {
  return hostGet('/themes')
}
