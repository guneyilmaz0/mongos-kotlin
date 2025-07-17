package net.guneyilmaz0.mongos4k.aggregation

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Projections
import com.mongodb.client.AggregateIterable
import net.guneyilmaz0.mongos4k.Database
import net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException
import net.guneyilmaz0.mongos4k.validation.MongoSValidator
import org.bson.Document
import org.bson.conversions.Bson

/**
 * Builder class for MongoDB aggregation pipelines.
 * Provides a fluent API for constructing complex aggregation queries.
 *
 * @author guneyilmaz0
 */
class MongoSAggregationBuilder {
    private val stages = mutableListOf<Bson>()
    
    /**
     * Adds a match stage to filter documents.
     *
     * @param filter The filter to apply.
     * @return This builder instance.
     */
    fun match(filter: Bson): MongoSAggregationBuilder {
        stages.add(Aggregates.match(filter))
        return this
    }
    
    /**
     * Adds a match stage with field equality.
     *
     * @param field The field name.
     * @param value The value to match.
     * @return This builder instance.
     */
    fun match(field: String, value: Any): MongoSAggregationBuilder {
        return match(Filters.eq(field, value))
    }
    
    /**
     * Adds a project stage to reshape documents.
     *
     * @param projection The projection to apply.
     * @return This builder instance.
     */
    fun project(projection: Bson): MongoSAggregationBuilder {
        stages.add(Aggregates.project(projection))
        return this
    }
    
    /**
     * Adds a project stage to include specific fields.
     *
     * @param fields The fields to include.
     * @return This builder instance.
     */
    fun projectInclude(vararg fields: String): MongoSAggregationBuilder {
        return project(Projections.include(*fields))
    }
    
    /**
     * Adds a project stage to exclude specific fields.
     *
     * @param fields The fields to exclude.
     * @return This builder instance.
     */
    fun projectExclude(vararg fields: String): MongoSAggregationBuilder {
        return project(Projections.exclude(*fields))
    }
    
    /**
     * Adds a sort stage.
     *
     * @param sort The sort specification.
     * @return This builder instance.
     */
    fun sort(sort: Bson): MongoSAggregationBuilder {
        stages.add(Aggregates.sort(sort))
        return this
    }
    
    /**
     * Adds a sort stage with ascending order.
     *
     * @param field The field to sort by.
     * @return This builder instance.
     */
    fun sortAsc(field: String): MongoSAggregationBuilder {
        return sort(Sorts.ascending(field))
    }
    
    /**
     * Adds a sort stage with descending order.
     *
     * @param field The field to sort by.
     * @return This builder instance.
     */
    fun sortDesc(field: String): MongoSAggregationBuilder {
        return sort(Sorts.descending(field))
    }
    
    /**
     * Adds a limit stage.
     *
     * @param limit The maximum number of documents to return.
     * @return This builder instance.
     */
    fun limit(limit: Int): MongoSAggregationBuilder {
        MongoSValidator.validateLimit(limit)
        stages.add(Aggregates.limit(limit))
        return this
    }
    
    /**
     * Adds a skip stage.
     *
     * @param skip The number of documents to skip.
     * @return This builder instance.
     */
    fun skip(skip: Int): MongoSAggregationBuilder {
        MongoSValidator.validateSkip(skip)
        stages.add(Aggregates.skip(skip))
        return this
    }
    
    /**
     * Adds a group stage.
     *
     * @param id The group key.
     * @param fieldAccumulators The field accumulators.
     * @return This builder instance.
     */
    fun group(id: Any?, fieldAccumulators: List<com.mongodb.client.model.BsonField>): MongoSAggregationBuilder {
        stages.add(Aggregates.group(id, fieldAccumulators))
        return this
    }
    
    /**
     * Adds a group stage with count.
     *
     * @param id The group key.
     * @param countField The field name for the count.
     * @return This builder instance.
     */
    fun groupWithCount(id: Any?, countField: String = "count"): MongoSAggregationBuilder {
        return group(id, listOf(com.mongodb.client.model.Accumulators.sum(countField, 1)))
    }
    
    /**
     * Adds a group stage with sum.
     *
     * @param id The group key.
     * @param sumField The field name for the sum.
     * @param field The field to sum.
     * @return This builder instance.
     */
    fun groupWithSum(id: Any?, sumField: String, field: String): MongoSAggregationBuilder {
        return group(id, listOf(com.mongodb.client.model.Accumulators.sum(sumField, "$field")))
    }
    
