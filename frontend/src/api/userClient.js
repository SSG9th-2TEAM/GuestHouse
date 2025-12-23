import { authenticatedRequest } from './authClient';

// 본인 계정 삭제
export async function deleteSelf(reasons, otherReason) {
  return authenticatedRequest('/api/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ reasons, otherReason }),
  });
}
