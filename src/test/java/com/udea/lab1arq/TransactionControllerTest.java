package com.udea.lab1arq;

import com.udea.lab1arq.DTO.TransactionDTO;
import com.udea.lab1arq.controller.TransactionController;
import com.udea.lab1arq.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web de TransactionController.
 *
 * A diferencia de CustomerController, este controlador atrapa el
 * IllegalArgumentException por su cuenta y devuelve el mensaje en el cuerpo de
 * la respuesta, en lugar de delegar en el manejador global. Las pruebas
 * reflejan ese comportamiento tal como esta implementado.
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private TransactionDTO transaccion(Long id, String origen, String destino, Double monto) {
        return new TransactionDTO(id, origen, destino, monto, LocalDateTime.now());
    }

    /**
     * Construye el cuerpo de la peticion como JSON literal. No se usa un
     * ObjectMapper porque Spring Boot 4 trabaja con Jackson 3 mientras el
     * pom.xml arrastra ademas Jackson 2, y asi la prueba no depende de cual
     * de los dos resuelve el contexto.
     */
    private String cuerpo(String origen, String destino, double monto) {
        return "{\"senderAccountNumber\":\"" + origen + "\","
                + "\"receiverAccountNumber\":\"" + destino + "\","
                + "\"amount\":" + monto + "}";
    }

    @Test
    @DisplayName("POST /api/transactions realiza la transferencia")
    void transferenciaExitosa() throws Exception {
        when(transactionService.transferMoney(any(TransactionDTO.class)))
                .thenReturn(transaccion(10L, "111", "222", 250.0));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("111", "222", 250.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.senderAccountNumber").value("111"))
                .andExpect(jsonPath("$.amount").value(250.0));
    }

    @Test
    @DisplayName("POST /api/transactions responde 400 cuando el saldo es insuficiente")
    void saldoInsuficiente() throws Exception {
        when(transactionService.transferMoney(any(TransactionDTO.class)))
                .thenThrow(new IllegalArgumentException("Saldo insuficiente en la cuenta del remitente."));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("111", "222", 999999.0)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Saldo insuficiente")));
    }

    @Test
    @DisplayName("POST /api/transactions responde 400 si origen y destino coinciden")
    void mismaCuenta() throws Exception {
        when(transactionService.transferMoney(any(TransactionDTO.class)))
                .thenThrow(new IllegalArgumentException(
                        "La cuenta de origen y la de destino no pueden ser la misma."));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("111", "111", 100.0)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no pueden ser la misma")));
    }

    @Test
    @DisplayName("GET /api/transactions/{cuenta} devuelve el historico de movimientos")
    void historicoDeLaCuenta() throws Exception {
        when(transactionService.getTransactionsForAccount("111")).thenReturn(List.of(
                transaccion(1L, "111", "222", 100.0),
                transaccion(2L, "333", "111", 70.0)));

        mockMvc.perform(get("/api/transactions/111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].receiverAccountNumber").value("222"))
                .andExpect(jsonPath("$[1].amount").value(70.0));
    }

    @Test
    @DisplayName("GET /api/transactions/{cuenta} devuelve lista vacia si no hay movimientos")
    void cuentaSinMovimientos() throws Exception {
        when(transactionService.getTransactionsForAccount("999")).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
