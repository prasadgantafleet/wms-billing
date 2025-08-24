package com.wms.billing.repository;

import com.wms.billing.domain.RateSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RateSheetRepository extends JpaRepository<RateSheet, Long> {

    RateSheet getRateSheetByRateSheetId(Long contractId);

    @Query("""
           SELECT rs
           FROM RateSheet rs
           JOIN rs.warehouses w
           WHERE rs.customerId = :customerId
             AND w.warehouseId = :warehouseId
             AND (:customerName IS NULL OR :customerName = '' OR rs.name = :customerName)
           """)
    Optional<RateSheet> findByCustomerIdAndNameAndWarehouseId(@Param("customerId") String customerId,
                                                              @Param("customerName") String customerName,
                                                              @Param("warehouseId") String warehouseId);

    // Strict: require that ALL requested warehouse ids exist under the same RateSheet
    @Query("""
           SELECT rs
           FROM RateSheet rs
           JOIN rs.warehouses w
           WHERE rs.customerId = :customerId
             AND w.warehouseId IN (:warehouseIds)
             AND (:customerName IS NULL OR :customerName = '' OR rs.name = :customerName)
           GROUP BY rs
           HAVING COUNT(DISTINCT w.warehouseId) = :warehouseCount
           """)
    List<RateSheet> findByCustomerAndAllWarehouseIds(@Param("customerId") String customerId,
                                                     @Param("customerName") String customerName,
                                                     @Param("warehouseIds") Collection<String> warehouseIds,
                                                     @Param("warehouseCount") long warehouseCount);

}