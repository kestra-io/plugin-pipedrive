package io.kestra.plugin.pipedrive.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for interacting with the Pipedrive API.
 * Handles authentication, request execution, and error handling.
 */
public class PipedriveClient {
    private static final String BASE_URL = "https://api.pipedrive.com/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiToken;
    private final Logger logger;

    public PipedriveClient(String apiToken, Logger logger) {
        this.apiToken = apiToken;
        this.logger = logger;
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(this::retryInterceptor)
            .build();
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    private Response retryInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        int tryCount = 0;
        int maxRetries = 3;

        while (tryCount < maxRetries) {
            try {
                response = chain.proceed(request);
                
                // Retry on rate limit or server errors
                if (response.code() == 429 || response.code() >= 500) {
                    if (tryCount < maxRetries - 1) {
                        response.close();
                        int waitTime = (int) Math.pow(2, tryCount) * 1000; // Exponential backoff
                        logger.warn("Request failed with code {}, retrying in {}ms...", response.code(), waitTime);
                        Thread.sleep(waitTime);
                        tryCount++;
                        continue;
                    }
                }
                
                return response;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            } catch (IOException e) {
                if (tryCount >= maxRetries - 1) {
                    throw e;
                }
                tryCount++;
                logger.warn("Request failed, retrying... (attempt {}/{})", tryCount, maxRetries);
            }
        }
        
        return response;
    }

    /**
     * Execute a GET request to Pipedrive API
     */
    public <T> PipedriveResponse<T> get(String endpoint, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        return executeRequest(request, typeRef);
    }

    /**
     * Execute a POST request to Pipedrive API
     */
    public <T> PipedriveResponse<T> post(String endpoint, Object body, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);
        String jsonBody = objectMapper.writeValueAsString(body);
        
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
        
        return executeRequest(request, typeRef);
    }

    /**
     * Execute a PUT request to Pipedrive API
     */
    public <T> PipedriveResponse<T> put(String endpoint, Object body, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);
        String jsonBody = objectMapper.writeValueAsString(body);
        
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
            .url(url)
            .put(requestBody)
            .build();
        
        return executeRequest(request, typeRef);
    }

    /**
     * Execute a DELETE request to Pipedrive API
     */
    public <T> PipedriveResponse<T> delete(String endpoint, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);
        
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        
        return executeRequest(request, typeRef);
    }

    private <T> T executeRequest(Request request, TypeReference<T> typeRef) throws IOException {
        logger.debug("Executing Pipedrive API request: {} {}", request.method(), request.url());
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                logger.error("Pipedrive API request failed: {} - {}", response.code(), responseBody);
                throw new IOException("Pipedrive API request failed: " + response.code() + " - " + responseBody);
            }
            
            logger.debug("Pipedrive API response: {}", responseBody);
            return objectMapper.readValue(responseBody, typeRef);
        }
    }

    private String buildUrl(String endpoint) {
        String separator = endpoint.contains("?") ? "&" : "?";
        return BASE_URL + endpoint + separator + "api_token=" + apiToken;
    }

    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
