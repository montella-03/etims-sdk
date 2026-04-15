package ke.co.montella.etims;

import ke.co.montella.etims.model.EtimsResult;
import ke.co.montella.etims.model.SalesTrnsItem;
import ke.co.montella.etims.spring.EtimsAutoConfiguration;
import ke.co.montella.etims.spring.EtimsProperties;

import java.util.List;

/**
 * KRA eTIMS Java SDK — primary entry point.
 *
 * <p>This is the only class most developers need to interact with.  Create
 * an instance, call {@link #initialize()} once at startup, then use the
 * submission methods for your business flows.
 *
 * <h3>Plain Java quick-start</h3>
 * <pre>{@code
 * EtimsConfig config = new EtimsConfig();
 * config.setTin("P051234567X");
 * config.setBhfId("00");
 * config.setDeviceSrlNo("SN-123456");
 * config.setProduction(false); // sandbox
 *
 * EtimsSdk sdk = new EtimsSdk(config);
 * sdk.initialize();
 *
 * SalesTrnsItem item = sdk.createItem("MED001", "Paracetamol 500mg", 2, 50.0, "V");
 * EtimsResult result = sdk.sendBill("INV-0001", "John Doe", null,
 *                                   List.of(item), "01");
 *
 * if (result.isSuccess()) {
 *     String qr = result.getQrCode().orElse("");
 *     System.out.println("KRA receipt no: " + result.getRcptNo().orElse("?"));
 * }
 * }</pre>
 *
 * <h3>Spring Boot</h3>
 * <p>Add the following to {@code application.properties} and the SDK bean is
 * auto-configured:
 * <pre>
 * etims.tin=P051234567X
 * etims.bhf-id=00
 * etims.device-srl-no=SN-123456
 * etims.production=false
 * </pre>
 *
 * @see EtimsConfig
 * @see EtimsResult
 * @see EtimsProperties
 * @see EtimsAutoConfiguration
 */
public class EtimsSdk {

    private final EtimsService service;

    /**
     * Creates an SDK instance backed by the supplied configuration.
     *
     * @param config SDK configuration (TIN, device serial, mode, etc.)
     */
    public EtimsSdk(EtimsConfig config) {
        this.service = new EtimsService(config);
    }

    // -----------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------

    /**
     * Initializes the device with KRA by calling {@code selectInitOsdcInfo}.
     *
     * <p>Must be called once at application startup before any transaction
     * submission.  The CMC Key obtained from KRA is stored automatically in
     * the config and included in subsequent requests.
     *
     * @return {@code true} if the device was initialized successfully
     */
    public boolean initialize() {
        return service.initialize();
    }

    /**
     * Returns {@code true} if the SDK has obtained a CMC Key from KRA.
     *
     * @return initialization state
     */
    public boolean isInitialized() {
        return service.getClient().isInitialized();
    }

    // -----------------------------------------------------------------
    // Sales transaction
    // -----------------------------------------------------------------

    /**
     * Sends a normal sales transaction (patient bill) to KRA eTIMS.
     *
     * <pre>{@code
     * EtimsResult r = sdk.sendBill("INV-001", "Jane Wanjiku", null,
     *                              items, "01");
     * r.throwIfFailed();
     * printReceiptQr(r.getQrCode().orElse(""));
     * }</pre>
     *
     * @param invoiceNo     unique invoice / receipt number in your system
     * @param patientName   patient or customer name
     * @param patientTin    customer KRA PIN — pass {@code null} for walk-in patients
     * @param items         non-empty list of line items
     * @param paymentModeCd payment type: {@code "01"}=Cash, {@code "02"}=Credit,
     *                      {@code "03"}=Mobile Money
     * @return result containing {@code rcptNo}, {@code qrCode}, and
     *         {@code intrlData} on success
     */
    public EtimsResult sendBill(
            String invoiceNo,
            String patientName,
            String patientTin,
            List<SalesTrnsItem> items,
            String paymentModeCd) {

        return service.sendPatientBill(invoiceNo, patientName, patientTin, items, paymentModeCd);
    }

    // -----------------------------------------------------------------
    // Credit note
    // -----------------------------------------------------------------

