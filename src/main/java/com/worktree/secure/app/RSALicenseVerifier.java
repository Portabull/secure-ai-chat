package com.worktree.secure.app;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class RSALicenseVerifier {

    // ✅ SAFE to embed in app
    private static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA78g87zfuYumQjKewGvYuKi6yFIOvQf1wbykFnKeui+mz9IQLU6AzZL+JaXVCEixxHop4cb3zJGxnXxiu9ycYoKo0mgPquCAn8zbWLvlJ8cQ6Tx0/zv0QA7Vl7IcKXzw5fjndo7UPa3umUK0ojKjPS6tJ0Vm5se4qCPnvvUHGtQn2UCgkadSIQX1WcbP4iu3sjjXsrrbjorOb+tUgjaz6lb4VyL6xn41W8ojYPPMgBxU9Wgu+RPYM8Lr+oECPUDSjCu/FNjFujJz2SJcUgAmMqLscujC+Qq0lgobB+7UD9NaKG6+ceuqG9VkDuubUGTCghegNwO3+E+6+oMmHNHptgQIDAQAB";

    public static LicenseStatus verify(String license) {

        LicenseStatus res = new LicenseStatus();

        try {
            if (license == null || !license.startsWith("LIC-")) {
                res.valid = false;
                res.message = "Invalid license format";
                return res;
            }

            String raw = license.substring(4);
            String[] parts = raw.split("\\.");

            if (parts.length != 2) {
                res.valid = false;
                res.message = "Corrupted license";
                return res;
            }

            String payloadBase64 = parts[0];
            byte[] signature = Base64.getUrlDecoder().decode(parts[1]);

            // 🔐 Verify signature
            if (!verifySignature(payloadBase64, signature)) {
                res.valid = false;
                res.message = "Invalid license signature";
                return res;
            }

            // 📦 Decode payload
            String payload = new String(
                    Base64.getUrlDecoder().decode(payloadBase64),
                    StandardCharsets.UTF_8
            );

            String[] fields = payload.split("\\|");
            if (fields.length < 3) {
                res.valid = false;
                res.message = "Invalid license payload";
                return res;
            }

            long expiresAtUtc = Long.parseLong(fields[2]);

            // ⏱️ UTC comparison (IMPORTANT)
            long nowUtc = Instant.now().toEpochMilli();

            if (nowUtc > expiresAtUtc) {
                res.valid = false;
                res.message = "License expired";
                res.expiresAt = expiresAtUtc;
                return res;
            }

            res.valid = true;
            res.message = "License valid";
            res.expiresAt = expiresAtUtc;
            return res;

        } catch (Exception e) {
            res.valid = false;
            res.message = "License verification failed";
            return res;
        }
    }

    private static boolean verifySignature(String data, byte[] sigBytes)
            throws Exception {

        PublicKey publicKey = loadPublicKey();

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data.getBytes(StandardCharsets.UTF_8));

        return sig.verify(sigBytes);
    }

    private static PublicKey loadPublicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(PUBLIC_KEY_BASE64);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(bytes));
    }
}
