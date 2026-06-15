import { Link } from "react-router-dom";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Combobox, ComboboxContent, ComboboxEmpty, ComboboxInput, ComboboxItem, ComboboxList } from "../ui/combobox";




export default function SessionFormCreate() {
    const votes = [
        { name: "Fibonacci", id:"01"}, { name: "T-shirts", id:"02"}
    ]
    
    function handleChoice() {

    }

    return(
        <div className="w-200">
            <Input placeholder="Nombre Para la Sesion"
            />
            <Combobox items={votes} >
            <ComboboxInput 
                placeholder="Select a framework" 
                
            />
            <ComboboxContent className="w-200">
                <ComboboxEmpty>No items found.</ComboboxEmpty>
                <ComboboxList>
                {(item) => (
                    <ComboboxItem key={item.name} value={item.name}>
                    {item.name}
                    </ComboboxItem>
                )}
                </ComboboxList>
            </ComboboxContent>
            </Combobox>
            
            <Button asChild size="lg" className="w-full sm:w-auto">
                <Link to="/auth/register">Crear Sesion</Link>
            </Button>
        </div>
    );
    
};