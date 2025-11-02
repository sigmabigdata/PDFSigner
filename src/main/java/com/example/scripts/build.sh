#!/bin/bash

echo "🔨 Сборка NBDsig Application..."

# Создаем директории
mkdir -p target/dist
mkdir -p target/jpackage

# Собираем проект с Maven
echo "📦 Сборка JAR файла..."
mvn clean package -DskipTests

# Проверяем успешность сборки
if [ $? -ne 0 ]; then
    echo "❌ Ошибка сборки Maven!"
    exit 1
fi

# Скачиваем JDK с JavaFX если нужно (раскомментировать если нужно)
# echo "📥 Загрузка JDK с JavaFX..."
# wget -O target/jdk.zip https://download2.gluonhq.com/openjfx/21.0.8/openjfx-21.0.8_osx-x64_bin-sdk.zip
# unzip -q target/jdk.zip -d target/

# Используем jpackage для создания нативного пакета
echo "🎁 Создание нативного пакета с jpackage..."

# Проверяем установлен ли jpackage
if ! command -v jpackage &> /dev/null; then
    echo "❌ jpackage не найден. Установите JDK 14+ с jpackage."
    echo "📥 Скачайте с: https://jdk.java.net/"
    exit 1
fi

# Создаем runtime
echo "🔧 Создание пользовательского runtime..."
jlink \
    --add-modules java.base,java.desktop,java.sql,java.naming,java.management,java.instrument,java.security.jgss,java.xml \
    --output target/runtime \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2

# Создаем приложение с jpackage
jpackage \
    --name "NBDsig" \
    --input target/ \
    --main-jar "nbdsig-1.0-SNAPSHOT-jar-with-dependencies.jar" \
    --main-class com.example.Launcher \
    --runtime-image target/runtime \
    --dest target/dist \
    --type app-image \
    --app-version "2.0.0" \
    --vendor "NBD Company" \
    --copyright "Copyright 2024 NBD" \
    --description "Professional PDF Signing Application" \
    --icon src/main/resources/com/example/images/icon.icns \
    --verbose

if [ $? -eq 0 ]; then
    echo "✅ Сборка завершена успешно!"
    echo "📁 Приложение находится в: target/dist/NBDsig.app"
    echo "🚀 Для запуска: open target/dist/NBDsig.app"
else
    echo "❌ Ошибка при создании пакета!"
    exit 1
fi