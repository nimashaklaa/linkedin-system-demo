# Apache Kafka (KRaft) and Spring Kafka: Under the Hood

This guide explains what Kafka and Spring Kafka do in this repository, how a message moves through the system, and what the current design means in practice. It describes the code and local configuration as they exist today; it is not a claim that the setup is production-ready.

## The short version

- **Apache Kafka** is the distributed event log: it stores records in topics and lets independent applications read them at their own pace.
- **KRaft** is Kafka's built-in metadata and controller consensus mode. It replaces ZooKeeper; it does not replace Kafka's message storage or consumer groups.
- **Spring Kafka** is the Spring integration around Kafka's Java client. It turns Java objects into Kafka records on the producer side and delivers records to annotated listener methods on the consumer side.
- In this project, User and Post services publish events; Feedback updates Redis feeds, Notification sends FCM pushes, and Search writes event data to Elasticsearch.

## The layers

```text
Business service
  KafkaTemplate.send(topic, key, Java object)
        |
        v
Spring Kafka + Kafka Java producer
  serialize key and value; send a Kafka record
        |
        v
Kafka broker
  append record to a partition's log; replicate it as configured
        |
        v
Spring Kafka listener container + Kafka Java consumer
  poll records; deserialize values; invoke @KafkaListener
        |
        v
Application side effect
  Redis feed / Elasticsearch document / Firebase push
```

Kafka does not call another service's method directly. Producers write records to Kafka, and consumers independently poll and process them. A producer can finish its HTTP request without waiting for a consumer to finish its work.

## Kafka fundamentals

### Topics, partitions, records, and keys

A **topic** is a named stream of records, such as `post.created`. A topic is split into **partitions**. Each partition is an append-only, ordered log: records are appended, not updated in place. Kafka retains records according to its retention policy; consuming a record does not remove it.

A record consists primarily of a topic, key, value, timestamp, and headers. The key is useful for routing related records to the same partition. Ordering is guaranteed within a partition, not across an entire topic. If a key consistently maps to one partition, records with that key retain their order there.

This project's producers normally use entity IDs as keys (for example, the user ID for user events and the post ID for post events). Values are Java maps serialized as JSON. Topics are not provisioned explicitly in the Compose file: broker-side auto topic creation is enabled. Partition count, replication factor, and retention are not explicitly set per application topic.

### Brokers, replication, and consumer groups

A **broker** stores partition log data and serves producer and consumer requests. In a multi-broker cluster, a partition can have replicas. One replica is the leader for reads and writes; followers copy its log. Replication improves availability only when there are multiple eligible brokers and appropriate replication settings.

A **consumer group** is a set of consumer instances sharing work for a topic. For each partition, a group assigns processing to one member at a time. Two different groups each get their own view of the stream: for example, the Feedback and Search services can both consume `post.created` independently. Within one group, adding instances can increase parallelism up to the number of partitions.

Each group tracks an **offset**, the position it has reached in each partition. Offsets let a consumer resume after a restart. They do not prove that the application's side effect (such as a Redis write) happened exactly once.

## What KRaft does

Kafka needs cluster metadata: which brokers exist, which topics and partitions exist, and which broker leads each partition. In **KRaft mode**, Kafka's controller nodes manage this metadata through a Raft-based metadata quorum. The metadata log is separate from the user-record logs stored in topic partitions.

KRaft therefore replaces the old ZooKeeper-based way of managing Kafka metadata. It is not a separate message broker and does not change the basic producer/topic/consumer model.

### This repository's KRaft setup

The `kafka` service in `docker-compose.yml` runs `confluentinc/cp-kafka:7.4.0` with:

