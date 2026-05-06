

import { authService } from "@/services/authService";

export function getTokenDuration() {
    const tokenDuration = localStorage.getItem('TokenDuration');
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
        return 'EXPIRED'
    }

    return token;
}

export function loader() {
    return getAuthToken();
}

export function checkAuthLoader() {
    const token = getAuthToken();
    if (!token) {
        authService.logout();
    }
}