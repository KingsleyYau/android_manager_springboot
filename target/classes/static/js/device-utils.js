// 设备管理工具函数

// 文档加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    console.log('device-utils.js loaded');
});

// 浏览文件函数
function browseFiles(deviceId) {
    console.log('browseFiles function called with deviceId:', deviceId);
    document.getElementById('browseDeviceId').textContent = deviceId;
    document.getElementById('currentDirectory').value = '/sdcard/';
    document.getElementById('fileBrowseModal').style.display = 'block';
    document.getElementById('uploadToCurrentDirForm').style.display = 'none';
    // 确保listDirectory函数存在
    if (typeof listDirectory === 'function') {
        listDirectory();
    } else {
        console.error('listDirectory function is not defined');
    }
}

// 打开执行ADB命令模态框
function executeAdbCommand(deviceId) {
    document.getElementById('commandDeviceId').textContent = deviceId;
    document.getElementById('adbCommandModal').style.display = 'block';
}

// 打开文件传输模态框
function fileTransfer(deviceId) {
    document.getElementById('transferDeviceId').textContent = deviceId;
    document.getElementById('fileTransferModal').style.display = 'block';
}

// 关闭模态框
function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
    // 清除结果显示
    if (modalId === 'adbCommandModal') {
        document.getElementById('commandResult').textContent = '';
    } else if (modalId === 'fileTransferModal') {
        document.getElementById('transferResult').textContent = '';
    } else if (modalId === 'fileBrowseModal') {
        document.getElementById('browseResult').textContent = '';
    }
}

// 发送ADB命令
function sendAdbCommand() {
    const deviceId = document.getElementById('commandDeviceId').textContent;
    const command = document.getElementById('adbCommand').value;
    const resultDiv = document.getElementById('commandResult');

    if (!command.trim()) {
        resultDiv.textContent = '请输入ADB命令';
        return;
    }

    resultDiv.textContent = '正在执行命令...';

    fetch('/android-devices/api/execute-command', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            deviceId: deviceId,
            command: command
        })
    })
        .then(response => response.text())
        .then(data => {
            resultDiv.textContent = data;
        })
        .catch(error => {
            resultDiv.textContent = '执行命令时发生错误: ' + error;
        });
}

