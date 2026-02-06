package com.worktree.secure.app;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class RSAKeyGenerator {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);

        KeyPair pair = gen.generateKeyPair();

        String privateKey = Base64.getEncoder()
                .encodeToString(pair.getPrivate().getEncoded());

        String publicKey = Base64.getEncoder()
                .encodeToString(pair.getPublic().getEncoded());

        System.out.println("PRIVATE KEY:\n" + privateKey);
        System.out.println("\nPUBLIC KEY:\n" + publicKey);
    }
}
