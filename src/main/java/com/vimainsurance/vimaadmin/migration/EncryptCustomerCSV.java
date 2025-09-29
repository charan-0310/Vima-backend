// package com.vimainsurance.vimaadmin.migration;

// import java.io.BufferedReader;
// import java.io.BufferedWriter;
// import java.io.FileReader;
// import java.io.FileWriter;

// import com.vimainsurance.vimaadmin.util.AESEncryptionUtil;

// public class EncryptCustomerCSV {
//     public static void main(String[] args) throws Exception {
//         // Load the AES key from properties file
//         System.out.println("Loading AES key from properties...");
//         AESEncryptionUtil.loadKeyFromProperties("src/main/resources/application.properties");
//         System.out.println("Key loaded. Starting encryption...");

//         // Update these paths as needed
//         String inputCsv = "E:\\customertable.csv";
//         String outputCsv = "E:\\customers_encrypted.csv";

//         try (
//             BufferedReader reader = new BufferedReader(new FileReader(inputCsv));
//             BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsv))
//         ) {
//             String header = reader.readLine();
//             if (header != null) {
//                 // Write header as is without encryption
//                 writer.write(header + "\n");
//             }
            
//             String line;
//             int rowNum = 0;  // Start from 0 to show actual row numbers
//             while ((line = reader.readLine()) != null) {
//                 rowNum++;  // Increment at the start so first data row is row 1
//                 // Assumes CSV columns with id at index 0, full_name at index 2, and phone at index 5
//                 String[] parts = line.split(",", -1); // -1 to keep empty fields
//                 if (parts.length < 6) {  // Changed to 6 since we need index 5 for phone
//                     System.err.println("Skipping malformed line " + rowNum + ": " + line);
//                     continue;
//                 }
//                 String id = parts[0];
//                 String fullName = parts[2];
//                 String phone = parts[5];

//                 try {
//                     // Encrypt only if not empty
//                     String encryptedFullName = fullName.isEmpty() ? "" : AESEncryptionUtil.encrypt(fullName);
//                     String encryptedPhone = phone.isEmpty() ? "" : AESEncryptionUtil.encrypt(phone);

//                     writer.write(id + "," + encryptedFullName + "," + encryptedPhone + "\n");
//                     System.out.println("Row " + rowNum + " encrypted: " + id);
//                 } catch (Exception e) {
//                     System.err.println("Error encrypting row " + rowNum + " (id=" + id + "): " + e.getMessage());
//                 }
//             }
//         }
//         System.out.println("Encryption complete. Output: " + outputCsv);
//     }
// } 