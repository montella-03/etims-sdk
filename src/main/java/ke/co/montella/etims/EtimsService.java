package ke.co.montella.etims;

import ke.co.montella.etims.exception.EtimsException;
import ke.co.montella.etims.model.EtimsResponse;
import ke.co.montella.etims.model.EtimsResult;
import ke.co.montella.etims.model.SalesTrnsItem;
import ke.co.montella.etims.model.SendSalesTrnsRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Business-logic layer for eTIMS operations.
 *
 * <p>This class orchestrates request building, validation, and delegation to
 * {@link EtimsClient}.  Application code should use {@link EtimsSdk} rather
 * than calling this class directly.
 */
@Slf4j
public class EtimsService {

    private static final DateTimeFormatter CFM_DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EtimsClient client;
    private final EtimsConfig config;

    /**
     * Creates a service with the supplied configuration.
     *
     * @param config SDK configuration
     */
    public EtimsService(EtimsConfig config) {
        this.config = config;
        this.client = new EtimsClient(config);
    }

    // -----------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------

    /**
     * Initializes the device with KRA and stores the CMC Key.
     *
     * @return {@code true} if initialization succeeded
     */
    public boolean initialize() {
        try {
            EtimsResponse<Object> resp = client.initializeDevice();
            if ("000".equals(resp.getResultCd())) {
                log.info("eTIMS initialized successfully.");
                return true;
            } else {
                log.error("eTIMS initialization failed: resultCd={}, msg={}",
                        resp.getResultCd(), resp.getResultMsg());
                return false;
            }
        } catch (EtimsException e) {
            log.error("eTIMS initialization error: {}", e.getMessage(), e);
            return false;
        }
    }

    // -----------------------------------------------------------------
    // Sales transaction
    // -----------------------------------------------------------------

    /**
     * Builds and submits a normal sales transaction ({@code rcptTyCd = "S"}).
     *
     * @param invoiceNo   your internal invoice number (must be unique per TIN+BHF)
     * @param patientName customer / patient name
     * @param patientTin  customer KRA PIN (may be {@code null} for individuals)
     * @param items       line items (non-empty)
     * @param pmtTyCd     payment type code ({@code "01"}=Cash, {@code "02"}=Credit,
     *                    {@code "03"}=Mobile Money)
     * @return parsed {@link EtimsResult}
     */
    public EtimsResult sendPatientBill(
            String invoiceNo,
            String patientName,
            String patientTin,
            List<SalesTrnsItem> items,
            String pmtTyCd) {

        log.debug("sendPatientBill: invoiceNo={}", invoiceNo);
        SendSalesTrnsRequest request = buildRequest(
                invoiceNo, null, patientName, patientTin, items, pmtTyCd, "S", "N");

        return EtimsResult.from(client.sendSalesTransaction(request));
    }

    // -----------------------------------------------------------------
    // Credit note
    // -----------------------------------------------------------------

    /**
     * Builds and submits a credit note ({@code rcptTyCd = "C"}) reversing a
     * previously issued invoice.
     *
     * <p>Items should carry <em>negative</em> quantities or amounts to
     * represent the reversal, as required by KRA.
     *
     * @param creditNoteNo      your credit note reference number
     * @param originalInvoiceNo the invoice number being reversed
     * @param customerName      customer / patient name
     * @param customerTin       customer KRA PIN (may be {@code null})
     * @param items             line items to reverse (non-empty)
     * @param pmtTyCd           original payment type code
     * @return parsed {@link EtimsResult}
     */
    public EtimsResult sendCreditNote(
            String creditNoteNo,
            String originalInvoiceNo,
            String customerName,
            String customerTin,
            List<SalesTrnsItem> items,
            String pmtTyCd) {

        validateOriginalInvoice(originalInvoiceNo, "credit note");
        log.debug("sendCreditNote: creditNoteNo={}, origInvcNo={}", creditNoteNo, originalInvoiceNo);

        SendSalesTrnsRequest request = buildRequest(
                creditNoteNo, originalInvoiceNo, customerName, customerTin, items, pmtTyCd, "C", "N");

        return EtimsResult.from(client.sendSalesTransaction(request));
    }

    // -----------------------------------------------------------------
    // Debit note
    // -----------------------------------------------------------------

