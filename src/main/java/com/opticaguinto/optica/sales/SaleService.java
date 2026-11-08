package com.opticaguinto.optica.sales;

import com.opticaguinto.optica.patients.Patient;
import com.opticaguinto.optica.patients.PatientService;
import com.opticaguinto.optica.products.Product;
import com.opticaguinto.optica.products.ProductService;
import com.opticaguinto.optica.services.ServiceItem;
import com.opticaguinto.optica.services.ServiceItemService;
import com.opticaguinto.optica.users.User;
import com.opticaguinto.optica.users.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class SaleService {
    private final SaleRepository repository;
    private final ProductService products;
    private final ServiceItemService shopServices;
    private final PatientService patients;
    private final UserService users;

    public SaleService(SaleRepository repository, ProductService products, ServiceItemService shopServices, PatientService patients, UserService users) {
        this.repository = repository; this.products = products; this.shopServices = shopServices; this.patients = patients; this.users = users;
    }

    @Transactional
    public SaleDtos.SaleResponse create(SaleDtos.CreateSaleRequest request, String sellerEmail) {
        User seller = users.getByEmail(sellerEmail);
        Patient patient = request.patientId() == null ? null : patients.getActive(request.patientId());
        BigDecimal subtotal = BigDecimal.ZERO;
        Sale sale = new Sale();
        sale.setSeller(seller);
        sale.setPatient(patient);
        sale.setDiscount(normalizeDiscount(request.discount()));

        for (SaleDtos.SaleItemRequest itemRequest : request.items()) {
            SaleItem item = buildItem(itemRequest);
            sale.addItem(item);
            subtotal = subtotal.add(item.getLineTotal());
        }
        if (sale.getDiscount().compareTo(subtotal) > 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El descuento no puede superar el subtotal");
        sale.setSubtotal(subtotal);
        sale.setTotal(subtotal.subtract(sale.getDiscount()).setScale(2, RoundingMode.HALF_UP));
        sale.setFolio("V-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (request.pago() == null) {
            sale.setStatus(SaleStatus.COMPLETADA);
        } else {
            BigDecimal pago = normalizeAmount(request.pago().monto());
            if (pago.compareTo(sale.getTotal()) > 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El pago no puede superar el total de la venta");
            SalePayment payment = new SalePayment();
            payment.setMetodo(request.pago().metodo());
            payment.setMonto(pago);
            sale.addPayment(payment);
            sale.setStatus(pago.compareTo(sale.getTotal()) >= 0 ? SaleStatus.PAGADA_POR_ENTREGAR : SaleStatus.PENDIENTE);
        }
        return SaleDtos.SaleResponse.from(repository.save(sale));
    }

    @Transactional
    public SaleDtos.SaleResponse registerPayment(UUID saleId, SaleDtos.PaymentRequest request) {
        Sale sale = loadSale(saleId);
        requireOpenForPayment(sale);
        BigDecimal monto = normalizeAmount(request.monto());
        BigDecimal pagado = totalPagado(sale).add(monto);
        if (pagado.compareTo(sale.getTotal()) > 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El pago no puede superar el total de la venta");
        SalePayment payment = new SalePayment();
        payment.setMetodo(request.metodo());
        payment.setMonto(monto);
        sale.addPayment(payment);
        sale.setStatus(pagado.compareTo(sale.getTotal()) >= 0 ? SaleStatus.PAGADA_POR_ENTREGAR : SaleStatus.PENDIENTE);
        return SaleDtos.SaleResponse.from(repository.save(sale));
    }

    @Transactional
    public SaleDtos.SaleResponse addItems(UUID saleId, SaleDtos.AddItemsRequest request) {
        Sale sale = loadSale(saleId);
        requireOpenForItems(sale);
        BigDecimal subtotal = sale.getSubtotal();
        for (SaleDtos.SaleItemRequest itemRequest : request.items()) {
            SaleItem item = buildItem(itemRequest);
            sale.addItem(item);
            subtotal = subtotal.add(item.getLineTotal());
        }
        if (sale.getDiscount().compareTo(subtotal) > 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El descuento no puede superar el subtotal");
        sale.setSubtotal(subtotal);
        sale.setTotal(subtotal.subtract(sale.getDiscount()).setScale(2, RoundingMode.HALF_UP));
        BigDecimal pagado = totalPagado(sale);
        if (!sale.getPayments().isEmpty()) {
            if (pagado.compareTo(sale.getTotal()) > 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El pago no puede superar el total de la venta");
            sale.setStatus(pagado.compareTo(sale.getTotal()) >= 0 ? SaleStatus.PAGADA_POR_ENTREGAR : SaleStatus.PENDIENTE);
        }
        return SaleDtos.SaleResponse.from(repository.save(sale));
    }

    @Transactional
    public SaleDtos.SaleResponse deliver(UUID saleId) {
        Sale sale = loadSale(saleId);
        if (sale.getStatus() == SaleStatus.COMPLETADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta ya fue entregada");
        if (sale.getStatus() == SaleStatus.CANCELADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta está cancelada");
        if (totalPagado(sale).compareTo(sale.getTotal()) < 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta tiene saldo pendiente, no puede entregarse");
        sale.setStatus(SaleStatus.COMPLETADA);
        return SaleDtos.SaleResponse.from(repository.save(sale));
    }

    @Transactional
    public SaleDtos.SaleResponse saveInvoice(UUID saleId, SaleDtos.InvoiceRequest request) {
        Sale sale = loadSale(saleId);
        if (sale.getStatus() == SaleStatus.CANCELADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta está cancelada");
        sale.setRfc(request.rfc().trim().toUpperCase());
        sale.setNumeroFactura(request.numeroFactura().trim());
        return SaleDtos.SaleResponse.from(repository.save(sale));
    }

    @Transactional(readOnly = true)
    public Page<SaleDtos.SaleResponse> search(String query, String status, Pageable pageable) {
        return repository.search(parseStatus(status), query == null || query.isBlank() ? "" : query.trim(), pageable)
                .map(SaleDtos.SaleResponse::from);
    }

    private SaleStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return SaleStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new com.opticaguinto.optica.shared.exception.BusinessException("Estado de venta inválido");
        }
    }

    @Transactional(readOnly = true)
    public Page<SaleDtos.SaleResponse> findByPatient(UUID patientId, Pageable pageable) {
        patients.getActive(patientId);
        return repository.findByPatientId(patientId, pageable).map(SaleDtos.SaleResponse::from);
    }

    @Transactional(readOnly = true)
    public SaleDtos.SaleResponse get(UUID id) {
        Sale sale = repository.findById(id)
                .orElseThrow(() -> new com.opticaguinto.optica.shared.exception.ResourceNotFoundException("Venta no encontrada"));
        return SaleDtos.SaleResponse.from(sale);
    }

    private BigDecimal normalizeDiscount(BigDecimal discount) {
        if (discount == null) return BigDecimal.ZERO.setScale(2);
        if (discount.signum() < 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El descuento no puede ser negativo");
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new com.opticaguinto.optica.shared.exception.BusinessException("El monto del pago debe ser mayor a cero");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private Sale loadSale(UUID saleId) {
        return repository.findById(saleId)
                .orElseThrow(() -> new com.opticaguinto.optica.shared.exception.ResourceNotFoundException("Venta no encontrada"));
    }

    private void requireOpenForPayment(Sale sale) {
        if (sale.getStatus() == SaleStatus.COMPLETADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta ya fue entregada, no admite pagos");
        if (sale.getStatus() == SaleStatus.CANCELADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta está cancelada");
    }

    private void requireOpenForItems(Sale sale) {
        if (sale.getStatus() == SaleStatus.COMPLETADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta ya fue entregada, no admite artículos");
        if (sale.getStatus() == SaleStatus.CANCELADA) throw new com.opticaguinto.optica.shared.exception.BusinessException("La venta está cancelada");
    }

    private SaleItem buildItem(SaleDtos.SaleItemRequest itemRequest) {
        boolean hasProduct = itemRequest.productId() != null;
        boolean hasService = itemRequest.serviceId() != null;
        if (hasProduct == hasService) {
            throw new com.opticaguinto.optica.shared.exception.BusinessException("Cada ítem debe tener productId o serviceId, pero no ambos");
        }
        SaleItem item = new SaleItem();
        BigDecimal unitPrice;
        if (hasProduct) {
            Product product = products.reserve(itemRequest.productId(), itemRequest.quantity());
            item.setProduct(product);
            unitPrice = product.getPrice();
        } else {
            ServiceItem service = shopServices.getActive(itemRequest.serviceId());
            item.setService(service);
            unitPrice = service.getPrice();
        }
        item.setQuantity(itemRequest.quantity());
        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity())).setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    private BigDecimal totalPagado(Sale sale) {
        return sale.getPayments().stream().map(SalePayment::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
