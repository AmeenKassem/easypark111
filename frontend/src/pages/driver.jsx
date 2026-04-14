import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { Autocomplete } from '@react-google-maps/api'
import '../styles/driver.css'
import MapComponent from '../components/map/mapComponent'
import { logout, getCurrentUser, subscribeAuthChanged } from '../services/session'
import ProfileModal from '../components/modals/ProfileModal'
import BookParkingModal from '../components/modals/BookParkingModal'
import DatePicker from 'react-datepicker'
import 'react-datepicker/dist/react-datepicker.css'
import { generateTimeOptions } from '../utils/timeOptions'
import TimeDropdown from '../components/inputs/TimeDropdown'
import {API_BASE_URL} from "../config.js";

const toYMD = (d) => {
    if (!d) return ''
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
}

const getCurrentTimeHHMM = () => {
    const now = new Date()
    return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}
const timeToMins = (hhmm) => {
    if (!hhmm) return 0
    const [h, m] = String(hhmm).split(':').map(Number)
    return h * 60 + m
}

const dateTimeToMins = (d) => d.getHours() * 60 + d.getMinutes()

const sameLocalDate = (a, b) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()

const matchesAvailabilityWindow = (spot, wantFrom, wantTo) => {
    const type = String(spot?.availabilityType || '').toUpperCase()

    // If the driver picked a window, be strict: spot must define availability.
    if (!type) return false

    // SPECIFIC: any slot fully contains the requested window
    if (type === 'SPECIFIC') {
        const slots = Array.isArray(spot?.specificAvailability) ? spot.specificAvailability : []
        return slots.some((sl) => {
            if (!sl?.start || !sl?.end) return false
            const slotStart = new Date(sl.start)
            const slotEnd = new Date(sl.end)
            return slotStart <= wantFrom && slotEnd >= wantTo
        })
    }

    // RECURRING: must be same day and within one of the day's schedules
    if (type === 'RECURRING') {
        if (!sameLocalDate(wantFrom, wantTo)) return false
        const day = wantFrom.getDay() // JS: 0=Sun..6=Sat (matches your DB comment)
        const wantStartM = dateTimeToMins(wantFrom)
        const wantEndM = dateTimeToMins(wantTo)

        const schedules = Array.isArray(spot?.recurringSchedule) ? spot.recurringSchedule : []
        return schedules.some((sc) => {
            if (sc?.dayOfWeek == null || !sc?.start || !sc?.end) return false
            if (Number(sc.dayOfWeek) !== day) return false
            const scStartM = timeToMins(sc.start)
            const scEndM = timeToMins(sc.end)
            return scStartM <= wantStartM && scEndM >= wantEndM
        })
    }

    return false
}

// --- HELPER: Generate Time Slots ---

