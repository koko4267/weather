package org.example;

/**
 * Исключение, возникающее при ошибках получения или обработки данных погоды.
 */
public class WeatherException extends Exception {
    public WeatherException(String message) {
        super(message);
    }

    public WeatherException(String message, Throwable cause) {
        super(message, cause);
    }
}
