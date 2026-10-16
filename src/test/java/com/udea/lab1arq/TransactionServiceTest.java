package com.udea.lab1arq;

import com.udea.lab1arq.DTO.TransactionDTO;
import com.udea.lab1arq.entity.Customer;
import com.udea.lab1arq.entity.Transaction;
import com.udea.lab1arq.repository.CustomerRepository;
import com.udea.lab1arq.repository.TransactionRepository;
import com.udea.lab1arq.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de TransactionService.
 *
 * Las dependencias del servicio estan anotadas con @Autowired sobre los
 * campos, de modo que no hay un constructor por el cual pasarlas. Se inyectan
 * con ReflectionTestUtils, que es el mecanismo que Spring ofrece justamente
 * para este caso.
 */
class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private CustomerRepository customerRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        customerRepository = mock(CustomerRepository.class);
        transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(transactionService, "customerRepository", customerRepository);
    }

    private Customer customer(Long id, String account, Double balance) {
        return new Customer(id, account, "Nombre" + id, "Apellido" + id, balance);
    }

    private TransactionDTO peticion(String origen, String destino, Double monto) {
        TransactionDTO dto = new TransactionDTO();
        dto.setSenderAccountNumber(origen);
        dto.setReceiverAccountNumber(destino);
        dto.setAmount(monto);
        return dto;
    }

    /** Hace que save devuelva la misma transaccion con un id asignado, como haria la base de datos. */
    private void simularPersistenciaDeTransaccion() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocacion -> {
            Transaction t = invocacion.getArgument(0);
            t.setId(10L);
            return t;
        });
    }

    @Test
    @DisplayName("transferMoney mueve el dinero entre las dos cuentas")
    void transferenciaExitosa() {
        Customer origen = customer(1L, "111", 1000.0);
        Customer destino = customer(2L, "222", 500.0);
        when(customerRepository.findByAccountNumber("111")).thenReturn(Optional.of(origen));
        when(customerRepository.findByAccountNumber("222")).thenReturn(Optional.of(destino));
        simularPersistenciaDeTransaccion();

        TransactionDTO resultado = transactionService.transferMoney(peticion("111", "222", 250.0));

        assertEquals(750.0, origen.getBalance(), "debio debitarse la cuenta de origen");
        assertEquals(750.0, destino.getBalance(), "debio acreditarse la cuenta de destino");
        assertEquals(10L, resultado.getId());
        assertEquals(250.0, resultado.getAmount());
        verify(customerRepository).save(origen);
        verify(customerRepository).save(destino);
    }

    @Test
    @DisplayName("transferMoney conserva el total de dinero del sistema")
    void laTransferenciaNoCreaNiDestruyeDinero() {
        Customer origen = customer(1L, "111", 1000.0);
        Customer destino = customer(2L, "222", 500.0);
        double totalAntes = origen.getBalance() + destino.getBalance();
        when(customerRepository.findByAccountNumber("111")).thenReturn(Optional.of(origen));
        when(customerRepository.findByAccountNumber("222")).thenReturn(Optional.of(destino));
        simularPersistenciaDeTransaccion();

        transactionService.transferMoney(peticion("111", "222", 300.0));

        assertEquals(totalAntes, origen.getBalance() + destino.getBalance());
    }

    @Test
    @DisplayName("transferMoney asigna la marca de tiempo en el servidor cuando el cliente no la envia")
    void laMarcaDeTiempoLaPoneElServidor() {
        Customer origen = customer(1L, "111", 1000.0);
        Customer destino = customer(2L, "222", 500.0);
        when(customerRepository.findByAccountNumber("111")).thenReturn(Optional.of(origen));
        when(customerRepository.findByAccountNumber("222")).thenReturn(Optional.of(destino));
        simularPersistenciaDeTransaccion();

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        TransactionDTO resultado = transactionService.transferMoney(peticion("111", "222", 100.0));

        assertNotNull(resultado.getTimestamp(), "la transaccion no puede quedar sin fecha");
        assertTrue(resultado.getTimestamp().isAfter(antes));
    }

    @Test
    @DisplayName("transferMoney rechaza la transferencia si falta algun numero de cuenta")
    void cuentasNulas() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion(null, "222", 100.0)));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", null, 100.0)));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("transferMoney rechaza una transferencia de una cuenta a si misma")
    void mismaCuentaDeOrigenYDestino() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "111", 100.0)));

        assertTrue(error.getMessage().toLowerCase().contains("misma"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("transferMoney rechaza montos nulos, cero o negativos")
    void montosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "222", null)));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "222", 0.0)));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "222", -50.0)));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("transferMoney falla si la cuenta de origen no existe")
    void cuentaDeOrigenInexistente() {
        when(customerRepository.findByAccountNumber("111")).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "222", 100.0)));

        assertTrue(error.getMessage().toLowerCase().contains("remitente"));
    }

    @Test
    @DisplayName("transferMoney falla si la cuenta de destino no existe")
    void cuentaDeDestinoInexistente() {
        when(customerRepository.findByAccountNumber("111"))
                .thenReturn(Optional.of(customer(1L, "111", 1000.0)));
        when(customerRepository.findByAccountNumber("222")).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "222", 100.0)));

        assertTrue(error.getMessage().toLowerCase().contains("receptor"));
    }

    @Test
    @DisplayName("transferMoney rechaza la operacion si el saldo no alcanza")
    void saldoInsuficiente() {
        Customer origen = customer(1L, "111", 50.0);
        Customer destino = customer(2L, "222", 500.0);
        when(customerRepository.findByAccountNumber("111")).thenReturn(Optional.of(origen));
        when(customerRepository.findByAccountNumber("222")).thenReturn(Optional.of(destino));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferMoney(peticion("111", "222", 100.0)));

        assertTrue(error.getMessage().toLowerCase().contains("saldo"));
        // Ningun saldo debio modificarse.
        assertEquals(50.0, origen.getBalance());
        assertEquals(500.0, destino.getBalance());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("getTransactionsForAccount devuelve los movimientos de la cuenta")
    void movimientosDeUnaCuenta() {
        Transaction enviada = new Transaction(1L, "111", "222", 100.0, LocalDateTime.now());
        Transaction recibida = new Transaction(2L, "333", "111", 70.0, LocalDateTime.now());
        when(transactionRepository.findBySenderAccountNumberOrReceiverAccountNumber("111", "111"))
                .thenReturn(List.of(enviada, recibida));

        List<TransactionDTO> resultado = transactionService.getTransactionsForAccount("111");

        assertEquals(2, resultado.size());
        assertEquals(100.0, resultado.get(0).getAmount());
        assertEquals("333", resultado.get(1).getSenderAccountNumber());
    }

    @Test
    @DisplayName("getTransactionsForAccount devuelve una lista vacia si no hay movimientos")
    void cuentaSinMovimientos() {
        when(transactionRepository.findBySenderAccountNumberOrReceiverAccountNumber("999", "999"))
                .thenReturn(List.of());

        assertTrue(transactionService.getTransactionsForAccount("999").isEmpty());
    }
}
