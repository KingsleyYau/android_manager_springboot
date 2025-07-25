package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.example.demo.model.FileInfo;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.service.AdbService;

@Controller
@RequestMapping("/android-devices")
public class AndroidDeviceController {

    private static final Logger log = LoggerFactory.getLogger(AndroidDeviceController.class);

    @Autowired
    private AdbService adbService;

    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<AndroidDevice>> getConnectedDevices() {
        List<AndroidDevice> devices = new ArrayList<>();

        try {
            // 执行adb devices命令
            Process process = Runtime.getRuntime().exec("adb devices");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                // 跳过第一行（标题行）
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 2) {
                        String deviceId = parts[0];
                        String status = parts[1];
                        devices.add(new AndroidDevice(deviceId, status));
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("执行adb devices命令失败，退出码: {}", exitCode);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            log.info("成功获取到 {} 台连接的安卓设备", devices.size());
            return ResponseEntity.ok(devices);

        } catch (IOException | InterruptedException e) {
            log.error("获取安卓设备列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping
    public String showDevices(Model model) {
        List<AndroidDevice> devices = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("adb devices");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 2) {
                        String deviceId = parts[0];
                        String status = parts[1];
                        devices.add(new AndroidDevice(deviceId, status));
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("获取设备列表失败", e);
        }
        model.addAttribute("devices", devices);
        return "device-list";
    }

    @PostMapping("/api/execute-command")
    @ResponseBody
    public ResponseEntity<String> executeAdbCommand(@RequestBody Map<String, String> request) {
        String deviceId = request.get("deviceId");
        String command = request.get("command");
        if (deviceId == null || command == null) {
            return ResponseEntity.badRequest().body("设备ID和命令不能为空");
        }
        try {
            // 构建带设备ID的ADB命令
            // 移除命令中可能包含的'adb'前缀
            String cleanCommand = command.trim();
            if (cleanCommand.startsWith("adb ")) {
                cleanCommand = cleanCommand.substring(4);
            }
            String adbCommand = String.format("adb -s %s shell %s", deviceId, cleanCommand);
            log.info("执行ADB命令: {}", adbCommand);

            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            StringBuilder error = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("ADB命令执行失败，设备ID: {}, 命令: {}, 错误: {}", deviceId, command, error.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(error.toString());
            }

            log.info("ADB命令执行成功，设备ID: {}, 命令: {}", deviceId, command);
            return ResponseEntity.ok(output.toString());

        } catch (IOException | InterruptedException e) {
            log.error("执行ADB命令时发生异常，设备ID: {}, 命令: {}", deviceId, command, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("执行命令时发生错误: " + e.getMessage());
        }
    }

    @PostMapping("/api/upload-file")
    @ResponseBody
    public ResponseEntity<String> uploadFile(@RequestParam String deviceId, @RequestParam String remotePath, 
                                            @RequestParam MultipartFile file) {
        try {
            // 保存上传的文件到临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            File tempFile = new File(tempDir, file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            // 构建ADB上传命令
            String adbCommand = String.format("adb -s %s push %s %s", deviceId, tempFile.getAbsolutePath(), remotePath);
            log.info("执行文件上传命令: {}", adbCommand);

            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }
            log.info("文件上传命令输出: {}", output.toString());

            int exitCode = process.waitFor();

            // 删除临时文件
            tempFile.delete();

            if (exitCode != 0) {
                log.error("文件上传失败，设备ID: {}, 远程路径: {}, 错误: {}", deviceId, remotePath, error.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件上传失败: " + error.toString());
            }

            log.info("文件上传成功，设备ID: {}, 远程路径: {}", deviceId, remotePath);
            return ResponseEntity.ok("文件上传成功");

        } catch (IOException | InterruptedException e) {
            log.error("文件上传时发生异常，设备ID: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文件上传时发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/api/download-file")
    @ResponseBody
    public ResponseEntity<?> downloadFile(@RequestParam String deviceId, @RequestParam String remotePath) {
        try {
            // 保存下载的文件到临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        // 创建唯一的临时目录，避免目录嵌套问题
        File tempDirObj = Files.createTempDirectory("adb-download-").toFile();
        String fileName = new File(remotePath).getName();
        File tempFile = new File(tempDirObj, fileName);
        log.info("准备下载文件到临时目录: {}", tempFile.getAbsolutePath());

        // 构建ADB下载命令
        String[] adbCommand = new String[] {"adb", "-s", deviceId, "pull", remotePath, tempFile.getAbsolutePath()};
        log.info("执行文件下载命令: {}", Arrays.toString(adbCommand));

            ProcessBuilder processBuilder = new ProcessBuilder(adbCommand);
            processBuilder.redirectErrorStream(true); // 合并错误流到标准输出
            Process process = processBuilder.start();

            // 同时读取标准输出和错误输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            log.info("ADB命令退出码: {}, 输出: {}", exitCode, output.toString());

            if (exitCode != 0) {
                String errorMessage = "文件下载失败: " + output.toString();
                log.error("文件下载失败，设备ID: {}, 远程路径: {}, 错误: {}", deviceId, remotePath, output.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
            }

            if (!tempFile.exists()) {
                String errorMessage = "下载的文件不存在: " + tempFile.getAbsolutePath();
                log.error(errorMessage);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
            }

            // 检查是否为目录
            log.info("下载文件类型检查: {}, 是否为目录: {}", tempFile.getAbsolutePath(), tempFile.isDirectory());
            if (tempFile.isDirectory()) {
                log.info("下载的是目录，准备压缩: {}", tempFile.getAbsolutePath());
                // 创建压缩文件
                File zipFile = new File(tempDir, fileName + ".zip");
                log.info("准备创建压缩文件: {}", zipFile.getAbsolutePath());
                zipDirectory(tempFile, zipFile);
                log.info("压缩完成，文件大小: {}", zipFile.length());
                tempFile = zipFile;
                fileName += ".zip";
                log.info("更新下载文件名: {}", fileName);
            } else {
                log.info("下载的是文件，直接提供下载: {}", tempFile.getAbsolutePath());
            }

            Path filePath = Paths.get(tempFile.getAbsolutePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("文件下载成功，设备ID: {}, 远程路径: {}, 文件大小: {}", deviceId, remotePath, tempFile.length());
                // 根据文件扩展名设置Content-Type
                String contentType = fileName.endsWith(".zip") ? "application/zip" : "application/octet-stream";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                String errorMessage = "下载的文件不可读: " + tempFile.getAbsolutePath();
                log.error(errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
            }

        } catch (IOException e) {
            String errorMessage = "文件下载时发生IO异常: " + e.getMessage();
            log.error("文件下载时发生IO异常，设备ID: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = "文件下载过程被中断: " + e.getMessage();
            log.error("文件下载过程被中断，设备ID: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

    /**
     * 压缩目录
     */
    private void zipDirectory(File directory, File zipFile) throws IOException {
        log.info("开始压缩目录: {}, 目标ZIP文件: {}", directory.getAbsolutePath(), zipFile.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            // 直接压缩目录内容，避免嵌套目录
            zipFile(directory, "", zos);
            log.info("目录压缩完成，ZIP文件大小: {} 字节", zipFile.length());
        } catch (IOException e) {
            log.error("压缩目录时发生异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 递归压缩文件
     */
    private void zipFile(File file, String fileName, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            log.info("添加目录到ZIP: {}", fileName + "/");
            // 创建目录条目
            zos.putNextEntry(new ZipEntry(fileName + "/"));
            zos.closeEntry();

            // 递归压缩子文件
            File[] children = file.listFiles();
            if (children != null) {
                log.info("目录 {} 包含 {} 个子文件/目录", fileName, children.length);
                for (File child : children) {
                    zipFile(child, fileName + "/" + child.getName(), zos);
                }
            } else {
                log.warn("无法列出目录 {} 的内容", fileName);
            }
        } else {
            log.info("添加文件到ZIP: {}, 大小: {} 字节", fileName, file.length());
            // 压缩文件
            try (FileInputStream fis = new FileInputStream(file)) {
                zos.putNextEntry(new ZipEntry(fileName));
                byte[] buffer = new byte[1024];
                int length;
                long totalWritten = 0;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                    totalWritten += length;
                }
                zos.closeEntry();
                log.info("文件 {} 压缩完成，写入 {} 字节", fileName, totalWritten);
            } catch (IOException e) {
                log.error("压缩文件 {} 时发生异常: {}", fileName, e.getMessage(), e);
                throw e;
            }
        }
    }

    @DeleteMapping("/api/delete-file")
    @ResponseBody
    public ResponseEntity<String> deleteFile(@RequestParam String deviceId, @RequestParam String remotePath) {
        try {
            // 构建ADB删除命令
            String adbCommand = String.format("adb -s %s shell rm -rf %s", deviceId, remotePath);
            log.info("执行文件删除命令: {}", adbCommand);

            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder error = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("文件删除失败，设备ID: {}, 远程路径: {}, 错误: {}", deviceId, remotePath, error.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件删除失败: " + error.toString());
            }

            log.info("文件删除成功，设备ID: {}, 远程路径: {}", deviceId, remotePath);
            return ResponseEntity.ok("文件删除成功");

        } catch (IOException | InterruptedException e) {
            log.error("文件删除时发生异常，设备ID: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文件删除时发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/api/list-directory")
    public ResponseEntity<?> listDirectory(@RequestParam String deviceId, @RequestParam String remotePath) {
        log.info("执行目录列出命令: adb -s {} shell ls -la '{}'", deviceId, remotePath);
        
        try {
            // 为路径添加单引号以处理包含空格的路径
            String result = adbService.executeCommand(deviceId, "ls -la '" + remotePath + "'");
            
            // 记录命令执行结果
            log.info("ADB命令执行结果: {}", result);
            
            // 增强错误检测: 检查结果是否以'total '开头，确保是有效的目录列表
            if (result == null) {
                log.error("ADB命令返回null结果");
                throw new Exception("ADB命令执行失败: 返回null结果");
            }

            String trimmedResult = result.trim();
            log.info("ADB命令原始结果: {}", result);
            log.info("ADB命令处理后结果: {}", trimmedResult);

            if (trimmedResult.isEmpty()) {
                log.error("ADB命令返回空结果");
                throw new Exception("ADB命令执行失败: 返回空结果");
            }
            
            if (result.contains("adb: error")) {
                log.error("ADB命令包含错误信息: {}", result);
                throw new Exception("ADB命令执行失败: 包含'adb: error'错误");
            }
            
            if (result.contains("No such file or directory")) {
                log.error("ADB命令报告文件或目录不存在: {}", result);
                throw new Exception("目录不存在: " + remotePath);
            }
            
            if (result.contains("permission denied")) {
                log.error("ADB命令报告权限被拒绝: {}", result);
                throw new Exception("权限被拒绝: 无法访问目录 " + remotePath);
            }
            
            List<FileInfo> fileInfos = parseDirectoryOutput(result, remotePath);
            log.info("成功解析目录内容，找到 {} 个文件/目录", fileInfos.size());
            return ResponseEntity.ok(fileInfos);
        } catch (Exception e) {
            log.error("目录列出失败: {}", e.getMessage(), e);
            // 返回标准化的错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("path", remotePath);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            // 根据异常类型设置合适的HTTP状态码
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (e.getMessage().contains("目录不存在")) {
                status = HttpStatus.NOT_FOUND;
            } else if (e.getMessage().contains("权限被拒绝")) {
                status = HttpStatus.FORBIDDEN;
            }
            
            return ResponseEntity.status(status).body(errorResponse);
        }
    }

    /**
     * 捕获设备截图
     */
    @GetMapping("/api/take-screenshot")
    @ResponseBody
    public ResponseEntity<Resource> takeScreenshot(@RequestParam String deviceId) {
        try {
            byte[] screenshotBytes = adbService.takeScreenshot(deviceId);
            ByteArrayResource resource = new ByteArrayResource(screenshotBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(resource);
        } catch (Exception e) {
            log.error("捕获设备截图失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取运行中的应用列表
     */
    @GetMapping("/api/get-app-list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAppList(@RequestParam String deviceId) {
        try {
            List<Map<String, String>> appList = adbService.getRunningApps(deviceId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("appList", appList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取应用列表失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取应用列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 发送触摸事件到设备
     */
    @PostMapping("/api/send-touch-event")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendTouchEvent(@RequestBody Map<String, Object> request) {
        try {
            String deviceId = (String) request.get("deviceId");
            Integer x = (Integer) request.get("x");
            Integer y = (Integer) request.get("y");

            if (deviceId == null || x == null || y == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "设备ID和坐标参数不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 执行ADB触摸命令
            String result = adbService.executeCommand(deviceId, "input tap " + x + " " + y);
            log.info("发送触摸事件到设备 {}: ({}, {}), 结果: {}", deviceId, x, y, result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "触摸事件发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送触摸事件失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "发送触摸事件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 检查设备当前是否有输入框弹出
     */
    @GetMapping("/api/check-input-focus")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkInputFocus(@RequestParam String deviceId) {
        try {
            // 执行ADB命令获取输入焦点信息
            String result = adbService.executeCommand(deviceId, "dumpsys input | grep -A 10 'FocusedApplication'");
            log.info("检查设备 {} 输入焦点结果: {}", deviceId, result);

            Map<String, Object> response = new HashMap<>();
            // 检查结果中是否包含输入法相关的包名或关键字
            // 使用正则表达式匹配包含InputMethod且visible=true的行
            boolean hasInputFocus = Pattern.compile("InputMethod.*visible=true").matcher(result).find();
            response.put("success", true);
            response.put("hasInputFocus", hasInputFocus);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查输入焦点失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "检查输入焦点失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 发送文本到输入框并确认
     */
    @PostMapping("/api/send-input-text")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendInputText(@RequestBody Map<String, Object> request) {
        try {
            String deviceId = (String) request.get("deviceId");
            String text = (String) request.get("text");

            if (deviceId == null || text == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "设备ID和文本参数不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 替换文本中的空格为%s（ADB input text命令的空格表示方式）
            String formattedText = text.replace(" ", "%s");
            // 执行ADB输入文本命令
            String inputResult = adbService.executeCommand(deviceId, "input text " + formattedText);
            // 发送确认键（KEYCODE_ENTER = 66）
            String enterResult = adbService.executeCommand(deviceId, "input keyevent 66");

            log.info("发送文本到设备 {}: {}, 结果: {}", deviceId, text, inputResult);
            log.info("发送确认键到设备 {}, 结果: {}", deviceId, enterResult);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文本输入和确认发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送文本输入失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "发送文本输入失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 内部类用于表示安卓设备信息
    public static class AndroidDevice {
        private String deviceId;
        private String status;

        public AndroidDevice(String deviceId, String status) {
            this.deviceId = deviceId;
            this.status = status;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getStatus() {
            return status;
        }
    }

    private List<FileInfo> parseDirectoryOutput(String output, String remotePath) {
        List<FileInfo> fileInfos = new ArrayList<>();
        String[] lines = output.split("\\n");
        boolean isFirstLine = true; // 跳过第一行(total行)
        
        log.info("解析目录输出，远程路径: {}", remotePath);
        log.info("目录输出行数: {}", lines.length);
        
        for (String line : lines) {
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 正确解析包含空格的文件名
            // 前8个字段用空格分隔，剩余部分为文件名
            String[] parts = line.split("\\s+", 8); // 最多分割为8个部分
            log.debug("解析行: {}, 长度: {}", line, parts.length);
            if (parts.length < 8) {
                log.warn("行解析失败，部分数不足8: {}", line);
                continue;
            }
            
            FileInfo fileInfo = new FileInfo();
            fileInfo.setPermissions(parts[0]);
            fileInfo.setOwner(parts[2]);
            fileInfo.setSize(parts[4]);
            fileInfo.setModifiedTime(parts[5] + " " + parts[6]);
            
            // 处理文件名，可能包含符号链接
            String fileName = parts[7];
            String linkTarget = null;
            
            // 检查是否包含符号链接
            if (fileName.contains("->")) {
                String[] linkParts = fileName.split("->", 2);
                fileName = linkParts[0].trim(); // 提取实际文件名
                if (linkParts.length > 1) {
                    linkTarget = linkParts[1].trim(); // 提取链接目标
                }
            }
            
            fileInfo.setName(fileName);
            
            // 检查是否为目录
            fileInfo.setDirectory(parts[0].startsWith("d"));
            
            // 构建完整路径，避免根目录下的双斜杠
            String fullPath;
            if (remotePath.lastIndexOf('/')  == remotePath.length() - 1) {
                fullPath = remotePath + fileName;
            } else {
                fullPath = remotePath + "/" + fileName;
            }
            fileInfo.setPath(fullPath);
            
            // 如果是符号链接，记录链接目标
            if (linkTarget != null) {
                log.debug("符号链接 {} 指向 {}", fileName, linkTarget);
            }
            
            log.debug("解析文件信息: 权限={}, 名称={}, 路径={}, 是目录={}", 
                      parts[0], fileName, fullPath, fileInfo.isDirectory());
            
            // 过滤掉.和..目录
            if (!fileName.equals(".") && !fileName.equals("..")) {
                fileInfos.add(fileInfo);
            } else {
                log.debug("跳过特殊目录: {}", fileName);
            }
        }
        
        log.info("解析完成，文件数量: {}", fileInfos.size());
        return fileInfos;
    }
}