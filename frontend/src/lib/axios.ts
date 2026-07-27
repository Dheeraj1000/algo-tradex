import axios from 'axios';
import { useAuthStore } from '../store/authStore';

// Configure the base instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    // We fetch the token from the Zustand store outside of React context
    const token = useAuthStore.getState().accessToken;
    
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token expiry & 401s
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    
    // If error is 401 and we haven't already retried this request
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // Attempt to refresh token using the current refresh token
        const refreshToken = useAuthStore.getState().refreshToken;
        
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }
        
        // This relies on the backend having an endpoint for refreshing tokens
        const response = await axios.post(`${api.defaults.baseURL}/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        
        // Update the store with new tokens
        useAuthStore.getState().setTokens(accessToken, newRefreshToken);
        
        // Retry the original request with the new access token
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
        
      } catch (refreshError) {
        // If refresh fails, log the user out
        useAuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
