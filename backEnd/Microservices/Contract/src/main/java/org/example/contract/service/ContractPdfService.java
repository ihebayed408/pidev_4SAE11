package org.example.contract.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.example.contract.entity.Conflict;
import org.example.contract.entity.Contract;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class ContractPdfService {

    // ── Brand colours ─────────────────────────────────────────────
    private static final DeviceRgb PRIMARY   = new DeviceRgb(99,  102, 241);  // indigo-500
    private static final DeviceRgb PRIMARY_D = new DeviceRgb(67,  56,  202);  // indigo-700
    private static final DeviceRgb NEUTRAL_9 = new DeviceRgb(17,  24,  39);   // gray-900
    private static final DeviceRgb NEUTRAL_6 = new DeviceRgb(75,  85,  99);   // gray-600
    private static final DeviceRgb NEUTRAL_4 = new DeviceRgb(156, 163, 175);  // gray-400
    private static final DeviceRgb NEUTRAL_1 = new DeviceRgb(243, 244, 246);  // gray-100
    private static final DeviceRgb SUCCESS   = new DeviceRgb(21,  128, 61);   // green-700
    private static final DeviceRgb WARNING   = new DeviceRgb(180, 83,  9);    // amber-700
    private static final DeviceRgb ERROR     = new DeviceRgb(185, 28,  28);   // red-700

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateContractPdf(Contract contract, boolean includeConflicts,
                                       String clientName, String freelancerName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter   writer  = new PdfWriter(baos);
            PdfDocument pdfDoc  = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 48, 40, 48);

            PdfFont bold    = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regular = PdfFontFactory.createFont("Helvetica");

            addHeader(document, bold, regular, contract);
            addSeparator(document);
            addParties(document, bold, regular, contract, clientName, freelancerName);
            addSeparator(document);
            addContractDetails(document, bold, regular, contract);
            addSeparator(document);
            addTerms(document, bold, regular, contract);
            addSeparator(document);
            addSignatures(document, bold, regular, contract);

            if (includeConflicts && contract.getConflicts() != null && !contract.getConflicts().isEmpty()) {
                addSeparator(document);
                addConflicts(document, bold, regular, contract.getConflicts());
            }

            addFooter(document, regular, contract);

            document.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF for contract " + contract.getId(), e);
        }
        return baos.toByteArray();
    }

    // ── Header ────────────────────────────────────────────────────

    private void addHeader(Document doc, PdfFont bold, PdfFont regular, Contract contract) {
        // Platform name
        Paragraph platform = new Paragraph("Smart Freelance Platform")
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(PRIMARY)
                .setTextAlignment(TextAlignment.RIGHT);
        doc.add(platform);

        // Contract title
        Paragraph title = new Paragraph(contract.getTitle())
                .setFont(bold)
                .setFontSize(22)
                .setFontColor(NEUTRAL_9)
                .setMarginTop(4)
                .setMarginBottom(6);
        doc.add(title);

        // Status badge inline with ID
        Table headerMeta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell idCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Contract #" + contract.getId())
                        .setFont(regular)
                        .setFontSize(10)
                        .setFontColor(NEUTRAL_4));

        String statusText = formatStatus(contract.getStatus() != null
                ? contract.getStatus().name() : "DRAFT");
        DeviceRgb statusColor = statusColor(contract.getStatus() != null
                ? contract.getStatus().name() : "DRAFT");

        Cell statusCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(statusText)
                        .setFont(bold)
                        .setFontSize(10)
                        .setFontColor(statusColor));

        headerMeta.addCell(idCell);
        headerMeta.addCell(statusCell);
        doc.add(headerMeta);
    }

    // ── Parties ───────────────────────────────────────────────────

    private void addParties(Document doc, PdfFont bold, PdfFont regular, Contract contract,
                             String clientName, String freelancerName) {
        doc.add(sectionTitle("Parties", bold));

        Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(4);

        String cDisplay = (clientName != null && !clientName.isBlank()) ? clientName : "User #" + contract.getClientId();
        String fDisplay = (freelancerName != null && !freelancerName.isBlank()) ? freelancerName : "User #" + contract.getFreelancerId();

        parties.addCell(partyCard("CLIENT", cDisplay, bold, regular));
        parties.addCell(partyCard("FREELANCER", fDisplay, bold, regular));
        doc.add(parties);
    }

    private Cell partyCard(String role, String displayName, PdfFont bold, PdfFont regular) {
        Cell cell = new Cell()
                .setBackgroundColor(NEUTRAL_1)
                .setBorder(Border.NO_BORDER)
                .setPadding(10)
                .setMargin(4);
        cell.add(new Paragraph(role)
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(PRIMARY)
                .setMarginBottom(2));
        cell.add(new Paragraph(displayName != null ? displayName : "—")
                .setFont(regular)
                .setFontSize(11)
                .setFontColor(NEUTRAL_9));
        return cell;
    }

    // ── Contract Details ──────────────────────────────────────────

    private void addContractDetails(Document doc, PdfFont bold, PdfFont regular, Contract contract) {
        doc.add(sectionTitle("Contract Details", bold));

        Table grid = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(4);

        grid.addCell(detailCard("Contract Value",
                contract.getAmount() != null ? "$" + String.format("%,.2f", contract.getAmount()) : "—",
                bold, regular));
        grid.addCell(detailCard("Start Date", formatDate(contract.getStartDate()), bold, regular));
        grid.addCell(detailCard("End Date",   formatDate(contract.getEndDate()),   bold, regular));

        if (contract.getCreatedAt() != null) {
            grid.addCell(detailCard("Created", contract.getCreatedAt().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")), bold, regular));
        }
        if (contract.getSignedAt() != null) {
            grid.addCell(detailCard("Signed At", contract.getSignedAt().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), bold, regular));
        }
        doc.add(grid);

        if (contract.getDescription() != null && !contract.getDescription().isBlank()) {
            doc.add(new Paragraph("Description")
                    .setFont(bold).setFontSize(10).setFontColor(NEUTRAL_6).setMarginTop(6).setMarginBottom(2));
            doc.add(new Paragraph(contract.getDescription())
                    .setFont(regular).setFontSize(10).setFontColor(NEUTRAL_9)
                    .setMultipliedLeading(1.4f));
        }
    }

    private Cell detailCard(String label, String value, PdfFont bold, PdfFont regular) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(NEUTRAL_1, 1))
                .setPadding(10)
                .setMargin(3);
        cell.add(new Paragraph(label)
                .setFont(bold).setFontSize(8).setFontColor(NEUTRAL_4).setMarginBottom(3));
        cell.add(new Paragraph(value)
                .setFont(bold).setFontSize(12).setFontColor(NEUTRAL_9));
        return cell;
    }

    // ── Terms ─────────────────────────────────────────────────────

    private void addTerms(Document doc, PdfFont bold, PdfFont regular, Contract contract) {
        doc.add(sectionTitle("Terms & Conditions", bold));
        String terms = contract.getTerms() != null && !contract.getTerms().isBlank()
                ? contract.getTerms() : "No terms specified.";
        doc.add(new Paragraph(terms)
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(NEUTRAL_9)
                .setMultipliedLeading(1.5f)
                .setMarginBottom(4));
    }

    // ── Signatures ────────────────────────────────────────────────

    private void addSignatures(Document doc, PdfFont bold, PdfFont regular, Contract contract) {
        doc.add(sectionTitle("Signatures", bold));

        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        sigTable.addCell(sigCell("Client", contract.getClientSignatureUrl(),   bold, regular));
        sigTable.addCell(sigCell("Freelancer", contract.getFreelancerSignatureUrl(), bold, regular));
        doc.add(sigTable);
    }

    private Cell sigCell(String party, String signatureData, PdfFont bold, PdfFont regular) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(NEUTRAL_1, 1.5f))
                .setPadding(12)
                .setMargin(4)
                .setMinHeight(100);

        cell.add(new Paragraph(party.toUpperCase())
                .setFont(bold).setFontSize(8).setFontColor(PRIMARY).setMarginBottom(6));

        if (signatureData != null && !signatureData.isBlank()) {
            try {
                byte[] imgBytes = decodeBase64Image(signatureData);
                Image sig = new Image(ImageDataFactory.create(imgBytes))
                        .setMaxHeight(70)
                        .setHorizontalAlignment(HorizontalAlignment.LEFT);
                cell.add(sig);
                cell.add(new Paragraph("✓ Signed")
                        .setFont(bold).setFontSize(8).setFontColor(SUCCESS).setMarginTop(4));
            } catch (Exception e) {
                cell.add(new Paragraph("Signature on file")
                        .setFont(regular).setFontSize(10).setFontColor(SUCCESS));
            }
        } else {
            cell.add(new Paragraph("Awaiting signature")
                    .setFont(regular).setFontSize(10).setFontColor(NEUTRAL_4)
                    .setItalic());
        }
        return cell;
    }

    // ── Conflicts ─────────────────────────────────────────────────

    private void addConflicts(Document doc, PdfFont bold, PdfFont regular, List<Conflict> conflicts) {
        doc.add(sectionTitle("Conflicts (" + conflicts.size() + ")", bold));

        for (Conflict cf : conflicts) {
            Table row = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(6);

            Cell info = new Cell().setBorder(Border.NO_BORDER)
                    .setBackgroundColor(NEUTRAL_1).setPadding(10);
            info.add(new Paragraph(cf.getReason() != null ? cf.getReason() : "—")
                    .setFont(bold).setFontSize(11).setFontColor(NEUTRAL_9).setMarginBottom(3));
            info.add(new Paragraph(cf.getDescription() != null ? cf.getDescription() : "")
                    .setFont(regular).setFontSize(10).setFontColor(NEUTRAL_6)
                    .setMultipliedLeading(1.4f));
            if (cf.getResolution() != null && !cf.getResolution().isBlank()) {
                info.add(new Paragraph("Resolution: " + cf.getResolution())
                        .setFont(regular).setFontSize(9).setFontColor(SUCCESS).setMarginTop(4));
            }

            DeviceRgb statusCol = cf.getStatus() != null
                    && cf.getStatus().name().equals("RESOLVED") ? SUCCESS : WARNING;
            Cell statusCell = new Cell().setBorder(Border.NO_BORDER)
                    .setBackgroundColor(NEUTRAL_1).setPadding(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            statusCell.add(new Paragraph(cf.getStatus() != null ? cf.getStatus().name() : "OPEN")
                    .setFont(bold).setFontSize(9).setFontColor(statusCol));

            row.addCell(info);
            row.addCell(statusCell);
            doc.add(row);
        }
    }

    // ── Footer ────────────────────────────────────────────────────

    private void addFooter(Document doc, PdfFont regular, Contract contract) {
        doc.add(new Paragraph(" ").setMarginTop(16));
        LineSeparator ls = new LineSeparator(new SolidLine(0.5f));
        ls.setStrokeColor(NEUTRAL_1);
        doc.add(ls);

        String generated = "Generated on " +
                LocalDate.now().format(DATE_FMT) +
                "  |  Smart Freelance Platform  |  Contract #" + contract.getId();
        doc.add(new Paragraph(generated)
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(NEUTRAL_4)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Paragraph sectionTitle(String text, PdfFont bold) {
        return new Paragraph(text)
                .setFont(bold)
                .setFontSize(12)
                .setFontColor(NEUTRAL_9)
                .setMarginTop(14)
                .setMarginBottom(6);
    }

    private void addSeparator(Document doc) {
        LineSeparator ls = new LineSeparator(new SolidLine(0.5f));
        ls.setStrokeColor(NEUTRAL_1);
        ls.setMarginTop(8);
        ls.setMarginBottom(4);
        doc.add(ls);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    private String formatStatus(String status) {
        return switch (status) {
            case "DRAFT"             -> "Draft";
            case "PENDING_SIGNATURE" -> "Pending Signature";
            case "ACTIVE"            -> "Active";
            case "IN_CONFLICT"       -> "In Conflict";
            case "COMPLETED"         -> "Completed";
            case "CANCELLED"         -> "Cancelled";
            default                  -> status;
        };
    }

    private DeviceRgb statusColor(String status) {
        return switch (status) {
            case "ACTIVE"    -> SUCCESS;
            case "IN_CONFLICT", "CANCELLED" -> ERROR;
            case "COMPLETED" -> PRIMARY;
            default          -> NEUTRAL_6;
        };
    }

    /** Strips the data-URL prefix and decodes the Base64 image bytes. */
    private byte[] decodeBase64Image(String dataUrl) {
        String base64 = dataUrl.contains(",") ? dataUrl.split(",", 2)[1] : dataUrl;
        return Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
    }
}
