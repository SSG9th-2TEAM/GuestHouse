export const USER_STATUS_LABELS = {
  ACTIVE: '활성',
  SUSPENDED: '정지',
  DORMANT: '휴면'
}

export const USER_STATUS_VARIANTS = {
  ACTIVE: 'success',
  SUSPENDED: 'danger',
  DORMANT: 'warning'
}

export const getUserStatusLabel = (status) => {
  const key = String(status ?? 'ACTIVE').toUpperCase()
  return USER_STATUS_LABELS[key] ?? '활성'
}

export const getUserStatusVariant = (status) => {
  const key = String(status ?? 'ACTIVE').toUpperCase()
  return USER_STATUS_VARIANTS[key] ?? 'success'
}
