package ke.co.montella.etims;

import lombok.Data;

/**
 * Configuration for the KRA eTIMS SDK.
 *
 * <p>Create and populate an instance of this class, then pass it to
 * {@link EtimsSdk}.  In a Spring Boot application you should use
 * {@code ke.co.hospital.etims.spring.EtimsProperties} with
 * {@code @ConfigurationProperties} instead — the auto-configuration wires
 * this class automatically from your {@code application.properties}.
 *
 * <h3>Plain Java example</h3>
 * <pre>{@code
 * EtimsConfig config = new EtimsConfig();
 * config.setTin("P051234567X");
 * config.setBhfId("00");
 * config.setDeviceSrlNo("SN-123456");
 * config.setProduction(false);   // use sandbox
 *
 * EtimsSdk sdk = new EtimsSdk(config);
 * sdk.initialize();
 * }</pre>
 */
@Data
public class EtimsConfig {

    // ---------------------------------------------------------------
    // Required fields
    // ---------------------------------------------------------------

    /** KRA Taxpayer Identification Number (TIN / KRA PIN). */
    private String tin;

    /**
     * Branch ID assigned by KRA.  Defaults to {@code "00"} for single-branch
     * businesses.
     */
    private String bhfId = "00";

    /** Hardware device serial number registered with KRA. */
    private String deviceSrlNo;

    // ---------------------------------------------------------------
    // OSCU (Online SCU – direct connection to KRA)
    // ---------------------------------------------------------------

    /**
     * When {@code false} (default) requests are sent to the KRA sandbox
     * ({@code etims-api-sbx.kra.go.ke}).  Set to {@code true} for live
     * production traffic.
     */
    private boolean production = false;

    // ---------------------------------------------------------------
    // VSCU (Virtual SCU – local device)
    // ---------------------------------------------------------------

    /**
     * Set to {@code true} to route all requests to a local VSCU device
     * instead of the KRA cloud endpoint.
     */
    private boolean useVscu = false;

    /**
     * Base URL of the local VSCU device.  Only used when
     * {@link #isUseVscu()} is {@code true}.
     */
    private String vscuBaseUrl = "http://localhost:8088/";

    // ---------------------------------------------------------------
    // Runtime state (populated after initialization)
    // ---------------------------------------------------------------

    /**
     * CMC Key received from KRA after a successful
     * {@code selectInitOsdcInfo} call.  This value is stored here and
     * included as the {@code cmcKey} HTTP header on subsequent requests.
     *
     * <p>Do not set this manually; it is populated by
     * {@link EtimsSdk#initialize()}.
     */
    private String cmcKey;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Returns the effective base URL for API calls, taking the current
     * mode ({@link #isUseVscu()}) and environment ({@link #isProduction()})
     * into account.
     *
     * @return base URL ending with {@code /}
     */
    public String getBaseUrl() {
        if (useVscu) {
            return vscuBaseUrl;
        }
        return production
                ? "https://etims-api.kra.go.ke/etims-api/"
                : "https://etims-api-sbx.kra.go.ke/etims-api/";
    }
}
