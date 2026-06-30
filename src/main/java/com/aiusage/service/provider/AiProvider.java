package com.aiusage.service.provider;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import java.time.LocalDate;
import java.util.List;

public interface AiProvider {
    String name();

    List<UsageData> fetchUsage(String apiKey, String baseUrl,
                                LocalDate startDate, LocalDate endDate)
        throws Exception;

    List<CostData> fetchCosts(String apiKey, String baseUrl,
                               LocalDate startDate, LocalDate endDate)
        throws Exception;

    boolean testConnection(String apiKey, String baseUrl);
}
