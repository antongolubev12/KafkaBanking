import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Banking API Service
 */
public class Application {

    private final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private final String VALID_TOPIC = "valid-transactions";
    private final String SUS_TOPIC = "suspicious-transactions";
    private final String HIGH_VALUE_TOPIC = "highvalue-transactions";

    public static void main(String[] args) {
        IncomingTransactionsReader incomingTransactionsReader = new IncomingTransactionsReader();
        CustomerAddressDatabase customerAddressDatabase = new CustomerAddressDatabase();

        Application kafkaApplication = new Application();
        Producer producer = kafkaApplication.createKafkaProducer(kafkaApplication.BOOTSTRAP_SERVERS);

        try {
            kafkaApplication.processTransactions(incomingTransactionsReader, customerAddressDatabase, producer);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            producer.flush();
            producer.close();
        }
    }

    public void processTransactions(IncomingTransactionsReader incomingTransactionsReader,
                                    CustomerAddressDatabase customerAddressDatabase,
                                    Producer<String, Transaction> kafkaProducer) throws ExecutionException, InterruptedException {

        String key="";
        int count=0;
        // Retrieve the next transaction from the IncomingTransactionsReader
        while (incomingTransactionsReader.hasNext()) {
            String topic = "";
            //Store the incoming transaction
            Transaction t = incomingTransactionsReader.next();
            //Save the user associated with the transaction
            String user = t.getUser();
            System.out.println("User :" + t.getUser());
            System.out.println("Amount : "+t.getAmount());
            String tLocation = t.getTransactionLocation();
            System.out.println("order location: " + t.getTransactionLocation());
            String custAddress = customerAddressDatabase.getUserResidence(user);
            System.out.println("User address : " + custAddress);
            //if address on transaction matches the customers address
            if (custAddress.equalsIgnoreCase(tLocation)) {
                System.out.println("Transaction is valid");
                topic = VALID_TOPIC;
                key="valid-transaction";
            } else {
                System.out.println("Transaction is sus");
                topic = SUS_TOPIC;
                key="suspicious-transaction";
            }

            ProducerRecord<String, Transaction> record = new ProducerRecord<>(topic,key, t);
            RecordMetadata recordMetadata = createKafkaProducer(BOOTSTRAP_SERVERS).send(record).get();

            //if transaction is high value, create another record and send it to the high value topic
            if(t.getAmount() > 1000)
            {
                topic= HIGH_VALUE_TOPIC;
                key="high-value-transaction";
                ProducerRecord<String, Transaction> record2 = new ProducerRecord<>(topic,key, t);
                RecordMetadata recordMetadata2 = createKafkaProducer(BOOTSTRAP_SERVERS).send(record2).get();
            }

            System.out.printf("Record with (key: %s, value: %s), was sent to (partition: %d, offset: %d%n",
                    record.key(), record.value(), recordMetadata.partition(), recordMetadata.offset());
            System.out.println("Sent to : "+topic);
        }
    }


    public Producer<String, Transaction> createKafkaProducer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "transactions-producer");

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //tell kafka how to serialise messages
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Transaction.TransactionSerializer.class.getName());

        return new KafkaProducer<>(properties);
    }

}