    /**
     * Builds and submits a debit note ({@code rcptTyCd = "D"}) supplementing a
     * previously issued invoice (e.g. to correct an under-billed amount).
     *
     * @param debitNoteNo       your debit note reference number
     * @param originalInvoiceNo the invoice number being supplemented
     * @param customerName      customer / patient name
     * @param customerTin       customer KRA PIN (may be {@code null})
     * @param items             additional line items (non-empty)
     * @param pmtTyCd           payment type code
     * @return parsed {@link EtimsResult}
     */
    public EtimsResult sendDebitNote(
            String debitNoteNo,
            String originalInvoiceNo,
            String customerName,
            String customerTin,
            List<SalesTrnsItem> items,
            String pmtTyCd) {

        validateOriginalInvoice(originalInvoiceNo, "debit note");
        log.debug("sendDebitNote: debitNoteNo={}, origInvcNo={}", debitNoteNo, originalInvoiceNo);

        SendSalesTrnsRequest request = buildRequest(
                debitNoteNo, originalInvoiceNo, customerName, customerTin, items, pmtTyCd, "D", "N");

        return EtimsResult.from(client.sendSalesTransaction(request));
    }

    // -----------------------------------------------------------------
    // Item helper
    // -----------------------------------------------------------------

    /**
     * Creates a {@link SalesTrnsItem} with automatic tax calculation.
     *
     * <p>Tax rates applied:
     * <ul>
     *   <li>{@code "V"} (VAT) → 16 %</li>
     *   <li>{@code "E"} (Exempt) / {@code "Z"} (Zero-rated) → 0 %</li>
     * </ul>
     *
     * @param itemCd   item code registered in the KRA item catalogue
     * @param itemNm   human-readable item name
     * @param qty      quantity dispensed / provided
     * @param unitPrice unit selling price (exclusive of tax for VAT items)
     * @param taxTyCd  tax type code: {@code "V"}, {@code "E"}, or {@code "Z"}
     * @return populated {@link SalesTrnsItem}
     */
    public SalesTrnsItem createItem(String itemCd, String itemNm, double qty,
                                    double unitPrice, String taxTyCd) {
        double splyAmt = round2(unitPrice * qty);
        double taxRate = "V".equals(taxTyCd) ? 0.16 : 0.0;
        double taxAmt  = round2(splyAmt * taxRate);
        double totAmt  = round2(splyAmt + taxAmt);

        return new SalesTrnsItem(itemCd, itemNm, qty, unitPrice, splyAmt,
                0.0, 0.0, taxTyCd, taxAmt, totAmt);
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------

    /**
     * Returns the underlying {@link EtimsClient} for advanced use-cases.
     *
     * @return HTTP client
     */
    public EtimsClient getClient() {
        return client;
    }

    // -----------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------

    private SendSalesTrnsRequest buildRequest(
            String invoiceNo,
            String orgInvcNo,
            String customerName,
            String customerTin,
            List<SalesTrnsItem> items,
            String pmtTyCd,
            String rcptTyCd,
            String salesTyCd) {

        if (items == null || items.isEmpty()) {
            throw new EtimsException("Items list cannot be empty");
        }

        double totTaxAmt = round2(items.stream().mapToDouble(SalesTrnsItem::getTaxAmt).sum());
        double totAmt    = round2(items.stream().mapToDouble(SalesTrnsItem::getTotAmt).sum());

        SendSalesTrnsRequest request = new SendSalesTrnsRequest();
        request.setTin(config.getTin());
        request.setBhfId(config.getBhfId());
        request.setInvcNo(invoiceNo);
        request.setOrgInvcNo(orgInvcNo);
        request.setCustNm(customerName != null && !customerName.trim().isEmpty()
                ? customerName : "CASH CUSTOMER");
        request.setCustTin(customerTin);
        request.setSalesTyCd(salesTyCd);
        request.setRcptTyCd(rcptTyCd);
        request.setPmtTyCd(pmtTyCd);
        request.setSalesSttsCd("1");
        request.setCfmDt(LocalDateTime.now().format(CFM_DT_FORMAT));
        request.setSalesTrnsItems(items);
        request.setTotItemCnt(items.size());
        request.setTotTaxAmt(totTaxAmt);
        request.setTotAmt(totAmt);

        return request;
    }

    private void validateOriginalInvoice(String orgInvcNo, String noteType) {
        if (orgInvcNo == null || orgInvcNo.trim().isEmpty()) {
            throw new EtimsException(
                    "originalInvoiceNo is required for a " + noteType);
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
