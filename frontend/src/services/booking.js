import axios from 'axios'
import {API_BASE_URL} from "../config.js";

const API_BASE = API_BASE_URL

function authHeaders() {
    const token = localStorage.getItem('easypark_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function createBooking({ parkingId, startTime, endTime }) {
    const res = await axios.post(
        `${API_BASE}/api/bookings`,
        { parkingId, startTime, endTime },
        { headers: authHeaders() },
    )
    return res.data
}

export async function getMyBookings() {
    const res = await axios.get(`${API_BASE}/api/bookings/my`, {
        headers: authHeaders(),
    })
    return res.data
}

export async function cancelBooking(id) {
    const res = await axios.put(`${API_BASE}/api/bookings/${id}/cancel`, null, {
        headers: authHeaders(),
    })
    return res.data
}
export async function getBusyIntervals(parkingId, from, to) {
    const res = await axios.get(`${API_BASE_URL}/api/parking-spots/${parkingId}/busy`, {
        params: { from, to },
        headers: { Authorization: `Bearer ${localStorage.getItem('easypark_token')}` },
    });
    return res.data || [];
}
export async function rateParking(parkingId, rating) {
    const res = await axios.post(
        `${API_BASE}/api/parking-spots/${parkingId}/rate`,
        { rating },
        { headers: authHeaders() },
    )
    return res.data
}

