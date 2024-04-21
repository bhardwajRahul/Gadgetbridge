package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import android.location.Location;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;

public class WeatherHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherHandler.class);

    // These get requested on connection at most every 5 minutes
    public static Object handleWeatherRequest(final String path, final Map<String, String> query) {
        final WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();

        if (weatherSpec == null) {
            LOG.warn("No weather in weather instance");
            return null;
        }

        switch (path) {
            case "/weather/v2/forecast/day": {
                final int lat = getQueryNum(query, "lat", 0);
                final int lon = getQueryNum(query, "lon", 0);
                final int duration = getQueryNum(query, "duration", 5);
                final String tempUnit = getQueryString(query, "tempUnit", "CELSIUS");
                final String provider = getQueryString(query, "provider", "dci");
                final List<WeatherForecastDay> ret = new ArrayList<>(duration);
                final GregorianCalendar date = new GregorianCalendar();
                date.setTime(new Date(weatherSpec.timestamp * 1000L));
                for (int i = 0; i < Math.min(duration, weatherSpec.forecasts.size()); i++) {
                    date.add(Calendar.DAY_OF_MONTH, 1);
                    ret.add(new WeatherForecastDay(date, weatherSpec.forecasts.get(i)));
                }
                return ret;
            }
            case "/weather/v2/forecast/hour": {
                final int lat = getQueryNum(query, "lat", 0);
                final int lon = getQueryNum(query, "lon", 0);
                final int duration = getQueryNum(query, "duration", 13);
                final String speedUnit = getQueryString(query, "speedUnit", "METERS_PER_SECOND");
                final String tempUnit = getQueryString(query, "tempUnit", "CELSIUS");
                final String provider = getQueryString(query, "provider", "dci");
                final String timesOfInterest = getQueryString(query, "timesOfInterest", "");
                final List<WeatherForecastHour> ret = new ArrayList<>(duration);
                for (int i = 0; i < Math.min(duration, weatherSpec.hourly.size()); i++) {
                    ret.add(new WeatherForecastHour(weatherSpec.hourly.get(i)));
                }
                return ret;
            }
            case "/weather/v2/current": {
                final int lat = getQueryNum(query, "lat", 0);
                final int lon = getQueryNum(query, "lon", 0);
                final String tempUnit = getQueryString(query, "tempUnit", "CELSIUS");
                final String speedUnit = getQueryString(query, "speedUnit", "METERS_PER_SECOND");
                final String provider = getQueryString(query, "provider", "dci");
                return new WeatherForecastCurrent(weatherSpec);
            }
        }

        LOG.warn("Unknown weather path {}", path);

        return null;
    }

    private static int getQueryNum(final Map<String, String> query, final String key, final int defaultValue) {
        final String str = query.get(key);
        if (str != null) {
            return Integer.parseInt(str);
        } else {
            return defaultValue;
        }
    }

    private static String getQueryString(final Map<String, String> query, final String key, final String defaultValue) {
        final String str = query.get(key);
        if (str != null) {
            return str;
        } else {
            return defaultValue;
        }
    }

    public static class WeatherForecastDay {
        public int dayOfWeek; // 1 monday .. 7 sunday
        public String description;
        public String summary;
        public WeatherValue high;
        public WeatherValue low;
        public Integer precipProb;
        public Integer icon;
        public Integer epochSunrise;
        public Integer epochSunset;
        public Wind wind;
        public Integer humidity;

        public WeatherForecastDay(final GregorianCalendar date, final WeatherSpec.Daily dailyForecast) {
            dayOfWeek = BLETypeConversions.dayOfWeekToRawBytes(date);
            description = "Unknown"; // TODO from conditionCode
            summary = "Unknown"; // TODO from conditionCode
            high = new WeatherValue(dailyForecast.maxTemp - 273f, "CELSIUS");
            low = new WeatherValue(dailyForecast.minTemp - 273f, "CELSIUS");
            precipProb = dailyForecast.precipProbability;
            icon = mapToCmfCondition(dailyForecast.conditionCode);

            if (dailyForecast.sunRise != 0 && dailyForecast.sunSet != 0) {
                epochSunrise = dailyForecast.sunRise;
                epochSunset = dailyForecast.sunSet;
            } else {
                final Location lastKnownLocation = new CurrentPosition().getLastKnownLocation();

                final GregorianCalendar[] sunriseTransitSet = SPA.calculateSunriseTransitSet(
                        date,
                        lastKnownLocation.getLatitude(),
                        lastKnownLocation.getLongitude(),
                        DeltaT.estimate(date)
                );

                epochSunrise = (int) (sunriseTransitSet[0].getTime().getTime() / 1000);
                epochSunset = (int) (sunriseTransitSet[2].getTime().getTime() / 1000);
            }

            wind = new Wind(new WeatherValue(dailyForecast.windSpeed * 3.6, "METERS_PER_SECOND"), dailyForecast.windDirection);
            humidity = dailyForecast.humidity;
        }
    }

    public static class WeatherForecastHour {
        public int epochSeconds;
        public String description;
        public WeatherValue temp;
        public Integer precipProb;
        public Wind wind;
        public Integer icon;
        public WeatherValue dewPoint;
        public Float uvIndex;
        public Integer relativeHumidity;
        public WeatherValue feelsLikeTemperature;
        public WeatherValue visibility;
        public WeatherValue pressure;
        public Object airQuality;
        public Integer cloudCover;

        public WeatherForecastHour(final WeatherSpec.Hourly hourlyForecast) {
            epochSeconds = hourlyForecast.timestamp;
            description = "Unknown"; // TODO from conditionCode
            temp = new WeatherValue(hourlyForecast.temp - 273f, "CELSIUS");
            precipProb = hourlyForecast.precipProbability;
            wind = new Wind(new WeatherValue(hourlyForecast.windSpeed * 3.6, "METERS_PER_SECOND"), hourlyForecast.windDirection);
            icon = mapToCmfCondition(hourlyForecast.conditionCode);
            //dewPoint = new WeatherValue(hourlyForecast.temp - 273f, "CELSIUS"); // TODO dewPoint
            uvIndex = hourlyForecast.uvIndex;
            relativeHumidity = hourlyForecast.humidity;
            //feelsLikeTemperature = new WeatherValue(hourlyForecast.temp - 273f, "CELSIUS"); // TODO feelsLikeTemperature
            //visibility = new WeatherValue(0, "METER"); // TODO visibility
            //pressure = new WeatherValue(0f, "INCHES_OF_MERCURY"); // TODO pressure
            //airQuality = null; // TODO airQuality
            //cloudCover = 0; // TODO cloudCover
        }
    }

    public static class WeatherForecastCurrent {
        public Integer epochSeconds;
        public WeatherValue temperature;
        public String description;
        public Integer icon;
        public WeatherValue feelsLikeTemperature;
        public WeatherValue dewPoint;
        public Integer relativeHumidity;
        public Wind wind;
        public String locationName;
        public WeatherValue visibility;
        public WeatherValue pressure;
        public WeatherValue pressureChange;

        public WeatherForecastCurrent(final WeatherSpec weatherSpec) {
            epochSeconds = weatherSpec.timestamp;
            temperature = new WeatherValue(weatherSpec.currentTemp - 273f, "CELSIUS");
            description = weatherSpec.currentCondition;
            icon = mapToCmfCondition(weatherSpec.currentConditionCode);
            feelsLikeTemperature = new WeatherValue(weatherSpec.currentTemp - 273f, "CELSIUS");
            dewPoint = new WeatherValue(weatherSpec.dewPoint - 273f, "CELSIUS");
            relativeHumidity = weatherSpec.currentHumidity;
            wind = new Wind(new WeatherValue(weatherSpec.windSpeed * 3.6, "METERS_PER_SECOND"), weatherSpec.windDirection);
            locationName = weatherSpec.location;
            visibility = new WeatherValue(weatherSpec.visibility, "METER");
            pressure = new WeatherValue(weatherSpec.pressure * 0.02953, "INCHES_OF_MERCURY");
            pressureChange = new WeatherValue(0f, "INCHES_OF_MERCURY");
        }
    }

    public static class WeatherValue {
        public Number value;
        public String units;

        public WeatherValue(final Number value, final String units) {
            this.value = value;
            this.units = units;
        }
    }

    public static class Wind {
        public WeatherValue speed;
        public String directionString; // NW
        public Integer direction;

        public Wind(final WeatherValue speed, final int direction) {
            this.speed = speed;
            this.direction = direction;
        }
    }

    public static int mapToCmfCondition(int openWeatherMapCondition) {
        // Icons mapped from a Venu 3:
        // 0 1 2 unk
        // 3 4 5 6 sunny
        // 7 8 9 10 sun cloudy
        // 11 12 cloudy with dashes below
        // 13 14 sun cloud 2 clouds
        // 15 16 clouds
        // 17 rain
        // 18 19 20 21 rain with sun (or night at night?)
        // 22 rain
        // 23 24 unk
        // 25 26 thunder with rain and sun behind
        // 27 thunder with rain
        // 28 29 rain
        // 30 31 32 33 34 snow with clouds
        // 35 36 37 snowflake
        // 38 snow with clouds, with big flake
        // 39 snow with rain
        // 40 41 snow with rain
        // 42 43 44 rain with snow
        // 45 rain with snow
        // 46 wind
        // 47 48 foggy (dashes?)
        // 49 50 51 unk

        switch (openWeatherMapCondition) {
        //Group 2xx: Thunderstorm
            case 210:  //light thunderstorm::  //11d
            case 200:  //thunderstorm with light rain:  //11d
            case 201:  //thunderstorm with rain:  //11d
            case 202:  //thunderstorm with heavy rain:  //11d
            case 230:  //thunderstorm with light drizzle:  //11d
            case 231:  //thunderstorm with drizzle:  //11d
            case 232:  //thunderstorm with heavy drizzle:  //11d
            case 211:  //thunderstorm:  //11d
            case 212:  //heavy thunderstorm:  //11d
            case 221:  //ragged thunderstorm:  //11d
                return 27;

        //Group 90x: Extreme
            case 901:  //tropical storm
        //Group 7xx: Atmosphere
            case 781:  //tornado:  //[[file:50d.png]]
        //Group 90x: Extreme
            case 900:  //tornado
        // Group 7xx: Atmosphere
            case 771:  //squalls:  //[[file:50d.png]]
        //Group 9xx: Additional
            case 960:  //storm
            case 961:  //violent storm
            case 902:  //hurricane
            case 962:  //hurricane
                return 46;

        //Group 3xx: Drizzle
            case 300:  //light intensity drizzle:  //09d
            case 301:  //drizzle:  //09d
            case 302:  //heavy intensity drizzle:  //09d
            case 310:  //light intensity drizzle rain:  //09d
            case 311:  //drizzle rain:  //09d
            case 312:  //heavy intensity drizzle rain:  //09d
            case 313:  //shower rain and drizzle:  //09d
            case 314:  //heavy shower rain and drizzle:  //09d
            case 321:  //shower drizzle:  //09d
        //Group 5xx: Rain
            case 500:  //light rain:  //10d
            case 501:  //moderate rain:  //10d
            case 502:  //heavy intensity rain:  //10d
            case 503:  //very heavy rain:  //10d
            case 504:  //extreme rain:  //10d
            case 520:  //light intensity shower rain:  //09d
            case 521:  //shower rain:  //09d
            case 522:  //heavy intensity shower rain:  //09d
            case 531:  //ragged shower rain:  //09d
                return 17;

        //Group 90x: Extreme
            case 906:  //hail
            case 615:  //light rain and snow:  //[[file:13d.png]]
            case 616:  //rain and snow:  //[[file:13d.png]]
            case 511:  //freezing rain:  //13d
                return 40;

        //Group 6xx: Snow
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //shower sleet:  //[[file:13d.png]]
        //Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
            case 601:  //snow:  //[[file:13d.png]]
        //Group 6xx: Snow
            case 602:  //heavy snow:  //[[file:13d.png]]
        //Group 6xx: Snow
            case 620:  //light shower snow:  //[[file:13d.png]]
            case 621:  //shower snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return 38;


        //Group 7xx: Atmosphere
            case 701:  //mist:  //[[file:50d.png]]
            case 711:  //smoke:  //[[file:50d.png]]
            case 721:  //haze:  //[[file:50d.png]]
            case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
            case 741:  //fog:  //[[file:50d.png]]
            case 751:  //sand:  //[[file:50d.png]]
            case 761:  //dust:  //[[file:50d.png]]
            case 762:  //volcanic ash:  //[[file:50d.png]]
                return 47;

        //Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return 5;

        //Group 90x: Extreme
            case 904:  //hot
                return 5;

        //Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                return 8;
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                return 15;

        //Group 80x: Clouds
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return 15;

        //Group 9xx: Additional
            case 905:  //windy
            case 951:  //calm
            case 952:  //light breeze
            case 953:  //gentle breeze
            case 954:  //moderate breeze
            case 955:  //fresh breeze
            case 956:  //strong breeze
            case 957:  //high windcase  near gale
            case 958:  //gale
            case 959:  //severe gale
                return 46;

            default:
        //Group 90x: Extreme
            case 903:  //cold
                return 35;
        }
    }
}