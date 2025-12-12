package com.vimainsurance.vimaadmin.config;

import com.vimainsurance.vimaadmin.util.CsvDealsReaderUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "csv.deals")
public class CsvDealsHeaderProperties {

    /**
     * Ordered list of CSV headers. If fewer values are provided, defaults are used.
     * Order (index based): employeeId, firstName, lastName, email, phone,
     * designation, dateOfJoining, status.
     */
    private List<String> headers = defaultHeaders();

    @PostConstruct
    public void apply() {
        CsvDealsReaderUtil.configureHeaderKeys(headers);
    }

    private static List<String> defaultHeaders() {
        List<String> defaults = new ArrayList<>(8);
        defaults.add("employee id");
        defaults.add("first name");
        defaults.add("last name");
        defaults.add("email");
        defaults.add("phone");
        defaults.add("designation");
        defaults.add("date of joining");
        defaults.add("status");
        return defaults;
    }
}

