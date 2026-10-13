package org.example;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Точка входа в консольное приложение для просмотра погоды.
 */
public class Main {

    private static final String SEPARATOR = "--------------------------------------------------";
    private static final String BANNER = """
            ==================================================
                       OpenWeatherMap CLI Client
            ==================================================
            Узнавайте актуальную погоду в любом городе мира.
            Поддерживаются названия на русском и английском языках.
            Для выхода введите: 'exit', 'quit' или 'выход'.
            ==================================================""";

    public static void main(String[] args) {
        System.out.println(BANNER);

        WeatherService weatherService = new WeatherService();
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        if (!weatherService.hasApiKey()) {
            System.out.println("[ВНИМАНИЕ] API-ключ OpenWeatherMap не найден в переменных окружения или файле .env.");
            System.out.print("Введите ваш API-ключ OpenWeatherMap (или Enter для выхода): ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String manualKey = scanner.nextLine().trim();
            if (manualKey.isEmpty()) {
                System.out.println("API-ключ не указан. Завершение работы.");
                return;
            }
            weatherService = new WeatherService(manualKey);
            System.out.println("[ИНФО] Ключ установлен (" + weatherService.getMaskedApiKey() + ")");
        } else {
            System.out.println("[ИНФО] Используется API-ключ (" + weatherService.getMaskedApiKey() + ")");
        }

        while (true) {
            System.out.print("\nВведите название города (или 'выход'): ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")
                    || input.equalsIgnoreCase("quit")
                    || input.equalsIgnoreCase("выход")
                    || input.equalsIgnoreCase("q")
                    || input.equals("0")) {
                System.out.println("До свидания! Хорошей погоды!");
                break;
            }

            if (input.isEmpty()) {
                System.out.println("[ПРЕДУПРЕЖДЕНИЕ] Название города не может быть пустым. Попробуйте еще раз.");
                continue;
            }

            try {
                System.out.println("Загрузка данных для: " + input + "...");
                WeatherData weather = weatherService.getWeather(input);
                printWeatherReport(weather);
            } catch (WeatherException e) {
                System.out.println("[ОШИБКА] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[ОШИБКА] Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }

    private static void printWeatherReport(WeatherData w) {
        String location = w.country().isBlank() ? w.city() : String.format("%s, %s", w.city(), w.country());
        String tempFormatted = WeatherData.formatTemperature(w.temp());
        String feelsLikeFormatted = WeatherData.formatTemperature(w.feelsLike());

        System.out.println(SEPARATOR);
        System.out.printf("📍 Город:         %s%n", location);
        System.out.printf("☁️ Погода:        %s%n", capitalize(w.description()));
        System.out.printf("🌡️ Температура:   %s (ощущается как %s)%n", tempFormatted, feelsLikeFormatted);
        System.out.printf("💧 Влажность:     %d%%%n", w.humidity());
        System.out.printf("🧭 Давление:      %d мм рт. ст. (%d гПа)%n", w.pressureMmHg(), w.pressure());
        System.out.printf("💨 Ветер:         %.1f м/с%n", w.windSpeed());
        System.out.println(SEPARATOR);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}