// 上传文件
function uploadFile() {
    const deviceId = document.getElementById('transferDeviceId').textContent;
    const fileInput = document.getElementById('fileToUpload');
    const remotePath = document.getElementById('remoteUploadPath').value;
    const resultDiv = document.getElementById('transferResult');

    if (!fileInput.files[0]) {
        resultDiv.textContent = '请选择要上传的文件';
        return;
    }

    if (!remotePath) {
        resultDiv.textContent = '请输入设备路径';
        return;
    }

    const formData = new FormData();
    formData.append('deviceId', deviceId);
    formData.append('remotePath', remotePath);
    formData.append('file', fileInput.files[0]);

    resultDiv.textContent = '正在上传文件...';

    fetch('/android-devices/api/upload-file', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(data => {
        resultDiv.textContent = data;
    })
    .catch(error => {
        resultDiv.textContent = '上传文件时发生错误: ' + error;
    });
}

// 显示上传文件到当前目录的表单
function uploadFileToCurrentDirectory() {
    document.getElementById('uploadToCurrentDirForm').style.display = 'block';
}

// 取消上传到当前目录
function cancelUploadToCurrentDirectory() {
    document.getElementById('uploadToCurrentDirForm').style.display = 'none';
    document.getElementById('fileToUploadCurrentDir').value = '';
}

// 打开设备截图模态框
function takeScreenshot(deviceId) {
    document.getElementById('screenshotDeviceId').textContent = deviceId;
    document.getElementById('screenshotModal').style.display = 'block';
    document.getElementById('screenshotResult').textContent = '';
    captureScreenshot();
}

// 捕获设备截图
function captureScreenshot() {
    const deviceId = document.getElementById('screenshotDeviceId').textContent;
    const resultDiv = document.getElementById('screenshotResult');

    resultDiv.textContent = '正在捕获截图...';

    fetch(`/android-devices/api/take-screenshot?deviceId=${encodeURIComponent(deviceId)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('截图失败: ' + response.statusText);
            }
            return response.blob();
        })
        .then(blob => {
            const url = URL.createObjectURL(blob);
            resultDiv.innerHTML = `<img src="${url}" alt="设备截图" style="max-width: 100%; max-height: 500px;" />`;
        })
        .catch(error => {
            resultDiv.textContent = '捕获截图时发生错误: ' + error.message;
        });
}

// 打开应用列表模态框
function showAppList(deviceId) {
    document.getElementById('appListDeviceId').textContent = deviceId;
    const appListModal = document.getElementById('appListModal');
    appListModal.style.display = 'block';
    // 调整模态窗口样式 - 移除全局蒙版效果
    appListModal.style.position = 'absolute';
    appListModal.style.top = '50%';
    appListModal.style.left = '50%';
    appListModal.style.transform = 'translate(-50%, -50%)';
    appListModal.style.zIndex = '1000'; // 确保在其他内容之上
    appListModal.style.backgroundColor = 'transparent';
    appListModal.style.boxShadow = 'none';
    appListModal.style.borderRadius = '8px';
    appListModal.style.width = '90%';
    appListModal.style.maxWidth = '900px';
    appListModal.style.margin = '0';
    appListModal.style.padding = '20px';
    appListModal.style.overflow = 'hidden';
    appListModal.style.maxHeight = '90vh';
    
    // 确保appListResult存在
    let appListResult = document.getElementById('appListResult');
    if (!appListResult) {
        // 如果不存在，则创建一个
        appListResult = document.createElement('div');
        appListResult.id = 'appListResult';
        appListResult.style.overflowY = 'auto';
        appListResult.style.maxHeight = 'calc(90vh - 100px)'; // 为搜索框和按钮留出空间
        appListModal.appendChild(appListResult);
    }
    appListResult.textContent = '';
    
    // 添加搜索框
    let searchDiv = document.getElementById('searchDiv');
    if (!searchDiv) {
        searchDiv = document.createElement('div');
        searchDiv.id = 'searchDiv';
        searchDiv.style.position = 'sticky';
        searchDiv.style.top = '0';
        searchDiv.style.backgroundColor = 'white';
        searchDiv.style.padding = '10px 0';
        searchDiv.style.marginBottom = '15px';
        searchDiv.style.zIndex = '10';
        searchDiv.innerHTML = '<input type="text" id="appSearchInput" placeholder="搜索应用..." style="width: 100%; padding: 8px; box-sizing: border-box; border: 1px solid #ddd; border-radius: 4px;">' +
            '<div style="margin-top: 10px; display: flex; gap: 10px;">' +
            '  <button id="filterAll" class="filter-btn active" style="padding: 5px 10px; background-color: #2196F3; color: white; border: none; border-radius: 4px; cursor: pointer;">所有</button>' +
            '  <button id="filterNotRunning" class="filter-btn" style="padding: 5px 10px; background-color: #f44336; color: white; border: none; border-radius: 4px; cursor: pointer;">未启动</button>' +
            '  <button id="filterRunning" class="filter-btn" style="padding: 5px 10px; background-color: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer;">运行中</button>' +
            '</div>';

        // 设置激活的过滤按钮
        function setActiveFilter(filterType) {
            // 移除所有按钮的active类
            document.querySelectorAll('.filter-btn').forEach(btn => {
                btn.classList.remove('active');
                btn.style.opacity = '0.7';
            });
            // 添加active类到选中的按钮
            if (filterType === 'all') {
                document.getElementById('filterAll').classList.add('active');
                document.getElementById('filterAll').style.opacity = '1';
            } else if (filterType === 'notRunning') {
                document.getElementById('filterNotRunning').classList.add('active');
                document.getElementById('filterNotRunning').style.opacity = '1';
            } else if (filterType === 'running') {
                document.getElementById('filterRunning').classList.add('active');
                document.getElementById('filterRunning').style.opacity = '1';
            }
            // 存储当前选中的过滤器
            window.currentAppFilter = filterType;
        }
        
        // 将搜索框添加到设备ID下方
          const deviceIdElement = document.getElementById('appListDeviceId');
          if (deviceIdElement && deviceIdElement.nextSibling) {
              appListModal.insertBefore(searchDiv, deviceIdElement.nextSibling);
          } else if (deviceIdElement) {
              deviceIdElement.parentNode.appendChild(searchDiv);
          } else {
              // 如果设备ID元素不存在，退回到添加到模态框开头
              if (appListModal.firstChild) {
                  appListModal.insertBefore(searchDiv, appListModal.firstChild);
              } else {
                  appListModal.appendChild(searchDiv);
              }
          }

          // 现在搜索框已添加到DOM，可以添加事件监听器
          // 添加过滤按钮事件监听
          document.getElementById('filterAll').addEventListener('click', function() {
              setActiveFilter('all');
              getAppList();
          });
          document.getElementById('filterNotRunning').addEventListener('click', function() {
              setActiveFilter('notRunning');
              getAppList();
          });
          document.getElementById('filterRunning').addEventListener('click', function() {
              setActiveFilter('running');
              getAppList();
          });

          // 初始化过滤器状态
          if (!window.currentAppFilter) {
              setActiveFilter('all');
          } else {
              setActiveFilter(window.currentAppFilter);
          }

          // 添加搜索事件监听 - 确保只添加一次
          document.getElementById('appSearchInput').addEventListener('input', function() {
              // 使用防抖来优化性能
              clearTimeout(window.searchTimeout);
              window.searchTimeout = setTimeout(getAppList, 300);
          });
    }
    getAppList();
}

// 启动应用
function startApp(deviceId, packageName) {
    const resultDiv = document.getElementById('appListResult');
    
    resultDiv.innerHTML += '<p>正在启动应用: ' + packageName + '...</p>';
    
    fetch('/android-devices/api/execute-command', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            deviceId: deviceId,
            command: 'monkey -p ' + packageName + ' -c android.intent.category.LAUNCHER 1'
        })
    })
    .then(response => response.text())
    .then(data => {
        resultDiv.innerHTML += '<p>应用启动结果: ' + data + '</p>';
        // 刷新应用列表 - 强制从服务器获取最新状态
        getAppList(true);
    })
    .catch(error => {
        resultDiv.innerHTML += '<p>启动应用时发生错误: ' + error + '</p>';
    });
}

// 停止应用
function stopApp(deviceId, packageName) {
    const resultDiv = document.getElementById('appListResult');
    
    resultDiv.innerHTML += '<p>正在停止应用: ' + packageName + '...</p>';
    
    fetch('/android-devices/api/execute-command', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            deviceId: deviceId,
            command: 'am force-stop ' + packageName
        })
    })
    .then(response => response.text())
    .then(data => {
        resultDiv.innerHTML += '<p>应用停止结果: ' + data + '</p>';
        // 刷新应用列表 - 强制从服务器获取最新状态
        getAppList(true);
    })
    .catch(error => {
        resultDiv.innerHTML += '<p>停止应用时发生错误: ' + error + '</p>';
    });
}

// 缓存应用列表数据
let cachedAppList = null;
let cachedDeviceId = null;

// 获取已安装的应用列表
function getAppList(forceRefresh = false) {
    const deviceId = document.getElementById('appListDeviceId').textContent;
    const resultDiv = document.getElementById('appListResult');
    const searchInput = document.getElementById('appSearchInput');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';

    // 检查是否需要从服务器获取数据
    if (!forceRefresh && cachedAppList && cachedDeviceId === deviceId) {
        // 使用缓存数据进行本地过滤
        filterAndRenderApps(cachedAppList, searchTerm, resultDiv, deviceId);
        return;
    }

    resultDiv.textContent = '正在获取已安装的应用列表...';

    fetch(`/android-devices/api/get-app-list?deviceId=${encodeURIComponent(deviceId)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('获取应用列表失败: ' + response.statusText);
            }
            return response.json();
        })
        .then(data => {
            if (!data || !data.appList || !Array.isArray(data.appList)) {
                throw new Error('应用列表数据格式不正确');
            }

            // 缓存数据
            cachedAppList = data.appList;
            cachedDeviceId = deviceId;

            // 过滤并渲染应用
            filterAndRenderApps(cachedAppList, searchTerm, resultDiv, deviceId);
        })
        .catch(error => {
            resultDiv.textContent = '获取应用列表时发生错误: ' + error.message;
        });
}

