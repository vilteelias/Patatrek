// Cómo se muestra cada estado de certificado (colores del tema: ver index.css)
export const ESTADOS = {
  PENDIENTE: { etiqueta: 'Sin certificado cargado', clase: 'text-muted-foreground', puedeCargar: true },
  CARGADO: { etiqueta: 'Cargado — en revisión', clase: 'text-secondary-foreground', nota: 'Esperando revisión' },
  APROBADO: { etiqueta: 'Aprobado', clase: 'text-accent-foreground' },
  RECHAZADO: { etiqueta: 'Rechazado', clase: 'text-destructive', puedeCargar: true },
  RECHAZADO_DEFINITIVO: {
    etiqueta: 'Rechazado definitivo',
    clase: 'text-destructive',
    nota: 'No se puede volver a cargar',
  },
};

export const iniciales = (apellido, nombre) => `${apellido?.[0] ?? ''}${nombre?.[0] ?? ''}`.toUpperCase();
