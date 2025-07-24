package com.example.demo.model;

public class FileInfo {
    private String permissions;
    private String owner;
    private String size;
    private String modifiedTime;
    private String name;
    private String path;
    private boolean isDirectory;

    // Getters and Setters
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(String modifiedTime) { this.modifiedTime = modifiedTime; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }
}