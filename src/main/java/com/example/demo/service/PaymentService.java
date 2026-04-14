package com.example.demo.service;

import com.example.demo.dto.CreatePaymentRequest;
import com.example.demo.model.Payment;

import java.util.List;

public interface PaymentService {
    Payment create(Long driverId, CreatePaymentRequest req);
    List<Payment> listMine(Long driverId);
    List<Payment> listForOwner(Long ownerId);
}
