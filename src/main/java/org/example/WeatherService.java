package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Сервис для взаимодействия с OpenWeatherMap API.
 */
public class WeatherService {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String apiKey;
    private final HttpClient httpClient;

    public WeatherService() {
        this(resolveApiKey(), HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build());
    }

    public WeatherService(String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build());
    }

    public WeatherService(String apiKey, HttpClient httpClient) {
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : "";
        this.httpClient = httpClient;
    }

    /**
     * Определяет ключ API: переменная окружения -> системное свойство -> файл .env.
     */
    private static String resolveApiKey() {
        String envKey = System.getenv("OPENWEATHER_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }

        String propKey = System.getProperty("openweather.api.key");
        if (propKey != null && !propKey.isBlank()) {
            return propKey.trim();
        }

        return readApiKeyFromDotEnv();
    }

    /**
     * Считывает ключ из локального файла .env (если он существует).
     */
    private static String readApiKeyFromDotEnv() {
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("OPENWEATHER_API_KEY=")) {
                        String value = line.substring("OPENWEATHER_API_KEY=".length()).trim();
                        if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1).trim();
                        }
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Игнорируем ошибки чтения .env, fallback на пустую строку
            }
        }
        return "";
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    public String getMaskedApiKey() {
        if (!hasApiKey()) {
            return "не задан";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Формирует полный URL для запроса погоды по названию города.
     */
    public String buildUrl(String city) {
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
        return String.format("%s?q=%s&appid=%s&units=metric&lang=ru", BASE_URL, encodedCity, apiKey);
    }

    /**
     * Получает текущую погоду для указанного города.
     *
     * @param city название города (на русском или английском)
     * @return модель {@link WeatherData} с информацией о погоде
     * @throws WeatherException если город не найден, ключ недействителен или произошла сетевая ошибка
     */
    public WeatherData getWeather(String city) throws WeatherException {
        if (!hasApiKey()) {
            throw new WeatherException("API-ключ OpenWeatherMap не задан. Установите переменную OPENWEATHER_API_KEY или добавьте ключ в файл .env.");
        }

        if (city == null || city.isBlank()) {
            throw new WeatherException("Название города не может быть пустым.");
        }

        String url = buildUrl(city);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .header("Accept", "application/json")
                    .build();
        } catch (IllegalArgumentException e) {
            throw new WeatherException("Некорректный запрос для города: " + city, e);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WeatherException("Ошибка сети при обращении к OpenWeatherMap. Проверьте интернет-соединение.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherException("Запрос был прерван.", e);
        }

        int statusCode = response.statusCode();
        String body = response.body();

        if (statusCode == 200) {
            return parseJsonResponse(body);
        } else if (statusCode == 404) {
            throw new WeatherException(String.format("Город '%s' не найден. Пожалуйста, проверьте правильность написания.", city.trim()));
        } else if (statusCode == 401) {
            throw new WeatherException("Неверный API-ключ OpenWeatherMap. Проверьте правильность ключа.");
        } else if (statusCode == 429) {
            throw new WeatherException("Превышен лимит запросов к OpenWeatherMap API. Попробуйте позже.");
        } else {
            String errorDetail = extractErrorMessage(body);
            throw new WeatherException(String.format("Ошибка сервиса OpenWeatherMap (код %d): %s", statusCode, errorDetail));
        }
    }

    /**
     * Разбирает JSON-ответ от OpenWeatherMap API в модель {@link WeatherData}.
     */
    public WeatherData parseJsonResponse(String jsonString) throws WeatherException {
        try {
            JSONObject json = new JSONObject(jsonString);

            String city = json.getString("name");

            JSONObject sys = json.optJSONObject("sys");
            String country = (sys != null) ? sys.optString("country", "") : "";

            JSONObject main = json.getJSONObject("main");
            double temp = main.getDouble("temp");
            double feelsLike = main.optDouble("feels_like", temp);
            int humidity = main.getInt("humidity");
            int pressure = main.getInt("pressure");

            JSONArray weatherArray = json.getJSONArray("weather");
            String description = "Неизвестно";
            if (!weatherArray.isEmpty()) {
                description = weatherArray.getJSONObject(0).getString("description");
            }

            JSONObject wind = json.optJSONObject("wind");
            double windSpeed = (wind != null) ? wind.optDouble("speed", 0.0) : 0.0;

            return new WeatherData(city, country, temp, feelsLike, description, humidity, pressure, windSpeed);
        } catch (JSONException e) {
            throw new WeatherException("Не удалось разобрать ответ от сервера погоды.", e);
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("message")) {
                    return json.getString("message");
                }
            } catch (JSONException ignored) {
                // Если не JSON, вернем тело с ограничением длины
            }
            if (responseBody.length() > 100) {
                return responseBody.substring(0, 100) + "...";
            }
            return responseBody;
        }
        return "Неизвестная ошибка";
    }
}
