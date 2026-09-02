
import { CreateSessionRequest, SessionResponse } from "@/types";
import instance from "../clients/APIClient";

const createSession = async (data: CreateSessionRequest): Promise<SessionResponse> => {

    const response = await instance.post<SessionResponse>('/sessions', data)
    return response.data;

}

const getSession = async (code: string): Promise<SessionResponse> => {
    const response = await instance.get<SessionResponse>(`/sessions/${code}`)
    return response.data;
}

const joinSession = async (code: string): Promise<SessionResponse> => {
    const response = await instance.post<SessionResponse>(`/sessions/${code}/join`)
    return response.data;
}

export const sessionServices = {
    createSession, 
    getSession, 
    joinSession
};

export default sessionServices;