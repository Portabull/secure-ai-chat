package com.worktree.secure;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class RSALicenseGenerator {


    private static final String PRIVATE_KEY = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDvyDzvN+5i6ZCMp7Aa9i4qLrIUg69B/XBvKQWcp66L6bP0hAtToDNkv4lpdUISLHEeinhxvfMkbGdfGK73JxigqjSaA+q4ICfzNtYu+UnxxDpPHT/O/RADtWXshwpfPDl+Od2jtQ9re6ZQrSiMqM9Lq0nRWbmx7ioI+e+9Qca1CfZQKCRp1IhBfVZxs/iK7eyONeyutuOis5v61SCNrPqVvhXIvrGfjVbyiNg88yAHFT1aC75E9gzwuv6gQI9QNKMK78U2MW6MnPZIlxSACYyouxy6ML5CrSWChsH7tQP01oobr5x66ob1WQO65tQZMKCF6A3A7f4T7r6gyYc0em2BAgMBAAECggEAAwJh1WS/zqj8we8m1CfLy4a7gGtoqyzZmOOMaPtnwuTDd+miVMHZc0W+HT81/ZFmOApn2ZGXOBlOMNDX/U0Vz4pjT52F+BqbNb+172/8gBbE3pJQ5zGg3lML/Oxy8mAInq9uOyw+DczG+RM7Q4yo0VvMeN6l6HhDOcqdOiRVsJbxsmbCxOOSgcd/wlvjg9YC8F5H1fALsJaSOoUYk4C4xMT1juiZwAbDQb4sr1lNjjjHW4LGEo+a2Y4bInBKgtpwRml2iO0S7Ihv8T/Se0Ae0HFydqR6YYWki7uxnoaajPx7jXHs4uPQTTDdCAlOx5Jz3vd3txD/7KMTjuSr9HIyYQKBgQD9+5k7FI4KooH3Hh3RLcoEu3mvz9u+uVU9pZQ+R8+H/f7Jt49Gy5FQI1qXR2K81qNsQSgXSFXh/xXGhWta26naY092miIOT6SaKlOrVZmZkFnZ3yPFIbNja8d9lNzoTNUtrP+LWMaFhAfrJU1OKQ63PyZPuqqiiO9dRczShDNrmQKBgQDxr8Q8s1Ix4PbY6Y+oJlpJv16lEe/1P9BXdQ4e72bHQSZfj6hJfDWE3L3CvdrOGDgOUpWowAh2hubVRS97X9wifu85erkNQf5H/Bl581CmMplQhcmUwHte/ty51C2Fc7SgdlH3qy6vHdn5pYX400UZi2/T9OwOT7R2/xkTlP8CKQKBgQDwo8fFEG6caOiWyPQbUzULUs2y5FAh3tuCszVH5SYehWs4mDCqNQoTUcvtdXd50Cu6z+k6VhD5NYTeOV9EEMhX4KxThSorQQ6F+wCXX11PAFQBd8geaRXcZyv2MmsztkhtJHJjBtPhgP03sHoUShZlTPHxrL0rjm6sgwvTkepeUQKBgQDYB9hMuaz60K9U7FBwaPfDe79JpWLrwQKlaggZNryT1nRKbspqBRCBlh44eCOgnJ1ekAseL6hg6exjLdhnWyQV4YhLxJqscxSrm3faPdcU9517EFtDD1o3a3A42qNl6irGU3yo+OZduT0hnQbHh3D4iF6JAyANGsmgpBzyF/hpIQKBgQDdFMO9kJ/4Tsz1/jTo4KjLWTgqF2uCCOmpbDkRlsvkEyBPLo3DdQRJ6pPLOCxV99VK+RhfENjor+oA4hU2Y5rb7nJj5WvtMR0DrpyORDKR0TEi4ZAa3mW1vWDjO7AgOyT3LssmxqHBl6oelVdYrVsfie0HBiI8nOpSVo3VSqK4qg==";

    public String generate(String customer, int validDays) throws Exception {

        // ✅ UTC time
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(validDays * 24L * 60 * 60);

        String payload =
                customer + "|" +
                        now.toEpochMilli() + "|" +
                        expires.toEpochMilli();

        String payloadBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signature = sign(payloadBase64);

        return "LIC-" +
                payloadBase64 +
                "." +
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(signature);
    }

    private static byte[] sign(String data) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(loadPrivateKey());
        sig.update(data.getBytes(StandardCharsets.UTF_8));

        return sig.sign();
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(PRIVATE_KEY);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static void main(String[] args) throws Exception {
        RSALicenseGenerator r  =new RSALicenseGenerator();
        System.out.println(r.generate("W",10));
    }

}
