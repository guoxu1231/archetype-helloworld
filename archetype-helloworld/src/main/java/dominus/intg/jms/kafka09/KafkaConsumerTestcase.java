package dominus.intg.jms.kafka09;


import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class KafkaConsumerTestcase extends KafkaZBaseTestCase {

    Producer producer;
    Consumer<String, String> consumer;
    final int MESSAGE_COUNT = 1000;

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();
        int partitionCount = Integer.valueOf(properties.getProperty("kafka.test.topic.partition"));
        this.createTestTopic(testTopicName);
        producer = this.createDefaultProducer(null);
        //prepare message
        produceTestMessage(producer, testTopicName, MESSAGE_COUNT);
        assertEquals(MESSAGE_COUNT, sumPartitionOffset(brokerList, testTopicName));
    }

    @Override
    protected void doTearDown() throws Exception {
        producer.close();
        this.deleteTestTopic(testTopicName);
        consumer.close();
        super.doTearDown();
    }


    /**
     * https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+SimpleConsumer+Example
     * EE: Consumer message from leader broker, You must keep track of the offsets in your application to know where you left off consuming.
     * (SimpleConsumer does not require zookeeper to work)
     */
    @Ignore
    public void test08SimpleConsumer() throws UnsupportedEncodingException, InterruptedException, ExecutionException, TimeoutException {

        int kafkaPort = Integer.valueOf(properties.getProperty("kafka.seed.broker.port"));
        int partitionCount = Integer.valueOf(properties.getProperty("kafka.test.topic.partition"));
        final int testMsgCount = 5;

        SimpleConsumer consumer = new SimpleConsumer(properties.getProperty("kafka.seed.broker"), kafkaPort, zkConnectionTimeout, 64 * KB, "leaderLookup");
        List<String> topics = Collections.singletonList(testTopicName);
        TopicMetadataRequest req = new TopicMetadataRequest(topics);
        TopicMetadataResponse resp = consumer.send(req);
        //EE: find TopicMetadata from seed kafka server
        List<TopicMetadata> metaData = resp.topicsMetadata();
        assertTrue(metaData.size() == 1);
        List<PartitionMetadata> partitionList = metaData.get(0).partitionsMetadata();
        assertTrue(partitionList.size() == partitionCount);
        for (PartitionMetadata partition : partitionList) {
            println(ANSI_RED, partition);
            String leadBroker = partition.leader().host();
            String clientName = "Client_" + testTopicName + "_" + partition.partitionId();
            SimpleConsumer partitionConsumer = new SimpleConsumer(leadBroker, kafkaPort, zkConnectionTimeout, 64 * KB, clientName);
            TopicAndPartition topicAndPartition = new TopicAndPartition(testTopicName, partition.partitionId());
            Map<TopicAndPartition, PartitionOffsetRequestInfo> leastRequestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
            leastRequestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.EarliestTime(), 1));
            Map<TopicAndPartition, PartitionOffsetRequestInfo> latestRequestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
            latestRequestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.LatestTime(), Integer.MAX_VALUE));
            //EE: build OffsetRequest and send it to leadBroker
            OffsetRequest leastOffsetRequest = new OffsetRequest(leastRequestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
            OffsetRequest latestOffsetRequest = new OffsetRequest(latestRequestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
            OffsetResponse leastResponse = partitionConsumer.getOffsetsBefore(leastOffsetRequest);
            OffsetResponse latestResponse = partitionConsumer.getOffsetsBefore(latestOffsetRequest);
            assertFalse("Error fetching data Offset Data the Broker. Reason: " + leastResponse.errorCode(testTopicName, partition.partitionId()), leastResponse.hasError());
            assertFalse("Error fetching data Offset Data the Broker. Reason: " + latestResponse.errorCode(testTopicName, partition.partitionId()), latestResponse.hasError());
            long leastOffset = leastResponse.offsets(testTopicName, partition.partitionId())[0];
            long latestOffset = latestResponse.offsets(testTopicName, partition.partitionId())[0];
//            assertEquals(MESSAGE_COUNT, latestOffset);
            printf(ANSI_RED, "partition [%d] least offset is %d  latest offset is %d\n", partition.partitionId(), leastOffset, latestOffset);

            kafka.api.FetchRequest fetchRequest = new FetchRequestBuilder()
                    .clientId(clientName)
                    .addFetch(testTopicName, partition.partitionId(), leastOffset, 100000) // Note: this fetchSize of 100000 might need to be increased if large batches are written to Kafka
                    .build();
            FetchResponse fetchResponse = partitionConsumer.fetch(fetchRequest);
            //EE: TODO find new leader when met error
            assertFalse("Error fetching data from the Broker:" + leadBroker + " Reason: " + fetchResponse.errorCode(testTopicName, partition.partitionId()), fetchResponse.hasError());

            //EE: read message from least offset
            long readOffset = leastOffset;
            long numRead = 0;
            for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(testTopicName, partition.partitionId())) {
                ByteBuffer payload = messageAndOffset.message().payload();
                byte[] bytes = new byte[payload.limit()];
                payload.get(bytes);
                println(ANSI_YELLOW, String.valueOf(messageAndOffset.offset()) + ": " + new String(bytes, "UTF-8"));
                numRead++;
            }
            assertEquals("fetch message count does not match test message count", MESSAGE_COUNT, numRead);
            if (partitionConsumer != null) partitionConsumer.close();
        }
        if (consumer != null) consumer.close();
    }


    @Test
    public void testSimpleConsumer() {

        consumer = this.createDefaultConsumer(testTopicName, null, true);

        long count = 0;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                out.println(record);
                count++;
            }
            consumer.commitSync();
            if (count == MESSAGE_COUNT) break;
        }
        assertEquals(MESSAGE_COUNT, count);
    }

    @Test
    public void testCommitByRecord() {

        consumer = this.createDefaultConsumer(testTopicName, null, true);

        int pollingTime = 0;
        int[] offsetLog = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
            pollingTime++;
            out.printf("[%d th], polling size:%d\n", pollingTime, records.count());

            //EE:only consume first record of partitions
            for (TopicPartition partition : records.partitions()) {
                List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                out.println(partition + "-" + partitionRecords.size());
                for (ConsumerRecord<String, String> record : partitionRecords) {
                    out.printf("processed record - %s\n", record);
                    consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(record.offset() + 1)));
                    //EE: consumer side seek
                    consumer.seek(partition, record.offset() + 1);
                    break;
                }
            }
            for (TopicPartition par : records.partitions())
                offsetLog[par.partition()] = offsetLog[par.partition()] + 1;

            if (sum(offsetLog) == MESSAGE_COUNT)
                break;
        }
        assertEquals(MESSAGE_COUNT, sum(offsetLog));
    }

    @Test
    public void testMessageOrderInPartition() {
        consumer = this.createDefaultConsumer(testTopicName, null, false);
        //EE:only assign partition 0 to consumer
        consumer.assign(Collections.singletonList(new TopicPartition(testTopicName, 0)));

        int expectedOffset = 0;
        while (true) {
            ConsumerRecords records = consumer.poll(pollTimeout);
            Iterator<ConsumerRecord> iterator = records.iterator();
            while (iterator.hasNext()) {
                final ConsumerRecord record = iterator.next();
                out.println(record);
                //EE: offset from 0 to largest
                assertEquals(expectedOffset++, record.offset());
                if (expectedOffset == testMessageMap.get(0).size())
                    return;
            }
        }
    }

    /**
     * If assign to multiple partition, can not guarantee the partition seek works!
     * Because each polling may fetch records from other partitions.
     */
    @Test
    public void testRandomSeekInPartition() {
        consumer = this.createDefaultConsumer(testTopicName, null, false);
        //EE:only assign partition 0 to consumer
        consumer.assign(Collections.singletonList(new TopicPartition(testTopicName, 0)));

        for (int i = 0; i < 10; i++) {
            KafkaTestMessage expectedMsg = testMessageMap.get(0).get(random.nextInt(testMessageMap.get(0).size()));
            consumer.seek(new TopicPartition(testTopicName, 0), expectedMsg.medadata.offset());
            //EE:random seek to partition first record
            ConsumerRecords<?, ?> records = consumer.poll(pollTimeout);
            out.printf("polling records count:%d\n", records.count());
            ConsumerRecord record = records.isEmpty() ? null : records.iterator().next();

            if (record != null) {
                out.printf("Expected Message:(%s,%s), %s\n", expectedMsg.medadata.partition(), expectedMsg.medadata.offset(), expectedMsg.message);
                out.printf("Actual   Message:(%s,%s), %s\n", record.partition(), record.offset(), record);
                assertEquals(expectedMsg.message.key(), record.key());
                assertEquals(expectedMsg.message.value(), record.value());
            }
        }
    }
}
