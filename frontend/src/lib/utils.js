import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

// Combina clases de Tailwind sin duplicados (la usan todos los componentes de shadcn/ui)
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}
