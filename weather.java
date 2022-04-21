import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration; 
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLParameters;

public class WeatherManager {

    public static void main(String[] args) {
        WeatherFetcher weatherFetcher = new WeatherFetcher();
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(weatherFetcher, 0, 1, TimeUnit.SECONDS);
    }

}

class WeatherFetcher implements Runnable {

    private static final Pattern p = Pattern.compile("\"the_temp\":([0-9]+)\\.([0-9]+)");

    private String temperature = null;

    public void run() {
        long startTime = System.currentTimeMillis();
        System.out.println(getTemperature());
        System.out.println("Getting temperature took " + (System.currentTimeMillis() - startTime) + " milliseconds");
    }

    public String getTemperature() {
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .executor(Executors.newFixedThreadPool(3))
                    .followRedirects(Redirect.NEVER)
                    .priority(2)
                    .proxy(ProxySelector.getDefault())
                    .version(Version.HTTP_2)
                    .sslParameters(new SSLParameters())
                    .build();
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.metaweather.com/api/location/44418/"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, BodyHandlers.ofString());
            return  parseOutTemperature(response.body());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String parseOutTemperature(String json) {
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1) + "." + m.group(2);
        }
        return "unknown";
    }
}
