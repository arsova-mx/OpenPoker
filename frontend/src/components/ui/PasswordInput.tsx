import * as React from "react";
import { RiEyeLine, RiEyeOffLine } from "@remixicon/react";
import { cn } from "@/lib/utils";
import { Input, type InputProps } from "./input";
import { InputGroup, InputGroupButton } from "./input-group";

interface PasswordInputProps extends InputProps {}

const PasswordInput = React.forwardRef<HTMLInputElement, PasswordInputProps>(
  ({ className, ...props }, ref) => {
    const [showPassword, setShowPassword] = React.useState(false);

    return (
      <InputGroup>
        <Input
          ref={ref}
          type={showPassword ? "text" : "password"}
          data-slot="input-group-control"
          className={cn("pr-10", className)}
          {...props}
        />
        <InputGroupButton
          type="button"
          variant="ghost"
          size="sm"
          className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
          onClick={() => setShowPassword((prev) => !prev)}
          tabIndex={-1}
        >
          {showPassword ? (
            <RiEyeOffLine className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
          ) : (
            <RiEyeLine className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
          )}
          <span className="sr-only">
            {showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
          </span>
        </InputGroupButton>
      </InputGroup>
    );
  }
);
PasswordInput.displayName = "PasswordInput";

export { PasswordInput };