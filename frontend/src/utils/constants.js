// API base URL from environment or default
export const apiBaseUrl = import.meta.env.VITE_API_URL || import.meta.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

// Token storage keys
export const TOKEN_KEYS = {
  ACCESS: 'access_token',
  REFRESH: 'refresh_token',
};

// LocalStorage helpers
export const storage = {
  getToken: () => localStorage.getItem(TOKEN_KEYS.ACCESS),
  setToken: (token) => localStorage.setItem(TOKEN_KEYS.ACCESS, token),
  removeToken: () => localStorage.removeItem(TOKEN_KEYS.ACCESS),
  getRefreshToken: () => localStorage.getItem(TOKEN_KEYS.REFRESH),
  setRefreshToken: (token) => localStorage.setItem(TOKEN_KEYS.REFRESH, token),
  removeRefreshToken: () => localStorage.removeItem(TOKEN_KEYS.REFRESH),
  clear: () => {
    localStorage.removeItem(TOKEN_KEYS.ACCESS);
    localStorage.removeItem(TOKEN_KEYS.REFRESH);
  },
};

// Format date for display
export const formatDate = (dateString) => {
  const options = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  };
  return new Date(dateString).toLocaleDateString('en-US', options);
};

// Truncate text
export const truncateText = (text, maxLength = 150) => {
  if (!text) return '';
  return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text;
};

// Extract JWT payload (for decoding user info)
export const decodeToken = (token) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Failed to decode token:', error);
    return null;
  }
};

// Get user ID from token
export const getUserIdFromToken = () => {
  const token = storage.getToken();
  if (!token) return null;

  const payload = decodeToken(token);
  return payload ? payload.sub || payload.userId : null;
};