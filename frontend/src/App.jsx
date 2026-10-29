import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import './App.css';

import { AdminLayout } from '@/components/layouts/admin-layout';
import { PublicLayout } from '@/components/layouts/public-layout';
import ListaExcursiones from './paginas/ListaExcursiones.jsx';
import AdminExcursiones from './paginas/admin/AdminExcursiones.jsx';
import FormularioExcursion from './paginas/admin/FormularioExcursion.jsx';
import AdminCertificados from './paginas/admin/AdminCertificados.jsx';
import Proximamente from './paginas/Proximamente.jsx';
import GuiaShell from './paginas/guia/GuiaShell.jsx';
import MisSalidasPagina from './paginas/guia/MisSalidasPagina.jsx';
import NominaPagina from './paginas/guia/NominaPagina.jsx';
import ControlAsistenciaPagina from './paginas/guia/ControlAsistenciaPagina.jsx';
import FinalizarSalidaPagina from './paginas/guia/FinalizarSalidaPagina.jsx';
import CargarCertificadosPagina from './paginas/certificados/CargarCertificadosPagina.jsx';
import MiCertificadoPagina from './paginas/certificados/MiCertificadoPagina.jsx';

// Módulos del Administrador ya implementados. Se agregan acá a medida que se terminan.
const MODULOS_DISPONIBLES = ['/admin/excursiones', '/admin/certificados'];

function App() {
    return (
        <Routes>
            {/* Visitante y Cliente */}
            <Route
                element={
                    <PublicLayout>
                        <Outlet />
                    </PublicLayout>
                }
            >
                <Route path="/" element={<ListaExcursiones />} />
                <Route path="*" element={<Proximamente titulo="Página no encontrada" texto="La dirección no existe." />} />
            </Route>

            {/* Cargar certificado médico (CU-24): Cliente y Participante. Cada página trae su propio marco. */}
            <Route path="/reservas/:idReserva/certificados" element={<CargarCertificadosPagina />} />
            <Route path="/reservas/:idReserva/mi-certificado" element={<MiCertificadoPagina />} />

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
                <Route path="certificados" element={<AdminCertificados />} />
                <Route path="*" element={<Proximamente />} />
            </Route>
        </Routes>
    );
}

export default App;