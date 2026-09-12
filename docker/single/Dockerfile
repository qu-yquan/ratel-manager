# ================================================
# 第一阶段：获取 JRE
# ================================================
FROM eclipse-temurin:25-jre AS jre-source

# ================================================
# 第二阶段：最终镜像（基于 Playwright 官方镜像，已包含所有浏览器依赖）
# ================================================
FROM mcr.microsoft.com/playwright/java:v1.62.0-noble

# 复制 Java 运行时（替换 Playwright 镜像自带的 JDK）
COPY --from=jre-source /opt/java/openjdk /opt/java/openjdk

# 安装 nginx
RUN apt-get update && apt-get install -y --no-install-recommends \
    nginx \
    && rm -rf /var/lib/apt/lists/*

# 移除 nginx 默认站点配置
RUN rm -f /etc/nginx/sites-enabled/default

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# 告诉 Playwright Java 浏览器路径，跳过运行时下载
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# 创建应用目录
WORKDIR /app

# 复制后端 jar
COPY business/application/single/gwsu/target/gwsu.jar /app/gwsu.jar

# 复制前端静态文件（主应用 + 子应用统一放在 html 目录下）
COPY web/apps/gwsu-main/dist /usr/share/nginx/html
COPY web/apps/gwsu-sub-system/dist /usr/share/nginx/html/sub-system
COPY web/apps/gwsu-sub-security/dist /usr/share/nginx/html/sub-security

# 复制 nginx 配置
COPY docker/nginx/gwsu.conf /etc/nginx/conf.d/default.conf

# 复制启动脚本
COPY docker/single/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 暴露 nginx 端口
EXPOSE 80

ENTRYPOINT ["sh", "/app/entrypoint.sh"]
