@echo off
setlocal enabledelayedexpansion

:: 设置控制台为 UTF-8，以支持中文
chcp 65001 > nul

echo 正在检查运行环境...

:: 1. 检查 Java 是否安装
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [错误] 未找到 Java 环境。
    echo 请安装 JDK 25 或更高版本: https://adoptium.net/
    pause
    exit /b 1
)

:: 2. 检查 Java 版本
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "full_version=%%i"
    set "full_version=!full_version:"=!"
    for /f "tokens=1 delims=." %%a in ("!full_version!") do set "major_version=%%a"
)

echo 当前 Java 版本: !major_version!

if !major_version! LSS 25 (
    echo [警告] 项目建议使用 Java 25，当前版本为 !major_version!。
    echo 如果编译失败，请升级 JDK。
)

:: 3. 检查 gradlew.bat 和 Wrapper 完整性
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [错误] 缺少 gradle\wrapper\gradle-wrapper.jar。
    echo 尝试在本地安装 gradle 后运行 "gradle wrapper" 重新生成。
    pause
    exit /b 1
)

if not exist "gradlew.bat" (
    echo [错误] 未找到 gradlew.bat，请确保脚本在项目根目录下运行。
    pause
    exit /b 1
)

:: 4. 运行项目
echo 正在编译并启动 HNSF Game Engine Demo...
call gradlew.bat run

if %ERRORLEVEL% neq 0 (
    echo [失败] 启动失败。请检查控制台输出。
    pause
)

endlocal
