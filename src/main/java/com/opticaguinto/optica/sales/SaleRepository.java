package com.opticaguinto.optica.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
    @Query(value = """
            select s from Sale s left join s.patient p
            where (:status is null or s.status = :status)
            and (:q = ''
                or lower(s.folio) like lower(concat('%', :q, '%'))
                or lower(p.firstName) like lower(concat('%', :q, '%'))
                or lower(p.lastName) like lower(concat('%', :q, '%'))
                or lower(s.rfc) like lower(concat('%', :q, '%'))
                or lower(s.numeroFactura) like lower(concat('%', :q, '%')))
            """,
            countQuery = """
            select count(s) from Sale s left join s.patient p
            where (:status is null or s.status = :status)
            and (:q = ''
                or lower(s.folio) like lower(concat('%', :q, '%'))
                or lower(p.firstName) like lower(concat('%', :q, '%'))
                or lower(p.lastName) like lower(concat('%', :q, '%'))
                or lower(s.rfc) like lower(concat('%', :q, '%'))
                or lower(s.numeroFactura) like lower(concat('%', :q, '%')))
            """)
    Page<Sale> search(@Param("status") SaleStatus status, @Param("q") String q, Pageable pageable);
    Page<Sale> findByPatientId(UUID patientId, Pageable pageable);
}
