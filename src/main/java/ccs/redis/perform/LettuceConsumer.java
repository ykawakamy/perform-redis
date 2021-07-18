package ccs.redis.perform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ccs.perform.util.SequencialPerformCounter;
import ccs.perform.util.PerformHistogram;
import ccs.perform.util.PerformSnapshot;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class LettuceConsumer {
    /** ロガー */
    private static final Logger log = LoggerFactory.getLogger(LettuceConsumer.class);

    public static void main(String[] args) {
        String topic = System.getProperty("ccs.perform.topic", "test3");
        String groupid = System.getProperty("ccs.perform.groupid", "defaultgroup");
        String key = System.getProperty("ccs.perform.key", "defaultkey");
        long loop_ns = 5_000_000_000L; // ns = 5s
        int iter = Integer.valueOf(System.getProperty("ccs.perform.iterate", "20"));

        RedisClient redisClient = RedisClient.create("redis://localhost:6379/0");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String, String> redis = connection.sync();

        PerformHistogram hist = new PerformHistogram();
        hist.addShutdownHook();
        LatencyMeasurePingDeserializer serializer = new LatencyMeasurePingDeserializer();
        try {
            // トピックを指定してメッセージを送信する

            SequencialPerformCounter pc = new SequencialPerformCounter();
            for( int i=0 ; i != iter ; i++ ) {
                long st = System.nanoTime();
                long et = 0;

                while( (et = System.nanoTime()) - st < loop_ns) {
                    String data = redis.get(key);
                    if (data != null) {
                        LatencyMeasurePing ping = serializer.deserializeBase64("", data);
                        pc.perform(ping.getSeq());
                        long latency = ping.getLatency();
                        pc.addLatency(latency);
                        hist.increament(latency);
                    }
                }

                PerformSnapshot snap = pc.reset();
                snap.print(log, et-st);
            }
        } catch( Throwable th ) {
            th.printStackTrace();
        } finally {
            connection.close();
        }
    }

    private static Integer getSeq(LatencyMeasurePing value, int i) {
        return value.getSeq();
    }

    private static Integer getSeq(Integer value, int i) {
        return value;
    }

    private static Integer getSeq(String value, int i) {
        try {
            return Integer.parseInt(value);
        }catch (Exception e) {
            log.warn("parseError:{}",value);
            return i;
        }
    }
}
