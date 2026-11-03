package com.patatrek.backend.notificacion;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Convierte un Correo en HTML con el diseño de PataTrek (mismos colores que el frontend)
 * y en texto plano para los programas de correo que no muestran HTML.
 * Los estilos van en línea porque Gmail y Outlook ignoran las hojas de estilo.
 */
@Component
public class PlantillaCorreo {

    /** Identificador del logo adjunto en el correo (se referencia como cid:logo-patatrek). */
    public static final String CID_LOGO = "logo-patatrek";

    private static final String VERDE = "#2d6a55";
    private static final String VERDE_OSCURO = "#1e4a3d";
    private static final String CREMA = "#f8f5ee";
    private static final String TEXTO = "#1f2a25";
    private static final String GRIS = "#6a6f68";
    private static final String BORDE = "#e6e0d2";

    public String html(Correo c, boolean conLogo) {
        StringBuilder h = new StringBuilder(4096);
        h.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(e(c.asunto())).append("</title></head>")
                .append("<body style=\"margin:0;padding:0;background:").append(CREMA).append(";\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:")
                .append(CREMA).append(";padding:24px 12px;\"><tr><td align=\"center\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;")
                .append("background:#ffffff;border:1px solid ").append(BORDE).append(";border-radius:14px;overflow:hidden;")
                .append("font-family:Arial,Helvetica,sans-serif;color:").append(TEXTO).append(";\">");

        // Encabezado
        h.append("<tr><td style=\"background:").append(VERDE_OSCURO).append(";padding:18px 24px;\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
        if (conLogo) {
            h.append("<td style=\"padding-right:12px;\"><img src=\"cid:").append(CID_LOGO)
                    .append("\" width=\"44\" height=\"44\" alt=\"\" style=\"display:block;border-radius:50%;\"></td>");
        }
        h.append("<td style=\"font-family:Georgia,'Times New Roman',serif;font-size:22px;font-weight:bold;color:#ffffff;\">PataTrek</td>")
                .append("</tr></table></td></tr>");

        // Cuerpo
        h.append("<tr><td style=\"padding:28px 24px 8px;\">")
                .append("<h1 style=\"margin:0 0 16px;font-family:Georgia,'Times New Roman',serif;font-size:24px;line-height:1.25;color:")
                .append(TEXTO).append(";\">").append(e(c.titulo())).append("</h1>");
        if (c.saludo() != null) {
            h.append(p(c.saludo()));
        }
        c.parrafos().forEach(par -> h.append(p(par)));

        if (!c.datos().isEmpty()) {
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px 0 20px;")
                    .append("border:1px solid ").append(BORDE).append(";border-radius:10px;border-collapse:separate;\">");
            for (int i = 0; i < c.datos().size(); i++) {
                Correo.Dato d = c.datos().get(i);
                String borde = i == 0 ? "" : "border-top:1px solid " + BORDE + ";";
                h.append("<tr><td style=\"padding:10px 14px;font-size:14px;color:").append(GRIS).append(";").append(borde)
                        .append("\">").append(e(d.etiqueta())).append("</td>")
                        .append("<td align=\"right\" style=\"padding:10px 14px;font-size:").append(d.destacado() ? "17px" : "14px")
                        .append(";font-weight:bold;color:").append(TEXTO).append(";").append(borde).append("\">")
                        .append(e(d.valor())).append("</td></tr>");
            }
            h.append("</table>");
        }

        if (c.textoBoton() != null && c.urlBoton() != null) {
            h.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:4px 0 24px;\"><tr>")
                    .append("<td style=\"background:").append(VERDE).append(";border-radius:8px;\">")
                    .append("<a href=\"").append(e(c.urlBoton())).append("\" style=\"display:inline-block;padding:12px 22px;")
                    .append("font-size:15px;font-weight:bold;color:#ffffff;text-decoration:none;\">")
                    .append(e(c.textoBoton())).append("</a></td></tr></table>");
        }
        if (c.nota() != null) {
            h.append("<p style=\"margin:0 0 20px;font-size:12px;line-height:1.5;color:").append(GRIS).append(";\">")
                    .append(conSaltos(c.nota())).append("</p>");
        }
        h.append("</td></tr>");

        // Pie
        h.append("<tr><td style=\"padding:16px 24px;background:").append(CREMA).append(";border-top:1px solid ").append(BORDE)
                .append(";font-size:12px;line-height:1.5;color:").append(GRIS).append(";\">")
                .append("Este es un correo automático de PataTrek, excursiones de senderismo. No hace falta que lo respondas.")
                .append("</td></tr></table></td></tr></table></body></html>");
        return h.toString();
    }

    public String texto(Correo c) {
        StringBuilder t = new StringBuilder();
        t.append(c.titulo()).append("\n\n");
        if (c.saludo() != null) t.append(c.saludo()).append("\n\n");
        c.parrafos().forEach(par -> t.append(par).append("\n\n"));
        if (!c.datos().isEmpty()) {
            c.datos().forEach(d -> t.append(d.etiqueta()).append(": ").append(d.valor()).append('\n'));
            t.append('\n');
        }
        if (c.textoBoton() != null && c.urlBoton() != null) {
            t.append(c.textoBoton()).append(": ").append(c.urlBoton()).append("\n\n");
        }
        if (c.nota() != null) t.append(c.nota()).append("\n\n");
        t.append("—\nPataTrek · Correo automático, no hace falta que lo respondas.");
        return t.toString();
    }

    private static String p(String texto) {
        return "<p style=\"margin:0 0 14px;font-size:15px;line-height:1.55;color:" + TEXTO + ";\">" + conSaltos(texto) + "</p>";
    }

    private static String conSaltos(String texto) {
        return e(texto).replace("\r\n", "\n").replace("\n", "<br>");
    }

    private static String e(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto, "UTF-8");
    }
}
