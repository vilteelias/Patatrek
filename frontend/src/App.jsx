import { useEffect, useState } from 'react'
import axios from 'axios'
import {MisSalidasPage} from './components/salidas/mis-salidas-page'

function App() {
  const [mensaje, setMensaje] = useState('Conectando...')

  useEffect(() => {
    axios.get('/api/ping')
        .then(res => setMensaje(res.data))
        .catch(() => setMensaje('Error: no se pudo conectar con el backend'))
  }, [])

  return (
    <div className="min-hscreen bg-background">
    {/*Banner de verificaci[on con el backend] */}
    <div className="bg-slate-800 text-white text-xs p-2 text-center">
    Estado del backend: <strong>{mensaje}</strong>  
    </div>

    {/*pantalla de salida */}
    <MisSalidasPage/>
    </div>
  )
}

export default App