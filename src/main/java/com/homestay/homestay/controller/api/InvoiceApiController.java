package com.homestay.homestay.controller.api;

import com.homestay.homestay.dto.PaymentDto;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceApiController {

    private final InvoiceService invoiceService;

    public InvoiceApiController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<Page<Invoice>> getInvoices(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(invoiceService.filterInvoices(keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Invoice> checkout(
            @RequestParam("bookingId") Long bookingId,
            @RequestBody(required = false) PaymentDto paymentDto) {
        String method = (paymentDto != null && paymentDto.getPaymentMethod() != null) ? paymentDto.getPaymentMethod() : "Tiền mặt";
        return ResponseEntity.ok(invoiceService.checkOutAndCreateInvoice(bookingId, method));
    }
}
