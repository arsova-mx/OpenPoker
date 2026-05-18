import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox"
import { CreateSessionRequest } from "@/types"
import { Item, ItemContent, ItemDescription, ItemTitle } from "../ui/item";



export default function VotingTypeCombobox() {
    const votes = [
        { name: "Fibonacci"}, { name: "T-shirts"}
    ]
    return (
        <Combobox items={votes} defaultValue={votes[0]}>
            <ComboboxInput placeholder="Selecciona el tipo de votación" showClear/>
            <ComboboxContent>
                <ComboboxEmpty>No se encontraron tipos</ComboboxEmpty>
                <ComboboxList>
                    {(item) => (
                        <ComboboxItem key={item.name} value={item.name}>
                            <Item size="xs" className="p-0">
                                <ItemContent>
                                    <ItemTitle className="whitespace-nowrap">
                                        {item.name}
                                    </ItemTitle>
                                    <ItemDescription>
                                        {item.name}
                                    </ItemDescription>
                                </ItemContent>
                            </Item>
                        </ComboboxItem>
                    )}
                </ComboboxList>
            </ComboboxContent>
        </Combobox>
    )
}