// 本地过滤并渲染应用列表
function filterAndRenderApps(apps, searchTerm, resultDiv, deviceId) {
    // 应用搜索过滤
    if (searchTerm) {
        apps = apps.filter(app => 
            app.name.toLowerCase().includes(searchTerm) || 
            app.packageName.toLowerCase().includes(searchTerm)
        );
    }

    // 应用状态过滤
    const filterType = window.currentAppFilter || 'all';
    if (filterType === 'running') {
        apps = apps.filter(app => app.isRunning === true || app.isRunning === 'true');
    } else if (filterType === 'notRunning') {
        apps = apps.filter(app => app.isRunning !== true && app.isRunning !== 'true');
    }

    // 按照包名排序
    apps.sort((a, b) => a.packageName.localeCompare(b.packageName));

    if (apps.length === 0) {
        resultDiv.textContent = '未找到匹配的应用';
        return;
    }

    let html = '<ul style="list-style-type: none; padding: 0; margin: 0;">';
    apps.forEach(app => {
        // 修正运行状态判断逻辑，同时处理字符串和布尔值
        const isRunning = app.isRunning === true || app.isRunning === 'true';
        const status = isRunning ? '运行中' : '未运行';
        const statusClass = isRunning ? 'status-running' : 'status-stopped';
        html += `<li style="padding: 12px 15px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; transition: background-color 0.2s;">`;
        html += `   <div style="flex: 1; min-width: 0; margin-right: 15px;">`;
        html += `       <div style="font-weight: 500; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${app.name}</div>`;
        html += `       <span class="${statusClass}" style="margin-top: 4px; display: inline-block; padding: 3px 8px; border-radius: 12px; font-size: 12px; background-color: ${isRunning ? '#4CAF50' : '#f44336'}; color: white;">${status}</span>`;
        html += `   </div>`;
        if (!isRunning) {
            html += `   <button onclick="startApp('${deviceId}', '${app.packageName}')" style="width: 80px !important; height: 35px !important; padding: 5px 10px; background-color: #2196F3; color: white; border: none; border-radius: 4px; cursor: pointer; text-align: center; box-sizing: border-box; transition: background-color 0.2s;">启动</button>`;
        } else {
            html += `   <button onclick="stopApp('${deviceId}', '${app.packageName}')" style="width: 80px !important; height: 35px !important; padding: 5px 10px; background-color: #f44336; color: white; border: none; border-radius: 4px; cursor: pointer; text-align: center; box-sizing: border-box; transition: background-color 0.2s;">停止</button>`;
        }
        html += `</li>`;
    });
    html += '</ul>';
    resultDiv.innerHTML = html;
}

