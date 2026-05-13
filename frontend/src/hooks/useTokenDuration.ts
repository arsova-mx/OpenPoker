

import { redirect } from "react-router-dom";

export function getTokenDuration() {
    const tokenDuration = localStorage.getItem('tokenDuration') ?? '';
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

    const tokenDuration = getTokenDuration();

    if(tokenDuration < 0) {
        return null;
    }

    return token;
}

export function loader() {
    return getAuthToken();
}

export function checkAuthLoader() {
    const token = getAuthToken();
    if(!token) {
        return redirect('/')
    }
    return null;
}