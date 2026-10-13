package org.example;

/**
 * Модель данных о текущей погоде.
 *
 * @param city        название города
 * @param country     код страны (например, RU, US, GB)
 * @param temp        текущая температура (°C)
 * @param feelsLike   ощущаемая температура (°C)
 * @param description текстовое описание погодных условий
 * @param humidity    влажность воздуха (%)
 * @param pressure    атмосферное давление (гПа)
 * @param windSpeed   скорость ветра (м/с)
 */
public record WeatherData(
        String city,
        String country,
        double temp,
        double feelsLike,
        String description,
        int humidity,
        int pressure,
        double windSpeed
) {
    /**
     * Конвертирует атмосферное давление из гПа (гектопаскалей) в мм рт. ст.
     * 1 гПа ≈ 0.75006 мм рт. ст.
     */
    public int pressureMmHg() {
        return (int) Math.round(pressure * 0.75006);
    }

    /**
     * Форматирует температуру со знаком '+' для положительных значений.
     */
    public static String formatTemperature(double t) {
        return String.format("%s%.1f °C", (t > 0 ? "+" : ""), t);
    }
}
