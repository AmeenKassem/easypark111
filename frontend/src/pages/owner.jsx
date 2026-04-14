import { useEffect, useState } from 'react'
import axios from 'axios'
import Layout from '../components/layout/layout'
import CreateParkingPage from '../pages/CreateParkingPage.jsx'
import Modal from '../components/modals/Modal.jsx'
import {API_BASE_URL} from "../config.js";

export default function OwnerPage() {
    const [createOpen, setCreateOpen] = useState(false)

    // NEW: My Spots modal state
    const [myOpen, setMyOpen] = useState(false)
    const [mySpots, setMySpots] = useState([])
    const [myLoading, setMyLoading] = useState(false)
    const [myError, setMyError] = useState('')
    // NEW: edit modal state
    const [editOpen, setEditOpen] = useState(false)
    const [editSpot, setEditSpot] = useState(null)
    const [editSaving, setEditSaving] = useState(false)
    const [editError, setEditError] = useState('')
    const [editForm, setEditForm] = useState({
        location: '',
        lat: null,
        lng: null,
        pricePerHour: '',
        covered: false,
        availableFrom: '',
        availableTo: '',
        active: true,
    })



    const API_BASE = API_BASE_URL
    const authHeader = () => ({
        Authorization: `Bearer ${localStorage.getItem('easypark_token')}`,
    })

// IMPORTANT: I don’t have your exact UpdateParkingRequest fields.
// This payload is the minimum that usually works (active/covered/pricePerHour).
// If your backend requires more fields, add them here.
    const buildUpdatePayload = (spot, overrides = {}) => {
        const payload = {
            // REQUIRED by backend:
            location: spot.location ?? '',
            pricePerHour: Number(spot.pricePerHour),

            // Optional but should be sent to keep state consistent:
            lat: spot.lat ?? null,
            lng: spot.lng ?? null,
            covered: !!spot.covered,
            availableFrom: spot.availableFrom ?? null,
            availableTo: spot.availableTo ?? null,
            active: !!spot.active,

            ...overrides,
        }

        // Safety: never send blank location (prevents 400 from @NotBlank)
        if (!payload.location || String(payload.location).trim().length === 0) {
            // last-resort fallback: do NOT guess; force a clear error early
            throw new Error('Spot location is missing. Cannot update because backend requires location.')
        }

        return payload
    }


    const openEditFor = (spot) => {
        setEditError('')
        setEditSpot(spot)
        setEditForm({
            location: spot.location ?? '',
            lat: spot.lat ?? null,
            lng: spot.lng ?? null,
            pricePerHour: String(spot.pricePerHour ?? ''),
            covered: !!spot.covered,
            availableFrom: spot.availableFrom ?? '',
            availableTo: spot.availableTo ?? '',
            active: !!spot.active,
        })

        setEditOpen(true)
    }

    const saveEdit = async () => {
        if (!editSpot) return
        setEditSaving(true)
        setEditError('')
        try {
            const payload = buildUpdatePayload(editSpot, {
                location: editForm.location,
                lat: editForm.lat,
                lng: editForm.lng,
                pricePerHour: parseFloat(editForm.pricePerHour),
                covered: !!editForm.covered,
                availableFrom: editForm.availableFrom || null,
                availableTo: editForm.availableTo || null,
                active: !!editForm.active,
            })


            await axios.put(`${API_BASE}/api/parking-spots/${editSpot.id}`, payload, {
                headers: authHeader(),
            })

            setEditOpen(false)
            setEditSpot(null)
            await fetchMySpots()
        } catch (e) {
            const status = e?.response?.status
            if (status === 401) setEditError('Not logged in / token expired.')
            else if (status === 403) setEditError('Permission denied: OWNER role required.')
            else setEditError(e?.response?.data?.message || 'Failed to update the spot.')
        } finally {
            setEditSaving(false)
        }
    }

    const toggleActive = async (spot) => {
        setMyError('')
        try {
            const payload = buildUpdatePayload(spot, { active: !spot.active })
            await axios.put(`${API_BASE}/api/parking-spots/${spot.id}`, payload, {
                headers: authHeader(),
            })
            await fetchMySpots()
        } catch (e) {
            const status = e?.response?.status
            if (status === 401) setMyError('Not logged in / token expired.')
            else if (status === 403) setMyError('Permission denied: OWNER role required.')
            else setMyError('Failed to toggle active.')
        }
    }

    const deleteSpot = async (spot) => {
        const ok = window.confirm(`Delete this spot?\n\n${spot.location || ''}`)
        if (!ok) return

        setMyError('')
        try {
            await axios.delete(`${API_BASE}/api/parking-spots/${spot.id}`, {
                headers: authHeader(),
            })
            await fetchMySpots()
        } catch (e) {
            const status = e?.response?.status
            if (status === 401) setMyError('Not logged in / token expired.')
            else if (status === 403) setMyError('Permission denied: OWNER role required.')
            else setMyError('Failed to delete the spot.')
        }
    }


    // NEW: fetch function you can reuse after creating/deleting
    const fetchMySpots = async () => {
        setMyLoading(true)
        setMyError('')
        try {
            const token = localStorage.getItem('easypark_token')
            const res = await axios.get(`${API_BASE}/api/parking-spots/my`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            setMySpots(res.data || [])
        } catch (e) {
            const status = e?.response?.status
            if (status === 401) setMyError('Not logged in / token expired.')
            else if (status === 403) setMyError('Permission denied: OWNER role required.')
            else setMyError('Failed to load your spots.')
        } finally {
            setMyLoading(false)
        }
    }

    // NEW: when modal opens -> load data immediately
    useEffect(() => {
        if (!myOpen) return
        fetchMySpots()
    }, [myOpen])

    return (
        <Layout title="Manage Spots">
            <div className="ep-card" style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                <button className="ep-btn ep-btn-primary" onClick={() => setCreateOpen(true)}>
                    + Create Parking
                </button>

                {/* CHANGED: make it functional */}
                <button className="ep-btn" onClick={() => setMyOpen(true)}>
                    My Spots
                </button>
            </div>

            {/* CREATE MODAL */}
            {createOpen && (
                <Modal onClose={() => setCreateOpen(false)}>
                    <div className="ep-modal">
                        <CreateParkingPage
                            onClose={() => setCreateOpen(false)}
                            onCreated={async () => {
                                // After create: close create modal and refresh "My Spots" if it's open
                                setCreateOpen(false)
                                if (myOpen) await fetchMySpots()
                            }}
                        />
                    </div>
                </Modal>
            )}

            {/* MY SPOTS MODAL */}
            {myOpen && (
                <Modal onClose={() => setMyOpen(false)}>
                    <div className="ep-modal" style={{ width: 'min(900px, 96vw)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                            <h2 style={{ margin: 0 }}>My Spots</h2>

                            <div style={{ display: 'flex', gap: 10 }}>
                                <button className="ep-btn" onClick={fetchMySpots} disabled={myLoading}>
                                    {myLoading ? 'Refreshing...' : 'Refresh'}
                                </button>
                                <button className="ep-btn" onClick={() => setMyOpen(false)}>
                                    Close
                                </button>
                            </div>
                        </div>

                        <div style={{ height: 12 }} />

                        {myError && (
                            <div style={{ padding: 12, borderRadius: 8, background: '#fee2e2', color: '#991b1b', fontWeight: 600 }}>
                                {myError}
                            </div>
                        )}

                        {!myError && myLoading && (
                            <div style={{ padding: 12, fontWeight: 700 }}>Loading...</div>
                        )}

                        {!myError && !myLoading && mySpots.length === 0 && (
                            <div style={{ padding: 12, fontWeight: 700 }}>
                                You don’t have any spots yet. Click “+ Create Parking”.
                            </div>
                        )}

                        {!myError && !myLoading && mySpots.length > 0 && (
                            <div style={{ marginTop: 10, display: 'grid', gap: 10 }}>
                                {mySpots.map((s) => (
                                    <div
                                        key={s.id}
                                        style={{
                                            border: '1px solid rgba(15, 23, 42, 0.12)',
                                            borderRadius: 12,
                                            padding: 12,
                                            background: 'white',
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            gap: 12,
                                            alignItems: 'center',
                                        }}
                                    >
                                        <div style={{ display: 'grid', gap: 4 }}>
                                            <div style={{ fontWeight: 900 }}>
                                                {s.location || 'Unknown location'}
                                            </div>
                                            <div style={{ color: '#475569', fontWeight: 600 }}>
                                                ₪{s.pricePerHour}/hr • {s.covered ? 'Covered' : 'Not covered'} • {s.active ? 'Active' : 'Inactive'}
                                            </div>
                                        </div>

                                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                                            <button
                                                className="ep-btn"
                                                onClick={() => toggleActive(s)}
                                                title={s.active ? 'Deactivate spot' : 'Activate spot'}
                                            >
                                                {s.active ? 'Deactivate' : 'Activate'}
                                            </button>

                                            <button
                                                className="ep-btn ep-btn-primary"
                                                onClick={() => openEditFor(s)}
                                            >
                                                Edit
                                            </button>

                                            <button
                                                className="ep-btn"
                                                onClick={() => deleteSpot(s)}
                                                style={{ background: 'rgba(239, 68, 68, 0.10)', color: '#ef4444', fontWeight: 900 }}
                                            >
                                                Delete
                                            </button>
                                        </div>

                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </Modal>
            )}
            {editOpen && (
                <Modal onClose={() => setEditOpen(false)}>
                    <div className="ep-modal" style={{ width: 'min(520px, 96vw)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                            <h2 style={{ margin: 0 }}>Edit Spot</h2>
                            <button className="ep-btn" onClick={() => setEditOpen(false)} disabled={editSaving}>
                                Close
                            </button>
                        </div>

                        <div style={{ height: 10 }} />

                        {editError && (
                            <div style={{ padding: 12, borderRadius: 8, background: '#fee2e2', color: '#991b1b', fontWeight: 600 }}>
                                {editError}
                            </div>
                        )}

                        <div style={{ height: 10 }} />

                        <div style={{ display: 'grid', gap: 12 }}>
                            <div>
                                <div style={{ fontWeight: 800, marginBottom: 6 }}>Price per hour (₪)</div>
                                <input
                                    value={editForm.pricePerHour}
                                    onChange={(e) => setEditForm((p) => ({ ...p, pricePerHour: e.target.value }))}
                                    type="number"
                                    min="0"
                                    step="0.5"
                                    style={{ width: '100%', padding: 10, borderRadius: 8, border: '1px solid #e5e7eb' }}
                                />
                            </div>

                            <label style={{ display: 'flex', gap: 10, alignItems: 'center', fontWeight: 800 }}>
                                <input
                                    type="checkbox"
                                    checked={editForm.covered}
                                    onChange={(e) => setEditForm((p) => ({ ...p, covered: e.target.checked }))}
                                />
                                Covered
                            </label>

                            <label style={{ display: 'flex', gap: 10, alignItems: 'center', fontWeight: 800 }}>
                                <input
                                    type="checkbox"
                                    checked={editForm.active}
                                    onChange={(e) => setEditForm((p) => ({ ...p, active: e.target.checked }))}
                                />
                                Active
                            </label>

                            <div style={{ display: 'flex', gap: 10, marginTop: 6 }}>
                                <button className="ep-btn" onClick={() => setEditOpen(false)} disabled={editSaving} style={{ flex: 1 }}>
                                    Cancel
                                </button>
                                <button className="ep-btn ep-btn-primary" onClick={saveEdit} disabled={editSaving} style={{ flex: 1 }}>
                                    {editSaving ? 'Saving...' : 'Save'}
                                </button>
                            </div>
                        </div>
                    </div>
                </Modal>
            )}

        </Layout>
    )
}

