import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('../views/home/HomeView.vue')
        },
        {
            path: '/profile',
            name: 'profile',
            component: () => import('../views/mypage/ProfileView.vue')
        },
        {
            path: '/room/:id',
            name: 'room-detail',
            component: () => import('../views/home/RoomDetailView.vue')
        },
        {
            path: '/booking/:id',
            name: 'booking',
            component: () => import('../views/booking/BookingView.vue')
        },
        {
            path: '/booking/success',
            name: 'booking-success',
            component: () => import('../views/booking/BookingSuccessView.vue')
        },
        {
            path: '/reservations',
            name: 'reservations',
            component: () => import('../views/mypage/ReservationHistoryView.vue')
        },
        {
            path: '/reservations/cancel/:id',
            name: 'reservation-cancel',
            component: () => import('../views/mypage/ReservationCancelView.vue')
        },
        {
            path: '/wishlist',
            name: 'wishlist',
            component: () => import('../views/mypage/WishlistView.vue')
        },
        {
            path: '/coupons',
            name: 'coupons',
            component: () => import('../views/mypage/CouponView.vue')
        },
        {
            path: '/reviews',
            name: 'reviews',
            component: () => import('../views/mypage/ReviewHistoryView.vue')
        },
        {
            path: '/write-review',
            name: 'write-review',
            component: () => import('../views/mypage/WriteReviewView.vue')
        },
        {
            path: '/delete-account',
            name: 'delete-account',
            component: () => import('../views/mypage/DeleteAccountView.vue')
        },
        {
            path: '/login',
            name: 'login',
            component: () => import('../views/LoginView/LoginView.vue')
        },
        {
            path: '/register',
            name: 'register',
            component: () => import('../views/RegisterView/RegisterView.vue')
        },
        {
            path: '/host',
            name: 'host-dashboard',
            component: () => import('../views/HostView/HostDashboardView.vue')
        }
    ]
})

export default router
