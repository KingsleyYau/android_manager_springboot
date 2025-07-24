package com.example.demo.service.impl;

import com.example.demo.service.AdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdbServiceImpl implements AdbService {
    private static final Logger log = LoggerFactory.getLogger(AdbServiceImpl.class);
    private static final long ADB_COMMAND_TIMEOUT_MS = 30000; // 30秒超时

    @Override
    public String executeCommand(String deviceId, String command) {
        try {
            // 构建ADB命令
            String adbCommand = String.format("adb -s %s shell %s", deviceId, command);
            log.info("执行ADB命令: {}", adbCommand);

            Process process = Runtime.getRuntime().exec(adbCommand);

            // 使用线程读取输出和错误流
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("读取命令输出失败", e);
                }
            });

            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("读取命令错误输出失败", e);
                }
            });

            outputThread.start();
            errorThread.start();

            // 等待命令执行完成，设置超时
            boolean completed = process.waitFor(ADB_COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroy();
                log.error("ADB命令执行超时，设备ID: {}, 命令: {}", deviceId, command);
                return "";
            }

            // 等待输出线程完成
            outputThread.join();
            errorThread.join();

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.error("ADB命令执行失败，设备ID: {}, 命令: {}, 退出码: {}, 错误: {}", 
                          deviceId, command, exitCode, error.toString());
                // 检查是否是协议错误
                if (error.toString().contains("protocol fault")) {
                    log.error("检测到ADB协议错误，可能是设备连接问题");
                    // return "ADB协议错误: 可能是设备连接问题，请检查设备连接状态"; 
                }
                return error.toString();
            }

            log.info("ADB命令执行成功，设备ID: {}, 命令: {}", deviceId, command);
            return output.toString();

        } catch (IOException | InterruptedException e) {
            log.error("执行ADB命令时发生异常，设备ID: {}, 命令: {}", deviceId, command, e);
            return e.getMessage();
        }
    }

    @Override
    public byte[] takeScreenshot(String deviceId) throws IOException {
        String adbCommand = String.format("adb -s %s shell screencap -p", deviceId);
        log.info("执行ADB截图命令: {}", adbCommand);

        Process process = Runtime.getRuntime().exec(adbCommand);

        try (InputStream inputStream = process.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 等待命令执行完成，设置超时
            boolean completed = process.waitFor(ADB_COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroy();
                log.error("ADB截图命令执行超时，设备ID: {}", deviceId);
                throw new IOException("ADB截图命令执行超时");
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                // 读取错误流
                StringBuilder error = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }
                log.error("ADB截图命令执行失败，设备ID: {}, 退出码: {}, 错误: {}", 
                          deviceId, exitCode, error.toString());
                throw new IOException("ADB截图命令执行失败: " + error.toString());
            }

            log.info("ADB截图命令执行成功，设备ID: {}", deviceId);
            return outputStream.toByteArray();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ADB截图命令执行被中断，设备ID: {}", deviceId, e);
            throw new IOException("ADB截图命令执行被中断", e);
        } finally {
            process.destroy();
        }
    }

    @Override
    public List<Map<String, String>> getRunningApps(String deviceId) {
        // 使用 pm list packages 命令获取已安装的应用
        String command = "pm list packages -f | sed 's/^package://'";
        String result = executeCommand(deviceId, command);
        
        // 解析命令结果
        List<Map<String, String>> appList = new ArrayList<>();
        if (result != null && !result.isEmpty()) {
            String[] lines = result.split("\\n");
            for (String line : lines) {
                // 提取包名
                // 格式示例: /data/app/com.example.app-1/base.apk=com.example.app
                int eqIndex = line.lastIndexOf('=');
                if (eqIndex > 0) {
                    String packageName = line.substring(eqIndex + 1);
                    Map<String, String> app = new HashMap<>();
                    app.put("packageName", packageName);
                    app.put("name", packageName); // 简化处理，实际应用中可能需要获取应用名称
                    
                    // 检查应用是否正在运行
                    String runningCommand = "dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp' | grep " + packageName;
                    String runningResult = executeCommand(deviceId, runningCommand);
                    log.info("查看应用 设备ID: {}, {} 是否运行", deviceId, packageName, runningResult.isEmpty());
                    app.put("isRunning", runningResult.contains(packageName) ? "true" : "false");
                    
                    appList.add(app);
                }
            }
        }
        
        return appList;
    }
}