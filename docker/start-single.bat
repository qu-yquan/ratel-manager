@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."

echo ============================================
echo   Ratel 单机版 Docker 启动脚本
echo ============================================
echo.

echo [INFO] 检查运行环境...

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到命令: mvn
    echo   请安装 Maven: https://maven.apache.org/install.html
    exit /b 1
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到命令: node
    echo   请安装 Node.js: https://nodejs.org/
    exit /b 1
)

where pnpm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到命令: pnpm
    echo   请安装 pnpm: npm install -g pnpm
    exit /b 1
)

where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到命令: docker
    echo   请安装 Docker: https://docs.docker.com/get-docker/
    exit /b 1
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker 未运行，请先启动 Docker
    exit /b 1
)

set "COMPOSE_CMD="
docker compose version >nul 2>&1
if %errorlevel% equ 0 (
    set "COMPOSE_CMD=docker compose"
) else (
    where docker-compose >nul 2>&1
    if %errorlevel% equ 0 (
        set "COMPOSE_CMD=docker-compose"
    ) else (
        echo [ERROR] 未找到 Docker Compose
        echo   请安装 Docker Compose: https://docs.docker.com/compose/install/
        exit /b 1
    )
)

echo.
echo [INFO] ========== 第 1 步：构建后端项目 ==========

echo [INFO] [1/4] 安装 project-pom ...
call mvn clean install -f "%PROJECT_ROOT%\project-pom\pom.xml"
if %errorlevel% neq 0 (
    echo [ERROR] project-pom 安装失败
    exit /b 1
)

echo [INFO] [2/4] 安装 root-pom ...
call mvn clean install -f "%PROJECT_ROOT%\root-pom\pom.xml"
if %errorlevel% neq 0 (
    echo [ERROR] root-pom 安装失败
    exit /b 1
)

echo [INFO] [3/4] 安装 common 模块 ...
call mvn clean install -DskipTests -f "%PROJECT_ROOT%\common\pom.xml"
if %errorlevel% neq 0 (
    echo [ERROR] common 模块安装失败
    exit /b 1
)

echo [INFO] [4/4] 安装 business 模块（含 single 应用）...
call mvn clean install -DskipTests -f "%PROJECT_ROOT%\business\pom.xml"
if %errorlevel% neq 0 (
    echo [ERROR] business 模块安装失败
    exit /b 1
)

if not exist "%PROJECT_ROOT%\business\application\single\gwsu\target\gwsu.jar" (
    echo [ERROR] 后端构建失败：未找到 gwsu.jar
    exit /b 1
)
echo [INFO] 后端构建完成

echo.
echo [INFO] ========== 第 2 步：构建前端项目 ==========
cd /d "%PROJECT_ROOT%\web"
if not exist "node_modules" (
    echo [INFO] 安装前端依赖: pnpm install
    call pnpm install
    if %errorlevel% neq 0 (
        echo [ERROR] 前端依赖安装失败
        exit /b 1
    )
)
echo [INFO] 执行前端构建: pnpm build:all
call pnpm build:all
if %errorlevel% neq 0 (
    echo [ERROR] 前端构建失败
    exit /b 1
)

set "FRONTEND_OK=1"
if not exist "%PROJECT_ROOT%\web\apps\gwsu-main\dist" set "FRONTEND_OK=0"
if not exist "%PROJECT_ROOT%\web\apps\sub-system\dist" set "FRONTEND_OK=0"
if not exist "%PROJECT_ROOT%\web\apps\sub-security\dist" set "FRONTEND_OK=0"
if %FRONTEND_OK% equ 0 (
    echo [ERROR] 前端构建失败：未找到部分 dist 目录
    exit /b 1
)
echo [INFO] 前端构建完成

echo.
echo [INFO] ========== 第 3 步：启动 Docker Compose ==========
cd /d "%PROJECT_ROOT%\docker\single"
echo [INFO] 执行: %COMPOSE_CMD% up -d --build
call %COMPOSE_CMD% up -d --build
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose 启动失败
    exit /b 1
)

echo.
echo [INFO] ============================================
echo [INFO]   Ratel 单机版启动完成！
echo [INFO]   访问地址: http://localhost:80
echo [INFO] ============================================

endlocal
