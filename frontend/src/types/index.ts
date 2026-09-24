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
  id: string;
  sessionCode: string;
  name: string;
  hostUsername: string;
  participantCount: number;
  createdAt: string;
};

// Tipos solicitados para el flujo de votación
export interface CastVoteRequest {
  cardValue: string;
}

export interface VoteResponse {
  voteId: string;
  username: string;
  cardValue: string;
  votedAt: string;
}

export interface VotingResultsResponse {
  sessionCode: string;
  votes: VoteResponse[];
  revealed: boolean;
}

export interface CardValueResponse {
  id: string;
  value: string;
  orderIndex: number;
}

export interface VotingDeckResponse {
  id: string;
  name: string;
  seriesType: string;
  description: string;
  cards: CardValueResponse[];
}

// Catálogo de series permitidas (Extensible)
export type CardDeckType = 'FIBONACCI' | 'T_SHIRT';

export const CARD_DECKS: Record<CardDeckType, string[]> = {
  FIBONACCI: ["0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?"],
  T_SHIRT: ["XS", "S", "M", "L", "XL", "XXL", "?"]
};

// Por compatibilidad con el checklist para el MVP
export const FIBONACCI_CARDS = CARD_DECKS.FIBONACCI;

// frontend/src/types/index.ts
export interface Participant {
  id?: string;
  participantId?: string;
  username?: string;
  displayName?: string;
  effectiveName?: string;
  role?: "HOST" | "VOTER";
  isGuest?: boolean;
}

// --- Issue: Votación en tiempo real y componentes modulares ---

// Estado de quién ha votado (compatible con el issue y el backend Map<UUID, Boolean>)
export interface VoteStatus {
  participantId: string;
  hasVoted: boolean;
}
export type VoteStatusMap = Record<string, boolean>;

// Voto individual (compatible con tu VoteResponse)
export interface Vote {
  participantId?: string;
  username: string;
  value: string;
  votedAt?: string;
}

// Estadísticas recibidas tras el reveal
export interface VoteStatistics {
  average: number;
  consensusPercentage: number;
  isFullConsensus: boolean;
  outlierVoteIds?: string[];
}

export interface VotingRRAverageResponse {
  sessionCode: string;
  votes: VoteResponse[];
  revealed: boolean;
  suggestedAverage?: number;
  suggestedCardValue?: string;
  statistics?: VoteStatistics;
}

// Tipo Deck para CardSelector
export type CardDeck = string[];