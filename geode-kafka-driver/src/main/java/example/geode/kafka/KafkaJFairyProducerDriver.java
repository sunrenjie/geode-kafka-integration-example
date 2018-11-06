package example.geode.kafka;

import com.google.common.io.Resources;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class KafkaJFairyProducerDriver {
    public static void main(String[] args) throws IOException {
        // set up the producer
        KafkaProducer<String, Customer> producer;
        try (InputStream props = Resources.getResource("producer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            properties.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, KafkaJFairyProducerDriver.class.getName());
            // TODO figure out what's relevant about the class loader
            //get the current context class loader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(StringSerializer.class.getClassLoader());
            producer = new KafkaProducer<>(properties);
            producer.initTransactions();
            //restore the class loader
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        Fairy fairy = Fairy.create();
        try {
            producer.beginTransaction();
            for (int i = 0; i < 10; i++) {
                Person person = fairy.person();
                Customer customer = Customer.builder()
                        .firstName(person.getFirstName())
                        .middleName(person.getMiddleName())
                        .lastName(person.getLastName())
                        .email(person.getEmail())
                        .username(person.getUsername())
                        .passportNumber(person.getPassportNumber())
                        .password(person.getPassword())
                        .telephoneNumber(person.getTelephoneNumber())
                        .dateOfBirth(person.getDateOfBirth().toString())
                        .age(person.getAge())
                        .companyEmail(person.getCompanyEmail())
                        .nationalIdentificationNumber(person.getNationalIdentificationNumber())
                        .nationalIdentityCardNumber(person.getNationalIdentityCardNumber())
                        .passportNumber(person.getPassportNumber())
                        .guid(UUID.randomUUID().toString()).build();

                System.out.println("Will send to kafka new customer.getGuid() = " + customer.getGuid());
                producer.send(new ProducerRecord<String, Customer>("test", customer.getGuid(), customer));
            }
            producer.commitTransaction();
        } catch (Throwable throwable) {
            producer.abortTransaction();
            System.out.printf("%s", throwable.getStackTrace());
        } finally {
            producer.close();
        }
    }
}
