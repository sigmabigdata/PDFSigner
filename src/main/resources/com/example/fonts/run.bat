@echo off
setlocal
cd /d "%~dp0"

echo Проверка файлов в текущей папке:
dir /b

echo.
echo Проверка JRE:
if not exist "jre\bin\java.exe" (
  echo ОШИБКА: java.exe не найден!
  dir /s /b "jre\bin\java.exe"
  pause
  exit /b 1
)

echo.
echo Запуск приложения...
"jre\bin\java.exe" -version
"jre\bin\java.exe" -jar "%~dp0*.jar"

if %errorlevel% neq 0 pause