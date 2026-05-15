

import { redirect } from "react-router-dom";

export function getTokenDuration() {

    const tokenDuration = localStorage.getItem('tokenDuration') ?? '0';
    
    const expiration = new Date(tokenDuration);
    const now = new Date()

    const duration = expiration.getTime()-now.getTime();

    return duration;
}

export function getAuthToken() {
    
    const token = localStorage.getItem('token');

    if (!token) {
        return null
    }

    return token;
}

export function loader() {
    return getAuthToken();
}

export function checkAuthLoader() {

    const token = getAuthToken();

    if(!token) {
        return redirect('/auth/login')
    }

    return null;
}