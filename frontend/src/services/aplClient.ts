/**
 * Placeholder module for API client services.
 *
 * Services to be created during development:
 *  - sessionService  – REST calls for session CRUD
 *  - storyService    – REST calls for user story management
 *  - voteService     – REST calls for vote submission & reveal
 *  - apiClient       – base Axios/Fetch wrapper (base URL, interceptors)
 */


import axios from "axios";
const API_URL = 'http://localhost:8080';

export const instance = axios.create({
    baseURL: API_URL,
    headers: {
    'Content-Type': 'application/json',
  },
});

instance.interceptors.request.use(
    function (config) {
        config.headers.Authorization = "Bearer"
        return config
    }
)

export default instance;