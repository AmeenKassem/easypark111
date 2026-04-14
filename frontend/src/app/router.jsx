import { createBrowserRouter } from 'react-router-dom'
import DriverPage from '../pages/driver.jsx'
import OwnerPage from '../pages/owner.jsx'
import SharedPage from '../pages/shared.jsx'
import LoginPage from '../pages/login.jsx'
import RegisterPage from '../pages/register.jsx'
import ResetPasswordPage from '../pages/ResetPasswordPage.jsx'
import CreateParkingPage from '../pages/CreateParkingPage.jsx'
import NoPermissionPage from '../pages/NoPermissionPage.jsx'
import ManageSpotsPage from '../pages/manageSpots.jsx'
import ManageProfilePage from '../pages/manage-profile'
import ChangePasswordPage from '../pages/ChangePasswordPage.jsx'
import MyBookingsPage from '../pages/MyBookingsPage.jsx'
import ForgotPasswordPage from '../pages/ForgotPasswordPage.jsx'
import RevenuesPage from '../pages/RevenuesPage.jsx'
import ExpensesPage from '../pages/ExpensesPage.jsx'

import RequireRole from '../components/auth/RequireRole.jsx'

export const router = createBrowserRouter([
    { path: '/', element: <LoginPage /> },
    { path: '/login', element: <LoginPage /> },
    { path: '/register', element: <RegisterPage /> },
    { path: '/reset-password', element: <ResetPasswordPage /> },
    { path: '/forgot-password', element: <ForgotPasswordPage /> },

    // Optional: dashboard should be role-aware, not always DriverPage
    {
        path: '/dashboard',
        element: (
            <RequireRole allow={['DRIVER', 'OWNER']}>
                <SharedPage />
            </RequireRole>
        ),
    },

    {
        path: '/driver',
        element: (
            <RequireRole allow={['DRIVER']}>
                <DriverPage />
            </RequireRole>
        ),
    },

    {
        path: '/owner',
        element: (
            <RequireRole allow={['OWNER']}>
                <OwnerPage />
            </RequireRole>
        ),
    },

    {
        path: '/manage-spots',
        element: (
            <RequireRole allow={['OWNER']}>
                <ManageSpotsPage />
            </RequireRole>
        ),
    },

    {
        path: '/manage-profile',
        element: (
            <RequireRole allow={['DRIVER', 'OWNER']}>
                <ManageProfilePage />
            </RequireRole>
        ),
    },
    {
        path: '/change-password',
        element: (
            <RequireRole allow={['DRIVER', 'OWNER']}>
                <ChangePasswordPage />
            </RequireRole>
        ),
    },
    {
        path: '/my-bookings',
        element: (
            <RequireRole allow={['DRIVER']}>
                <MyBookingsPage />
            </RequireRole>
        ),
    },

    {
        path: '/revenues',
        element: (
            <RequireRole allow={['DRIVER', 'OWNER']}>
                <RevenuesPage />
            </RequireRole>
        ),
    },

    {
        path: '/expenses',
        element: (
            <RequireRole allow={['DRIVER']}>
                <ExpensesPage />
            </RequireRole>
        ),
    },


    {
        path: '/create-parking',
        element: (
            <RequireRole allow={['OWNER']}>
                <CreateParkingPage />
            </RequireRole>
        ),
    },

    { path: '/no-permission', element: <NoPermissionPage /> },
])
