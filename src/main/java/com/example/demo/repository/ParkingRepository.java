package com.example.demo.repository;

import com.example.demo.model.BookingStatus;
import com.example.demo.model.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ParkingRepository extends JpaRepository<Parking, Long> {
    List<Parking> findByOwnerId(Long ownerId);
    @Query("""
    select p from Parking p
    where p.active = true
      and (:covered is null or p.covered = :covered)
      and (:minPrice is null or p.pricePerHour >= :minPrice)
      and (:maxPrice is null or p.pricePerHour <= :maxPrice)
      and not exists (
        select 1 from Booking b
        where b.parking = p
          and b.status in :busyStatuses
          and b.startTime < :to
          and b.endTime > :from
      )
""")
    List<Parking> searchAvailable(
            @Param("covered") Boolean covered,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("busyStatuses") Collection<BookingStatus> busyStatuses
    );

}
