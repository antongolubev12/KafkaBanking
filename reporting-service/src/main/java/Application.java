import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Application {
    private final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private final String TOPIC = "suspicious-transactions";
    private final String TOPIC2= "valid-transactions";

    public static void main(String[] args) {
        Application kafkaApplication = new Application();

        String consumerGroup = "reporting";

        if (args.length == 1) {
            consumerGroup = args[0];

        }
        System.out.println("Group: "+consumerGroup);

        Consumer<String, Transaction> consumer= kafkaApplication.createKafkaConsumer(kafkaApplication.BOOTSTRAP_SERVERS, consumerGroup);

        kafkaApplication.consumeMessages(kafkaApplication.TOPIC, kafkaApplication.TOPIC2, consumer);
    }

    public static void consumeMessages(String topic1, String topic2, Consumer<String, Transaction> kafkaConsumer) {
        //Subscribe to two topics
        kafkaConsumer.subscribe(Arrays.asList(topic1,topic2));

        while (true) {
            ConsumerRecords<String, Transaction> record = kafkaConsumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, Transaction> records : record) {
                sendUserNotification(records.key(),records.value());
            }
            kafkaConsumer.commitAsync();
        }
    }

    public static Consumer<String, Transaction> createKafkaConsumer(String bootstrapServers, String consumerGroup) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Transaction.TransactionDeserializer.class.getName());

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        KafkaConsumer<String, Transaction> kafkaConsumer = new KafkaConsumer<>(properties);
        return kafkaConsumer;
    }

    private static void sendUserNotification(String key,Transaction transaction) {
        //print whether the transaction is valid/suspicious based on the key
        System.out.println(transaction.toString());
        if(key.equalsIgnoreCase("valid-transaction"))
        {
            System.out.println("Transaction is valid");
        }
        else{
            System.out.println("Transaction is suspicious");
        }

    }

}
