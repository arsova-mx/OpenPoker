

import { redirect } from "react-router-dom";

export function getTokenDuration() {

    const tokenDuration = localStorage.getItem('tokenDuration') ?? '0';
    
    const expiration = new Date(tokenDuration);
    const now = new Date()

    const duration = expiration.getTime()-now.getTime();

    return duration;
}

/**
 * Token guardado en localStorage, descartando valores inválidos como "undefined" o "null"
 * que pudieron quedar de versiones anteriores del registro.
 */
export function readStoredToken(): string | null {
    const token = localStorage.getItem('token');

    if (!token || token === 'undefined' || token === 'null') {
        return null;
    }

    return token;
}

export function getAuthToken() {

    const token = readStoredToken();

    if (!token) {
        return null
    }

    const tokenDuration = getTokenDuration();
    
    if (tokenDuration < 0) {
        return 'EXPIRED';
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