import { Outlet } from "react-router-dom";

export default function Auth() {
    return (
        <>
        <div className="mt-10 bg-amber-600 ">
            <Outlet/>
        </div>
        </>
    )
}