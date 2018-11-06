package example.geode.kafka;

import com.google.common.io.Resources;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;


public class KafkaConsumerGeodeLoader {
    public static void main(String[] args) throws IOException {
        // set up house-keeping

        ClientCache clientCache = new ClientCacheFactory()
                .addPoolLocator("localhost", 10334)
                .setPdxSerializer(new ReflectionBasedAutoSerializer("example.geode.kafka.*"))
                .setPdxReadSerialized(false)
                .set("log-level", "warning")
                .create();
        Region<String, Customer> customerRegion = clientCache.<String, Customer>createClientRegionFactory(ClientRegionShortcut.PROXY).create("test");

        // and the consumer
        KafkaConsumer<String, Customer> consumer;
        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            // properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
            properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "your_client_id");
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumer = new KafkaConsumer<>(properties);
        }
        consumer.subscribe(Arrays.asList("test"));
        int timeouts = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            // read records with a short timeout. If we time out, we don't really care.
            ConsumerRecords<String, Customer> records = consumer.poll(1000);
            if (records.count() == 0) {
                timeouts++;
            } else {
                System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
                timeouts = 0;
            }
            for (ConsumerRecord<String, Customer> record : records) {
                switch (record.topic()) {
                    case "test":
                        Customer c = record.value();
                        if (c == null) {
                            throw new IOException("Read null object from kafka.");
                        }
                        System.out.println("Got a new customer from kafka with guid=" + c.getGuid());
                        customerRegion.put(c.getGuid(), c);
                        break;
                    default:
                        throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
                }
            }
        }
    }
}
