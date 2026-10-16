package com.udea.lab1arq;

import com.udea.lab1arq.controller.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web de HealthController.
 *
 * El DataSource es un doble de prueba: asi se simulan los tres estados de la
 * base de datos (disponible, conexion invalida y conexion rechazada) sin
 * depender de que H2 o PostgreSQL esten realmente en ese estado.
 */
@WebMvcTest(controllers = HealthController.class, properties = "app.version=1.2.3")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @Test
    @DisplayName("GET /version informa el nombre y la version del artefacto")
    void version() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("lab1arq-test"))
                .andExpect(jsonPath("$.version").value("1.2.3"));
    }

    @Test
    @DisplayName("GET /health responde 200 y UP cuando la base de datos acepta la conexion")
    void saludableConBaseDeDatos() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));

        // La conexion se devuelve al pool en lugar de quedar abierta
        verify(connection).close();
    }

    @Test
    @DisplayName("GET /health responde 503 cuando la conexion no es valida")
    void conexionInvalida() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(false);

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }

    @Test
    @DisplayName("GET /health responde 503 cuando la base de datos rechaza la conexion")
    void baseDeDatosCaida() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
