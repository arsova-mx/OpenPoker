
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { loader as TokenLoader } from './hooks/useTokenDuration';

import Login from './routes/Login';
import {action as loginAction} from './routes/AuthAction';
import { action as logoutAction } from './routes/Logout';
import Register from './routes/Register';
import Main from './routes/Main';
import Root from './routes/Root';
import ErrorGlobal from './routes/ErrorGlobal';
import Auth from './routes/Auth';
import SessionLobby from './routes/SessionLobby'

import { Toaster } from 'sonner';
import { ProtectedRoute } from './components/Wrapper/ProtectedRoute';
/**
 * Root application component.
 *
 * TODO: Replace with actual application shell (router, layout, context providers)
 * once development begins.
 */
  const isAuthenticated = !!sessionStorage.getItem('auth')

  const router = createBrowserRouter(
    [
      {
        path:'/',
        id: 'root',
        element: <Root/>,
        errorElement: <ErrorGlobal/>,
        loader: TokenLoader,
        children: [
          { 
            index: true,
            element: <Main/>,
          },
          {
            path: 'auth',
            element: <Auth/>,
            action: loginAction,
            children: [
              {
                index: true,
                element: <Navigate to="/auth/login" replace />,
              },
              {
                path:'login',
                element: <Login/>,
              },
              {
                path:'register',
                element: <Register/>,
              },
              {
                path: 'logout',
                action: logoutAction,
              },
            ],
          },
          {
            children:[
              {
                path: 'SessionLobby',
              element: <SessionLobby/>
              }
            ]
          },
          {
            element: <ProtectedRoute isAllowed={isAuthenticated}/>,
            path: 'sessions',
            children: 
            [
              {
                path: ':code'
              }
            ]
          },
        ]
      }
    ]
  );
function App() {
  return (
    <>
      <Toaster position="top-right" richColors closeButton/>
      <RouterProvider router={router} />
    </>
    
  );
}

export default App;
