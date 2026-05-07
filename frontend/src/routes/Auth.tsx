import { Outlet, redirect } from "react-router-dom";

export default function Auth() {
    return (
        <div className="flex justify-center mt-10">
            <div>
                <Outlet/>
            </div>
        </div>
    )
}

export async function action() {
    
    return redirect("/");
}