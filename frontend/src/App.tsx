
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { loader as TokenLoader } from './hooks/useTokenDuration';
import Login from './routes/Login';
import {action as loginAction} from './routes/Auth';
import { action as logoutAction } from './routes/Logout';
import Register from './routes/Register';
import Main from './routes/Main';
import Root from './routes/Root';
import { Toaster } from 'sonner';
import ErrorGlobal from './routes/ErrorGlobal';
import Auth from './routes/Auth';

/**
 * Root application component.
 *
 * TODO: Replace with actual application shell (router, layout, context providers)
 * once development begins.
 */


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
                index: true
              },
              {
                path:'login',
                element: <Login/>,
                
              },
              {
                path:'register',
                element: <Register/>
              },
              {
                path: 'logout',
                action: logoutAction
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