// 提交上传到当前目录
function submitUploadToCurrentDirectory() {
    const deviceId = document.getElementById('browseDeviceId').textContent;
    const currentDir = document.getElementById('currentDirectory').value;
    const fileInput = document.getElementById('fileToUploadCurrentDir');
    const resultDiv = document.getElementById('browseResult');

    if (!fileInput.files[0]) {
        resultDiv.textContent = '请选择要上传的文件';
        return;
    }

    const formData = new FormData();
    formData.append('deviceId', deviceId);
    formData.append('remotePath', currentDir);
    formData.append('file', fileInput.files[0]);

    resultDiv.textContent = '正在上传文件...';

    fetch('/android-devices/api/upload-file', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(data => {
        resultDiv.textContent = data;
        listDirectory(); // 刷新目录
        cancelUploadToCurrentDirectory();
    })
    .catch(error => {
        resultDiv.textContent = '上传文件时发生错误: ' + error;
    });
}

// 列出目录内容
function listDirectory() {
    const deviceId = document.getElementById('browseDeviceId').textContent;
    let currentDir = document.getElementById('currentDirectory').value;
    const contentsDiv = document.getElementById('directoryContents');
    const resultDiv = document.getElementById('browseResult');

    // 检查路径是否以斜杠结尾，如果不是则添加
    if (currentDir && currentDir !== '/' && !currentDir.endsWith('/')) {
        currentDir += '/';
        document.getElementById('currentDirectory').value = currentDir;
    }

    contentsDiv.innerHTML = '正在加载目录内容...';
    resultDiv.textContent = '';

    // 添加按钮样式
    addButtonStyles();

    fetch(`/android-devices/api/list-directory?deviceId=${encodeURIComponent(deviceId)}&remotePath=${encodeURIComponent(currentDir)}`)
        .then(response => {
            if (!response.ok) {
                // 尝试解析JSON错误响应
                return response.json().then(err => {
                    throw new Error(err.error || `HTTP错误! 状态码: ${response.status}`);
                }).catch(() => {
                    // 如果解析JSON失败，使用默认错误信息
                    throw new Error(`HTTP错误! 状态码: ${response.status}`);
                });
            }
            return response.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                throw new Error('目录数据格式不正确，预期是数组');
            }
            displayDirectoryContents(deviceId, currentDir, data);
        })
        .catch(error => {
            resultDiv.textContent = `加载目录时发生错误: ${error.message}`;
            console.error('目录加载错误详情:', error);
            contentsDiv.innerHTML = '';
        });
}

