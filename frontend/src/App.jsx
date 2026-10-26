import { useEffect, useState } from 'react'
import axios from 'axios'
import { MisSalidasPage } from './components/salidas/mis-salidas-page'
import { PlanificarSalida } from './components/salidas/PlanificarSalida'

function App() {
  const [mensaje, setMensaje] = useState('Conectando...')

  useEffect(() => {
    axios.get('http://localhost:8080/api/ping')
        .then(res => setMensaje(res.data))
        .catch(() => setMensaje('Error: no se pudo conectar con el backend'))
  }, [])

  return (
    <div className="min-h-screen bg-background">
      {/* Banner de verificación con el backend */}
      <div className="bg-slate-800 text-white text-xs p-2 text-center">
        Estado del backend: <strong>{mensaje}</strong>   
      </div>

      {/* Pantalla de planificar salida */}
      <PlanificarSalida />
    </div>
  )
}

export default App