package com.example.demo.repository;

import com.example.demo.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    @Query("""
           select pay from Payment pay
           join fetch pay.booking b
           join fetch b.parking p
           join fetch b.driver d
           where d.id = :driverId
           order by pay.createdAt desc
           """)
    List<Payment> findMine(@Param("driverId") Long driverId);

    @Query("""
           select pay from Payment pay
           join fetch pay.booking b
           join fetch b.parking p
           join fetch b.driver d
           where p.ownerId = :ownerId
           order by pay.createdAt desc
           """)
    List<Payment> findForOwner(@Param("ownerId") Long ownerId);
}
