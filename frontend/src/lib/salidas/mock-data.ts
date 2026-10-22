import type { SalidaDTO, UsuarioSesionDTO } from "./types"

export const MOCK_USUARIO: UsuarioSesionDTO = {
  id: 7,
  nombre: "Martín",
  apellido: "Almeida",
  rol: "GUIA",
}

export const MOCK_SALIDAS: SalidaDTO[] = [
  {
    id: 101,
    nombre: "Ascenso Cerro Perito Moreno",
    fechaInicio: "2026-10-02",
    fechaFin: "2026-10-02",
    rol: "GUIA_RESPONSABLE",
    estado: "EN_CURSO",
    cupoMaximo: 14,
    guiasAyudantes: [{ id: 12, nombre: "Brenda", apellido: "Coronel" }],
    refugios: [],
  },
  {
    id: 102,
    nombre: "Travesía Cerro Alto — Río Senguer",
    fechaInicio: "2026-11-14",
    fechaFin: "2026-11-16",
    rol: "GUIA_RESPONSABLE",
    estado: "DISPONIBLE",
    cupoMaximo: 20,
    guiasAyudantes: [{ id: 12, nombre: "Brenda", apellido: "Coronel" }],
    refugios: [{ id: 3, nombre: "Refugio Cerro Alto" }],
  },
  {
    id: 103,
    nombre: "Circuito Lagos Patagónicos",
    fechaInicio: "2026-10-20",
    fechaFin: "2026-10-24",
    rol: "GUIA_AYUDANTE",
    estado: "FINALIZADA",
    cupoMaximo: 18,
    guiasAyudantes: [],
    refugios: [],
  },
]
