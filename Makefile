SHELL := /bin/bash

PROJECT_NAME := primary_software
GRADLEW := ./gradlew

# 默认使用本机已安装的 GraalVM 25；可在命令行覆盖：
#   make GRAALVM_HOME=/path/to/graalvm package
GRAALVM_HOME ?= /usr/lib/jvm/java-25-graalvm

NATIVE_DIR := build/native/nativeCompile
NATIVE_BIN := $(NATIVE_DIR)/$(PROJECT_NAME)
NATIVE_PACKAGE_DIR := build/native/package/$(PROJECT_NAME)-native-linux-x86_64
NATIVE_ZIP := build/distributions/$(PROJECT_NAME)-native-linux-x86_64.zip

GRAALVM_ENV = JAVA_HOME=$(GRAALVM_HOME) PATH=$(GRAALVM_HOME)/bin:$(PATH)

.PHONY: help clean test build shadow native native-compile package native-package run-native

help:
	@echo "Targets:"
	@echo "  make test           - 运行测试"
	@echo "  make build          - 完整构建（测试 + Shadow JAR + native）"
	@echo "  make shadow         - 生成 Shadow JAR / 分发包"
	@echo "  make native         - 编译 GraalVM 原生 binary"
	@echo "  make package        - 打包原生 binary 为 zip"
	@echo "  make run-native     - 直接运行原生 binary"
	@echo "  make clean          - 清理构建产物"

test:
	$(GRAALVM_ENV) $(GRADLEW) test

shadow:
	$(GRADLEW) shadowJar shadowDistZip

native-compile:
	$(GRAALVM_ENV) $(GRADLEW) --no-daemon nativeCompile

native:
	$(MAKE) native-compile

native-package: native-compile
	rm -rf $(NATIVE_PACKAGE_DIR) $(NATIVE_ZIP)
	mkdir -p $(NATIVE_PACKAGE_DIR)
	cp $(NATIVE_DIR)/$(PROJECT_NAME) $(NATIVE_DIR)/lib*.so $(NATIVE_PACKAGE_DIR)/
	cd build/native/package && zip -qr ../../distributions/$(PROJECT_NAME)-native-linux-x86_64.zip $(PROJECT_NAME)-native-linux-x86_64

package: native-package

run-native: native-compile
	cd $(NATIVE_DIR) && LD_LIBRARY_PATH=. ./$(PROJECT_NAME)

build: test shadow native-package

clean:
	$(GRADLEW) clean
	rm -rf build/native/package/$(PROJECT_NAME)-native-linux-x86_64
	rm -f $(NATIVE_ZIP)
