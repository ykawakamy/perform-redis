package ccs.redis.perform;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

public class LettuceAsyncProducer {
    private static final Logger log = LoggerFactory.getLogger(LettuceAsyncProducer.class);

    // ----- static methods -------------------------------------------------

    public static void main(String[] asArgs) {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisAsyncCommands<String, String> redis = connection.async();

        String key = System.getProperty("ccs.perform.key", "defaultkey");
        long loop_ns = 5_000_000_000L; // ns = 5s
        int iter = Integer.valueOf(System.getProperty("ccs.perform.iterate", "20"));

        SecureRandom rand = new SecureRandom();

        LatencyMeasurePingSerializer serializer = new LatencyMeasurePingSerializer();
        try {
            int seq = 0;
            for( int i=0 ; i != iter ; i++ ) {
                int cnt =0;
                long st = System.nanoTime();
                long et = 0;
                while( (et = System.nanoTime()) - st < loop_ns) {
                    redis.set(key, serializer.serializeBase64(null, new LatencyMeasurePing(seq)));
                    seq++;
                    cnt++;
                }

                log.info("{}: {} ns. {} times. {} ns/op", key, et-st, cnt, (et-st)/(double)cnt);
            }
        }catch(Throwable th) {
            log.error("occ exception.", th);

        }finally {
            connection.close();
        }

    }
}
