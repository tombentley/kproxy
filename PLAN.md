# Why is Record Manipulation so hard?

## Unhelpful APIs

There is a lot of structure in batches of records, lots of invariants to be maintained, otherwise broker or client will barf when they see those batches.
Sequence numbers, control records, old-style wrapper records, compression and so on.
Batches are a central concept, but you can't change much about a batch without also making corresponding changes to the records, if you want to maintain the semantics on brokerss/clients.
`MemoryRecords` is  the API we've inherited from Apache Kafka for manipulating records. but it's difficult API to use.
The `RecordStream` is the abstraction we have written on top of `MemoryRecords` to try to make it safer to transform records, but it's still difficult to use.
We did try to have something more like `java.util.stream.Stream`, but we couldn't make it work with the constraints we have.

## Needing schemas adds a lot of complication!

Next: Even the _simple_ version of the computation is non-trivial. 
In order to deserialize Avro or Proto record data we need the schemas.
Strongest simplifying assumption is to just assume that `topic => schema` is known from configuration. 
The next strongest assumption is that `topic => schema id` is known from configuration: we can resolve those ids from a schema registry at start up. 
But both of those assumptions fail in the real world:

1. Topics can contain records with different schemas
2. If you have to configure all this in the proxy then you'll need to bounce your proxy whenever a producer starts publishing with a new schema. 
   In practice that's often going to be hard-to-impossible to coordinate in an enterprise.

So the actually useful pattern is:

1. Allow the filter to figure out the schema ids of records by traversing the batch(es) containing those records. 
2. And then fetch those from the registry.

That requires remote service request on the hot path => more complexity in the form of errors, timeouts etc.

## Two pass computation

It also suggests a two pass computation: We need the first pass to get the schema ids which we need to resolve, and then a second pass to actually do the transform using those schemas once they've been resolved.

A further complication: As demonstrated by RecordEncryption, there is not actually anything super-special about schemas. There are other "related entities" which might be needed in order to perform the transformations. For decryption it involves obtaining encrypted DEKs from records, and getting a KMS to decrypt them, so we have the keys to decrypt the records in the second pass. 

## Three pass computation

And there are use cases which require something even more complex. 
Consider wanting to do encryption where the KEK id is selected from some property of a record's Avro-encoded value. 
For example, suppose the record value contains a property that is the `user_id`, and each user's data is supposed to be encrypted 
with their own KEK id: We need the schema to get the `user_id`, and then we can get the KEK id, and only then can we encrypt.

We would need three passes:

1. A pass to get the schema id from the record key => Fetch that schema from the registry
2. A pass to read the Avro-encoded record value, to extract the property value and determine the KEK ID => Fetch a (DEK, EDEK)-pair from the KMS using that KEK
3. A pass to use the DEK to encrypt the record value. 

## One pass computation?

In the above we were persusing a model where:
1. We pessimistically perform two passes, 
2. We try not to special case schemas. 

That's necessary if all your "transformations" are sync, and you're trying to put all the async operations (the lookups of schema ids, or KEK ids) in the services, so that the final pass can be fast. 

But in general we are anyway reliant on almost always having cached all the things we're looking up, in order to amortize the cost of making high latency requests to external services. 
Without that the throughput of records transiting the filter is limited by the throughput of the external services we depend one.

However, performing multiple passes imposes its own costs: We either materialze the decompressed data (~ x4 more space needed), or even the deserialized data (even more heap needed!)

There is a different way, if we allow transformations with async results. 
In the empty-cache case this will be painfully slow, as we visit each record:
1. obtain the schema id
2. lookup the schema
3. deserialize
4. get the property and figure out the KEK Id
5. get the DEK-EDEK pair and encrypt
6. yield the ciphertext.

However, if we assume a ~stationary set of KEK ids and schemas then eventually all the async calls are hitting the cache and we're actually almost always able to do the computation without needing to use any external service at all. 

This could actually be much more space efficient, as we traverse once, and only need ~2x memory + 1 deserialized record on the heap, and for a shorter time than the multiple-pass computation.

It assumes all the transforms share the same cache.
