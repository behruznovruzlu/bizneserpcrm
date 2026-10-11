package az.company.erp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceEntry(
        long id,
        String invoiceNo,
        String deliveryDocNo,
        LocalDateTime issueDate,
        String sellerName,
        String sellerVoen,
        String sellerAddress,
        String sellerBankDetails,
        String customerName,
        String customerVoen,
        String paymentType,
        BigDecimal vatRate,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        String notes,
        List<InvoiceItem> items
) {
    public record InvoiceItem(
            long id,
            String itemName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}
