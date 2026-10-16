package com.udea.lab1arq;

import com.udea.lab1arq.DTO.CustomerDTO;
import com.udea.lab1arq.controller.CustomerController;
import com.udea.lab1arq.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web de CustomerController.
 *
 * @WebMvcTest levanta unicamente el controlador, el manejador global de
 * excepciones y la configuracion de MVC, sin base de datos ni capa de
 * servicio real. Lo que se verifica aqui es el contrato HTTP: rutas, codigos
 * de estado y forma del JSON.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    // Los cuerpos de las peticiones se escriben como JSON literal en lugar de
    // serializarse con un ObjectMapper. Spring Boot 4 usa Jackson 3, mientras
    // que el pom.xml arrastra ademas Jackson 2; escribir el JSON a mano evita
    // depender de cual de los dos resuelve el contexto de pruebas.

    private CustomerDTO dto(Long id, String first, String last, String account, Double balance) {
        return new CustomerDTO(id, first, last, account, balance);
    }

    @Test
    @DisplayName("GET /api/customers devuelve la lista de clientes")
    void listarClientes() throws Exception {
        when(customerService.getAllCustomer()).thenReturn(List.of(
                dto(1L, "Ana", "Gomez", "111", 100.0),
                dto(2L, "Luis", "Perez", "222", 200.0)));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("Ana"))
                .andExpect(jsonPath("$[1].accountNumber").value("222"));
    }

    @Test
    @DisplayName("GET /api/customers/{id} devuelve un cliente")
    void obtenerClientePorId() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(dto(1L, "Ana", "Gomez", "111", 100.0));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    @DisplayName("GET /api/customers/{id} responde 400 cuando el cliente no existe")
    void obtenerClienteInexistente() throws Exception {
        when(customerService.getCustomerById(99L))
                .thenThrow(new IllegalArgumentException("No existe un cliente con id 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No existe un cliente con id 99"));
    }

    @Test
    @DisplayName("POST /api/customers registra un cliente")
    void crearCliente() throws Exception {
        when(customerService.createCustomer(any(CustomerDTO.class)))
                .thenReturn(dto(3L, "Marta", "Ruiz", "333", 500.0));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Marta\",\"lastName\":\"Ruiz\","
                                + "\"accountNumber\":\"333\",\"balance\":500.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.accountNumber").value("333"));
    }

    @Test
    @DisplayName("POST /api/customers responde 400 si falta el saldo inicial")
    void crearClienteSinSaldo() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Marta\",\"lastName\":\"Ruiz\","
                                + "\"accountNumber\":\"333\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El saldo inicial es obligatorio"));

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    @DisplayName("POST /api/customers responde 400 si falta el numero de cuenta")
    void crearClienteSinCuenta() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Marta\",\"lastName\":\"Ruiz\","
                                + "\"accountNumber\":\"   \",\"balance\":500.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El numero de cuenta es obligatorio"));

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    @DisplayName("PUT /api/customers/{id} actualiza un cliente")
    void actualizarCliente() throws Exception {
        when(customerService.updateCustomer(eq(1L), any(CustomerDTO.class)))
                .thenReturn(dto(1L, "Anabel", "Gomez", "111", 100.0));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Anabel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anabel"));
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} responde 204 sin cuerpo")
    void eliminarCliente() throws Exception {
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(1L);
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} responde 400 cuando el cliente no existe")
    void eliminarClienteInexistente() throws Exception {
        doThrow(new IllegalArgumentException("No existe un cliente con id 99"))
                .when(customerService).deleteCustomer(99L);

        mockMvc.perform(delete("/api/customers/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No existe un cliente con id 99"));
    }
}
