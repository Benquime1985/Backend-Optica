package com.opticaguinto.optica.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SaleDtos {
    private SaleDtos() {}
    public record SaleItemRequest(UUID productId, UUID serviceId, @Min(1) int quantity) {}
    public enum ItemType { PRODUCT, SERVICE }
    public record PaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal monto, @NotNull PaymentMethod metodo) {}
    public record InvoiceRequest(@NotBlank String rfc, @NotBlank String numeroFactura) {}
    public record AddItemsRequest(@NotEmpty List<@Valid SaleItemRequest> items) {}
    public record CreateSaleRequest(UUID patientId, BigDecimal discount,
                                    @NotEmpty List<@Valid SaleItemRequest> items,
                                    @Valid PaymentRequest pago) {}
    public record SaleItemResponse(UUID productId, UUID serviceId, String productName, String serviceName,
                                   ItemType itemType, int quantity,
                                   BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record PaymentResponse(UUID id, PaymentMethod metodo, BigDecimal monto, Instant createdAt) {}
    public record SaleResponse(UUID id, String folio, UUID patientId, UUID sellerId, String sellerName,
                               BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                               SaleStatus status, Instant createdAt, List<SaleItemResponse> items,
                               String rfc, String numeroFactura, List<PaymentResponse> payments,
                               BigDecimal totalEfectivo, BigDecimal totalTarjeta,
                               BigDecimal totalPagado, BigDecimal saldoPendiente) {
        public static SaleResponse from(Sale sale) {
            List<SaleItemResponse> items = sale.getItems().stream()
                    .map(item -> new SaleItemResponse(
                            item.getProduct() == null ? null : item.getProduct().getId(),
                            item.getService() == null ? null : item.getService().getId(),
                            item.getProduct() == null ? null : item.getProduct().getName(),
                            item.getService() == null ? null : item.getService().getName(),
                            item.getService() != null ? ItemType.SERVICE : ItemType.PRODUCT,
                            item.getQuantity(), item.getUnitPrice(), item.getLineTotal())).toList();
            List<PaymentResponse> payments = sale.getPayments().stream()
                    .map(p -> new PaymentResponse(p.getId(), p.getMetodo(), p.getMonto(), p.getCreatedAt())).toList();
            BigDecimal totalEfectivo = payments.stream().filter(p -> p.metodo() == PaymentMethod.EFECTIVO)
                    .map(PaymentResponse::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTarjeta = payments.stream().filter(p -> p.metodo() == PaymentMethod.TARJETA)
                    .map(PaymentResponse::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
            // shortcut: venta legacy COMPLETADA sin pagos se trata como pagada en su totalidad, upgrade cuando existan pagos reales
            BigDecimal totalPagado = payments.isEmpty() && sale.getStatus() == SaleStatus.COMPLETADA
                    ? sale.getTotal() : totalEfectivo.add(totalTarjeta);
            BigDecimal saldoPendiente = sale.getTotal().subtract(totalPagado);
            return new SaleResponse(sale.getId(), sale.getFolio(), sale.getPatient() == null ? null : sale.getPatient().getId(),
                    sale.getSeller().getId(), sale.getSeller().getName(), sale.getSubtotal(), sale.getDiscount(), sale.getTotal(),
                    sale.getStatus(), sale.getCreatedAt(), items, sale.getRfc(), sale.getNumeroFactura(),
                    payments, totalEfectivo, totalTarjeta, totalPagado, saldoPendiente);
        }
    }
}
