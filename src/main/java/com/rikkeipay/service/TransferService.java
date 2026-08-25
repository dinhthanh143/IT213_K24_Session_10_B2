package com.rikkeipay.service;

import com.rikkeipay.config.LangfuseProperties;
import com.rikkeipay.util.PiiMaskingUtils;
import io.langfuse.client.LangfuseClient;
import io.langfuse.client.model.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service handling financial transfer operations with secure Langfuse Tracing.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final LangfuseClient langfuseClient;
    private final LangfuseProperties langfuseProperties;

    // Constructor Injection (Best Practice)
    public TransferService(LangfuseClient langfuseClient, LangfuseProperties langfuseProperties) {
        this.langfuseClient = langfuseClient;
        this.langfuseProperties = langfuseProperties;
    }

    /**
     * Process transfer with full trace context, session tracking, and PII masking.
     *
     * @param user Sender username or customer ID
     * @param toAccount Destination bank account number
     * @param amount Transaction amount
     * @param sessionId Session ID of the chat/transaction session
     */
    public boolean processTransfer(String user, String toAccount, double amount, String sessionId) {
        String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : "sess-" + UUID.randomUUID();
        String maskedUser = PiiMaskingUtils.maskUsername(user);
        String maskedAccount = PiiMaskingUtils.maskAccountNumber(toAccount);
        String formattedAmount = PiiMaskingUtils.formatCurrency(amount);

        // 1. Prepare Telemetry Metadata (Without exposing cleartext PII)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("service", "TransferService");
        metadata.put("channel", "RikkeiPay-Assistant-AI");
        metadata.put("environment", langfuseProperties.getEnvironment());
        metadata.put("currency", "VND");

        // 2. Initialize Trace with complete contextual identifiers
        Trace trace = langfuseClient.trace(new Trace()
                .name("bank-transfer")
                .userId(user) // Unique identifier for user analytics in Langfuse
                .sessionId(effectiveSessionId) // Group traces by chat session
                .release(langfuseProperties.getRelease())
                .tags(List.of("financial-transaction", "transfer", "rikkeipay-assistant"))
                .metadata(metadata)
                .input(Map.of(
                        "sender", maskedUser,
                        "destination_account", maskedAccount,
                        "amount", formattedAmount,
                        "timestamp", System.currentTimeMillis()
                )));

        log.info("Starting transfer execution for user: [{}], session: [{}]", maskedUser, effectiveSessionId);

        try {
            // 3. Execute core banking transfer logic
            if (amount <= 0) {
                throw new IllegalArgumentException("Số tiền chuyển khoản phải lớn hơn 0");
            }
            if (toAccount == null || toAccount.trim().isEmpty()) {
                throw new IllegalArgumentException("Số tài khoản nhận không hợp lệ");
            }

            // Simulate banking gateway execution...
            executeCoreBankingTransfer(user, toAccount, amount);

            // 4. Update Trace on Success with masked telemetry
            Map<String, Object> outputData = Map.of(
                    "status", "SUCCESS",
                    "transaction_id", "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    "summary", String.format("Thành công chuyển khoản %s sang tài khoản %s", formattedAmount, maskedAccount)
            );
            trace.output(outputData);
            log.info("Transfer completed successfully for session: [{}]", effectiveSessionId);
            return true;

        } catch (Exception ex) {
            // 5. Update Trace on Failure with detailed error level
            log.error("Transfer failed for session [{}]: {}", effectiveSessionId, ex.getMessage(), ex);
            
            Map<String, Object> errorOutput = Map.of(
                    "status", "FAILED",
                    "error_code", "TXN_EXECUTION_ERROR",
                    "error_message", ex.getMessage()
            );
            trace.output(errorOutput);
            trace.level("ERROR");
            return false;
        }
    }

    /**
     * Overloaded method for backward compatibility.
     */
    public boolean processTransfer(String user, String toAccount, double amount) {
        return processTransfer(user, toAccount, amount, null);
    }

    private void executeCoreBankingTransfer(String user, String toAccount, double amount) {
        // Mock business core banking call
        log.debug("Calling Core Banking System for account transfer processing...");
    }
}
