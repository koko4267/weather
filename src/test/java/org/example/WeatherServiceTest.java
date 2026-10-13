package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    private WeatherService service;

    @BeforeEach
    void setUp() {
        service = new WeatherService("test-api-key");
    }

    @Test
    @DisplayName("Корректно формирует URL с кодированием кириллицы")
    void testBuildUrlWithCyrillic() {
        String url = service.buildUrl("Санкт-Петербург");
        assertTrue(url.startsWith("https://api.openweathermap.org/data/2.5/weather"));
        assertTrue(url.contains("appid=test-api-key"));
        assertTrue(url.contains("units=metric"));
        assertTrue(url.contains("lang=ru"));
        assertTrue(url.contains("q=%D0%A1%D0%B0%D0%BD%D0%BA%D1%82-%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3"));
    }

    @Test
    @DisplayName("Корректно формирует URL с городами, содержащими пробелы")
    void testBuildUrlWithSpaces() {
        String url = service.buildUrl("New York");
        assertTrue(url.contains("q=New+York"));
    }

    @Test
    @DisplayName("Выбрасывает исключение при пустом названии города")
    void testGetWeatherEmptyCity() {
        assertThrows(WeatherException.class, () -> service.getWeather("   "));
        assertThrows(WeatherException.class, () -> service.getWeather(null));
    }

    @Test
    @DisplayName("Корректно парсит валидный JSON ответ OpenWeatherMap")
    void testParseJsonResponse() throws WeatherException {
        String sampleJson = """
                {
                  "name": "Москва",
                  "sys": { "country": "RU" },
                  "main": {
                    "temp": 15.6,
                    "feels_like": 14.8,
                    "humidity": 65,
                    "pressure": 1013
                  },
                  "weather": [
                    { "description": "ясно" }
                  ],
                  "wind": { "speed": 4.2 }
                }
                """;

        WeatherData data = service.parseJsonResponse(sampleJson);

        assertNotNull(data);
        assertEquals("Москва", data.city());
        assertEquals("RU", data.country());
        assertEquals(15.6, data.temp(), 0.01);
        assertEquals(14.8, data.feelsLike(), 0.01);
        assertEquals(65, data.humidity());
        assertEquals(1013, data.pressure());
        assertEquals("ясно", data.description());
        assertEquals(4.2, data.windSpeed(), 0.01);

        // Проверка перевода давления в мм рт. ст. (1013 * 0.75006 ≈ 760)
        assertEquals(760, data.pressureMmHg());
        assertEquals("+15.6 °C", WeatherData.formatTemperature(data.temp()));
    }

    @Test
    @DisplayName("Выбрасывает WeatherException при поврежденном JSON")
    void testParseInvalidJson() {
        assertThrows(WeatherException.class, () -> service.parseJsonResponse("{ invalid-json }"));
    }
}
