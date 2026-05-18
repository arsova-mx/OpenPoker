import { Link } from "react-router-dom";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import VotingTypeCombobox from "./VotingTypeCombobox";
import { CreateSessionRequest } from "@/types";



export default function SessionFormCreate() {
    // const votingOptions: CreateSessionRequest[] = [{name: "Fibonacci"},{name: "T-shirts"},{name: 'Colors'}]
    
    return(
        <div>
            <Input placeholder="Nombre Para la Sesion"
            />
            <VotingTypeCombobox />
            <Button asChild size="lg" className="w-full sm:w-auto">
                <Link to="/auth/register">Crear Sesion</Link>
            </Button>
        </div>
    );
    
};