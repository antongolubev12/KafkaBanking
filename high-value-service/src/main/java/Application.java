import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Application {
    private final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private final String TOPIC = "highvalue-transactions";

    public static void main(String[] args) {
        Application kafkaApplication = new Application();

        String consumerGroup = "highValue";
        //If a command-line argument is passed in when running this application, then use it as the value of consumerGroup, otherwise use a default value
        if (args.length == 1) {
            consumerGroup = args[0];

        }
        System.out.println("Group: "+consumerGroup);

        Consumer<String, Transaction> consumer= kafkaApplication.createKafkaConsumer(kafkaApplication.BOOTSTRAP_SERVERS, consumerGroup);

        kafkaApplication.consumeMessages(kafkaApplication.TOPIC, consumer);
    }

    public static void consumeMessages(String topic, Consumer<String, Transaction> kafkaConsumer) {
        //Collections.singletonList turns our single topic into a list
        kafkaConsumer.subscribe(Collections.singletonList(topic));

        //infinite loop
        while (true) {
            //retrieves messages from the Kafka topic
            ConsumerRecords<String, Transaction> record = kafkaConsumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, Transaction>records : record) {
                approveTransaction(records.value());
            }
            kafkaConsumer.commitAsync();
        }
    }

    public static Consumer<String, Transaction> createKafkaConsumer(String bootstrapServers, String consumerGroup) {
        Properties properties = new Properties();

        //Bootstrap servers used to connect to the Kafka cluster
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        //The class to use when deserializing message keys
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Transaction.TransactionDeserializer.class.getName());

        //The name of the consumer group this consumer is a part of
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);

        //manually notify Kafka when it has successfully processed a message
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        KafkaConsumer<String, Transaction> kafkaConsumer = new KafkaConsumer<>(properties);
        return kafkaConsumer;
    }

    private static void approveTransaction(Transaction transaction) {
        // Print transaction information to the console
        System.out.println(transaction.toString());
    }

}
