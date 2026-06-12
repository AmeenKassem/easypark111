import axios from 'axios';
import { getAuthToken } from './session';
import {API_BASE_URL} from "../config.js";

const API_URL = `${API_BASE_URL}/api/reports`

const getAuthHeaders = () => {
    const token = getAuthToken();
    return { headers: { Authorization: `Bearer ${token}` } };
};

export const getOwnerDashboard = async () => {
    const response = await axios.get(`${API_URL}/owner-dashboard`, getAuthHeaders());
    return response.data;
};

export const getDriverReport = async () => {
    const response = await axios.get(`${API_URL}/driver-report`, getAuthHeaders());
    return response.data;
};