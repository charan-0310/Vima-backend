// package com.vimainsurance.vimaadmin.util;
// import java.io.File;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.security.KeyPair;
// import java.security.KeyPairGenerator;
// import java.security.PrivateKey;
// import java.security.PublicKey;
// import java.security.SecureRandom;
// import java.util.Base64;

// public class RSAKeyPairGenerator {
    
//     public static void main(String[] args) throws Exception {
//         // Generate RSA key pair
//         KeyPair keyPair = generateRSAKeyPair(2048);
        
//         // Convert to PEM format
//         String privateKeyPEM = convertToPEM(keyPair.getPrivate(), "RSA PRIVATE KEY");
//         String publicKeyPEM = convertToPEM(keyPair.getPublic(), "PUBLIC KEY");
        
//         // Print to console
//         System.out.println("Private Key (PEM):");
//         System.out.println(privateKeyPEM);
//         System.out.println("\nPublic Key (PEM):");
//         System.out.println(publicKeyPEM);
        
//         // Save to files
//         saveToFile("private_key.pem", privateKeyPEM);
//         saveToFile("public_key.pem", publicKeyPEM);
        
//         System.out.println("\nKeys saved to files: private_key.pem, public_key.pem");
//     }
    
//     public static KeyPair generateRSAKeyPair(int keySize) throws Exception {
//         KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//         keyPairGenerator.initialize(keySize, new SecureRandom());
//         return keyPairGenerator.generateKeyPair();
//     }
    
//     private static String convertToPEM(java.security.Key key, String header) {
//         String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
//         StringBuilder pem = new StringBuilder();
//         pem.append("-----BEGIN ").append(header).append("-----\n");
        
//         // Split into 64-character lines
//         for (int i = 0; i < base64Key.length(); i += 64) {
//             int end = Math.min(i + 64, base64Key.length());
//             pem.append(base64Key.substring(i, end)).append("\n");
//         }
        
//         pem.append("-----END ").append(header).append("-----\n");
//         return pem.toString();
//     }
    
//     private static void saveToFile(String filename, String content) throws IOException {
//         try (FileWriter writer = new FileWriter(filename)) {
//             writer.write(content);
//         }
//     }
// }