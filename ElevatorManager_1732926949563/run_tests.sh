#!/bin/bash

# 电梯系统测试运行脚本
# 如果Java环境可用，可以使用此脚本运行测试

echo "=== 电梯系统测试运行脚本 ==="
echo

# 检查Java环境
if command -v java &> /dev/null; then
    echo "✅ Java环境已找到"
    java -version
    echo
else
    echo "❌ Java环境未找到"
    echo "请安装Java 8或更高版本后再运行测试"
    exit 1
fi

# 检查Maven环境
if command -v mvn &> /dev/null; then
    echo "✅ Maven环境已找到"
    echo
else
    echo "❌ Maven环境未找到"
    echo "请安装Maven后再运行测试"
    exit 1
fi

# 进入项目目录
cd "$(dirname "$0")"

echo "=== 编译项目 ==="
mvn clean compile

echo "=== 运行测试 ==="
mvn test

echo "=== 生成测试覆盖率报告 ==="
mvn jacoco:report

echo "=== 运行变异测试 ==="
mvn pitest:mutationCoverage

echo "=== 测试完成 ==="
echo "测试报告位于："
echo "- 测试结果：target/surefire-reports/"
echo "- 覆盖率报告：target/site/jacoco/"
echo "- 变异测试报告：target/pit-reports/"