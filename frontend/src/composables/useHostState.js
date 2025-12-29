export const normalizeApprovalStatus = (status) => {
  if (status === null || status === undefined) return 'unknown'
  const value = String(status).trim().toLowerCase()
  if (value === 'approved' || value === 'approve') return 'approved'
  if (
    value === 'pending' ||
    value === 'inspection' ||
    value === 'review' ||
    value === 'reviewing' ||
    value === 'under_review' ||
    value === 'underreview' ||
    value === '검수중'
  ) {
    return 'pending'
  }
  if (value === 'rejected' || value === 'reject' || value === 'denied') return 'rejected'
  if (
    value === 'active' ||
    value === 'inactive' ||
    value === 'operating' ||
    value === 'stopped' ||
    value === 'stop' ||
    value === 'paused'
  ) {
    return 'approved'
  }
  if (value === '1' || value === 'true') return 'approved'
  if (value === '2') return 'pending'
  if (value === '3' || value === '0') return 'rejected'
  return 'unknown'
}

export const deriveHostState = (accommodations = []) => {
  const list = Array.isArray(accommodations) ? accommodations : []
  const counts = {
    total: list.length,
    approved: 0,
    pending: 0,
    rejected: 0,
    unknown: 0
  }

  list.forEach((item) => {
    const statusSource = item?.approvalStatus ?? item?.status ?? item?.accommodationStatus ?? item?.reviewStatus
    const normalized = normalizeApprovalStatus(statusSource)
    if (normalized === 'approved') counts.approved += 1
    else if (normalized === 'pending') counts.pending += 1
    else if (normalized === 'rejected') counts.rejected += 1
    else counts.unknown += 1
  })

  let hostState = 'empty'
  if (counts.total === 0) hostState = 'empty'
  else if (counts.approved > 0) hostState = 'active'
  else if (counts.pending > 0) hostState = 'pending-only'
  else if (counts.rejected > 0) hostState = 'rejected'
  else hostState = 'pending-only'

  return { hostState, counts }
}
