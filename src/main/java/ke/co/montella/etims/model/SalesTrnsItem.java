package ke.co.montella.etims.model;

import ke.co.montella.etims.EtimsSdk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single line item in a KRA eTIMS sales transaction.
 *
 * <p>Create instances via {@link EtimsSdk#createItem}
 * which handles tax calculation automatically, or build them manually for
 * full control.
 *
 * <h3>Tax type codes ({@code taxTyCd})</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>Code</th><th>Description</th><th>Rate</th></tr>
 *   <tr><td>{@code V}</td><td>Standard VAT</td><td>16%</td></tr>
 *   <tr><td>{@code E}</td><td>VAT Exempt</td><td>0%</td></tr>
 *   <tr><td>{@code Z}</td><td>Zero-rated</td><td>0%</td></tr>
 * </table>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrnsItem {

    /** Item code registered in the KRA item catalogue. */
    private String itemCd;

    /** Human-readable item name (drug name, service name, etc.). */
    private String itemNm;

    /** Quantity dispensed or provided. */
    private double qty;

    /** Unit selling price (exclusive of tax for VAT items). */
    private double prc;

    /**
     * Supply amount: {@code qty × prc}.
     * For VAT items this is the tax-exclusive subtotal.
     */
    private double splyAmt;

    /** Discount rate as a percentage (e.g. {@code 10.0} for 10 %). */
    private double dcRt;

    /** Discount amount in KES ({@code splyAmt × dcRt / 100}). */
    private double dcAmt;

    /**
     * Tax type code.
     *
     * @see SalesTrnsItem class-level Javadoc for valid codes
     */
    private String taxTyCd;

    /** Calculated tax amount in KES. */
    private double taxAmt;

    /** Total line amount including tax ({@code splyAmt - dcAmt + taxAmt}). */
    private double totAmt;
}
