
import { CreateSessionRequest, SessionResponse } from "@/types";
import instance from "./APIClient";

const createSession = async (data: CreateSessionRequest): Promise<SessionResponse> => {

    const response = await instance.post<SessionResponse>('/api/sessions', data)
    return response.data;

}

const getSession = async (code: string): Promise<string> => {
    const response = await instance.get(`/api/sessions/${code}`)
    return response.data;
}

const joinSession = async (code: string): Promise<string> => {
    const response = await instance.get(`/api/sessions/${code}/join`)
    return response.data;
}

export const sessionServices = {
    createSession, getSession, joinSession
}