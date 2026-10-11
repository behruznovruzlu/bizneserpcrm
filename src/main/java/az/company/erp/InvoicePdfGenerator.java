package az.company.erp;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class InvoicePdfGenerator {

    private static Font getFont(float size, int style) {
        try {
            String fontPath = "C:/Windows/Fonts/arial.ttf";
            if (new File(fontPath).exists()) {
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style, Color.BLACK);
            }
        } catch (Exception ignore) {}
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, Color.BLACK);
    }

    public static void generatePdf(InvoiceEntry invoice, File destinationFile) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(destinationFile));
        document.open();

        Font titleFont = getFont(16, Font.BOLD);
        Font boldFont = getFont(10, Font.BOLD);
        Font regularFont = getFont(9, Font.NORMAL);
        Font headerFont = getFont(9, Font.BOLD);

        // Title
        Paragraph title = new Paragraph("QAİMƏ-FAKTURA / İNVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph invoiceNo = new Paragraph("Nömrə: " + invoice.invoiceNo() + "  |  Tarix: " + invoice.issueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), getFont(11, Font.BOLD));
        invoiceNo.setAlignment(Element.ALIGN_CENTER);
        invoiceNo.setSpacingAfter(15);
        document.add(invoiceNo);

        // Info Table (Seller & Customer)
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        PdfPCell sellerCell = new PdfPCell();
        sellerCell.setBorderWidth(1);
        sellerCell.setPadding(8);
        sellerCell.setBackgroundColor(new Color(245, 247, 250));
        sellerCell.addElement(new Paragraph("SATICI MƏLUMATLARI", boldFont));
        sellerCell.addElement(new Paragraph("Şirkət: " + (invoice.sellerName() == null ? "—" : invoice.sellerName()), regularFont));
        sellerCell.addElement(new Paragraph("VÖEN: " + (invoice.sellerVoen() == null ? "—" : invoice.sellerVoen()), regularFont));
        if (invoice.sellerAddress() != null && !invoice.sellerAddress().isBlank()) {
            sellerCell.addElement(new Paragraph("Ünvan: " + invoice.sellerAddress(), regularFont));
        }
        if (invoice.sellerBankDetails() != null && !invoice.sellerBankDetails().isBlank()) {
            sellerCell.addElement(new Paragraph("Bank / Hesab: " + invoice.sellerBankDetails(), regularFont));
        }
        infoTable.addCell(sellerCell);

        PdfPCell customerCell = new PdfPCell();
        customerCell.setBorderWidth(1);
        customerCell.setPadding(8);
        customerCell.setBackgroundColor(new Color(245, 247, 250));
        customerCell.addElement(new Paragraph("ALICI MƏLUMATLARI", boldFont));
        customerCell.addElement(new Paragraph("Müştəri: " + (invoice.customerName() == null ? "—" : invoice.customerName()), regularFont));
        if (invoice.customerVoen() != null && !invoice.customerVoen().isBlank()) {
            customerCell.addElement(new Paragraph("VÖEN: " + invoice.customerVoen(), regularFont));
        }
        customerCell.addElement(new Paragraph("Ödəniş Növü: " + (invoice.paymentType() == null ? "—" : invoice.paymentType()), regularFont));
        if (invoice.deliveryDocNo() != null && !invoice.deliveryDocNo().isBlank()) {
            customerCell.addElement(new Paragraph("Təhvil Sənədi №: " + invoice.deliveryDocNo(), regularFont));
        }
        infoTable.addCell(customerCell);

        document.add(infoTable);

        // Items Table
        PdfPTable itemTable = new PdfPTable(new float[]{1, 4, 1.5f, 1.5f, 2, 2});
        itemTable.setWidthPercentage(100);
        itemTable.setSpacingAfter(15);

        String[] headers = {"№", "Məhsul / Xidmət Adı", "Vahid", "Miqdar", "Qiymət (AZN)", "Məbləğ (AZN)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new Color(220, 230, 242));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            itemTable.addCell(cell);
        }

        int index = 1;
        if (invoice.items() != null && !invoice.items().isEmpty()) {
            for (InvoiceEntry.InvoiceItem item : invoice.items()) {
                itemTable.addCell(createCell(String.valueOf(index++), regularFont, Element.ALIGN_CENTER));
                itemTable.addCell(createCell(item.itemName(), regularFont, Element.ALIGN_LEFT));
                itemTable.addCell(createCell(item.unit() == null ? "ədəd" : item.unit(), regularFont, Element.ALIGN_CENTER));
                itemTable.addCell(createCell(item.quantity().toString(), regularFont, Element.ALIGN_RIGHT));
                itemTable.addCell(createCell(String.format("%.2f", item.unitPrice()), regularFont, Element.ALIGN_RIGHT));
                itemTable.addCell(createCell(String.format("%.2f", item.lineTotal()), regularFont, Element.ALIGN_RIGHT));
            }
        } else {
            PdfPCell empty = new PdfPCell(new Phrase("Məhsul yoxdur", regularFont));
            empty.setColspan(6);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            itemTable.addCell(empty);
        }

        document.add(itemTable);

        // Total Summary Table
        PdfPTable totalTable = new PdfPTable(new float[]{4, 2});
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingAfter(20);

        totalTable.addCell(createSummaryCell("Cəmi (ƏDV-siz):", boldFont, Element.ALIGN_RIGHT));
        totalTable.addCell(createSummaryCell(String.format("%.2f AZN", invoice.subtotal()), regularFont, Element.ALIGN_RIGHT));

        totalTable.addCell(createSummaryCell("ƏDV (" + invoice.vatRate() + "%):", boldFont, Element.ALIGN_RIGHT));
        totalTable.addCell(createSummaryCell(String.format("%.2f AZN", invoice.vatAmount()), regularFont, Element.ALIGN_RIGHT));

        PdfPCell grandLabel = createSummaryCell("YEKUN ÖDƏNİLƏCƏK:", getFont(10, Font.BOLD), Element.ALIGN_RIGHT);
        grandLabel.setBackgroundColor(new Color(220, 230, 242));
        totalTable.addCell(grandLabel);

        PdfPCell grandVal = createSummaryCell(String.format("%.2f AZN", invoice.totalAmount()), getFont(10, Font.BOLD), Element.ALIGN_RIGHT);
        grandVal.setBackgroundColor(new Color(220, 230, 242));
        totalTable.addCell(grandVal);

        document.add(totalTable);

        // Notes / Extra details block
        if (invoice.notes() != null && !invoice.notes().isBlank()) {
            PdfPTable notesTable = new PdfPTable(1);
            notesTable.setWidthPercentage(100);
            notesTable.setSpacingAfter(15);

            PdfPCell notesCell = new PdfPCell();
            notesCell.setBorderWidth(1);
            notesCell.setPadding(8);
            notesCell.setBackgroundColor(new Color(250, 250, 250));
            notesCell.addElement(new Paragraph("QEYDLƏR VƏ ƏLAVƏ ŞƏRTLƏR:", boldFont));
            notesCell.addElement(new Paragraph(invoice.notes(), regularFont));
            notesTable.addCell(notesCell);

            document.add(notesTable);
        }

        // Signatures Block
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setSpacingBefore(30);

        PdfPCell signSeller = new PdfPCell();
        signSeller.setBorder(Rectangle.NO_BORDER);
        signSeller.addElement(new Paragraph("Təhvil verdi (Satıcı):", boldFont));
        signSeller.addElement(new Paragraph("İmza: ____________________", regularFont));
        signSeller.addElement(new Paragraph("M.Y.", regularFont));
        signTable.addCell(signSeller);

        PdfPCell signCustomer = new PdfPCell();
        signCustomer.setBorder(Rectangle.NO_BORDER);
        signCustomer.addElement(new Paragraph("Təhvil aldı (Alıcı):", boldFont));
        signCustomer.addElement(new Paragraph("İmza: ____________________", regularFont));
        signCustomer.addElement(new Paragraph("M.Y.", regularFont));
        signTable.addCell(signCustomer);

        document.add(signTable);

        document.close();
    }

    private static PdfPCell createCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private static PdfPCell createSummaryCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        return cell;
    }
}
