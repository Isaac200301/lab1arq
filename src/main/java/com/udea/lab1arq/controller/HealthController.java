package com.udea.lab1arq.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints de diagnostico del servicio desplegado.
 *
 * /version informa que version del artefacto esta corriendo, lo que permite
 * comprobar despues de un despliegue que la nube sirve el build esperado.
 *
 * /health no se limita a responder que el proceso esta vivo: abre una conexion
 * real a la base de datos. En Cloud Run eso distingue un contenedor que arranco
 * de uno que ademas alcanza PostgreSQL, que es la falla mas probable de un
 * despliegue (credenciales, host del pooler o red).
 */
@RestController
public class HealthController {

    // Segundos que se espera a que la base de datos confirme la conexion
    private static final int DB_VALIDATION_TIMEOUT = 2;

    private final DataSource dataSource;
    private final String applicationName;
    private final String version;

    public HealthController(DataSource dataSource,
                            @Value("${spring.application.name}") String applicationName,
                            @Value("${app.version:desconocida}") String version) {
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.version = version;
    }

    //Version del artefacto desplegado
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("application", applicationName);
        body.put("version", version);
        return ResponseEntity.ok(body);
    }

    //Estado del servicio y de su conexion a la base de datos
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean databaseUp = isDatabaseUp();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", databaseUp ? "UP" : "DOWN");
        body.put("database", databaseUp ? "UP" : "DOWN");

        // 503 y no 200: un balanceador o un monitor externo debe poder
        // detectar el fallo por el codigo de estado sin leer el cuerpo
        HttpStatus status = databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DB_VALIDATION_TIMEOUT);
        } catch (SQLException e) {
            return false;
        }
    }
}
