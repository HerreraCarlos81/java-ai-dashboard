package com.aiusage.service.provider;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class OpenAIProvider implements AiProvider {
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient client;

    public OpenAIProvider() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String name() { return "openai"; }

    @Override
    public List<UsageData> fetchUsage(String apiKey, String baseUrl,
                                       LocalDate startDate, LocalDate endDate) throws Exception {
        long startEpoch = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long endEpoch = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        String adminBase = baseUrl.replace("/v1", "").replace("v1", "");
        HttpUrl url = HttpUrl.parse(adminBase + "/v1/organization/usage/completions").newBuilder()
            .addQueryParameter("start_time", String.valueOf(startEpoch))
            .addQueryParameter("end_time", String.valueOf(endEpoch))
            .addQueryParameter("bucket_width", "1d")
            .addQueryParameter("limit", "31")
            .addQueryParameter("group_by", "api_key_id")
            .build();

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String hint = response.code() == 404
                    ? "The Usage API requires an Admin API key (Settings → Organization → Admin keys), not a project key."
                    : "";
                throw new IOException("OpenAI usage API error: " + response.code()
                    + (hint.isEmpty() ? "" : " - " + hint)
                    + " " + body);
            }
            return parseUsageResponse(body);
        }
    }

    @Override
    public List<CostData> fetchCosts(String apiKey, String baseUrl,
                                      LocalDate startDate, LocalDate endDate) throws Exception {
        long startEpoch = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long endEpoch = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        String adminBase = baseUrl.replace("/v1", "").replace("v1", "");
        HttpUrl url = HttpUrl.parse(adminBase + "/v1/organization/costs").newBuilder()
            .addQueryParameter("start_time", String.valueOf(startEpoch))
            .addQueryParameter("end_time", String.valueOf(endEpoch))
            .addQueryParameter("bucket_width", "1d")
            .addQueryParameter("limit", "31")
            .addQueryParameter("group_by", "api_key_id")
            .build();

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String hint = response.code() == 404
                    ? "The Costs API requires an Admin API key (Settings → Organization → Admin keys), not a project key."
                    : "";
                throw new IOException("OpenAI cost API error: " + response.code()
                    + (hint.isEmpty() ? "" : " - " + hint)
                    + " " + body);
            }
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
                            ud.setInputTokens(r.path("input_tokens").asLong(0));
                            ud.setOutputTokens(r.path("output_tokens").asLong(0));
                            ud.setCachedInputTokens(r.path("input_cached_tokens").asLong(0));
                            ud.setTotalTokens(
                                ud.getInputTokens() + ud.getOutputTokens() + ud.getCachedInputTokens()
                            );
                            ud.setNumRequests(r.path("num_model_requests").asInt(0));
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
            System.err.println("Failed to parse OpenAI usage: " + e.getMessage());
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
                            var amountNode = r.get("amount");
                            if (amountNode != null && amountNode.isObject()) {
                                cd.setAmount(amountNode.path("value").asDouble(0.0));
                                cd.setCurrency(amountNode.path("currency").asText("usd"));
                            } else {
                                cd.setAmount(r.path("amount").asDouble(0.0));
                                cd.setCurrency("usd");
                            }
                            if (r.has("line_item") && !r.path("line_item").isNull()) {
                                cd.setModel(r.path("line_item").asText());
                            }
                            results.add(cd);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse OpenAI costs: " + e.getMessage());
        }
        return results;
    }
}
