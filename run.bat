@echo off
REM Tomasulo Simulator Build and Run Script
REM Make sure JAVAFX_HOME environment variable is set

if "%JAVAFX_HOME%"=="" (
    echo ERROR: JAVAFX_HOME environment variable is not set.
    echo Please set it to your JavaFX SDK directory, e.g.:
    echo   set JAVAFX_HOME=C:\path\to\javafx-sdk
    pause
    exit /b 1
)

echo Compiling...
javac -d out --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml src\core\*.java src\parser\*.java src\gui\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Running Tomasulo Simulator...
java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml -cp out gui.TomasuloApp

pause
