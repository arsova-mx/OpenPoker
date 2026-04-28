import useAuthStore from "../store/authStore";

export default function Main() {
    const username = useAuthStore( (state) => state.username)
    return(
        <h1>Bienvenido {username} </h1>
    );
}