// 添加按钮样式
function addButtonStyles() {
    // 检查样式是否已存在
    if (document.getElementById('fileBrowserStyles')) return;

    const style = document.createElement('style');
    style.id = 'fileBrowserStyles';
    style.textContent = `
        .action-buttons {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 22px;
        }
        .action-buttons button {
            white-space: nowrap;
            margin: 0 3px;
        }
        .file-permissions {
            width: 110px;
            display: inline-block;
            font-family: monospace;
            white-space: nowrap;
        }
        .file-size {
            width: 80px;
            text-align: right;
            margin-right: 10px;
            white-space: nowrap;
        }
        .file-path {
            flex-grow: 1;
            margin: 0 10px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .file-item > span {
            display: inline-block;
        }
        .action-buttons button:hover {
            background-color: #45a049;
        }
        .file-item {
            padding: 5px;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .file-item:hover {
            background-color: #f5f5f5;
        }
        .directory-item {
            font-weight: bold;
            color: #2196F3;
        }
    `;
    document.head.appendChild(style);
}

// 显示目录内容
function displayDirectoryContents(deviceId, currentDir, items) {
    const contentsDiv = document.getElementById('directoryContents');
    contentsDiv.innerHTML = '';

    // 添加返回上一级按钮（如果不是根目录）
    if (currentDir !== '/') {
        const parentDirItem = document.createElement('div');
        parentDirItem.className = 'file-item directory-item';
        parentDirItem.innerHTML = `
            <span onclick="navigateToParentDirectory('${deviceId}')">.. (返回上一级)</span>
            <div class="action-buttons"></div>
        `;
        contentsDiv.appendChild(parentDirItem);
    }

    // 添加目录和文件项
    items.forEach(item => {
        const itemDiv = document.createElement('div');
        const isDirectory = item.directory;
        itemDiv.className = `file-item ${isDirectory ? 'directory-item' : ''}`;

        // 构建操作按钮
        let actionButtons = '';
        actionButtons = `<button onclick="downloadFile('${deviceId}', '${escapeQuotes(item.path)}')">下载</button>`;
        actionButtons += `<button onclick="deleteFile('${deviceId}', '${escapeQuotes(item.path)}')">删除</button>`;

        // 格式化权限字符串为固定长度
        const formattedPermissions = (item.permissions || 'N/A').padEnd(10, ' ');

        // 不清理路径，直接使用原始路径
        let cleanPath = item.path;

        itemDiv.innerHTML = `
            <span class="file-permissions">${formattedPermissions}</span>
            <span class="file-path" onclick="${isDirectory ? `navigateToDirectory('${deviceId}', '${escapeQuotes(cleanPath)}')` : ''}">${item.name}</span>
            <span class="file-size">${isDirectory ? '-' : item.size || '0'}</span>
            <div class="action-buttons">${actionButtons}</div>
        `;

        // 为文件路径添加悬停样式（如果是目录）
        if (isDirectory) {
            const pathElement = itemDiv.querySelector('.file-path');
            if (pathElement) {
                pathElement.style.cursor = 'pointer';
                pathElement.style.textDecoration = 'underline';
            }
        }
        contentsDiv.appendChild(itemDiv);
    });
}

