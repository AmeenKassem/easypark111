import axios from 'axios';
import { getAuthToken } from './session';

const API_URL = 'http://localhost:8080/api/reports';

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