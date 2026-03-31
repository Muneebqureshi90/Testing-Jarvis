import { useDispatch, useSelector } from 'react-redux';
import { login, register, logout, initializeAuth, clearError } from '../store/slices/authSlice';

export const useAuth = () => {
  const dispatch = useDispatch();
  const { user, token, loading, error, initialized } = useSelector((state) => state.auth);

  const loginUser = (credentials) => dispatch(login(credentials));
  const registerUser = (userData) => dispatch(register(userData));
  const logoutUser = () => dispatch(logout());
  const initializeAuthState = () => dispatch(initializeAuth());
  const clearAuthError = () => dispatch(clearError());

  return {
    user,
    token,
    loading,
    error,
    initialized,
    loginUser,
    registerUser,
    logoutUser,
    initializeAuthState,
    clearAuthError,
    isAuthenticated: !!token,
  };
};