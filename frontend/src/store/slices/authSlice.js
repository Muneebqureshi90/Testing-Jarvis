import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { authApi } from '../../api';
import { storage } from '../../utils/constants';

// Async thunks
export const register = createAsyncThunk(
  'auth/register',
  async (userData, { rejectWithValue }) => {
    try {
      const response = await authApi.register(userData);
      return response.data;
    } catch (error) {
      return rejectWithValue(
        error.response?.data?.message || 'Registration failed'
      );
    }
  }
);

export const login = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await authApi.login(credentials);
      const { accessToken, refreshToken, user } = response.data;

      // Store tokens
      storage.setToken(accessToken);
      storage.setRefreshToken(refreshToken);

      return { accessToken, refreshToken, user };
    } catch (error) {
      return rejectWithValue(
        error.response?.data?.message || 'Login failed'
      );
    }
  }
);

export const logout = createAsyncThunk(
  'auth/logout',
  async (_, { rejectWithValue }) => {
    try {
      // Clear tokens from storage
      storage.clear();
      return null;
    } catch (error) {
      return rejectWithValue('Logout failed');
    }
  }
);

// Initialize auth state from localStorage (called on app load)
export const initializeAuth = createAsyncThunk(
  'auth/initialize',
  async (_, { getState, rejectWithValue }) => {
    const token = storage.getToken();
    if (!token) {
      return { user: null, token: null };
    }

    // In a real implementation, we might validate token with backend
    // For now, we just decode user info from token
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const userId = payload.sub || payload.userId;

      // We could fetch user profile here if needed
      return { user: { id: userId, username: payload.username }, token };
    } catch (error) {
      storage.clear();
      return rejectWithValue('Invalid token');
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    user: null,
    token: null,
    loading: false,
    error: null,
    initialized: false,
  },
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setUser: (state, action) => {
      state.user = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      // Register
      .addCase(register.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(register.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.accessToken;
      })
      .addCase(register.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      // Login
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.accessToken;
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      // Logout
      .addCase(logout.fulfilled, (state) => {
        state.user = null;
        state.token = null;
        state.error = null;
      })

      // Initialize
      .addCase(initializeAuth.pending, (state) => {
        state.initialized = false;
      })
      .addCase(initializeAuth.fulfilled, (state, action) => {
        state.initialized = true;
        state.user = action.payload.user;
        state.token = action.payload.token;
      })
      .addCase(initializeAuth.rejected, (state) => {
        state.initialized = true;
        state.user = null;
        state.token = null;
      });
  },
});

export const { clearError, setUser } = authSlice.actions;
export default authSlice.reducer;