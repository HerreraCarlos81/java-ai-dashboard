package com.aiusage.model;

public class ApiKeyEntry {
    private String id;
    private String label;
    private String key;
    private boolean enabled;
    private boolean admin;

    public ApiKeyEntry() {}

    public ApiKeyEntry(String id, String label, String key) {
        this.id = id;
        this.label = label;
        this.key = key;
        this.enabled = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public String getMaskedKey() {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
