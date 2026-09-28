/**
 * Tipos compartidos para OpenPoker.
 */

// Autenticación
export type LoginRequest = { 
  username: string; 
  password: string; 
};

export type RegisterRequest = { 
  username: string; 
  email: string; 
  password: string; 
};

export type AuthResponse = { 
  token: string; 
  username: string; 
};

// Sesiones
export type CreateSessionRequest = {
  name: string;
};

export interface SessionResponse {
  id: string;
  sessionCode: string;
  name: string;
  hostUsername: string;
  participantCount: number;
  createdAt: string;
  deckId?: string;
  seriesType?: CardSeriesType;
}

export interface Participant {
  id?: string;
  participantId?: string;
  username?: string;
  displayName?: string;
  effectiveName?: string;
  role?: "HOST" | "VOTER";
  isGuest?: boolean;
}

// Catálogo de barajas y cartas
export type CardSeriesType = 'FIBONACCI' | 'T_SHIRT' | 'DOT_VOTING';

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

export interface CardDeck {
  id: string;
  name: string;
  seriesType: CardSeriesType;
  values: string[];
  description: string;
  cards?: CardValueResponse[];
}

// Flujo de votación
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

export interface VoteStatus {
  participantId: string;
  hasVoted: boolean;
}

export type VoteStatusMap = Record<string, boolean>;

export interface Vote {
  participantId?: string;
  username: string;
  value: string;
  votedAt?: string;
}

// Estadísticas tras el reveal
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