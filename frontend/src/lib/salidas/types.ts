export type EstadoSalida = "DISPONIBLE" | "EN_CURSO" | "FINALIZADA"

export type RolGuia = "GUIA_RESPONSABLE" | "GUIA_AYUDANTE"

export interface GuiaDTO {
  id: number
  nombre: string
  apellido: string
}

export interface RefugioDTO {
  id: number
  nombre: string
}

/** Mirrors the SalidaDTO returned by the Spring Boot endpoint GET /api/guias/{id}/salidas */
export interface SalidaDTO {
  id: number
  nombre: string
  fechaInicio: string
  fechaFin: string
  rol: RolGuia
  estado: EstadoSalida
  cupoMaximo: number
  guiasAyudantes: GuiaDTO[]
  refugios: RefugioDTO[]
}

export interface UsuarioSesionDTO {
  id: number
  nombre: string
  apellido: string
  rol: "GUIA" | "ADMIN" | "PARTICIPANTE"
}
