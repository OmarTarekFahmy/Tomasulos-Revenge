#!/bin/bash
# Tomasulo Simulator Build and Run Script
# Make sure JAVAFX_HOME environment variable is set

if [ -z "$JAVAFX_HOME" ]; then
    echo "ERROR: JAVAFX_HOME environment variable is not set."
    echo "Please set it to your JavaFX SDK directory, e.g.:"
    echo "  export JAVAFX_HOME=/path/to/javafx-sdk"
    exit 1
fi

echo "Compiling..."
javac -d out --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml src/core/*.java src/parser/*.java src/gui/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Running Tomasulo Simulator..."
java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml -cp out gui.TomasuloApp
