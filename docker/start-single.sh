#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

info()  { printf "${GREEN}[INFO]${NC} %s\n" "$1"; }
warn()  { printf "${YELLOW}[WARN]${NC} %s\n" "$1"; }
error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

check_command() {
    if ! command -v "$1" &>/dev/null; then
        error "未找到命令: $1"
        case "$1" in
            mvn)
                printf "  请安装 Maven: https://maven.apache.org/install.html\n"
                ;;
            pnpm)
                printf "  请安装 pnpm: npm install -g pnpm\n"
                ;;
            node)
                printf "  请安装 Node.js: https://nodejs.org/\n"
                ;;
            docker)
                printf "  请安装 Docker: https://docs.docker.com/get-docker/\n"
                ;;
        esac
        exit 1
    fi
}

check_docker_compose() {
    if docker compose version &>/dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &>/dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        error "未找到 Docker Compose"
        printf "  请安装 Docker Compose: https://docs.docker.com/compose/install/\n"
        exit 1
    fi
}

check_docker_running() {
    if ! docker info &>/dev/null; then
        error "Docker 未运行，请先启动 Docker"
        exit 1
    fi
}

printf "============================================\n"
printf "  Ratel 单机版 Docker 启动脚本\n"
printf "============================================\n"
printf "\n"

info "检查运行环境..."
check_command mvn
check_command node
check_command pnpm
check_command docker
check_docker_compose
check_docker_running

JAVA_VERSION=$(mvn -version 2>/dev/null | head -1 | sed -n 's/.*Java \([0-9]*\).*/\1/p')
if [ -n "$JAVA_VERSION" ] && [ "$JAVA_VERSION" -lt 25 ]; then
    warn "当前 Java 版本为 $JAVA_VERSION，项目要求 Java 25+"
fi

NODE_VERSION=$(node -v 2>/dev/null | sed 's/v//' | cut -d. -f1)
if [ -n "$NODE_VERSION" ] && [ "$NODE_VERSION" -lt 18 ]; then
    warn "当前 Node.js 版本为 $(node -v)，建议使用 18+"
fi

printf "\n"
info "========== 第 1 步：构建后端项目 =========="
cd "$PROJECT_ROOT"

info "[1/4] 安装 project-pom ..."
mvn clean install -f "$PROJECT_ROOT/project-pom/pom.xml"

info "[2/4] 安装 root-pom ..."
mvn clean install -f "$PROJECT_ROOT/root-pom/pom.xml"

info "[3/4] 安装 common 模块 ..."
mvn clean install -DskipTests -f "$PROJECT_ROOT/common/pom.xml"

info "[4/4] 安装 business 模块（含 single 应用）..."
mvn clean install -DskipTests -f "$PROJECT_ROOT/business/pom.xml"

if [ ! -f "$PROJECT_ROOT/business/application/single/gwsu/target/gwsu.jar" ]; then
    error "后端构建失败：未找到 gwsu.jar"
    exit 1
fi
info "后端构建完成"

printf "\n"
info "========== 第 2 步：构建前端项目 =========="
cd "$PROJECT_ROOT/web"
if [ ! -d "node_modules" ]; then
    info "安装前端依赖: pnpm install"
    pnpm install
fi
info "执行前端构建: pnpm build:all"
pnpm build:all

FRONTEND_APPS=("gwsu-main" "sub-system" "sub-security")
for app in "${FRONTEND_APPS[@]}"; do
    if [ ! -d "$PROJECT_ROOT/web/apps/$app/dist" ]; then
        error "前端构建失败：未找到 $app/dist"
        exit 1
    fi
done
info "前端构建完成"

printf "\n"
info "========== 第 3 步：启动 Docker Compose =========="
cd "$PROJECT_ROOT/docker/single"
info "执行: $COMPOSE_CMD up -d --build"
$COMPOSE_CMD up -d --build

printf "\n"
info "============================================"
info "  Ratel 单机版启动完成！"
info "  访问地址: http://localhost:${NGINX_PORT:-80}"
info "============================================"
