import RegisterForm from "../components/registerForm/RegisterForm";


export default function Register() {
    return (
        <div className=" h-100 w-120 rounded-xl bg-emerald-200 hover:shadow-xl/50 shadow-xl">
            <h1 className="text-center">Página de Registro de usuario</h1>
            <RegisterForm/>
        </div>
    );
}