package com.example.demo.service;

import com.example.demo.dto.CreateBookingRequest;
import com.example.demo.dto.UpdateBookingStatusRequest;
import com.example.demo.model.Booking;

import java.util.List;

public interface BookingService {
    Booking create(Long driverId, CreateBookingRequest req);
    List<Booking> listMine(Long driverId);
    List<Booking> listForOwner(Long ownerId);
    Booking updateStatus(Long ownerId, Long bookingId, UpdateBookingStatusRequest req);
    Booking cancel(Long driverId, Long bookingId);
}
