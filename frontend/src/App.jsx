import { useEffect, useState } from 'react'
import axios from 'axios'

function App() {
  const [mensaje, setMensaje] = useState('Conectando...')

  useEffect(() => {
    axios.get('/api/ping')
        .then(res => setMensaje(res.data))
        .catch(() => setMensaje('Error: no se pudo conectar con el backend'))
  }, [])

  return <h1>{mensaje}</h1>
}

export default App