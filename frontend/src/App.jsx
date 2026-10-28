import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import './App.css';

import { RequiereRol } from '@/sesion/RequiereRol';
import AdminShell from './paginas/admin/AdminShell.jsx';
import IngresarPagina from './paginas/auth/IngresarPagina.jsx';
import RegistrarmePagina from './paginas/auth/RegistrarmePagina.jsx';
import ListaExcursiones from './paginas/ListaExcursiones.jsx';
import AdminExcursiones from './paginas/admin/AdminExcursiones.jsx';
import FormularioExcursion from './paginas/admin/FormularioExcursion.jsx';
import AdminReservasPagina from './paginas/admin/AdminReservasPagina.jsx';
import AdminValoracionesPagina from './paginas/admin/AdminValoracionesPagina.jsx';
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
import ValorarPagina from './paginas/cliente/ValorarPagina.jsx';

// Módulos del Administrador ya implementados. Se agregan acá a medida que se terminan.
const MODULOS_DISPONIBLES = ['/admin/excursiones', '/admin/reservas', '/admin/valoraciones'];

function App() {
  return (
    <Routes>
      {/* CU-02 Iniciar sesión y CU-01 Registrar cliente (pantalla completa, sin encabezado) */}
      <Route path="/ingresar" element={<IngresarPagina />} />
      <Route path="/registrarme" element={<RegistrarmePagina />} />

      {/* Visitante y Cliente */}
      <Route element={<ClienteShell />}>
        <Route path="/" element={<ListaExcursiones />} />
        <Route path="/excursiones/:id" element={<ExcursionDetallePagina />} />
        {/* El participante sin cuenta valora con el enlace del correo (no necesita iniciar sesión) */}
        <Route path="/valorar" element={<ValorarPagina />} />
        {/* Solo con sesión de Cliente: si no, se pide iniciar sesión y después se vuelve acá */}
        <Route
          element={
            <RequiereRol roles={['CLIENTE']}>
              <Outlet />
            </RequiereRol>
          }
        >
          <Route path="/reservar/:idSalida" element={<ReservarPagina />} />
          <Route path="/mis-reservas" element={<MisReservasPagina />} />
          <Route path="/reservas/:id" element={<ReservaDetallePagina />} />
          <Route path="/reservas/:id/pago" element={<PagoPagina />} />
        </Route>
        <Route path="*" element={<Proximamente titulo="Página no encontrada" texto="La dirección no existe." />} />
      </Route>

      {/* Guía (pantallas generadas con v0). Solo con sesión de Guía (ver GuiaShell) */}
      <Route path="/guia" element={<GuiaShell />}>
        <Route index element={<Navigate to="salidas" replace />} />
        <Route path="salidas" element={<MisSalidasPagina />} />
        <Route path="salidas/:id" element={<NominaPagina />} />
        <Route path="salidas/:id/control" element={<ControlAsistenciaPagina />} />
        <Route path="salidas/:id/finalizar" element={<FinalizarSalidaPagina />} />
      </Route>

      {/* Administrador. Solo con sesión de Administrador (ver AdminShell) */}
      <Route path="/admin" element={<AdminShell modulosDisponibles={MODULOS_DISPONIBLES} />}>
        <Route index element={<Navigate to="excursiones" replace />} />
        <Route path="excursiones" element={<AdminExcursiones />} />
        <Route path="excursiones/nueva" element={<FormularioExcursion />} />
        <Route path="excursiones/:id/editar" element={<FormularioExcursion />} />
        <Route path="reservas" element={<AdminReservasPagina />} />
        <Route path="valoraciones" element={<AdminValoracionesPagina />} />
        <Route path="*" element={<Proximamente />} />
      </Route>
    </Routes>
  );
}

export default App;
