Objective
===================

This project demonstrates a working example of cluster sharding. 

## Cluster sharding
 
When a group of stateful actors (typically persistent actors with durable state) acting together, consume resources that exceed the capacity of a single machine, cluster sharding set-up is employed. 
In this set-up, actors are distributed across several nodes in the cluster, where they're interacted using their logical identifier. 

### Terms 

* Each actor with an identifier (i.e. `entity-id`) is termed as an `entity`.

* A `Shard` is a group of entities that are managed together. 

* A cluster singleton actor named `ShardCoordinator` designates a `ShardRegion` actor as the owner of a shard. The `ShardRegion` actor is started on each node in the cluster, or group of nodes tagged with a specific role.  

* The designated `ShardRegion` actor then creates a `Shard supervisor` child actor to manage the shard. 

* Individual `Entities` are created by the `Shard supervisor` actor, when needed. Incoming messages to a target `entity` are routed via its `ShardRegion` actor and `Shard supervisor`.

### Message transmission 

* Messages are sent to an entity via a `ShardRegion` actor; which knows how to route a message with an `entity-id` to its final destination. In other words, the `ShardRegion` actor facilitates one to send messages to an entity, without any knowledge its location.

* `ShardRegion` extracts **_entity identifier_** and **_shard identifier_** from incoming messages. 

#### Resolution of a shard's location
 
* For the very first message destined to a specific shard (indicated by a shard identifier), the `ShardRegion` actor resolves the location of the shard, with the help of central coordinator i.e. `ShardCoordinator` if the shard's location is unknown.
  - While resolving the location of a shard, all incoming messages for that shard are buffered. They are delivered later upon successful resolution of destined shard's location.

* Subsequent messages to a resolved shard are delivered directly to its target destination, without any involvement of the `ShardCoordinator`.

* If the shard identifier indicated in the incoming message is owned by another `ShardRegion`, the incoming message is forwarded to that `ShardRegion` instance.

### Warning!

Do not use `Cluster Sharding` together with `Automatic Downing`, as it may cause a cluster to split up into two. Once split, multiple shards and entities are started in each separate cluster.

