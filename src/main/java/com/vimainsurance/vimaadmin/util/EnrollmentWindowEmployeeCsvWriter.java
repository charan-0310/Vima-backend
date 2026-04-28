package com.vimainsurance.vimaadmin.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RFC 4180-style CSV lines for enrollment window employee export (UTF-8 BOM for Excel).
 */
public final class EnrollmentWindowEmployeeCsvWriter {

    public static final List<String> HEADERS = List.of(
            "employeeId",
            "relationship",
            "employeeName",
            "gender",
            "dateOfBirth",
            "sum_insured",
            "topup_sum_insured",
            "super_topup_sum_insured",
            "email",
            "mobile",
            "date_of_joining",
            "department",
            "ctc",
            "marital_status",
            "submission_status");

    private EnrollmentWindowEmployeeCsvWriter() {}

    public static String escapeField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains("\"") || value.contains(",") || value.contains("\r") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static void writeRow(Writer w, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(escapeField(cells.get(i)));
        }
        w.write("\r\n");
    }

    public static void writeBom(OutputStream os) throws IOException {
        os.write(0xEF);
        os.write(0xBB);
        os.write(0xBF);
    }

    public static Writer newUtf8Writer(OutputStream os) {
        return new OutputStreamWriter(os, StandardCharsets.UTF_8);
    }
}
