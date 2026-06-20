import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: string[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const { user, accessToken } = useSelector((state: RootState) => state.auth);

  if (!accessToken || !user) {
    // Redirect to login if unauthenticated
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles) {
    // Check if the user has any of the allowed roles
    const hasRole = user.roles.some((role) => allowedRoles.includes(role.toUpperCase()));
    if (!hasRole) {
      // Redirect to dashboard if user has insufficient privileges
      return <Navigate to="/" replace />;
    }
  }

  return <>{children}</>;
};
