export const dashboardStats = [
  { label: '전체 플랫폼 이용자', value: '12,480명', sub: '이달 +4.2%', tone: 'primary' },
  { label: '플랫폼 수익', value: '₩184,200,000', sub: '전월 대비 +6.8%', tone: 'success' },
  { label: '이번 달 거래 건수', value: '3,280건', sub: '전월 대비 +3.1%', tone: 'neutral' },
  { label: '등록 숙소 수', value: '1,248 (활성 1,180)', sub: '승인 대기 18건', tone: 'accent' }
]

export const dashboardPendingListings = [
  { name: '해변 감성 스테이', host: '이호스트', location: '부산 해운대', submittedAt: '2025-01-06' },
  { name: '도심 스튜디오', host: '박호스트', location: '서울 강남', submittedAt: '2025-01-05' },
  { name: '한옥 리트릿', host: '정호스트', location: '전주 한옥마을', submittedAt: '2025-01-03' }
]

export const dashboardRevenueTrend = {
  months: ['8월', '9월', '10월', '11월', '12월', '1월'],
  values: [22, 25, 28, 30, 34, 37]
}

export const dashboardTransactions = {
  yearly: [320, 280, 360, 410, 380, 420, 460, 440, 400, 430, 470, 520],
  monthly: [12, 18, 16, 20, 22, 26, 28, 24, 30, 32, 29, 34]
}

export const usersStats = {
  total: '24,200명',
  guests: '16,840명',
  hosts: '7,360명'
}

export const adminUsers = [
  {
    name: '이서준',
    email: 'seojun@example.com',
    type: '게스트',
    joinedAt: '2024-11-12',
    activity: '최근 30일 내 3회 예약',
    status: '활성'
  },
  {
    name: '김하늘',
    email: 'haneul@example.com',
    type: '호스트',
    joinedAt: '2024-08-21',
    activity: '신규 숙소 등록 2건',
    status: '활성'
  },
  {
    name: '박지민',
    email: 'jimin@example.com',
    type: '게스트',
    joinedAt: '2023-12-04',
    activity: '최근 90일 미접속',
    status: '휴면'
  },
  {
    name: '최민서',
    email: 'minseo@example.com',
    type: '호스트',
    joinedAt: '2022-05-19',
    activity: '월 매출 4,200,000원',
    status: '활성'
  }
]

export const accommodations = [
  {
    name: '바다 전망 스위트',
    host: '이호스트',
    location: '부산 해운대',
    rating: 4.8,
    bookings: 128,
    revenue: '₩48,200,000',
    status: '승인됨',
    registeredAt: '2024-10-02'
  },
  {
    name: '한강 뷰 아파트',
    host: '김호스트',
    location: '서울 용산',
    rating: 4.6,
    bookings: 94,
    revenue: '₩35,100,000',
    status: '검토중',
    registeredAt: '2024-12-14'
  },
  {
    name: '숲속 캠핑돔',
    host: '박호스트',
    location: '가평',
    rating: 4.7,
    bookings: 76,
    revenue: '₩21,900,000',
    status: '승인됨',
    registeredAt: '2024-08-30'
  }
]

export const bookings = [
  {
    id: 'BK-202501-341',
    accommodation: '바다 전망 스위트',
    host: '이호스트',
    guest: '최가람',
    checkIn: '2025-01-21',
    checkOut: '2025-01-24',
    people: 2,
    price: '₩520,000',
    status: '확정'
  },
  {
    id: 'BK-202501-297',
    accommodation: '한강 뷰 아파트',
    host: '김호스트',
    guest: '박수진',
    checkIn: '2025-01-18',
    checkOut: '2025-01-20',
    people: 3,
    price: '₩410,000',
    status: '대기'
  },
  {
    id: 'BK-202501-255',
    accommodation: '숲속 캠핑돔',
    host: '박호스트',
    guest: '김민재',
    checkIn: '2025-01-25',
    checkOut: '2025-01-27',
    people: 4,
    price: '₩360,000',
    status: '체크인'
  }
]

export const paymentsStats = {
  totalVolume: '₩184,200,000',
  platformFee: '₩12,894,000',
  transactions: '3,280건'
}

export const payments = [
  {
    id: 'PM-98211',
    host: '김호스트',
    guest: '최가람',
    accommodation: '바다 전망 스위트',
    amount: '₩520,000',
    fee: '₩36,400',
    type: '예약',
    status: '완료',
    date: '2025-01-12'
  },
  {
    id: 'PM-98104',
    host: '이호스트',
    guest: '박수진',
    accommodation: '한강 뷰 아파트',
    amount: '₩410,000',
    fee: '₩28,700',
    type: '예약',
    status: '환불',
    date: '2025-01-10'
  },
  {
    id: 'PM-97945',
    host: '박호스트',
    guest: '김민재',
    accommodation: '숲속 캠핑돔',
    amount: '₩360,000',
    fee: '₩25,200',
    type: '예약',
    status: '완료',
    date: '2025-01-08'
  }
]

export const reportsStats = {
  pending: 6,
  inProgress: 3,
  resolved: 18
}

export const reports = [
  {
    id: 'RP-2025-104',
    reporter: '김유나',
    target: '박호스트',
    reason: '청결 문제',
    summary: '체크인 시 침구 상태 불량',
    createdAt: '2025-01-12 09:30',
    status: '대기'
  },
  {
    id: 'RP-2025-099',
    reporter: '이도윤',
    target: '이호스트',
    reason: '환불 분쟁',
    summary: '취소 수수료 관련 문의',
    createdAt: '2025-01-10 15:10',
    status: '처리중'
  },
  {
    id: 'RP-2025-093',
    reporter: '최수민',
    target: '김호스트',
    reason: '소음 신고',
    summary: '야간 소음 지속',
    createdAt: '2025-01-08 21:45',
    status: '완료'
  }
]
