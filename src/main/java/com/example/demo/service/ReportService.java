package com.example.demo.service;

import com.example.demo.dto.BookingResponse;
import com.example.demo.dto.OwnerDashboardResponse;
import com.example.demo.dto.DriverReportResponse;
import com.example.demo.model.Booking;
import com.example.demo.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final BookingRepository bookingRepository;


    public ReportService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public OwnerDashboardResponse getOwnerDashboard(Long ownerId) {

        List<Booking> ownerBookings = bookingRepository.findForOwner(ownerId);
        List<BookingResponse> history = ownerBookings.stream()
                .map(BookingResponse::from) //
                .collect(Collectors.toList());

        Double revenue = bookingRepository.calculateTotalRevenueForOwner(ownerId);

        return new OwnerDashboardResponse(revenue, history.size(), history);
    }

    public DriverReportResponse getDriverReport(Long driverId) {

        List<Booking> driverBookings = bookingRepository.findMine(driverId); //
        List<BookingResponse> history = driverBookings.stream()
                .map(BookingResponse::from) //
                .collect(Collectors.toList());

        Double expenses = bookingRepository.calculateTotalExpensesForDriver(driverId);

        return new DriverReportResponse(expenses, history.size(), history);
    }
}