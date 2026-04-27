
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Login from './routes/Login';
import Register from './routes/Register';
import Main from './routes/Main';

/**
 * Root application component.
 *
 * TODO: Replace with actual application shell (router, layout, context providers)
 * once development begins.
 */
function App() {

  const router = createBrowserRouter(
    [
      {
        path:'/',
        children: [
          { 
            index: true,
            element: <Main/>
          },
          {
            path:'/login',
            element: <Login/>
          },
          {
            path:'/register',
            element: <Register/>
          }
        ]
      }
    ]
  );

  return (
    <>
      <RouterProvider router={router} />
    </>
    
  );
}

export default App;
