import { Navigate, Outlet, useLocation, useRouteLoaderData } from 'react-router-dom';


export function ProtectedRoute() {
  const token = useRouteLoaderData('root') as string | null;
  const location = useLocation();

  console.log("Componente Wrapper: Token: "+token);

  if (!token) {
    console.log("Componente Wrapper: redirigir a Login")
    return <Navigate to={'/auth/login'} state={{from: location}} replace />;
  }

  return <Outlet />;
};