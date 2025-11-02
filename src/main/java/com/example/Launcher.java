package com.example;

public class Launcher {
    public static void main(String[] args) {
        // Убедимся, что JavaFX правильно инициализируется
        try {
            MainApp.main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Ошибка запуска приложения: " + e.getMessage());
        }
    }
}