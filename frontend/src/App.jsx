import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import './App.css';

import { AdminLayout } from '@/components/layouts/admin-layout';
import ListaExcursiones from './paginas/ListaExcursiones.jsx';
import AdminExcursiones from './paginas/admin/AdminExcursiones.jsx';
import FormularioExcursion from './paginas/admin/FormularioExcursion.jsx';
import AdminReservasPagina from './paginas/admin/AdminReservasPagina.jsx';
import Proximamente from './paginas/Proximamente.jsx';
import GuiaShell from './paginas/guia/GuiaShell.jsx';
import MisSalidasPagina from './paginas/guia/MisSalidasPagina.jsx';
import NominaPagina from './paginas/guia/NominaPagina.jsx';
import ControlAsistenciaPagina from './paginas/guia/ControlAsistenciaPagina.jsx';
import FinalizarSalidaPagina from './paginas/guia/FinalizarSalidaPagina.jsx';
import ClienteShell from './paginas/cliente/ClienteShell.jsx';
import ExcursionDetallePagina from './paginas/cliente/ExcursionDetallePagina.jsx';
import ReservarPagina from './paginas/cliente/ReservarPagina.jsx';
import ReservaDetallePagina from './paginas/cliente/ReservaDetallePagina.jsx';
import PagoPagina from './paginas/cliente/PagoPagina.jsx';
import MisReservasPagina from './paginas/cliente/MisReservasPagina.jsx';

// Módulos del Administrador ya implementados. Se agregan acá a medida que se terminan.
const MODULOS_DISPONIBLES = ['/admin/excursiones', '/admin/reservas'];

function App() {
  return (
    <Routes>
      {/* Visitante y Cliente (por ahora con la clienta de demostración, hasta Autenticación) */}
      <Route element={<ClienteShell />}>
        <Route path="/" element={<ListaExcursiones />} />
        <Route path="/excursiones/:id" element={<ExcursionDetallePagina />} />
        <Route path="/reservar/:idSalida" element={<ReservarPagina />} />
        <Route path="/mis-reservas" element={<MisReservasPagina />} />
        <Route path="/reservas/:id" element={<ReservaDetallePagina />} />
        <Route path="/reservas/:id/pago" element={<PagoPagina />} />
        <Route path="*" element={<Proximamente titulo="Página no encontrada" texto="La dirección no existe." />} />
      </Route>

      {/* Guía (pantallas generadas con v0). La protección por rol llega con el módulo de Autenticación */}
      <Route path="/guia" element={<GuiaShell />}>
        <Route index element={<Navigate to="salidas" replace />} />
        <Route path="salidas" element={<MisSalidasPagina />} />
        <Route path="salidas/:id" element={<NominaPagina />} />
        <Route path="salidas/:id/control" element={<ControlAsistenciaPagina />} />
        <Route path="salidas/:id/finalizar" element={<FinalizarSalidaPagina />} />
      </Route>

      {/* Administrador (la protección por rol llega con el módulo de Autenticación) */}
      <Route
        path="/admin"
        element={
          <AdminLayout modulosDisponibles={MODULOS_DISPONIBLES}>
            <Outlet />
          </AdminLayout>
        }
      >
        <Route index element={<Navigate to="excursiones" replace />} />
        <Route path="excursiones" element={<AdminExcursiones />} />
        <Route path="excursiones/nueva" element={<FormularioExcursion />} />
        <Route path="excursiones/:id/editar" element={<FormularioExcursion />} />
        <Route path="reservas" element={<AdminReservasPagina />} />
        <Route path="*" element={<Proximamente />} />
      </Route>
    </Routes>
  );
}

export default App;
