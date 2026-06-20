import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import axios from 'axios';

interface UserInfo {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  isLoading: boolean;
  isError: boolean;
  errorMessage: string | null;
}

// Retrieve from localStorage if available
const storedUser = localStorage.getItem('user');
const storedAccess = localStorage.getItem('accessToken');
const storedRefresh = localStorage.getItem('refreshToken');

const initialState: AuthState = {
  user: storedUser ? JSON.parse(storedUser) : null,
  accessToken: storedAccess || null,
  refreshToken: storedRefresh || null,
  isLoading: false,
  isError: false,
  errorMessage: null,
};

export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials: { username: string; password: string }, thunkAPI) => {
    try {
      const response = await axios.post('/api/v1/auth/login', credentials);
      const data = response.data.data; // Align with com.ems.common.dto.ApiResponse schema
      
      // Store in localStorage
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      const userPayload: UserInfo = {
        id: data.id,
        username: data.username,
        email: data.email,
        roles: data.roles,
      };
      localStorage.setItem('user', JSON.stringify(userPayload));
      
      return data;
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Login failed';
      return thunkAPI.rejectWithValue(msg);
    }
  }
);

export const registerUser = createAsyncThunk(
  'auth/register',
  async (userData: { username: string; email: string; password: string; role: string }, thunkAPI) => {
    try {
      const response = await axios.post('/api/v1/auth/register', userData);
      const data = response.data.data;
      
      // Store in localStorage
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      const userPayload: UserInfo = {
        id: data.id,
        username: data.username,
        email: data.email,
        roles: data.roles,
      };
      localStorage.setItem('user', JSON.stringify(userPayload));
      
      return data;
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Registration failed';
      return thunkAPI.rejectWithValue(msg);
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      localStorage.removeItem('user');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    },
    updateTokens: (state, action: PayloadAction<{ accessToken: string; refreshToken: string }>) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      localStorage.setItem('accessToken', action.payload.accessToken);
      localStorage.setItem('refreshToken', action.payload.refreshToken);
    },
    clearError: (state) => {
      state.isError = false;
      state.errorMessage = null;
    }
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(loginUser.pending, (state) => {
        state.isLoading = true;
        state.isError = false;
        state.errorMessage = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.isLoading = false;
        state.accessToken = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
        state.user = {
          id: action.payload.id,
          username: action.payload.username,
          email: action.payload.email,
          roles: action.payload.roles,
        };
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.isLoading = false;
        state.isError = true;
        state.errorMessage = action.payload as string;
      })
      // Register
      .addCase(registerUser.pending, (state) => {
        state.isLoading = true;
        state.isError = false;
        state.errorMessage = null;
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.isLoading = false;
        state.accessToken = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
        state.user = {
          id: action.payload.id,
          username: action.payload.username,
          email: action.payload.email,
          roles: action.payload.roles,
        };
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.isLoading = false;
        state.isError = true;
        state.errorMessage = action.payload as string;
      });
  },
});

export const { logout, updateTokens, clearError } = authSlice.actions;
export default authSlice.reducer;
