
import { CreateSessionRequest } from "@/types";
import instance from "./APIClient";

const createSession = async (data: CreateSessionRequest): Promise<string> => {

}

const getSession = async (code: string): Promise => {

}

const joinSession = async (code: string): Promise => {

}

export const sessionServices = {
    createSession, getSession, joinSession
}