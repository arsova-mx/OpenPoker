/**
 * Placeholder module for shared TypeScript types and interfaces.
 *
 * Types to be defined during development:
 *  - Session       – planning poker session
 *  - Story         – user story within a session
 *  - Participant   – a user in a session
 *  - Vote          – a single vote cast by a participant
 *  - CardDeck      – the set of estimation cards (Fibonacci, T-shirt, etc.)
 */

export type LoginRequest = { 
    username: string, 
    password: string 
} 
export type RegisterRequest = { 
    username: string, 
    email:string, 
    password: string 
}
export type AuthResponse = { 
    token: string, 
    username: string 
}

export type CreateSessionRequest = {
    name: string
}
export type SessionResponse = {
    id: string,
    sessionCode: string, 
    name: string,
    hostUsername: string, 
    status: string,
    participantCount: number,
    createdAt: string 
}