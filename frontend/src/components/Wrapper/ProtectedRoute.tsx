import { Navigate, Outlet } from 'react-router-dom';

type ProtectedRouteProps = {
  isAllowed: boolean;
  redirectTo?: string;
}

export const ProtectedRoute = ({ 
  isAllowed, 
  redirectTo = "/auth/login" 
}: ProtectedRouteProps) => {
  
  if (!isAllowed) {
    return <Navigate to={redirectTo} replace />;
  }

  return <Outlet />;
};