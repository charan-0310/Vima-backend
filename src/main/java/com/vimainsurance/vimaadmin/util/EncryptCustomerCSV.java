package com.vimainsurance.vimaadmin.util;

import com.vimainsurance.vimaadmin.util.AESEncryptionUtil;

import java.io.*;

public class EncryptCustomerCSV {
    public static void main(String[] args) throws Exception {
        // Update these paths as needed
        String inputCsv = "E:\\customertable.csv";
        String outputCsv = "E:\\customers_encrypted.csv";

        try (
            BufferedReader reader = new BufferedReader(new FileReader(inputCsv));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsv))
        ) {
            String header = reader.readLine();
            writer.write(header + "\n");
            String line;
            while ((line = reader.readLine()) != null) {
                // Assumes CSV columns: id,full_name,phone_number
                String[] parts = line.split(",", -1); // -1 to keep empty fields
                if (parts.length < 3) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                String id = parts[0];
                String fullName = parts[2];
                String phone = parts[5];

                // Encrypt only if not empty
                String encryptedFullName = fullName.isEmpty() ? "" : AESEncryptionUtil.encrypt(fullName);
                String encryptedPhone = phone.isEmpty() ? "" : AESEncryptionUtil.encrypt(phone);

                writer.write(id + "," + encryptedFullName + "," + encryptedPhone + "\n");
            }
        }
        System.out.println("Encryption complete. Output: " + outputCsv);
    }
}