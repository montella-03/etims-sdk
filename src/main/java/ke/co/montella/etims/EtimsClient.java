package ke.co.montella.etims;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.montella.etims.exception.EtimsException;
import ke.co.montella.etims.model.EtimsResponse;
import ke.co.montella.etims.model.InitOsdcRequest;
import ke.co.montella.etims.model.SendSalesTrnsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Low-level HTTP client for the KRA eTIMS REST API.
 *
 * <p>This class is an internal implementation detail of the SDK.  Application
 * code should use {@link EtimsSdk} instead of calling this class directly.
 *
 * <p>A new {@link RestClient} is built for every request so that base-URL
 * changes (e.g. switching from VSCU to OSCU via
 * {@link #switchToVscu}/{@link #switchToOscu}) take effect immediately.
 */
@Slf4j
public class EtimsClient {

    private static final String SUCCESS_CODE = "000";

    private final EtimsConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a client bound to the supplied configuration.
     *
     * @param config SDK configuration (TIN, device serial, mode, etc.)
     */
    public EtimsClient(EtimsConfig config) {
        this.config = config;
    }

    // -----------------------------------------------------------------
    // Device initialization
    // -----------------------------------------------------------------

    /**
     * Calls {@code selectInitOsdcInfo} to obtain a CMC Key from KRA.
     *
     * <p>On success the CMC Key is automatically stored in {@link EtimsConfig}
     * and will be sent as the {@code cmcKey} header on subsequent requests.
     *
     * @return raw API response
     * @throws EtimsException if the HTTP call fails or the response cannot be parsed
     */
    public EtimsResponse<Object> initializeDevice() {
        InitOsdcRequest request = new InitOsdcRequest(
                config.getTin(), config.getBhfId(), config.getDeviceSrlNo());

        log.info("Initializing eTIMS device: TIN={}, BHF={}, SN={}",
                config.getTin(), config.getBhfId(), config.getDeviceSrlNo());

        String responseBody = post("selectInitOsdcInfo", request, false);

        try {
            EtimsResponse<Object> response = objectMapper.readValue(responseBody, EtimsResponse.class);

            if (SUCCESS_CODE.equals(response.getResultCd())) {
                var dataNode = objectMapper.valueToTree(response.getData());
                String cmcKey = dataNode.path("info").path("cmcKey").asText(null);
                if (cmcKey != null && !cmcKey.isEmpty()) {
                    config.setCmcKey(cmcKey);
                    log.info("Device initialized successfully. CMC Key stored.");
                } else {
                    log.warn("Initialization succeeded (resultCd=000) but no CMC Key was returned.");
                }
            } else {
                log.warn("Initialization failed: resultCd={}, resultMsg={}",
                        response.getResultCd(), response.getResultMsg());
            }
            return response;
        } catch (Exception e) {
            throw new EtimsException("Failed to parse initialization response", e);
        }
    }

    // -----------------------------------------------------------------
    // Sales transaction
    // -----------------------------------------------------------------

    /**
     * Calls {@code sendSalesTrns} to submit a sales, credit note, or debit
     * note transaction to KRA.
     *
     * @param request fully-populated transaction request
     * @return raw API response
     * @throws EtimsException if the device is not initialized (OSCU mode), the
     *                        HTTP call fails, or the response cannot be parsed
     */
    public EtimsResponse<Object> sendSalesTransaction(SendSalesTrnsRequest request) {
        if (!config.isUseVscu()
                && (config.getCmcKey() == null || config.getCmcKey().isEmpty())) {
            throw new EtimsException(
                    "CMC Key is missing. Call EtimsSdk.initialize() before sending transactions.");
        }

        log.info("Sending sales transaction: invoiceNo={}, rcptTyCd={}, totAmt={}",
                request.getInvcNo(), request.getRcptTyCd(), request.getTotAmt());

        String responseBody = post("sendSalesTrns", request, true);

        try {
            EtimsResponse<Object> response = objectMapper.readValue(responseBody, EtimsResponse.class);
            log.debug("sendSalesTrns response: resultCd={}, resultMsg={}",
                    response.getResultCd(), response.getResultMsg());
            return response;
        } catch (Exception e) {
            throw new EtimsException("Failed to parse sendSalesTrns response", e);
        }
    }

    // -----------------------------------------------------------------
    // Mode switching
    // -----------------------------------------------------------------

    /**
     * Switches to VSCU mode, routing requests to a local virtual device.
     *
     * @param baseUrl optional override for the VSCU base URL; pass {@code null}
     *                to keep the current value
     */
    public void switchToVscu(String baseUrl) {
        config.setUseVscu(true);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            config.setVscuBaseUrl(baseUrl);
        }
        log.info("Switched to VSCU mode. Base URL: {}", config.getVscuBaseUrl());
    }

    /**
     * Switches to OSCU mode, routing requests directly to the KRA cloud API.
     */
    public void switchToOscu() {
        config.setUseVscu(false);
        log.info("Switched to OSCU mode. Base URL: {}", config.getBaseUrl());
    }

    /**
     * Returns {@code true} if a CMC Key has been received and stored in config.
     *
     * @return initialization state
     */
    public boolean isInitialized() {
        return config.getCmcKey() != null && !config.getCmcKey().isEmpty();
    }

    // -----------------------------------------------------------------
    // Internal HTTP helper
    // -----------------------------------------------------------------

    /**
     * Executes a POST request against the current base URL.
     *
     * <p>A fresh {@link RestClient} is constructed for every call so that
     * changes to {@link EtimsConfig#getBaseUrl()} (e.g. mode switching) are
     * always reflected.
     *
     * @param path           relative path (e.g. {@code "sendSalesTrns"})
     * @param body           request body (serialised to JSON by RestClient)
     * @param includeCmcKey  if {@code true} and in OSCU mode, adds the
     *                       {@code cmcKey} request header
     * @return raw response body as a String
     * @throws EtimsException on network or HTTP error
     */
    private String post(String path, Object body, boolean includeCmcKey) {
        RestClient client = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .build();

        try {
            return client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (config.isUseVscu()) {
                            String token = config.getVscuIntegrationToken();
                            if (token != null && !token.isEmpty()) {
                                headers.add("Authorization", "Bearer " + token);
                            }
                        } else if (includeCmcKey && config.getCmcKey() != null) {
                            headers.add("cmcKey", config.getCmcKey());
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new EtimsException(
                    "HTTP request to '" + config.getBaseUrl() + path + "' failed: " + e.getMessage(), e);
        }
    }
}
