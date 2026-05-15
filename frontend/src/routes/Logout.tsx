import { redirect } from "react-router-dom";

export function action() {
    
    sessionStorage.removeItem('auth');
    localStorage.removeItem('token');
    localStorage.removeItem('tokenDuration');
    
    return redirect('/');
}