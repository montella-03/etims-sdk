# KRA eTIMS Java SDK

A Java SDK for integrating Hospital Management Systems (and any Java application) with the Kenya Revenue Authority (KRA) **eTIMS OSCU/VSCU** API.

---

## Features

- **Simple API** — one class (`EtimsSdk`) covers 90 % of use-cases
- **Fluent response handling** — `EtimsResult` exposes `getQrCode()`, `getRcptNo()`, `getIntrlData()` and `throwIfFailed()`
- **Credit & Debit Notes** — `sendCreditNote()` and `sendDebitNote()` out of the box
- **Spring Boot ready** — zero-config auto-configuration via `@ConfigurationProperties`
- **SLF4J logging** — works with Logback, Log4j 2, or any SLF4J-compatible backend
- **Sandbox / Production** — toggle with a single property
- **VSCU support** — route requests to a local virtual device for development

---

## Requirements

| Dependency  | Version |
|-------------|---------|
| Java        | 17+     |
| Spring Web  | 6.x     |
| Jackson     | 2.x     |
| SLF4J API   | 2.x     |

Spring Boot auto-configuration requires Spring Boot **3.x**.

---

## Installation

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.montella-03</groupId>
    <artifactId>etims-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

> The SDK ships the SLF4J API but **not** a logging backend.  Add your preferred
> backend (Logback, Log4j 2, etc.) to your project or Spring Boot will provide one
> automatically.

---

## Quick Start — Plain Java

```java
// 1. Configure
EtimsConfig config = new EtimsConfig();
config.setTin("P051234567X");         // Your KRA PIN
config.setBhfId("00");                 // Branch ID
config.setDeviceSrlNo("SN-123456");    // Device serial
config.setProduction(false);           // false = sandbox

// 2. Create SDK and initialize (once at startup)
EtimsSdk sdk = new EtimsSdk(config);
sdk.initialize();

// 3. Build line items
SalesTrnsItem paracetamol = sdk.createItem("MED001", "Paracetamol 500mg", 2, 50.0, "V");
SalesTrnsItem consultation = sdk.createItem("SVC001", "Consultation Fee",  1, 500.0, "E");

// 4. Send the bill
EtimsResult result = sdk.sendBill(
        "INV-20240001",       // unique invoice number
        "Jane Wanjiku",       // patient name
        null,                 // patient KRA PIN (null for individuals)
        List.of(paracetamol, consultation),
        "01"                  // payment type: 01=Cash
);

// 5. Use the result
if (result.isSuccess()) {
    System.out.println("KRA receipt no : " + result.getRcptNo().orElse("N/A"));
    System.out.println("QR code        : " + result.getQrCode().orElse(""));
    System.out.println("Signed data    : " + result.getIntrlData().orElse(""));
} else {
    System.err.println("Submission failed: " + result.getResultMsg());
}
```

### Fail-fast style

```java
EtimsResult result = sdk.sendBill(...);
result.throwIfFailed(); // throws EtimsException if not successful

String qr = result.getQrCode().orElseThrow();
```

---

## Quick Start — Spring Boot

### 1. Add properties

```properties
# application.properties
etims.tin=P051234567X
etims.bhf-id=00
etims.device-srl-no=SN-123456
etims.production=false
etims.auto-initialize=true
```

### 2. Inject and use

```java
@Service
public class BillingService {

    private final EtimsSdk etimsSdk;

    public BillingService(EtimsSdk etimsSdk) {
        this.etimsSdk = etimsSdk;
    }

    public String processInvoice(String invoiceNo, String patientName,
                                 List<SalesTrnsItem> items) {
        EtimsResult result = etimsSdk.sendBill(invoiceNo, patientName, null, items, "01");
        result.throwIfFailed();
        return result.getRcptNo().orElseThrow();
    }
}
```

No `@Bean`, no `@Import` — the SDK registers itself automatically via Spring Boot's auto-configuration mechanism.

---

## Credit Notes and Debit Notes

### Credit Note (reversal)

Use a credit note to reverse a previously issued invoice.  Pass **negative quantities** for items being returned/refunded.

```java
SalesTrnsItem refund = sdk.createItem("MED001", "Paracetamol 500mg", -2, 50.0, "V");

EtimsResult cn = sdk.sendCreditNote(
        "CN-20240001",    // credit note number
        "INV-20240001",   // original invoice being reversed
        "Jane Wanjiku",
        null,
        List.of(refund),
        "01"
);
cn.throwIfFailed();
```