// --- ICONS ---
function IconUser({ size = 18 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 12a4.2 4.2 0 1 0-4.2-4.2A4.2 4.2 0 0 0 12 12Z" stroke="currentColor" strokeWidth="1.8" />
            <path d="M4.5 20.2c1.4-4.2 13.6-4.2 15 0" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}
function IconSearch({ size = 18 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M10.5 18.5a8 8 0 1 1 5.2-14.1A8 8 0 0 1 10.5 18.5Z" stroke="currentColor" strokeWidth="1.8" />
            <path d="M16.8 16.8 21 21" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
    )
}
function IconSliders({ size = 18 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 6h10M18 6h2M4 12h2M10 12h10M4 18h6M14 18h6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            <path d="M14 6a2 2 0 1 0 0 .01ZM8 12a2 2 0 1 0 0 .01ZM12 18a2 2 0 1 0 0 .01Z" fill="currentColor" />
        </svg>
    )
}

// --- STYLES ---
const labelStyle = {
    fontSize: '13px',
    fontWeight: '600',
    color: '#64748b',
    marginBottom: '4px',
    display: 'block'
}

const inputStyle = {
    width: '100%',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    padding: '0 12px',
    height: '48px',
    fontSize: '16px',
    outline: 'none',
    color: '#0f172a',
    fontFamily: 'inherit',
    backgroundColor: '#fff'
}

const selectStyle = {
    ...inputStyle,
    cursor: 'pointer',
    appearance: 'none',
    backgroundImage: `url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%2364748b%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E")`,
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'right 12px center',
    backgroundSize: '12px',
    paddingRight: '35px'
}

const inputWrapperStyle = {
    display: 'flex',
    alignItems: 'center',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    height: '48px',
    backgroundColor: '#fff',
    transition: 'border-color 0.2s'
};

const stepperBtnStyle = {
    border: 'none',
    background: 'transparent',
    color: '#64748b',
    fontSize: '18px',
    padding: '0 10px',
    cursor: 'pointer',
    height: '100%',
    display: 'flex',
    alignItems: 'center'
};

const ToggleSwitch = ({ label, checked, onChange }) => (
    <div
        onClick={() => onChange(!checked)}
        style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            border: '1px solid #e2e8f0',
            borderRadius: '10px',
            height: '48px',
            padding: '0 15px',
            cursor: 'pointer',
            backgroundColor: checked ? '#eff6ff' : '#fff',
            borderColor: checked ? '#3b82f6' : '#e2e8f0',
            transition: 'all 0.2s',
            marginBottom: '0'
        }}
    >
        <span style={{ fontSize: '14px', fontWeight: '500', color: checked ? '#1d4ed8' : '#64748b' }}>
            {label}
        </span>
        <div style={{
            width: '36px',
            height: '20px',
            backgroundColor: checked ? '#3b82f6' : '#cbd5e1',
            borderRadius: '20px',
            position: 'relative',
            transition: 'all 0.2s'
        }}>
            <div style={{
                width: '16px',
                height: '16px',
                backgroundColor: 'white',
                borderRadius: '50%',
                position: 'absolute',
                top: '2px',
                left: checked ? '18px' : '2px',
                transition: 'all 0.2s',
                boxShadow: '0 1px 2px rgba(0,0,0,0.2)'
            }} />
        </div>
    </div>
)

const defaultCenter = { lat: 32.0853, lng: 34.7818 }

