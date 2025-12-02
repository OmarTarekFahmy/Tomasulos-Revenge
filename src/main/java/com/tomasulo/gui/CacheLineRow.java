package com.tomasulo.gui;

/**
 * Row model for Cache table.
 */
public class CacheLineRow {
    private int index;
    private String valid;
    private String tag;
    private String data;
    private String lastAccess;

    public CacheLineRow(int index, boolean valid, String tag, String data, String lastAccess) {
        this.index = index;
        this.valid = valid ? "Yes" : "No";
        this.tag = tag;
        this.data = data;
        this.lastAccess = lastAccess;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getValid() { return valid; }
    public void setValid(String valid) { this.valid = valid; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getLastAccess() { return lastAccess; }
    public void setLastAccess(String lastAccess) { this.lastAccess = lastAccess; }
}