### Debit Note (supplement)

Use a debit note to add charges omitted from a previous invoice.

```java
SalesTrnsItem extra = sdk.createItem("SVC002", "X-Ray Fee", 1, 1500.0, "E");

EtimsResult dn = sdk.sendDebitNote(
        "DN-20240001",    // debit note number
        "INV-20240001",   // original invoice being supplemented
        "Jane Wanjiku",
        null,
        List.of(extra),
        "01"
);
dn.throwIfFailed();
```

---

## Tax Type Codes

| Code | Description | Rate |
|------|-------------|------|
| `V`  | Standard VAT | 16 % |
| `E`  | VAT Exempt   | 0 %  |
| `Z`  | Zero-rated   | 0 %  |

`EtimsSdk.createItem()` automatically calculates `taxAmt` and `totAmt` based on the code.

---

## Payment Type Codes

| Code | Description |
|------|-------------|
| `01` | Cash |
| `02` | Credit / Account |
| `03` | Mobile Money (M-Pesa, etc.) |
| `04` | Bank Transfer |
| `05` | Other |

---

## VSCU Mode (Local Virtual Device)

```java
// Switch to local VSCU for development / testing
sdk.useVscu("http://localhost:8088/");

// Switch back to KRA cloud (OSCU)
sdk.useOscu();
```

In Spring Boot:

```properties
etims.use-vscu=true
etims.vscu-base-url=http://localhost:8088/
```

---

## Exception Handling

All SDK errors are wrapped in `EtimsException` (unchecked):

```java
try {
    EtimsResult result = sdk.sendBill(...);
    result.throwIfFailed();
} catch (EtimsException ex) {
    // ex.getResultCd() — KRA result code (e.g. "101", "201"), or null for network errors
    // ex.getMessage()  — human-readable description
    logger.error("eTIMS error [{}]: {}", ex.getResultCd(), ex.getMessage());
}
```

---

## Configuration Reference

### `EtimsConfig` (Plain Java)

| Property | Type | Default | Description |
|---|---|---|---|
| `tin` | String | — | **Required.** KRA Taxpayer TIN |
| `bhfId` | String | `"00"` | Branch ID |
| `deviceSrlNo` | String | — | **Required.** Device serial number |
| `production` | boolean | `false` | `true` = live KRA endpoint |
| `useVscu` | boolean | `false` | `true` = local VSCU device |
| `vscuBaseUrl` | String | `http://localhost:8088/` | VSCU base URL |

### `application.properties` (Spring Boot)

| Key | Type | Default | Description |
|---|---|---|---|
| `etims.tin` | String | — | **Required.** KRA Taxpayer TIN |
| `etims.bhf-id` | String | `"00"` | Branch ID |
| `etims.device-srl-no` | String | — | **Required.** Device serial number |
| `etims.production` | boolean | `false` | `true` = live KRA endpoint |
| `etims.use-vscu` | boolean | `false` | `true` = local VSCU device |
| `etims.vscu-base-url` | String | `http://localhost:8088/` | VSCU base URL |
| `etims.auto-initialize` | boolean | `true` | Auto-init device on startup |

---

## KRA API Endpoints

| Mode | Environment | Base URL |
|------|-------------|----------|
| OSCU | Sandbox | `https://etims-api-sbx.kra.go.ke/etims-api/` |
| OSCU | Production | `https://etims-api.kra.go.ke/etims-api/` |
| VSCU | Local | configurable (default `http://localhost:8088/`) |

---

## Project Structure

```
src/main/java/ke/co/montella/etims/
├── EtimsSdk.java                  ← public API entry point
├── EtimsConfig.java               ← configuration bean
├── EtimsClient.java               ← HTTP layer
├── EtimsService.java              ← business logic
├── exception/
│   └── EtimsException.java        ← custom runtime exception
├── model/
│   ├── EtimsResult.java           ← response wrapper (qrCode, rcptNo, …)
│   ├── EtimsResponse.java         ← raw JSON envelope
│   ├── SalesTrnsItem.java         ← line item DTO
│   ├── SendSalesTrnsRequest.java  ← transaction request DTO
│   └── InitOsdcRequest.java       ← initialization request DTO
└── spring/
    ├── EtimsProperties.java       ← @ConfigurationProperties
    └── EtimsAutoConfiguration.java← Spring Boot auto-configuration
```

---

## License

MIT — see `LICENSE` for details.
