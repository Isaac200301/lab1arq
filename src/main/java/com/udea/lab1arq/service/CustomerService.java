package com.udea.lab1arq.service;

import com.udea.lab1arq.DTO.CustomerDTO;
import com.udea.lab1arq.entity.Customer;
import com.udea.lab1arq.mapper.CustomerMapper;
import com.udea.lab1arq.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Autowired
    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    public List<CustomerDTO> getAllCustomer() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDTO).toList();
    }

    public CustomerDTO getCustomerById(Long id) {
        return customerRepository.findById(id).map(customerMapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("No existe un cliente con id " + id));
    }

    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        customerRepository.findByAccountNumber(customerDTO.getAccountNumber())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Ya existe un cliente con el numero de cuenta " + customerDTO.getAccountNumber());
                });

        Customer customer = customerMapper.toEntity(customerDTO);
        return customerMapper.toDTO(customerRepository.save(customer));
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * Se carga la entidad de la base de datos y se le aplican los cambios, en
     * lugar de mapear el DTO a una entidad nueva. Si se hiciera lo segundo y el
     * DTO viniera con algun campo nulo, se sobreescribiria el valor guardado
     * con null.
     */
    public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe un cliente con id " + id));

        // El numero de cuenta es unique: si cambia, hay que verificar que el
        // nuevo no lo tenga ya otro cliente, o la base de datos rechaza el save.
        if (customerDTO.getAccountNumber() != null
                && !customerDTO.getAccountNumber().equals(customer.getAccountNumber())) {
            customerRepository.findByAccountNumber(customerDTO.getAccountNumber())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "El numero de cuenta " + customerDTO.getAccountNumber() + " ya esta en uso");
                    });
            customer.setAccountNumber(customerDTO.getAccountNumber());
        }

        if (customerDTO.getFirstName() != null) {
            customer.setFirstName(customerDTO.getFirstName());
        }
        if (customerDTO.getLastName() != null) {
            customer.setLastName(customerDTO.getLastName());
        }
        if (customerDTO.getBalance() != null) {
            customer.setBalance(customerDTO.getBalance());
        }

        return customerMapper.toDTO(customerRepository.save(customer));
    }

    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe un cliente con id " + id);
        }
        customerRepository.deleteById(id);
    }
}
