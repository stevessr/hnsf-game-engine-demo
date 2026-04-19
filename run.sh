#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # 无颜色

echo -e "${GREEN}正在检查运行环境...${NC}"

# 1. 检查 Java 是否安装
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未找到 Java 环境。${NC}"
    echo "请安装 JDK 25 或更高版本: https://adoptium.net/"
    exit 1
fi

# 2. 检查 Java 版本 (提取主版本号)
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
# 处理一些 OpenJDK 输出格式
if [ -z "$JAVA_VERSION" ]; then
    JAVA_VERSION=$(java -version 2>&1 | grep "version" | awk '{print $3}' | sed 's/"//g' | cut -d'.' -f1)
fi

echo "当前 Java 版本: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 25 ]; then
    echo -e "${YELLOW}警告: 项目建议使用 Java 25，当前版本为 $JAVA_VERSION。${NC}"
    echo "如果编译失败，请升级 JDK。"
fi

# 3. 检查并修复 Gradle Wrapper 权限与完整性
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo -e "${RED}错误: 缺少 gradle/wrapper/gradle-wrapper.jar。${NC}"
    echo "这可能是由于 .gitignore 忽略了该文件。"
    echo "尝试运行以下命令重新生成 (如果已安装 gradle):"
    echo "  gradle wrapper"
    exit 1
fi

if [ ! -x "./gradlew" ]; then
    echo -e "${YELLOW}正在修复 gradlew 权限...${NC}"
    chmod +x gradlew
fi

# 4. 运行项目
echo -e "${GREEN}正在编译并启动 HNSF Game Engine Demo...${NC}"
./gradlew run

if [ $? -ne 0 ]; then
    echo -e "${RED}启动失败。请检查控制台输出。${NC}"
    exit 1
fi