    /**
     * Sends a credit note ({@code rcptTyCd = "C"}) to KRA eTIMS.
     *
     * <p>Use this to reverse or partially reverse a previously submitted
     * invoice.  Items should have <strong>negative</strong> quantities or
     * amounts to represent the reversal.
     *
     * <pre>{@code
     * SalesTrnsItem refundItem = sdk.createItem("MED001", "Paracetamol 500mg", -2, 50.0, "V");
     * EtimsResult cn = sdk.sendCreditNote("CN-001", "INV-001",
     *                                     "Jane Wanjiku", null,
     *                                     List.of(refundItem), "01");
     * cn.throwIfFailed();
     * }</pre>
     *
     * @param creditNoteNo      unique credit note reference number in your system
     * @param originalInvoiceNo invoice number being reversed (must not be blank)
     * @param customerName      customer name
     * @param customerTin       customer KRA PIN — pass {@code null} if not available
     * @param items             items being reversed (non-empty)
     * @param paymentModeCd     payment type code of the original invoice
     * @return result with KRA receipt data on success
     */
    public EtimsResult sendCreditNote(
            String creditNoteNo,
            String originalInvoiceNo,
            String customerName,
            String customerTin,
            List<SalesTrnsItem> items,
            String paymentModeCd) {

        return service.sendCreditNote(creditNoteNo, originalInvoiceNo,
                customerName, customerTin, items, paymentModeCd);
    }

    // -----------------------------------------------------------------
    // Debit note
    // -----------------------------------------------------------------

    /**
     * Sends a debit note ({@code rcptTyCd = "D"}) to KRA eTIMS.
     *
     * <p>Use this to supplement a previously submitted invoice, for example
     * when additional charges were omitted from the original bill.
     *
     * <pre>{@code
     * SalesTrnsItem extraItem = sdk.createItem("SVC002", "Consultation Fee", 1, 500.0, "V");
     * EtimsResult dn = sdk.sendDebitNote("DN-001", "INV-001",
     *                                    "Jane Wanjiku", null,
     *                                    List.of(extraItem), "01");
     * dn.throwIfFailed();
     * }</pre>
     *
     * @param debitNoteNo       unique debit note reference number in your system
     * @param originalInvoiceNo invoice number being supplemented (must not be blank)
     * @param customerName      customer name
     * @param customerTin       customer KRA PIN — pass {@code null} if not available
     * @param items             additional line items (non-empty)
     * @param paymentModeCd     payment type code
     * @return result with KRA receipt data on success
     */
    public EtimsResult sendDebitNote(
            String debitNoteNo,
            String originalInvoiceNo,
            String customerName,
            String customerTin,
            List<SalesTrnsItem> items,
            String paymentModeCd) {

        return service.sendDebitNote(debitNoteNo, originalInvoiceNo,
                customerName, customerTin, items, paymentModeCd);
    }

    // -----------------------------------------------------------------
    // Item helper
    // -----------------------------------------------------------------

    /**
     * Convenience factory for creating a {@link SalesTrnsItem} with automatic
     * tax calculation (16 % VAT for code {@code "V"}, 0 % for {@code "E"}/
     * {@code "Z"}).
     *
     * @param itemCd    item code registered in the KRA item catalogue
     * @param itemName  human-readable item name
     * @param qty       quantity
     * @param unitPrice unit price (exclusive of tax for VAT items)
     * @param taxTyCd   tax type: {@code "V"} (VAT), {@code "E"} (Exempt),
     *                  {@code "Z"} (Zero-rated)
     * @return populated line item
     */
    public SalesTrnsItem createItem(String itemCd, String itemName, double qty,
                                    double unitPrice, String taxTyCd) {
        return service.createItem(itemCd, itemName, qty, unitPrice, taxTyCd);
    }

    // -----------------------------------------------------------------
    // Mode switching
    // -----------------------------------------------------------------

    /**
     * Switches to VSCU mode, routing requests to a local virtual device.
     *
     * @param vscuBaseUrl base URL of the VSCU device (e.g.
     *                    {@code "http://localhost:8088/"})
     */
    public void useVscu(String vscuBaseUrl) {
        service.getClient().switchToVscu(vscuBaseUrl);
    }

    /**
     * Switches back to OSCU mode (direct connection to the KRA cloud API).
     */
    public void useOscu() {
        service.getClient().switchToOscu();
    }

    // -----------------------------------------------------------------
    // Advanced access
    // -----------------------------------------------------------------

    /**
     * Provides direct access to the underlying {@link EtimsClient} for
     * advanced use-cases not covered by the higher-level API.
     *
     * @return low-level HTTP client
     */
    public EtimsClient getClient() {
        return service.getClient();
    }
}
