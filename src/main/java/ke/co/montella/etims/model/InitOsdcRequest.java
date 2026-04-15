package ke.co.montella.etims.model;

import ke.co.montella.etims.EtimsClient;
import ke.co.montella.etims.EtimsSdk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for the KRA eTIMS {@code selectInitOsdcInfo} endpoint.
 *
 * <p>This class is populated internally by {@link EtimsClient}
 * during device initialization.  Application code should call
 * {@link EtimsSdk#initialize()} instead of constructing
 * this class directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitOsdcRequest {

    /** KRA Taxpayer Identification Number. */
    private String tin;

    /** Branch ID (e.g. {@code "00"}). */
    private String bhfId;

    /** Hardware device serial number registered with KRA. */
    private String dvcSrlNo;
}
