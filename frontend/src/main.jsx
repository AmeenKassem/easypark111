import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { LoadScript } from '@react-google-maps/api'
import { GoogleOAuthProvider } from '@react-oauth/google'
import { router } from './app/router.jsx'
import './index.css'

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const libraries = ['places']

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <GoogleOAuthProvider clientId={googleClientId}>
            <LoadScript
                googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_KEY}
                libraries={libraries}
                loadingElement={<div>Loading Maps...</div>}
            >
                <RouterProvider router={router} />
            </LoadScript>
        </GoogleOAuthProvider>
    </React.StrictMode>
)
