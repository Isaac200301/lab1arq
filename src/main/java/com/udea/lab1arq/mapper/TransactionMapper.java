package com.udea.lab1arq.mapper;

import com.udea.lab1arq.DTO.TransactionDTO;
import com.udea.lab1arq.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);
    TransactionDTO toDTO(Transaction transaction);



}

