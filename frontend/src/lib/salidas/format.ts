import type { GuiaDTO, RolGuia } from "./types"

const MESES = [
  "enero", "febrero", "marzo", "abril", "mayo", "junio",
  "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
]

function parseISODate(iso: string) {
  const [year, month, day] = iso.split("-").map(Number)
  return { year, month: month - 1, day }
}

export function formatRangoFechas(inicio: string, fin: string): string {
  const a = parseISODate(inicio)
  const b = parseISODate(fin)

  if (inicio === fin) return `${a.day} de ${MESES[a.month]}, ${a.year}`
  if (a.month === b.month && a.year === b.year) {
    return `${a.day} al ${b.day} de ${MESES[b.month]}, ${b.year}`
  }
  return `${a.day} de ${MESES[a.month]} al ${b.day} de ${MESES[b.month]}, ${b.year}`
}

export const ROL_LABEL: Record<RolGuia, string> = {
  GUIA_RESPONSABLE: "Guía responsable",
  GUIA_AYUDANTE: "Guía ayudante",
}

export function formatGuias(guias: GuiaDTO[]): string {
  if (guias.length === 0) return "—"
  return guias.map((g) => `${g.apellido}, ${g.nombre}`).join(" · ")
}

export function getIniciales(nombre: string, apellido: string): string {
  return `${apellido.charAt(0)}${nombre.charAt(0)}`.toUpperCase()
}
