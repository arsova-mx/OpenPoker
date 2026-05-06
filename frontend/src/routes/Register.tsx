import RegisterForm from "../components/registerForm/RegisterForm";


export default function Register() {
    return (
        <div className="rounded-xl p-10 bg-emerald-200 hover:shadow-xl/50 shadow-xl">
            <h1>Página de Registro de usuario</h1>
            <RegisterForm/>
        </div>
    );
}