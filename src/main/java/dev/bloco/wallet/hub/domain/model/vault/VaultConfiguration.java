package dev.bloco.wallet.hub.domain.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class VaultConfiguration {
    private final Map<String, String> parameters;

    public VaultConfiguration(Map<String, String> parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public static VaultConfiguration forAwsKms(String region, String keyId, String accessKeyId, String secretAccessKey) {
        Map<String, String> params = new HashMap<>();
        params.put("region", region);
        params.put("keyId", keyId);
        params.put("accessKeyId", accessKeyId);
        params.put("secretAccessKey", secretAccessKey);
        return new VaultConfiguration(params);
    }

    public static VaultConfiguration forAzureKeyVault(String vaultUri, String tenantId, String clientId, String clientSecret) {
        Map<String, String> params = new HashMap<>();
        params.put("vaultUri", vaultUri);
        params.put("tenantId", tenantId);
        params.put("clientId", clientId);
        params.put("clientSecret", clientSecret);
        return new VaultConfiguration(params);
    }

    // Factory methods for other vault types
}