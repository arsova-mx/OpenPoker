import { Navigate, Outlet } from 'react-router-dom';
import { getAuthToken } from '../hooks/useTokenDuration';

export const ProtectedRoute = () => {
  const token = getAuthToken();

  // 1. Valida tanto que exista el token como que no esté marcado como expirado
  if (!token || token === "EXPIRED") {
    // 2. Redirige a la ruta real configurada en el Router: /auth/login
    return <Navigate to="/auth/login" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;