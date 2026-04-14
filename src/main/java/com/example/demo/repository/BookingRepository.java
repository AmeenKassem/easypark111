package com.example.demo.repository;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Fetch bookings created by a specific driver
    @Query("""
           select b from Booking b
           join fetch b.parking p
           where b.driver.id = :driverId
           order by b.startTime desc
           """)
    List<Booking> findMine(@Param("driverId") Long driverId);

    // Fetch bookings for parking spots owned by a specific owner (Parking has ownerId field)
    @Query("""
           select b from Booking b
           join fetch b.parking p
           where p.ownerId = :ownerId
           order by b.startTime desc
           """)
    List<Booking> findForOwner(@Param("ownerId") Long ownerId);

    // Detect overlapping bookings for the same parking spot (for active statuses)
    @Query("""
           select count(b) from Booking b
           where b.parking.id = :parkingId
             and b.status in :activeStatuses
             and b.startTime < :endTime
             and b.endTime > :startTime
           """)
    long countOverlaps(@Param("parkingId") Long parkingId,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime,
                       @Param("activeStatuses") Collection<BookingStatus> activeStatuses);

    // Calculate total revenue for an owner based on APPROVED bookings only
    // Assuming 'APPROVED' is the status where money is earned.
    @Query("""
           select sum(b.totalPrice) from Booking b
           join b.parking p
           where p.ownerId = :ownerId
           and b.status = 'APPROVED'
           """)
    Double calculateTotalRevenueForOwner(@Param("ownerId") Long ownerId);

    // Calculate total expenses for a driver
    @Query("""
           select sum(b.totalPrice) from Booking b
           where b.driver.id = :driverId
           and b.status = 'APPROVED'
           """)
    Double calculateTotalExpensesForDriver(@Param("driverId") Long driverId);

    @Query("""
       select b from Booking b
       where b.parking.id = :parkingId
         and b.status in :activeStatuses
         and b.startTime < :to
         and b.endTime > :from
       order by b.startTime asc
       """)
    List<Booking> findOverlaps(@Param("parkingId") Long parkingId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("activeStatuses") Collection<BookingStatus> activeStatuses);
    @Query("""
       select count(b) > 0 from Booking b
       where b.parking.id = :parkingId
         and b.status = com.example.demo.model.BookingStatus.APPROVED
         and b.endTime > :now
       """)
    boolean existsApprovedNotEndedForParking(@Param("parkingId") Long parkingId,
                                             @Param("now") LocalDateTime now);

    boolean existsByParkingIdAndDriverIdAndStatus(Long parkingId, Long driverId, BookingStatus status);
}
