package ke.co.montella.etims.model;

import ke.co.montella.etims.EtimsSdk;
import ke.co.montella.etims.EtimsService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the KRA eTIMS {@code sendSalesTrns} endpoint.
 *
 * <p>This class is populated internally by
 * {@link EtimsService} and should not need to be
 * constructed manually in most cases.  Use the high-level methods on
 * {@link EtimsSdk} instead.
 *
 * <h3>Receipt type codes ({@code rcptTyCd})</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>Code</th><th>Description</th></tr>
 *   <tr><td>{@code S}</td><td>Normal sale</td></tr>
 *   <tr><td>{@code C}</td><td>Credit note (reversal)</td></tr>
 *   <tr><td>{@code D}</td><td>Debit note (supplement)</td></tr>
 * </table>
 *
 * <h3>Payment type codes ({@code pmtTyCd})</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>Code</th><th>Description</th></tr>
 *   <tr><td>{@code 01}</td><td>Cash</td></tr>
 *   <tr><td>{@code 02}</td><td>Credit / Account</td></tr>
 *   <tr><td>{@code 03}</td><td>Mobile Money (M-Pesa, etc.)</td></tr>
 *   <tr><td>{@code 04}</td><td>Bank Transfer</td></tr>
 *   <tr><td>{@code 05}</td><td>Other</td></tr>
 * </table>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendSalesTrnsRequest {

    /** Taxpayer TIN. */
    private String tin;

    /** Branch ID. */
    private String bhfId;

    /** Your internal invoice / receipt number (unique per TIN+BHF). */
    private String invcNo;

    /**
     * Original invoice number.
     * Required for credit notes ({@code rcptTyCd = "C"}) and debit notes
     * ({@code rcptTyCd = "D"}).  {@code null} for normal sales.
     */
    private String orgInvcNo;

    /** Customer KRA PIN — {@code null} for walk-in / individual customers. */
    private String custTin;

    /** Customer or patient name. Defaults to {@code "CASH CUSTOMER"} if blank. */
    private String custNm;

    /**
     * Sales type code.
     * Use {@code "N"} for normal sales (the only supported value at present).
     */
    private String salesTyCd = "N";

    /**
     * Receipt type code.
     *
     * @see SendSalesTrnsRequest class-level Javadoc for valid codes
     */
    private String rcptTyCd = "S";

    /**
     * Payment type code.
     *
     * @see SendSalesTrnsRequest class-level Javadoc for valid codes
     */
    private String pmtTyCd = "01";

    /**
     * Sales status code.
     * Use {@code "1"} for a confirmed sale.
     */
    private String salesSttsCd = "1";

    /** Confirmation date/time in {@code yyyyMMddHHmmss} format. */
    private String cfmDt;

    /** Line items. */
    private List<SalesTrnsItem> salesTrnsItems;

    /** Total number of distinct line items. */
    private int totItemCnt;

    /** Sum of all {@link SalesTrnsItem#getTaxAmt()} values. */
    private double totTaxAmt;

    /** Grand total including tax. Sum of all {@link SalesTrnsItem#getTotAmt()} values. */
    private double totAmt;
}
