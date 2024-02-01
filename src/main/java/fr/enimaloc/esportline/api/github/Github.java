package fr.enimaloc.esportline.api.github;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.enimaloc.esportline.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Github {
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();
    public static final Logger LOGGER = LoggerFactory.getLogger(Github.class);
    private static Map<String, Map.Entry<Object, Long>> cached = new HashMap<>();
    private static final Thread cacheThread = new Thread(() -> {
        while (true) {
            try {
                Thread.sleep(1);
                for (String url : cached.keySet()) {
                    cached.put(url, Map.entry(cached.get(url).getKey(), cached.get(url).getValue() - 1));
                    if (cached.get(url).getValue() <= 0) {
                        LOGGER.debug("[CACHE] Removed {} from cache", url);
                    }
                }
                cached.entrySet().removeIf(entry -> entry.getValue().getValue() <= 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    public static LatestGithubRelease getLastRelease(String organization, String repositories) throws IOException {
        ensureThreadStarted();
        LOGGER.debug("Getting last release of {} from {}", repositories, organization);
        URL url = URI.create(Constant.Github.LATEST_RELEASE.formatted(organization, repositories)).toURL();
        LatestGithubRelease release;
        try {
            release = get(url, LatestGithubRelease.class, TimeUnit.MINUTES.toMillis(15));
        } catch (IOException e) {
            String tagEnv = System.getenv("GIT_" + organization + "_" + repositories + "_TAG");
            if (e.getMessage().contains("403") && tagEnv != null) {
                LOGGER.warn("Github API rate limit reached, using env variable GIT_{}_{}_TAG", organization, repositories);
                release = new LatestGithubRelease(null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        -1,
                        null,
                        tagEnv,
                        null,
                        null,
                        null,
                        false,
                        false,
                        null,
                        null,
                        null,
                        new GithubAsset[]{
                                new GithubAsset(
                                        null,
                                        null,
                                        0,
                                        null,
                                        "melee_player_database",
                                        null,
                                        null,
                                        null,
                                        0,
                                        0,
                                        null,
                                        null,
                                        null
                                ),
                                new GithubAsset(
                                        null,
                                        null,
                                        0,
                                        null,
                                        "ultimate_player_database",
                                        null,
                                        null,
                                        null,
                                        0,
                                        0,
                                        null,
                                        null,
                                        null
                                )
                        },
                        null,
                        null,
                        0,
                        null,
                        null);
                cache(url, release, TimeUnit.MINUTES.toMillis(15));
            } else {
                throw e;
            }
        }
        return release;
    }

    private static void ensureThreadStarted() {
        if (!cacheThread.isAlive()) {
            cacheThread.start();
        }
    }

    private static <T> T get(URL url, Class<T> clazz, long cacheExpirationInMillis) throws IOException {
        ensureThreadStarted();
        if (cached.containsKey(url.toString())) {
            T obj = (T) cached.get(url.toString()).getKey();
            LOGGER.debug("[CACHE] Got {} from cache (expires in {}ms)", url, cached.get(url.toString()).getValue());
            return obj;
        }
        T obj = JSON.readValue(url, clazz);
        cache(url, obj, cacheExpirationInMillis);
        LOGGER.debug("Got {} from {}", url, obj);
        return obj;
    }

    private static void cache(URL url, Object obj, long cacheExpirationInMillis) {
        ensureThreadStarted();
        cached.put(url.toString(), Map.entry(obj, cacheExpirationInMillis));
        LOGGER.debug("[CACHE] Added {} to cache (expires in {}ms)", url, cacheExpirationInMillis);
    }
}
