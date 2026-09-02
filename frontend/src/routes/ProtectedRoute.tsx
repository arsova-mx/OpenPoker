import { Navigate, Outlet } from 'react-router-dom';
import { authService } from '../api/services/authService';

export const ProtectedRoute = () => {
  const token = authService.getToken();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;