// 导航到目录
function navigateToDirectory(deviceId, path) {
    document.getElementById('currentDirectory').value = path;
    listDirectory();
}

// 导航到父目录
function navigateToParentDirectory(deviceId) {
    let currentDir = document.getElementById('currentDirectory').value;
    if (currentDir === '/') return;
    if (currentDir.lastIndexOf('/') == currentDir.length - 1) 
        currentDir = currentDir.substring(0, currentDir.length - 1) 
    const parentDir = currentDir.substring(0, currentDir.lastIndexOf('/')) || '/';
    document.getElementById('currentDirectory').value = parentDir;
    listDirectory();
}

// 下载文件
function downloadFile(deviceId, remotePath) {
    const resultDiv = document.getElementById('browseResult');
    resultDiv.textContent = '正在准备下载...';

    fetch(`/android-devices/api/download-file?deviceId=${encodeURIComponent(deviceId)}&remotePath=${encodeURIComponent(remotePath)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`下载失败: HTTP状态码 ${response.status}`);
            }
            return response.blob();
        })
        .then(blob => {
            // 获取文件名
            const fileName = remotePath.split('/').pop() || 'downloaded_file';
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            // 清理
            setTimeout(() => {
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
                resultDiv.textContent = '下载成功';
            }, 0);
        })
        .catch(error => {
            resultDiv.textContent = `下载时发生错误: ${error.message}`;
            console.error('下载错误:', error);
        });
}

// 删除文件
function deleteFile(deviceId, remotePath) {
    if (!confirm(`确定要删除文件: ${remotePath} 吗?`)) return;

    const resultDiv = document.getElementById('browseResult');
    resultDiv.textContent = '正在删除文件...';

    fetch(`/android-devices/api/delete-file?deviceId=${encodeURIComponent(deviceId)}&remotePath=${encodeURIComponent(remotePath)}`, {
        method: 'DELETE'
    })
    .then(response => response.text())
    .then(data => {
        resultDiv.textContent = data;
        listDirectory(); // 刷新目录
    })
    .catch(error => {
        resultDiv.textContent = '删除文件时发生错误: ' + error;
    });
}

// 辅助函数：转义引号
function escapeQuotes(str) {
    if (!str) return '';
    return str.replace(/'/g, '\\\'');
}