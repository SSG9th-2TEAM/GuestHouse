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
            path: '/list',
            name: 'list',
            component: () => import('../views/home/List.vue')
        },
        {
            path: '/map',
            name: 'map',
            component: () => import('../views/home/MapView.vue')
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
            component: () => import('../views/HostView/HostDashboardView.vue'),
            children: [
                {
                    path: '',
                    name: 'host-home',
                    component: () => import('../views/HostView/HostHomeView.vue')
                },
                {
                    path: 'accommodation',
                    name: 'host-accommodation',
                    component: () => import('../views/HostView/HostAccommodationView.vue')
                },
                {
                    path: 'accommodation/register',
                    name: 'host-register',
                    component: () => import('../views/HostView/HostAccommodationRegister.vue')
                },
                {
                    path: 'accommodation/edit/:id',
                    name: 'host-edit',
                    component: () => import('../views/HostView/HostAccommodationEdit.vue')
                },
                {
                    path: 'booking',
                    name: 'host-booking',
                    component: () => import('../views/HostView/HostBookingView.vue')
                },
                {
                    path: 'review',
                    name: 'host-review',
                    component: () => import('../views/HostView/HostReviewView.vue')
                },
                {
                    path: 'revenue',
                    name: 'host-revenue',
                    component: () => import('../views/HostView/HostRevenueView.vue')
                }
            ]
        }
    ]
})

export default router