    /**
     * Adds a group stage with average.
     *
     * @param id The group key.
     * @param avgField The field name for the average.
     * @param field The field to average.
     * @return This builder instance.
     */
    fun groupWithAvg(id: Any?, avgField: String, field: String): MongoSAggregationBuilder {
        return group(id, listOf(com.mongodb.client.model.Accumulators.avg(avgField, "$field")))
    }
    
    /**
     * Adds a group stage with minimum.
     *
     * @param id The group key.
     * @param minField The field name for the minimum.
     * @param field The field to find minimum.
     * @return This builder instance.
     */
    fun groupWithMin(id: Any?, minField: String, field: String): MongoSAggregationBuilder {
        return group(id, listOf(com.mongodb.client.model.Accumulators.min(minField, "$field")))
    }
    
    /**
     * Adds a group stage with maximum.
     *
     * @param id The group key.
     * @param maxField The field name for the maximum.
     * @param field The field to find maximum.
     * @return This builder instance.
     */
    fun groupWithMax(id: Any?, maxField: String, field: String): MongoSAggregationBuilder {
        return group(id, listOf(com.mongodb.client.model.Accumulators.max(maxField, "$field")))
    }
    
    /**
     * Adds an unwind stage to deconstruct an array field.
     *
     * @param field The array field to unwind.
     * @return This builder instance.
     */
    fun unwind(field: String): MongoSAggregationBuilder {
        stages.add(Aggregates.unwind("$field"))
        return this
    }
    
    /**
     * Adds a lookup stage for left outer join.
     *
     * @param from The collection to join.
     * @param localField The field from the input documents.
     * @param foreignField The field from the documents of the "from" collection.
     * @param as The output array field.
     * @return This builder instance.
     */
    fun lookup(from: String, localField: String, foreignField: String, `as`: String): MongoSAggregationBuilder {
        stages.add(Aggregates.lookup(from, localField, foreignField, `as`))
        return this
    }
    
    /**
     * Adds a custom aggregation stage.
     *
     * @param stage The custom aggregation stage.
     * @return This builder instance.
     */
    fun addStage(stage: Bson): MongoSAggregationBuilder {
        stages.add(stage)
        return this
    }
    
    /**
     * Adds a custom aggregation stage from a document.
     *
     * @param stageDocument The stage document.
     * @return This builder instance.
     */
    fun addStage(stageDocument: Document): MongoSAggregationBuilder {
        stages.add(stageDocument)
        return this
    }
    
    /**
     * Builds the aggregation pipeline.
     *
     * @return The list of aggregation stages.
     */
    fun build(): List<Bson> = stages.toList()
    
    /**
     * Executes the aggregation pipeline on the specified collection.
     *
     * @param database The database instance.
     * @param collection The collection name.
     * @return The aggregation result iterable.
     * @throws MongoSOperationException If the operation fails.
     */
    fun execute(database: Database, collection: String): AggregateIterable<Document> {
        MongoSValidator.validateCollectionName(collection)
        
        return try {
            database.database.getCollection(collection).aggregate(build())
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to execute aggregation pipeline", e)
        }
    }
    
    /**
     * Executes the aggregation pipeline and returns results as a list.
     *
     * @param database The database instance.
     * @param collection The collection name.
     * @return The aggregation results as a list.
     * @throws MongoSOperationException If the operation fails.
     */
    fun executeToList(database: Database, collection: String): List<Document> {
        return try {
            execute(database, collection).toList()
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to execute aggregation pipeline to list", e)
        }
    }
    
    /**
     * Executes the aggregation pipeline and converts results to the specified type.
     *
     * @param T The type to convert results to.
     * @param database The database instance.
     * @param collection The collection name.
     * @return The aggregation results as a list of the specified type.
     * @throws MongoSOperationException If the operation fails.
     */
    inline fun <reified T : Any> executeAs(database: Database, collection: String): List<T> {
        return try {
            executeToList(database, collection).mapNotNull { doc ->
                database.documentToTargetType<T>(doc)
            }
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to execute aggregation pipeline with type conversion", e)
        }
    }
    
    companion object {
        /**
         * Creates a new aggregation builder instance.
         *
         * @return A new MongoSAggregationBuilder instance.
         */
        fun create(): MongoSAggregationBuilder = MongoSAggregationBuilder()
        
        /**
         * Creates an aggregation builder with an initial configuration.
         *
         * @param builderAction A lambda that configures the aggregation builder.
         * @return A configured MongoSAggregationBuilder instance.
         */
        fun build(builderAction: MongoSAggregationBuilder.() -> Unit): MongoSAggregationBuilder {
            val builder = MongoSAggregationBuilder()
            builder.builderAction()
            return builder
        }
    }
}