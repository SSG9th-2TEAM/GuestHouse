import { createRouter, createWebHistory } from 'vue-router'
import { getUserInfo, getAccessToken, getCurrentUser, saveUserInfo } from '@/api/authClient';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    scrollBehavior(to, from, savedPosition) {
        // 뒤로가기/앞으로가기 시 이전 스크롤 위치 복원
        if (savedPosition) {
            return savedPosition;
        }
        // 새 페이지 이동 시 상단으로 스크롤
        return { top: 0 };
    },
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('../views/home/HomeView.vue')
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
            component: () => import('../views/booking/BookingView.vue'),
            props: (route) => ({
                accommodationsId: route.params.id || null,
                roomId: route.query.roomId || null,
                guestCount: route.query.guestCount || null,
                checkin: route.query.checkin || null,
                checkout: route.query.checkout || null
            })
        },
        {
            path: '/booking/success',
            name: 'booking-success',
            component: () => import('../views/booking/BookingSuccessView.vue')
        },
        {
            path: '/payment/:reservationId',
            name: 'payment',
            component: () => import('../views/payment/PaymentView.vue')
        },
        {
            path: '/payment/success',
            name: 'payment-success',
            component: () => import('../views/payment/PaymentSuccessView.vue')
        },
        {
            path: '/payment/fail',
            name: 'payment-fail',
            component: () => import('../views/payment/PaymentFailView.vue')
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
            path: '/social-signup',
            name: 'social-signup',
            component: () => import('../views/SocialSignupView/SocialSignupView.vue')
        },
        {
            path: '/oauth2/redirect',
            name: 'oauth-redirect',
            component: () => import('../views/OAuth2RedirectView/OAuth2RedirectView.vue')
        },
        {
            path: '/find-email',
            name: 'find-email',
            component: () => import('../views/LoginView/FindEmailView.vue')
        },
        {
            path: '/find-password',
            name: 'find-password',
            component: () => import('../views/LoginView/FindPasswordView.vue')
        },
        {
            path: '/reset-password',
            name: 'reset-password',
            component: () => import('../views/LoginView/ResetPasswordView.vue')
        },
        {
            path: '/reset-password-success',
            name: 'reset-password-success',
            component: () => import('../views/LoginView/ResetPasswordSuccessView.vue')
        },
        {
            path: '/oauth2/redirect',
            name: 'oauth2-redirect',
            component: () => import('../views/LoginView/OAuth2RedirectHandler.vue')
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
                },
                {
                    path: 'report',
                    name: 'host-report',
                    component: () => import('../views/HostView/HostReportView.vue')
                }
            ]
        },
        {
            path: '/admin',
            component: () => import('../views/AdminView/AdminLayout.vue'),
            children: [
                {
                    path: '',
                    redirect: '/admin/dashboard'
                },
                {
                    path: 'dashboard',
                    component: () => import('../views/AdminView/AdminDashboardLayout.vue'),
                    children: [
                        {
                            path: '',
                            name: 'admin-dashboard',
                            component: () => import('../views/AdminView/AdminDashboardView.vue')
                        },
                        {
                            path: 'issues',
                            name: 'admin-dashboard-issues',
                            component: () => import('../views/AdminView/AdminIssuesView.vue')
                        },
                        {
                            path: 'weekly',
                            name: 'admin-dashboard-weekly',
                            component: () => import('../views/AdminView/AdminWeeklyReportView.vue')
                        },
                        {
                            path: 'audit',
                            name: 'admin-dashboard-audit',
                            component: () => import('../views/AdminView/AdminAuditLogView.vue')
                        }
                    ]
                },
                {
                    path: 'users',
                    name: 'admin-users',
                    component: () => import('../views/AdminView/AdminUsersView.vue')
                },
                {
                    path: 'accommodations',
                    name: 'admin-accommodations',
                    component: () => import('../views/AdminView/AdminAccommodationsView.vue')
                },
                {
                    path: 'bookings',
                    name: 'admin-bookings',
                    component: () => import('../views/AdminView/AdminBookingsView.vue')
                },
                {
                    path: 'payments',
                    name: 'admin-payments',
                    component: () => import('../views/AdminView/AdminPaymentsView.vue')
                },
                {
                    path: 'reports',
                    name: 'admin-reports',
                    component: () => import('../views/AdminView/AdminReportsView.vue')
                },
                {
                    path: 'issues',
                    redirect: '/admin/dashboard/issues'
                },
                {
                    path: 'reports/weekly',
                    redirect: '/admin/dashboard/weekly'
                }
            ]
        },
        {
            path: '/policy',
            name: 'policy',
            component: () => import('../views/policy/PolicyView.vue')
        }
    ]
})

router.beforeEach(async (to, from, next) => {
    const token = getAccessToken();
    let userInfo = getUserInfo();
    const isAdminRoute = to.path.startsWith('/admin');
    const isHostReportRoute = to.path.startsWith('/host/report');
    const isHostRole = (info) => {
        if (!info) return false;
        const role = info.role;
        return role === 'HOST' || role === 'ROLE_HOST' || info.hostApproved === true;
    };

    if (!userInfo && token) {
        const response = await getCurrentUser();
        if (response.ok && response.data) {
            saveUserInfo(response.data);
            userInfo = response.data;
        } else if (response.status === 401) {
            next('/login');
            return;
        } else if (response.status === 403) {
            alert('접근 권한이 없습니다.');
            next('/host');
            return;
        }
    }

    if (isAdminRoute) {
        if (!userInfo) {
            next('/login');
            return;
        }
        if (userInfo.role !== 'ADMIN') {
            alert('접근 권한이 없습니다.');
            next('/');
            return;
        }
        next();
        return;
    }

    if (isHostReportRoute) {
        if (!userInfo) {
            next('/login');
            return;
        }
        if (!isHostRole(userInfo) && token) {
            const response = await getCurrentUser();
            if (response.ok && response.data) {
                saveUserInfo(response.data);
                userInfo = response.data;
            } else if (response.status === 401) {
                next('/login');
                return;
            } else if (response.status === 403) {
                alert('접근 권한이 없습니다.');
                next('/host');
                return;
            }
        }
        if (!isHostRole(userInfo)) {
            alert('접근 권한이 없습니다.');
            next('/host');
            return;
        }
        next();
        return;
    }

    next();
});

export default router
