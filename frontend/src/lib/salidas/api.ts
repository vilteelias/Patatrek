import { MOCK_SALIDAS } from "./mock-data"
import type { SalidaDTO } from "./types"

const API_BASE_URL = "http://localhost:8080/api"
const USE_MOCK = true

/**
 * Replace USE_MOCK with false once the Spring Boot backend is running.
 * With axios: `const { data } = await axios.get<SalidaDTO[]>(`${API_BASE_URL}/guias/${guiaId}/salidas`)`
 */
export async function getSalidasDelGuia(guiaId: number): Promise<SalidaDTO[]> {
  if (USE_MOCK) {
    await new Promise((resolve) => setTimeout(resolve, 400))
    return MOCK_SALIDAS
  }

  const response = await fetch(`${API_BASE_URL}/guias/${guiaId}/salidas`, {
    headers: { Accept: "application/json" },
    credentials: "include",
  })

  if (!response.ok) {
    throw new Error(`Error ${response.status} al obtener las salidas`)
  }

  return response.json()
}
