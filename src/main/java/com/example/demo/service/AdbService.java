package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.io.IOException;

public interface AdbService {
    String executeCommand(String deviceId, String command);
    byte[] takeScreenshot(String deviceId) throws IOException;
    List<Map<String, String>> getRunningApps(String deviceId);
}