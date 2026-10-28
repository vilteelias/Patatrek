package com.patatrek.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Si la tabla "pago" la creó Hibernate (ddl-auto=update) en lugar del script patatrek_sqlserver.sql,
 * SQL Server le agregó una restricción CHECK que solo admite los modos SENA, TOTAL y SALDO.
 * Esa restricción impide registrar la transacción de reintegro (modo REINTEGRO, CU-27).
 * Al arrancar, se quitan las restricciones CHECK de pago.modo_pago que no admitan REINTEGRO.
 * El script oficial no tiene esa restricción, así que en una base creada con él no hace nada.
 */
@Component
@Order(0)
public class AjusteRestriccionesBase implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteRestriccionesBase.class);

    private final DataSource dataSource;

    public AjusteRestriccionesBase(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection con = dataSource.getConnection()) {
            if (!con.getMetaData().getDatabaseProductName().toLowerCase().contains("sql server")) {
                return; // H2 (perfil demo y tests): Hibernate ya crea la tabla con todos los modos
            }
            List<String> restricciones = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    select cc.name from sys.check_constraints cc
                    join sys.columns c on c.object_id = cc.parent_object_id and c.column_id = cc.parent_column_id
                    where cc.parent_object_id = object_id('dbo.pago') and c.name = 'modo_pago'
                      and cc.definition not like '%REINTEGRO%'""");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    restricciones.add(rs.getString(1));
                }
            }
            for (String nombre : restricciones) {
                try (Statement st = con.createStatement()) {
                    st.execute("alter table dbo.pago drop constraint [" + nombre.replace("]", "]]") + "]");
                }
                log.info("Se quitó la restricción {} de pago.modo_pago para admitir el modo REINTEGRO", nombre);
            }
        } catch (Exception e) {
            log.warn("No se pudo revisar la restricción de pago.modo_pago: {}", e.getMessage());
        }
    }
}
