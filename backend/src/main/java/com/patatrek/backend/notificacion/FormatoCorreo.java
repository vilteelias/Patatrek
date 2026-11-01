package com.patatrek.backend.notificacion;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de los datos que se muestran en los correos (iguales a los de las pantallas). */
public final class FormatoCorreo {

    private static final Locale AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private FormatoCorreo() {}

    /** $ 95.000 (sin decimales, como en las pantallas). */
    public static String pesos(BigDecimal importe) {
        NumberFormat f = NumberFormat.getNumberInstance(AR);
        f.setMaximumFractionDigits(0);
        return "$ " + f.format(importe == null ? BigDecimal.ZERO : importe);
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String fechaHora(LocalDateTime momento) {
        return momento == null ? "" : momento.format(FECHA_HORA);
    }

    /** "06/11/2026, 08:00 h" o, si dura varios días, "06/11/2026 al 08/11/2026, 08:00 h". */
    public static String salida(LocalDate inicio, LocalDate fin, LocalTime hora) {
        String dias = fin == null || fin.equals(inicio) ? fecha(inicio) : fecha(inicio) + " al " + fecha(fin);
        return hora == null ? dias : dias + ", " + hora.format(HORA) + " h";
    }
}
