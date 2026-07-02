package com.aiusage.service.provider;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnthropicProvider implements AiProvider {
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient client;

    public AnthropicProvider() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String name() { return "anthropic"; }

    @Override
    public List<UsageData> fetchUsage(String apiKey, String baseUrl,
                                       LocalDate startDate, LocalDate endDate) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        HttpUrl parsedBase = HttpUrl.parse(baseUrl);
        String origin = parsedBase.scheme() + "://" + parsedBase.host();
        HttpUrl url = HttpUrl.parse(origin + "/v1/organizations/usage_report/messages").newBuilder()
            .addQueryParameter("starting_at", startDate.atStartOfDay().format(fmt))
            .addQueryParameter("ending_at", endDate.plusDays(1).atStartOfDay().format(fmt))
            .addQueryParameter("bucket_width", "1d")
            .addQueryParameter("limit", "31")
            .addQueryParameter("group_by[]", "api_key_id")
            .addQueryParameter("group_by[]", "model")
            .build();

        Request request = new Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Anthropic usage API error: " + response.code() + " " + response.body().string());
            }
            String body = response.body().string();
            return parseUsageResponse(body);
        }
    }

    @Override
    public List<CostData> fetchCosts(String apiKey, String baseUrl,
                                      LocalDate startDate, LocalDate endDate) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        HttpUrl parsedBase = HttpUrl.parse(baseUrl);
        String origin = parsedBase.scheme() + "://" + parsedBase.host();
        HttpUrl url = HttpUrl.parse(origin + "/v1/organizations/cost_report").newBuilder()
            .addQueryParameter("starting_at", startDate.atStartOfDay().format(fmt))
            .addQueryParameter("ending_at", endDate.plusDays(1).atStartOfDay().format(fmt))
            .addQueryParameter("bucket_width", "1d")
            .addQueryParameter("limit", "31")
            .build();

        Request request = new Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Anthropic cost API error: " + response.code());
            }
            String body = response.body().string();
            return parseCostResponse(body);
        }
    }

    @Override
    public boolean testConnection(String apiKey, String baseUrl) {
        try {
            fetchUsage(apiKey, baseUrl, LocalDate.now().minusDays(1), LocalDate.now());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, String> fetchApiKeyNames(String apiKey, String baseUrl) throws Exception {
        Map<String, String> result = new HashMap<>();
        String after = null;
        HttpUrl parsedBase = HttpUrl.parse(baseUrl);
        String origin = parsedBase.scheme() + "://" + parsedBase.host();

        for (int page = 0; page < 10; page++) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(origin + "/v1/organizations/api_keys").newBuilder()
                .addQueryParameter("limit", "100")
                .addQueryParameter("status", "active");
            if (after != null) {
                urlBuilder.addQueryParameter("after_id", after);
            }
            Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    System.err.println("fetchApiKeyNames HTTP " + response.code() + ": " + body);
                    return result;
                }
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                var root = mapper.readTree(body);
                var data = root.get("data");
                if (data != null && data.isArray()) {
                    for (var key : data) {
                        String id = key.path("id").asText(null);
                        String name = key.path("name").asText(null);
                        if (id != null && name != null) {
                            result.put(id, name);
                        }
                    }
                }
                boolean hasMore = root.path("has_more").asBoolean(false);
                if (!hasMore) break;
                after = root.path("last_id").asText(null);
                if (after == null) break;
            }
        }
        return result;
    }

    private List<UsageData> parseUsageResponse(String json) {
        List<UsageData> results = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(json);
            var data = root.get("data");
            if (data != null && data.isArray()) {
                for (var bucket : data) {
                    var resultsArr = bucket.get("results");
                    if (resultsArr != null && resultsArr.isArray()) {
                        for (var r : resultsArr) {
                            UsageData ud = new UsageData();
                            ud.setInputTokens(r.path("uncached_input_tokens").asLong(0));
                            ud.setOutputTokens(r.path("output_tokens").asLong(0));
                            var cache = r.path("cache_read_input_tokens");
                            ud.setCachedInputTokens(cache.asLong(0));
                            ud.setTotalTokens(
                                ud.getInputTokens() + ud.getOutputTokens() + ud.getCachedInputTokens()
                            );
                            ud.setNumRequests(r.path("num_requests").asInt(0));
                            if (r.has("api_key_id") && !r.path("api_key_id").isNull()) {
                                ud.setApiKeyId(r.path("api_key_id").asText());
                            }
                            if (r.has("model") && !r.path("model").isNull()) {
                                ud.setModel(r.path("model").asText());
                            }
                            results.add(ud);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Anthropic usage: " + e.getMessage());
        }
        return results;
    }

    private List<CostData> parseCostResponse(String json) {
        List<CostData> results = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(json);
            var data = root.get("data");
            if (data != null && data.isArray()) {
                for (var bucket : data) {
                    var resultsArr = bucket.get("results");
                    if (resultsArr != null && resultsArr.isArray()) {
                        for (var r : resultsArr) {
                            CostData cd = new CostData();
                            String amountStr = r.path("amount").asText("0");
                            try {
                                cd.setAmount(Double.parseDouble(amountStr) / 100.0);
                            } catch (NumberFormatException e) {
                                cd.setAmount(0.0);
                            }
                            cd.setCurrency("USD");
                            if (r.has("model") && !r.path("model").isNull()) {
                                cd.setModel(r.path("model").asText());
                            }

                            results.add(cd);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Anthropic costs: " + e.getMessage());
        }
        return results;
    }
}
