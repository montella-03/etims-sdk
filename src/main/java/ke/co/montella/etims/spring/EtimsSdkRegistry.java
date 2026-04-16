package ke.co.montella.etims.spring;

import ke.co.montella.etims.EtimsSdk;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Holds one {@link EtimsSdk} instance per named tenant.
 *
 * <p>This bean is registered automatically by {@link EtimsAutoConfiguration}
 * when the {@code etims.tenants} map is non-empty (multi-tenant mode).
 * Inject it wherever you need to dispatch eTIMS calls to a specific facility
 * or branch:
 *
 * <pre>{@code
 * @Service
 * public class BillingService {
 *
 *     private final EtimsSdkRegistry etimsRegistry;
 *
 *     public BillingService(EtimsSdkRegistry etimsRegistry) {
 *         this.etimsRegistry = etimsRegistry;
 *     }
 *
 *     public void bill(String facilityId, String invoiceNo, ...) {
 *         EtimsSdk sdk = etimsRegistry.getSdk(facilityId);
 *         EtimsResult result = sdk.sendBill(invoiceNo, ...);
 *     }
 * }
 * }</pre>
 *
 * <h3>application.yml</h3>
 * <pre>
 * etims:
 *   production: true
 *   default-tenant: nairobi
 *   tenants:
 *     kenyattaHospital:
 *       tin: P051234567X
 *       bhf-id: "00"
 *       device-srl-no: SN-001
 *     afyaBridgeHospital:
 *       tin: P051234567X
 *       bhf-id: "01"
 *       device-srl-no: SN-002
 * </pre>
 *
 * @see EtimsProperties
 * @see EtimsAutoConfiguration
 */
public class EtimsSdkRegistry {

    private final Map<String, EtimsSdk> sdks;
    private final String defaultTenantId;

    /**
     * Creates a registry from a pre-built map of SDKs.
     *
     * @param sdks            tenant-id → SDK map (must not be {@code null})
     * @param defaultTenantId key used by {@link #getDefaultSdk()} — may be
     *                        {@code null} if a default is not required
     */
    public EtimsSdkRegistry(Map<String, EtimsSdk> sdks, String defaultTenantId) {
        this.sdks = Collections.unmodifiableMap(sdks);
        this.defaultTenantId = defaultTenantId;
    }

    /**
     * Returns the {@link EtimsSdk} for the given tenant.
     *
     * @param tenantId the key as configured under {@code etims.tenants}
     * @return the SDK for that tenant
     * @throws IllegalArgumentException if no tenant with that key is registered
     */
    public EtimsSdk getSdk(String tenantId) {
        EtimsSdk sdk = sdks.get(tenantId);
        if (sdk == null) {
            throw new IllegalArgumentException(
                    "No eTIMS tenant configured with id '" + tenantId + "'. "
                    + "Configured tenants: " + sdks.keySet());
        }
        return sdk;
    }

    /**
     * Returns the SDK for the tenant configured as {@code etims.default-tenant}.
     *
     * @return the default tenant's SDK
     * @throws IllegalStateException if {@code etims.default-tenant} is not set
     */
    public EtimsSdk getDefaultSdk() {
        if (defaultTenantId == null || defaultTenantId.isBlank()) {
            throw new IllegalStateException(
                    "etims.default-tenant is not configured. "
                    + "Set it to one of: " + sdks.keySet());
        }
        return getSdk(defaultTenantId);
    }

    /**
     * Returns an unmodifiable view of all registered tenant IDs.
     *
     * @return tenant key set
     */
    public Set<String> getTenantIds() {
        return sdks.keySet();
    }

    /**
     * Returns {@code true} if a tenant with the given ID is registered.
     *
     * @param tenantId tenant key to check
     * @return {@code true} if present
     */
    public boolean hasTenant(String tenantId) {
        return sdks.containsKey(tenantId);
    }

    /**
     * Returns the number of tenants registered in this registry.
     *
     * @return tenant count
     */
    public int size() {
        return sdks.size();
    }
}