- `KAFKA_PROCESS_ROLES=broker,controller`: this one process serves client traffic and participates in controller quorum work.
- `KAFKA_NODE_ID=1` and `KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093`: a one-member controller quorum.
- A controller listener at `kafka:9093`, an internal broker listener at `kafka:29092`, and a host listener at `localhost:9092`.
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1` and automatic topic creation enabled.

The Spring applications currently connect from the host using `localhost:9092`. A service running inside the Compose network should use the internal advertised address `kafka:29092`, not `localhost` (which would refer to that service's own container).

This is a convenient **single-node development cluster**, not a highly available cluster. There is no second controller to maintain a quorum and no second broker to hold replicas. A broker outage stops Kafka service, and a single replica cannot protect data from loss. The Compose service also has no named Kafka data volume, so do not treat its local data as durable across container removal/recreation.

## What Spring Kafka does

Spring Boot auto-configuration reads `spring.kafka.*` settings and creates/configures Kafka clients and Spring Kafka components. The project includes `spring-boot-starter-kafka` in its Kafka-using services.

### Producing

`KafkaTemplate` is Spring Kafka's producer API. In this code, services call `kafkaTemplate.send(topic, key, value)`:

1. The application builds a Java `Map` event after its business operation.
2. The configured key serializer converts the string key to bytes.
3. Spring's `JsonSerializer` converts the value to JSON bytes.
4. The Kafka producer batches and sends the record to the broker; the key determines its partition.
5. Kafka appends the record and responds according to the producer's acknowledgment settings.

The current services do not attach a callback or wait on the result of `send`. A log immediately after `send` means the application submitted the send operation; it is not proof that the broker acknowledged it. The database write and Kafka publish are also separate operations, so one can succeed while the other fails.

### Consuming

`@KafkaListener` marks a method as a message handler. Spring Kafka creates listener containers that run Kafka consumers, poll records, deserialize them, and invoke the method with the resulting payload. The configured JSON deserializer maps values to `Map<String, Object>` in Feedback, Notification, and Search services.

The listener container also coordinates partition assignment and offset commits. Commit timing, retries, and recovery depend on container and consumer configuration. This repository does not define an explicit listener error/retry policy, so do not assume failed processing is retried indefinitely or that an external side effect and offset commit are atomic.

## Event flow in this application

| Topic | Producer | Consumer(s) | Effect |
|---|---|---|---|
| `user.created` | User Service registration | Notification Service; Search Service | Welcome push; index user document |
| `user.logged_in` | User Service login | Notification Service | Sign-in push |
| `profile.updated` | User Service profile update | No matching listener currently | No downstream action |
| `connection.requested` | User Service request, and currently also its reject path | Notification Service | Connection-related push |
| `connection.accepted` | User Service acceptance | Notification Service | Acceptance push |
| `post.created` | Post Service post creation | Feedback Service; Search Service | Populate Redis feeds; index post document |
| `post.liked` | Post Service like | Notification Service | Like push |
| `comment.created` | Post Service comment | Notification Service | Comment push |

Two independent consumers of a topic normally use distinct group IDs, so each gets the event. In this repository, Feedback, Notification, and Search configure their own consumer group IDs.

### Example: creating a post

1. Post Service saves the post in MySQL.
2. It builds a JSON event containing `postId`, `authorId`, `content`, `mediaUrl`, and `createdAt`, and submits it to `post.created` with the post ID as key.
3. Feedback Service consumes the event, fetches the author's connections over HTTP, and pushes the post ID into Redis feed lists.
4. Search Service consumes the same topic independently and saves a post search document in Elasticsearch.

This is **eventual consistency**: the HTTP request can return before Redis and Elasticsearch reflect the new post. A consumer outage can delay those projections while Kafka retains the record, subject to retention and cluster health.

## Project perspective: strengths and gaps

### What this design gives you

- **Loose coupling:** Post Service need not know which downstream services build feeds, search indexes, or notifications.
- **Independent work:** Feed, Search, and Notification can process events at different speeds and scale separately.
- **Replayable history:** consumers can resume from offsets or, if retention permits, rebuild derived data by replaying events.
- **A natural fit for projections:** Redis feeds and Elasticsearch documents are derived views of the primary MySQL records.

### What it does not guarantee by itself

- **Atomic database + event writes:** a MySQL commit followed by a failed Kafka send can leave no event; publishing before commit can expose an event for data that later rolls back. The transactional outbox pattern is a common way to close this gap.
- **Exactly-once business effects:** a consumer can perform an external side effect and fail before its offset is committed, so a replay can repeat that effect. Consumers should be idempotent (for example, use stable event IDs and upserts).
- **Automatic recovery policy:** retries, dead-letter handling, and poison-message policy need explicit configuration and operational monitoring.
- **High availability in this Compose topology:** one broker/controller node with replication factor one is not resilient.
- **A stable event contract:** plain JSON maps are flexible but make field names and types easy to drift. Versioned event schemas or typed DTOs help evolve producers and consumers safely.

There are also code-specific details worth knowing:

- `FeedEventConsumer` catches and logs exceptions inside its listener. Since the exception does not escape to the listener container, normal container error handling cannot see that failure as a failed delivery; the event may be treated as processed even when the Redis/HTTP work was incomplete.
- `SearchEvenConsumer` likewise catches processing exceptions. Its `post.created` handler reads `imageUrl`, but Post Service currently publishes the media field as `mediaUrl`, so that field is not populated by this event.
- Producer calls are not awaited, and database transactions are not coordinated with Kafka. The success log statements after `send` are therefore not delivery confirmations.
- Broker listeners use `PLAINTEXT`; this local Compose setup does not configure TLS or client authentication. Do not expose it as-is to an untrusted network.
- Search has event consumers in code, despite older README text saying indexing was not implemented. User profile updates currently publish `profile.updated`, while Search listens for `user.updated`, so the update handler does not receive that producer's event.

## When to think of Kafka vs. HTTP

Use synchronous HTTP when a caller needs an immediate answer from a specific service, such as reading a profile. Use Kafka when the producer should announce that something happened and independent consumers can react asynchronously, such as updating a feed, search index, or notification.

Kafka introduces operational and consistency concerns—broker availability, topic/partition design, serialization compatibility, lag, retries, and replay—so it is not automatically better than an HTTP call. In this project it is most valuable for fan-out and derived data, provided delivery and consumer failure handling are strengthened before production use.
