import { useEffect, useRef, useState } from "react"
import { Link } from "react-router-dom"
import { RiArrowDownSLine, RiMenuLine, RiUser3Line } from "@remixicon/react"

import { Button } from "@/components/ui/button"
import useAuthStore from "@/store/authStore"
import { useLogout } from "@/hooks/useLogout"

type HeaderMenuItem = {
	label: string
	to: string
}

type HeaderProps = {
	title?: string
	profileName?: string
	menuItems?: HeaderMenuItem[]
}

// Solo rutas registradas en AppRouter. "Cerrar sesión" es una acción, no una ruta.
const defaultMenuItems: HeaderMenuItem[] = [
	{ label: "Inicio", to: "/home" },
]

const menuItemClassName =
	"block w-full rounded-lg px-3 py-2 text-left text-sm text-foreground transition-colors hover:bg-accent hover:text-accent-foreground"

export function Header({
	title = "OpenPoker",
	profileName,
	menuItems = defaultMenuItems,
}: HeaderProps) {
	const [isOpen, setIsOpen] = useState(false)
	const menuRef = useRef<HTMLDivElement | null>(null)
	const username = useAuthStore((state) => state.username)
	const logout = useLogout()
	const displayName = profileName ?? username ?? "Jugador"

	useEffect(() => {
		function handleClickOutside(event: MouseEvent) {
			if (!menuRef.current?.contains(event.target as Node)) {
				setIsOpen(false)
			}
		}

		function handleEscape(event: KeyboardEvent) {
			if (event.key === "Escape") {
				setIsOpen(false)
			}
		}

		document.addEventListener("mousedown", handleClickOutside)
		document.addEventListener("keydown", handleEscape)

		return () => {
			document.removeEventListener("mousedown", handleClickOutside)
			document.removeEventListener("keydown", handleEscape)
		}
	}, [])

	return (
		<header className="w-full border-b border-border/50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
			<div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
				<div className="flex items-center gap-3">
					<div className="flex size-10 items-center justify-center rounded-full border border-border bg-primary/10 text-primary">
						<RiUser3Line className="size-5" aria-hidden="true" />
					</div>
					<div className="flex flex-col leading-tight">
						<span className="text-xs text-muted-foreground">Perfil activo</span>
						<span className="text-sm font-semibold">{displayName}</span>
					</div>
				</div>

				<div className="hidden text-sm font-semibold sm:block">{title}</div>

				<div className="relative" ref={menuRef}>
					<Button
						type="button"
						variant="outline"
						aria-haspopup="menu"
						aria-expanded={isOpen}
						aria-label="Abrir menú"
						className="gap-2"
						onClick={() => setIsOpen((prev) => !prev)}
					>
						<RiMenuLine className="size-4" aria-hidden="true" />
						Menú
						<RiArrowDownSLine
							className={`size-4 transition-transform ${isOpen ? "rotate-180" : ""}`}
							aria-hidden="true"
						/>
					</Button>

					{isOpen ? (
						<div
							role="menu"
							className="absolute right-0 z-50 mt-2 w-48 rounded-xl border border-border bg-background p-1 shadow-lg"
						>
							{menuItems.map((item) => (
								<Link
									key={item.to}
									to={item.to}
									role="menuitem"
									className={menuItemClassName}
									onClick={() => setIsOpen(false)}
								>
									{item.label}
								</Link>
							))}
							<button
								type="button"
								role="menuitem"
								className={menuItemClassName}
								onClick={() => {
									setIsOpen(false)
									logout()
								}}
							>
								Cerrar sesión
							</button>
						</div>
					) : null}
				</div>
			</div>
		</header>
	)
}

export default Header
