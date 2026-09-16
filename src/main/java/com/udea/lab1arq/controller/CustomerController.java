package com.udea.lab1arq.controller;


import com.udea.lab1arq.DTO.CustomerDTO;
import com.udea.lab1arq.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerFacade;

    //Obtener todos los clientes
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerFacade.getAllCustomer());
    }

    //Obtener un cliente por un Id
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerFacade.getCustomerById(id));
    }


    //Crear un nuevo cliente
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerDTO customerDTO) {
        if (customerDTO.getBalance() == null) {
            throw new IllegalArgumentException("El saldo inicial es obligatorio");
        }
        if (customerDTO.getAccountNumber() == null || customerDTO.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("El numero de cuenta es obligatorio");
        }

        return ResponseEntity.ok(customerFacade.createCustomer(customerDTO));
    }

    //Actualizar un cliente existente
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable Long id,
                                                     @RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.ok(customerFacade.updateCustomer(id, customerDTO));
    }

    //Borrar un cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerFacade.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
