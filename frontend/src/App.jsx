import { Navigate, Route, Routes } from 'react-router-dom';
import './App.css';

import LayoutPublico from './componentes/LayoutPublico.jsx';
import LayoutAdmin from './componentes/LayoutAdmin.jsx';
import ListaExcursiones from './paginas/ListaExcursiones.jsx';
import AdminExcursiones from './paginas/admin/AdminExcursiones.jsx';
import FormularioExcursion from './paginas/admin/FormularioExcursion.jsx';
import Proximamente from './paginas/Proximamente.jsx';

function App() {
  return (
    <Routes>
      {/* Visitante y Cliente */}
      <Route element={<LayoutPublico />}>
        <Route path="/" element={<ListaExcursiones />} />
        <Route path="*" element={<Proximamente titulo="Página no encontrada" texto="La dirección no existe." />} />
      </Route>

      {/* Administrador (la protección por rol llega con el módulo de Autenticación) */}
      <Route path="/admin" element={<LayoutAdmin />}>
        <Route index element={<Navigate to="excursiones" replace />} />
        <Route path="excursiones" element={<AdminExcursiones />} />
        <Route path="excursiones/nueva" element={<FormularioExcursion />} />
        <Route path="excursiones/:id/editar" element={<FormularioExcursion />} />
        <Route path="*" element={<Proximamente />} />
      </Route>
    </Routes>
  );
}

export default App;
