package com.udea.lab1arq;

import com.udea.lab1arq.DTO.CustomerDTO;
import com.udea.lab1arq.entity.Customer;
import com.udea.lab1arq.mapper.CustomerMapper;
import com.udea.lab1arq.repository.CustomerRepository;
import com.udea.lab1arq.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de CustomerService.
 *
 * El repositorio y el mapper se sustituyen por dobles de prueba, de modo que
 * se verifica la logica del servicio sin depender de una base de datos. Esto
 * es lo que permite que las pruebas corran en el runner de GitHub Actions, que
 * no tiene ningun servidor MySQL instalado.
 */
class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private CustomerMapper customerMapper;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerMapper = mock(CustomerMapper.class);
        customerService = new CustomerService(customerRepository, customerMapper);
    }

    private Customer customer(Long id, String account, String first, String last, Double balance) {
        return new Customer(id, account, first, last, balance);
    }

    private CustomerDTO dto(Long id, String account, String first, String last, Double balance) {
        return new CustomerDTO(id, first, last, account, balance);
    }

    @Test
    @DisplayName("getAllCustomer devuelve todos los clientes mapeados a DTO")
    void getAllCustomerDevuelveLaLista() {
        Customer uno = customer(1L, "111", "Ana", "Gomez", 100.0);
        Customer dos = customer(2L, "222", "Luis", "Perez", 200.0);
        when(customerRepository.findAll()).thenReturn(List.of(uno, dos));
        when(customerMapper.toDTO(uno)).thenReturn(dto(1L, "111", "Ana", "Gomez", 100.0));
        when(customerMapper.toDTO(dos)).thenReturn(dto(2L, "222", "Luis", "Perez", 200.0));

        List<CustomerDTO> resultado = customerService.getAllCustomer();

        assertEquals(2, resultado.size());
        assertEquals("Ana", resultado.get(0).getFirstName());
        assertEquals("222", resultado.get(1).getAccountNumber());
        verify(customerRepository).findAll();
    }

    @Test
    @DisplayName("getCustomerById devuelve el cliente cuando existe")
    void getCustomerByIdExistente() {
        Customer encontrado = customer(1L, "111", "Ana", "Gomez", 100.0);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(encontrado));
        when(customerMapper.toDTO(encontrado)).thenReturn(dto(1L, "111", "Ana", "Gomez", 100.0));

        CustomerDTO resultado = customerService.getCustomerById(1L);

        assertEquals(1L, resultado.getId());
        assertEquals("Ana", resultado.getFirstName());
    }

    @Test
    @DisplayName("getCustomerById lanza excepcion cuando el id no existe")
    void getCustomerByIdInexistente() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> customerService.getCustomerById(99L));

        assertTrue(error.getMessage().contains("99"));
    }

    @Test
    @DisplayName("createCustomer guarda el cliente cuando la cuenta no esta repetida")
    void createCustomerExitoso() {
        CustomerDTO entrada = dto(null, "333", "Marta", "Ruiz", 500.0);
        Customer entidad = customer(null, "333", "Marta", "Ruiz", 500.0);
        Customer guardada = customer(3L, "333", "Marta", "Ruiz", 500.0);

        when(customerRepository.findByAccountNumber("333")).thenReturn(Optional.empty());
        when(customerMapper.toEntity(entrada)).thenReturn(entidad);
        when(customerRepository.save(entidad)).thenReturn(guardada);
        when(customerMapper.toDTO(guardada)).thenReturn(dto(3L, "333", "Marta", "Ruiz", 500.0));

        CustomerDTO resultado = customerService.createCustomer(entrada);

        assertEquals(3L, resultado.getId());
        verify(customerRepository).save(entidad);
    }

    @Test
    @DisplayName("createCustomer rechaza un numero de cuenta ya registrado")
    void createCustomerCuentaDuplicada() {
        CustomerDTO entrada = dto(null, "111", "Otra", "Persona", 50.0);
        when(customerRepository.findByAccountNumber("111"))
                .thenReturn(Optional.of(customer(1L, "111", "Ana", "Gomez", 100.0)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(entrada));

        assertTrue(error.getMessage().contains("111"));
        // Lo relevante: la operacion se aborta antes de escribir en la base.
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCustomer modifica solo los campos que vienen informados")
    void updateCustomerCamposParciales() {
        Customer existente = customer(1L, "111", "Ana", "Gomez", 100.0);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(customerRepository.save(existente)).thenReturn(existente);
        when(customerMapper.toDTO(existente)).thenReturn(dto(1L, "111", "Anabel", "Gomez", 100.0));

        // Solo se envia el nombre: apellido, cuenta y saldo deben conservarse.
        CustomerDTO cambios = new CustomerDTO();
        cambios.setFirstName("Anabel");

        customerService.updateCustomer(1L, cambios);

        assertEquals("Anabel", existente.getFirstName());
        assertEquals("Gomez", existente.getLastName(), "el apellido no debia cambiar");
        assertEquals("111", existente.getAccountNumber(), "la cuenta no debia cambiar");
        assertEquals(100.0, existente.getBalance(), "el saldo no debia cambiar");
    }

    @Test
    @DisplayName("updateCustomer rechaza un numero de cuenta que ya usa otro cliente")
    void updateCustomerCuentaEnUso() {
        Customer existente = customer(1L, "111", "Ana", "Gomez", 100.0);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(customerRepository.findByAccountNumber("222"))
                .thenReturn(Optional.of(customer(2L, "222", "Luis", "Perez", 200.0)));

        CustomerDTO cambios = new CustomerDTO();
        cambios.setAccountNumber("222");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer(1L, cambios));

        assertTrue(error.getMessage().contains("222"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCustomer lanza excepcion cuando el cliente no existe")
    void updateCustomerInexistente() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer(99L, new CustomerDTO()));
    }

    @Test
    @DisplayName("deleteCustomer elimina al cliente cuando existe")
    void deleteCustomerExistente() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteCustomer lanza excepcion cuando el cliente no existe")
    void deleteCustomerInexistente() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> customerService.deleteCustomer(99L));
        verify(customerRepository, never()).deleteById(anyLong());
    }
}