export default function DriverPage() {
    const nav = useNavigate()
    const location = useLocation()

    const [user, setUser] = useState(getCurrentUser())
    const roles = useMemo(() => new Set(user?.roles ?? []), [user])

    // Map control
    const mapRef = useRef(null)

    // Autocomplete refs/state
    const [autocomplete, setAutocomplete] = useState(null)
    const isPlaceSelectedRef = useRef(false)
    const inputRef = useRef(null)

    // Filters State
    const [address, setAddress] = useState('')
    const [searchBounds, setSearchBounds] = useState(null)
    const [filtersOpen, setFiltersOpen] = useState(false)
    const [coveredOnly, setCoveredOnly] = useState(false)
    const [maxPrice, setMaxPrice] = useState('')

    // Date/Time State
    const [filterDate, setFilterDate] = useState('')
    const [filterStart, setFilterStart] = useState('')
    const [filterEnd, setFilterEnd] = useState('')
    const getStartOptions = () => timeOptions

    const getValidStartTimes = (selectedDateYMD) => {
        if (!selectedDateYMD) return getStartOptions()

        const todayStr = toYMD(new Date())

        if (selectedDateYMD === todayStr) {
            const currentHm = getCurrentTimeHHMM()
            return getStartOptions().filter((t) => t > currentHm)
        }
        return getStartOptions()
    }

    const getValidEndTimes = (selectedDateYMD, startTime) => {
        if (!selectedDateYMD) return timeOptions
        if (!startTime) return timeOptions
        return timeOptions.filter((t) => t > startTime)
    }

    const timeOptions = useMemo(() => generateTimeOptions(), []);

    // Data
    const [allSpots, setAllSpots] = useState([])
    const [loading, setLoading] = useState(false)
    const [mapCenter, setMapCenter] = useState(defaultCenter)

    // UI Modals
    const [bookingOpen, setBookingOpen] = useState(false)
    const [bookingSpot, setBookingSpot] = useState(null)
    const [bookingToast, setBookingToast] = useState('')
    const [profileOpen, setProfileOpen] = useState(false)
    const profileBtnRef = useRef(null)
    const [profileMenuPos, setProfileMenuPos] = useState({ top: 0, left: 0 })
    const [isProfileModalOpen, setProfileModalOpen] = useState(false)
    useEffect(() => {
        if (!filterDate) {
            if (filterStart) setFilterStart('')
            if (filterEnd) setFilterEnd('')
            return
        }

        // If start is no longer valid for the selected date (e.g., today and time already passed) -> clear it
        const validStarts = getValidStartTimes(filterDate)
        if (filterStart && !validStarts.includes(filterStart)) {
            setFilterStart('')
        }
    }, [filterDate]) // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        if (!filterStart) {
            if (filterEnd) setFilterEnd('')
            return
        }
        if (filterEnd && filterEnd <= filterStart) {
            setFilterEnd('')
        }
    }, [filterStart])

    // Sync User
    useEffect(() => {
        return subscribeAuthChanged(() => setUser(getCurrentUser()))
    }, [])

    useEffect(() => {
        setProfileOpen(false)
        setFiltersOpen(false)
    }, [location.key])

    useEffect(() => {
        setUser(getCurrentUser())
    }, [location.key])

    // SERVER FETCH (only when server-query params change)
    useEffect(() => {
        const fetchSpots = async () => {
            setLoading(true)
            try {
                const params = {}
                if (coveredOnly) params.covered = true

                const max = Number(maxPrice)
                if (Number.isFinite(max) && max > 0) params.maxPrice = max

                const response = await axios.get(`${API_BASE_URL}/api/parking-spots/search`, {
                    params,
                    headers: { Authorization: `Bearer ${localStorage.getItem('easypark_token')}` },
                })

                setAllSpots(response.data || [])
            } catch (error) {
                console.error('Failed to fetch spots', error)
                setAllSpots([])
            } finally {
                setLoading(false)
            }
        }

        fetchSpots()
    }, [coveredOnly, maxPrice])

    // CLIENT FILTERING logic
    const filteredSpots = useMemo(() => {
        let data = Array.isArray(allSpots) ? [...allSpots] : []

        data = data.filter((s) => s?.active === true)


        // Date/Time Logic
// Date/Time Logic (uses availabilityType / schedules from ParkingResponse)
        if (filterDate) {
            const startStr = filterStart || '00:00'
            const endStr = filterEnd || '23:59'

            if (filterStart && filterEnd && endStr <= startStr) {
                return []
            }

            const wantFrom = new Date(`${filterDate}T${startStr}:00`)
            const wantTo = new Date(`${filterDate}T${endStr}:00`)

            data = data.filter((s) => matchesAvailabilityWindow(s, wantFrom, wantTo))
        }



        // Bounds Logic
        if (searchBounds) {
            data = data.filter((s) => {
                if (s?.lat == null || s?.lng == null) return false
                return (
                    s.lat <= searchBounds.north &&
                    s.lat >= searchBounds.south &&
                    s.lng <= searchBounds.east &&
                    s.lng >= searchBounds.west
                )
            })
            return data
        }

        // Text Search Logic
        if (address.trim()) {
            const q = address.trim().toLowerCase()
            data = data.filter((s) => (s?.location || '').toLowerCase().includes(q))
        }

        return data
    }, [allSpots,  filterDate, filterStart, filterEnd, searchBounds, address])

    // --- Place selection logic ---
    const handlePlaceSelect = (place) => {
        if (!place?.geometry?.location) return
        const lat = place.geometry.location.lat()
        const lng = place.geometry.location.lng()
        setMapCenter({ lat, lng })
        const types = place.types || []
        const isBroadArea = types.includes('locality') || types.includes('administrative_area_level_1') || types.includes('administrative_area_level_2')

        if (isBroadArea && place.geometry.viewport) {
            const bounds = place.geometry.viewport
            setSearchBounds({
                north: bounds.getNorthEast().lat(),
                south: bounds.getSouthWest().lat(),
                east: bounds.getNorthEast().lng(),
                west: bounds.getSouthWest().lng(),
            })
            if (mapRef.current?.fitBounds) mapRef.current.fitBounds(bounds)
        } else {
            const delta = 0.015
            setSearchBounds({
                north: lat + delta,
                south: lat - delta,
                east: lng + delta,
                west: lng - delta,
            })
            if (mapRef.current?.setZoom && mapRef.current?.panTo) {
                mapRef.current.setZoom(15)
                mapRef.current.panTo({ lat, lng })
            }
        }
        if (place.formatted_address) setAddress(place.formatted_address)
    }

    const onLoadAutocomplete = (au) => setAutocomplete(au)
    const onPlaceChanged = () => {
        isPlaceSelectedRef.current = true
        if (!autocomplete) return
        const place = autocomplete.getPlace()
        handlePlaceSelect(place)
    }

    const fetchFirstPredictionDetails = (inputText) => {
        return new Promise((resolve, reject) => {
            if (!window.google?.maps?.places) return reject(new Error('Google Maps Places API not loaded'))
            const autocompleteService = new window.google.maps.places.AutocompleteService()
            autocompleteService.getPlacePredictions({ input: inputText }, (predictions, status) => {
                if (status !== window.google.maps.places.PlacesServiceStatus.OK || !predictions || predictions.length === 0) {
                    return reject(new Error('No predictions found'))
                }
                const first = predictions[0]
                setAddress(first.description)
                const placesService = new window.google.maps.places.PlacesService(document.createElement('div'))
                placesService.getDetails({ placeId: first.place_id, fields: ['geometry', 'formatted_address', 'types', 'place_id'] }, (place, st) => {
                    if (st === window.google.maps.places.PlacesServiceStatus.OK && place) resolve(place)
                    else reject(new Error('Failed to get place details'))
                })
            })
        })
    }

    const handleSearchClick = () => {
        if (!address.trim()) return
        const highlighted = document.querySelector('.pac-item-selected')
        if (highlighted) return

        isPlaceSelectedRef.current = false
        setTimeout(async () => {
            if (isPlaceSelectedRef.current) return
            try {
                const place = await fetchFirstPredictionDetails(address)
                handlePlaceSelect(place)
                inputRef.current?.blur()
            } catch (err) { console.warn('Search click failed', err) }
        }, 250)
    }

    const handleInputKeyDown = (e) => {
        if (e.key !== 'Enter') return
        if (!address.trim()) return
        const highlighted = document.querySelector('.pac-item-selected')
        if (highlighted) return
        isPlaceSelectedRef.current = false
        setTimeout(async () => {
            if (isPlaceSelectedRef.current) return
            try {
                const place = await fetchFirstPredictionDetails(address)
                handlePlaceSelect(place)
                inputRef.current?.blur()
            } catch (err) { console.warn('Enter fallback failed', err) }
        }, 250)
    }

    const handleReset = () => {
        setAddress('')
        setSearchBounds(null)
        setCoveredOnly(false)
        setMaxPrice('')
        setFilterDate('')
        setFilterStart('')
        setFilterEnd('')
    }

    const doLogout = () => {
        try { localStorage.removeItem('easypark_token') } catch {}
        try { logout() } catch {}
        setProfileOpen(false)
        nav('/', { replace: true })
    }

    const openProfileMenu = () => {
        const el = profileBtnRef.current
        if (el) {
            const r = el.getBoundingClientRect()
            const menuWidth = 220
            const gap = 10
            const left = Math.min(window.innerWidth - menuWidth - 12, Math.max(12, r.right - menuWidth))
            const top = r.bottom + gap
            setProfileMenuPos({ top, left })
        }
        setProfileOpen(true)
    }

    useEffect(() => {
        if (!profileOpen) return
        const onResizeOrScroll = () => {
            const el = profileBtnRef.current
            if (!el) return
            const r = el.getBoundingClientRect()
            const menuWidth = 220
            const gap = 10
            const left = Math.min(window.innerWidth - menuWidth - 12, Math.max(12, r.right - menuWidth))
            const top = r.bottom + gap
            setProfileMenuPos({ top, left })
        }
        window.addEventListener('resize', onResizeOrScroll)
        window.addEventListener('scroll', onResizeOrScroll, true)
        return () => {
            window.removeEventListener('resize', onResizeOrScroll)
            window.removeEventListener('scroll', onResizeOrScroll, true)
        }
    }, [profileOpen])

    return (
        <div style={{ position: 'relative', height: '100dvh', width: '100vw', overflow: 'hidden', background: '#0b1220' }}>
            <div style={{ position: 'absolute', inset: 0, zIndex: 0 }}>
                <MapComponent
                    key={location.key}
                    spots={filteredSpots}
                    center={mapCenter}
                    currentUserId={user?.id}
                    onSpotClick={(spot) => {
                        setBookingSpot(spot)
                        setBookingOpen(true)
                    }}
                    onMapLoad={(map) => { mapRef.current = map }}
                />
            </div>

            <div style={{ position: 'absolute', inset: 0, zIndex: 10, pointerEvents: 'none' }}>
                {/* Header */}
                <div style={{ position: 'absolute', top: 0, left: 0, right: 0, padding: '12px 14px', background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(10px)', boxShadow: '0 8px 30px rgba(15, 23, 42, 0.12)', pointerEvents: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div onClick={() => nav('/driver')} role="button" style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
                        <span style={{ width: 42, height: 42, borderRadius: 999, background: 'rgb(37,99,235)', display: 'grid', placeItems: 'center', color: 'white', overflow: 'hidden' }} aria-hidden="true">
                          <img src="Logo_notext.png" alt="Logo" style={{ width: 41, height: 50, display: 'block', marginLeft: -1 }} />
                        </span>
                        <div style={{ fontWeight: 900, fontSize: 18, color: '#0549fa' }}>EasyPark</div>
                    </div>
                    <button ref={profileBtnRef} type="button" onClick={() => { if (profileOpen) setProfileOpen(false); else openProfileMenu() }} aria-label="Profile menu" style={{ width: 42, height: 42, borderRadius: 999, border: 0, background: '#2563eb', color: 'white', cursor: 'pointer', display: 'grid', placeItems: 'center', boxShadow: '0 10px 20px rgba(37, 99, 235, 0.25)' }}>
                        <IconUser size={18} />
                    </button>
                </div>

                {/* Search bar */}
                <div style={{ position: 'absolute', top: 74, left: 12, right: 12, pointerEvents: 'auto' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', borderRadius: 999, background: 'rgba(255,255,255,0.96)', boxShadow: '0 14px 40px rgba(15, 23, 42, 0.14)' }}>
                        <button
                            type="button"
                            onClick={handleSearchClick}
                            aria-label="Search"
                            style={{
                                border: 0,
                                background: 'transparent',
                                cursor: 'pointer',
                                padding: 0,
                                display: 'flex',
                                alignItems: 'center',
                                color: '#94a3b8'
                            }}
                        >
                            <IconSearch size={18} />
                        </button>
                        <div style={{ flex: 1 }}>
                            <Autocomplete onLoad={onLoadAutocomplete} onPlaceChanged={onPlaceChanged}>
                                <input ref={inputRef} value={address} onChange={(e) => { setAddress(e.target.value); if (e.target.value === '') setSearchBounds(null) }} onKeyDown={handleInputKeyDown} placeholder="Search location..." autoComplete="off" style={{ width: '100%', border: 0, outline: 'none', fontSize: 16, background: 'transparent', color: '#0f172a' }} />
                            </Autocomplete>
                        </div>
                        <button type="button" onClick={() => setFiltersOpen(true)} aria-label="Filters" style={{ width: 40, height: 40, borderRadius: 999, border: 0, background: 'transparent', cursor: 'pointer', display: 'grid', placeItems: 'center', color: '#2563eb' }}>
                            <IconSliders size={18} />
                        </button>
                    </div>
                </div>

                {/* Footer stats */}
                <div style={{ position: 'absolute', left: 12, bottom: 18, pointerEvents: 'none' }}>
                    <div style={{ pointerEvents: 'auto', background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(10px)', borderRadius: 999, padding: '8px 12px', boxShadow: '0 14px 40px rgba(15, 23, 42, 0.14)', fontWeight: 800, color: '#0f172a' }}>
                        {loading ? 'Loading...' : `${filteredSpots.length} spots found`}
                    </div>
                </div>

                {/* --- FILTERS MODAL (COMPACT & CENTERED) --- */}
                {filtersOpen && (
                    <div style={{
                        position: 'absolute',
                        inset: 0,
                        zIndex: 20000,
                        pointerEvents: 'auto',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        padding: '20px'
                    }}>
                        {/* Dark overlay backdrop */}
                        <button
                            type="button"
                            onClick={() => setFiltersOpen(false)}
                            style={{ position: 'absolute', inset: 0, background: 'rgba(15, 23, 42, 0.45)', border: 0 }}
                        />

                        {/* Modal Card */}
                        <div style={{
                            position: 'relative',
                            width: '100%',
                            maxWidth: '400px',
                            background: '#ffffff',
                            borderRadius: '20px',
                            padding: '24px',
                            boxShadow: '0 20px 50px rgba(0,0,0,0.2)',
                            maxHeight: '85vh',
                            overflowY: 'auto'
                        }}>

                            {/* Header */}
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
                                <div style={{ fontSize: '20px', fontWeight: '800', color: '#1e293b' }}>Filter Spots</div>
                                <button
                                    type="button"
                                    onClick={() => setFiltersOpen(false)}
                                    style={{ border: 0, background: 'transparent', fontSize: '24px', cursor: 'pointer', color: '#94a3b8', padding: 0, lineHeight: 1 }}
                                >
                                    &times;
                                </button>
                            </div>

                            <div style={{ display: 'grid', gap: '8px' }}>
                                {/* Toggles */}
                                <div style={{ marginBottom: '8px' }}>
                                    <ToggleSwitch label="Covered Only" checked={coveredOnly} onChange={setCoveredOnly} />
                                </div>


                                {/* Max Price */}
                                <div style={{ marginBottom: '16px' }}>
                                    <label style={labelStyle}>Max Price / Hour</label>
                                    <div style={{...inputWrapperStyle, padding: '0 8px'}}>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                const newVal = parseFloat(maxPrice || 0) - 1;
                                                if (newVal >= 0) setMaxPrice(newVal === 0 ? '' : newVal);
                                            }}
                                            style={stepperBtnStyle}
                                        >
                                            −
                                        </button>
                                        <div style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '2px' }}>
                                            <input
                                                type="number"
                                                value={maxPrice}
                                                onChange={(e) => setMaxPrice(e.target.value)}
                                                placeholder="Any"
                                                style={{
                                                    ...inputStyle,
                                                    border: 'none',
                                                    textAlign: 'right',
                                                    fontSize: '18px',
                                                    fontWeight: 'bold',
                                                    width: '55px',
                                                    padding: 0,
                                                    backgroundColor: 'transparent'
                                                }}
                                            />
                                            <span style={{ fontSize: '16px', fontWeight: 'bold', color: '#64748b' }}>₪</span>
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                const newVal = (parseFloat(maxPrice) || 0) + 1;
                                                setMaxPrice(newVal);
                                            }}
                                            style={stepperBtnStyle}
                                        >
                                            +
                                        </button>
                                    </div>
                                </div>

                                {/* Date Selection */}
                                <div style={{ marginBottom: '16px' }}>
                                    <label style={labelStyle}>Date</label>
                                    <DatePicker
                                        selected={filterDate ? new Date(`${filterDate}T00:00:00`) : null}
                                        onChange={(d) => setFilterDate(d ? toYMD(d) : '')}
                                        minDate={new Date()}
                                        dateFormat="MM/dd/yyyy"
                                        placeholderText="Select date"
                                        className="ep-date-picker"
                                        wrapperClassName="ep-date-wrap"
                                    />

                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px' }}>
                                    <div>
                                        <label style={labelStyle}>Start Time</label>
                                        <TimeDropdown
                                            label="Start Time"
                                            value={filterStart}
                                            disabled={!filterDate}
                                            options={getValidStartTimes(filterDate)}
                                            onChange={setFilterStart}
                                            labelStyle={labelStyle}
                                            buttonStyle={selectStyle}
                                        />
                                    </div>
                                    <div>
                                        <label style={labelStyle}>End Time</label>
                                        <TimeDropdown
                                            label="End Time"
                                            value={filterEnd}
                                            disabled={!filterDate}
                                            options={getValidEndTimes(filterDate, filterStart)}
                                            onChange={setFilterEnd}
                                            labelStyle={labelStyle}
                                            buttonStyle={selectStyle}
                                        />
                                    </div>
                                </div>

                                {/* Action Buttons */}
                                <div style={{ display: 'flex', gap: '12px', marginTop: '24px', paddingTop: '20px', borderTop: '1px solid #f1f5f9' }}>
                                    <button
                                        type="button"
                                        onClick={handleReset}
                                        style={{
                                            padding: '12px 24px',
                                            backgroundColor: '#fff',
                                            color: '#64748b',
                                            border: '1px solid #e2e8f0',
                                            borderRadius: '10px',
                                            fontWeight: '600',
                                            cursor: 'pointer',
                                            flex: 1
                                        }}
                                    >
                                        Reset
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setFiltersOpen(false)}
                                        style={{
                                            flex: 1,
                                            padding: '12px',
                                            backgroundColor: '#0f172a',
                                            color: 'white',
                                            border: 'none',
                                            borderRadius: '10px',
                                            fontWeight: '600',
                                            cursor: 'pointer',
                                            boxShadow: '0 4px 12px rgba(15, 23, 42, 0.2)'
                                        }}
                                    >
                                        Apply Filters
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Profile Menu */}
            {profileOpen && (
                <div style={{ position: 'fixed', inset: 0, zIndex: 999999, pointerEvents: 'auto' }}>
                    <button type="button" onClick={() => setProfileOpen(false)} style={{ position: 'absolute', inset: 0, background: 'transparent', border: 0, padding: 0, margin: 0 }} />
                    <div className= "profile-menu" style={{ position: 'absolute', top: profileMenuPos.top, left: profileMenuPos.left, width: 220, background: 'rgba(255,255,255,0.98)', backdropFilter: 'blur(10px)', border: '1px solid rgba(15, 23, 42, 0.10)', boxShadow: '0 18px 40px rgba(15, 23, 42, 0.18)', borderRadius: 14, padding: 8 }}>
                        {roles.has('OWNER') && ( <button type="button" onClick={() => { setProfileOpen(false); nav('/manage-spots') }} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: '#e2e8f0', textAlign: 'left', padding: '0 12px', fontWeight: 700, cursor: 'pointer', color: '#1e293b', marginBottom: '5px' }}>Manage Spots</button> )}
                        {roles.has('DRIVER') && ( <button type="button" onClick={() => { setProfileOpen(false); nav('/my-bookings') }} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: 'transparent', textAlign: 'left', padding: '0 12px', fontWeight: 600, cursor: 'pointer', color: '#1e293b', marginBottom: '5px' }}>My Bookings</button> )}
                        <button type="button" onClick={() => { setProfileOpen(false); nav('/revenues') }} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: 'transparent', textAlign: 'left', padding: '0 12px', fontWeight: 600, cursor: 'pointer', color: '#1e293b', marginBottom: '5px' }}>My Revenues</button>
                        <button type="button" onClick={() => { setProfileOpen(false); nav('/expenses') }} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: 'transparent', textAlign: 'left', padding: '0 12px', fontWeight: 600, cursor: 'pointer', color: '#1e293b', marginBottom: '5px' }}>My Expenses</button>
                        <button type="button" onClick={() => { setProfileOpen(false); setProfileModalOpen(true); nav('/manage-profile') }} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: 'transparent', textAlign: 'left', padding: '0 12px', fontWeight: 600, cursor: 'pointer', color: '#1e293b', marginBottom: '5px' }}>Manage Profile</button>
                        <button type="button" onClick={() => { setProfileOpen(false); nav('/change-password') }} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: 'transparent', textAlign: 'left', padding: '0 12px', fontWeight: 600, cursor: 'pointer', color: '#1e293b', marginBottom: '5px' }}>Change Password</button>
                        <button type="button" onClick={doLogout} style={{ width: '100%', height: 42, borderRadius: 12, border: 0, background: 'rgba(239, 68, 68, 0.10)', textAlign: 'left', padding: '0 12px', fontWeight: 900, cursor: 'pointer', color: '#ef4444' }}>Logout</button>
                    </div>
                </div>
            )}

            <ProfileModal isOpen={isProfileModalOpen} onClose={() => setProfileModalOpen(false)} onUpdateSuccess={(updatedUser) => { const u = updatedUser ?? getCurrentUser(); const nextRoles = new Set(u?.roles ?? []); if (nextRoles.has('OWNER') && !nextRoles.has('DRIVER')) { nav('/owner', { replace: true }); return; } }} />
            <BookParkingModal isOpen={bookingOpen} spot={bookingSpot} onClose={() => setBookingOpen(false)} onBooked={(b) => { const total = b?.totalPrice != null ? `₪${b.totalPrice}` : ''; setBookingToast(`Booking created (#${b?.id}). Status: ${b?.status || 'PENDING'} ${total}`); setTimeout(() => setBookingToast(''), 3500) }} />

            {bookingToast && ( <div style={{ position: 'absolute', left: 12, right: 12, bottom: 80, zIndex: 50000, pointerEvents: 'none' }}> <div style={{ pointerEvents: 'auto', background: 'rgba(255,255,255,0.96)', border: '1px solid rgba(15,23,42,0.12)', borderRadius: 14, padding: 12, boxShadow: '0 14px 40px rgba(15, 23, 42, 0.14)', fontWeight: 900, color: '#0f172a' }}>{bookingToast}</div></div>)}
        </div>
    )
}