package net.guneyilmaz0.mongos4k.query

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.conversions.Bson
import java.util.regex.Pattern

/**
 * A fluent query builder for MongoDB queries.
 * Provides a type-safe and intuitive way to build complex MongoDB queries.
 *
 * @author guneyilmaz0
 */
class MongoSQueryBuilder {
    private val filters = mutableListOf<Bson>()
    private val sorts = mutableListOf<Bson>()
    private var skipValue: Int? = null
    private var limitValue: Int? = null
    
    /**
     * Adds an equality filter.
     *
     * @param field The field name.
     * @param value The value to match.
     * @return This query builder instance.
     */
    fun eq(field: String, value: Any?): MongoSQueryBuilder {
        filters.add(Filters.eq(field, value))
        return this
    }
    
    /**
     * Adds a not equals filter.
     *
     * @param field The field name.
     * @param value The value to not match.
     * @return This query builder instance.
     */
    fun ne(field: String, value: Any?): MongoSQueryBuilder {
        filters.add(Filters.ne(field, value))
        return this
    }
    
    /**
     * Adds a greater than filter.
     *
     * @param field The field name.
     * @param value The value to compare.
     * @return This query builder instance.
     */
    fun gt(field: String, value: Any): MongoSQueryBuilder {
        filters.add(Filters.gt(field, value))
        return this
    }
    
    /**
     * Adds a greater than or equal filter.
     *
     * @param field The field name.
     * @param value The value to compare.
     * @return This query builder instance.
     */
    fun gte(field: String, value: Any): MongoSQueryBuilder {
        filters.add(Filters.gte(field, value))
        return this
    }
    
    /**
     * Adds a less than filter.
     *
     * @param field The field name.
     * @param value The value to compare.
     * @return This query builder instance.
     */
    fun lt(field: String, value: Any): MongoSQueryBuilder {
        filters.add(Filters.lt(field, value))
        return this
    }
    
    /**
     * Adds a less than or equal filter.
     *
     * @param field The field name.
     * @param value The value to compare.
     * @return This query builder instance.
     */
    fun lte(field: String, value: Any): MongoSQueryBuilder {
        filters.add(Filters.lte(field, value))
        return this
    }
    
    /**
     * Adds a field exists filter.
     *
     * @param field The field name.
     * @return This query builder instance.
     */
    fun exists(field: String): MongoSQueryBuilder {
        filters.add(Filters.exists(field))
        return this
    }
    
    /**
     * Adds a field not exists filter.
     *
     * @param field The field name.
     * @return This query builder instance.
     */
    fun notExists(field: String): MongoSQueryBuilder {
        filters.add(Filters.exists(field, false))
        return this
    }
    
    /**
     * Adds an "in" filter for multiple values.
     *
     * @param field The field name.
     * @param values The values to match.
     * @return This query builder instance.
     */
    fun `in`(field: String, values: List<Any?>): MongoSQueryBuilder {
        filters.add(Filters.`in`(field, values))
        return this
    }
    
    /**
     * Adds a "not in" filter for multiple values.
     *
     * @param field The field name.
     * @param values The values to not match.
     * @return This query builder instance.
     */
    fun nin(field: String, values: List<Any?>): MongoSQueryBuilder {
        filters.add(Filters.nin(field, values))
        return this
    }
    
    /**
     * Adds a regex filter.
     *
     * @param field The field name.
     * @param pattern The regex pattern.
     * @return This query builder instance.
     */
    fun regex(field: String, pattern: String): MongoSQueryBuilder {
        filters.add(Filters.regex(field, pattern))
        return this
    }
    
    /**
     * Adds a regex filter with options.
     *
     * @param field The field name.
     * @param pattern The regex pattern.
     * @param options The regex options.
     * @return This query builder instance.
     */
    fun regex(field: String, pattern: String, options: String): MongoSQueryBuilder {
        filters.add(Filters.regex(field, pattern, options))
        return this
    }
    
    /**
     * Adds a case-insensitive text search filter.
     *
     * @param field The field name.
     * @param text The text to search for.
     * @return This query builder instance.
     */
    fun containsIgnoreCase(field: String, text: String): MongoSQueryBuilder {
        filters.add(Filters.regex(field, Pattern.quote(text), "i"))
        return this
    }
    
    /**
     * Adds a range filter (between two values).
     *
     * @param field The field name.
     * @param min The minimum value (inclusive).
     * @param max The maximum value (inclusive).
     * @return This query builder instance.
     */
    fun between(field: String, min: Any, max: Any): MongoSQueryBuilder {
        filters.add(Filters.and(Filters.gte(field, min), Filters.lte(field, max)))
        return this
    }
    
    /**
     * Adds an OR condition combining multiple filters.
     *
     * @param builderAction A lambda that configures the OR conditions.
     * @return This query builder instance.
     */
    fun or(builderAction: MongoSQueryBuilder.() -> Unit): MongoSQueryBuilder {
        val orBuilder = MongoSQueryBuilder()
        orBuilder.builderAction()
        if (orBuilder.filters.isNotEmpty()) {
            filters.add(Filters.or(orBuilder.filters))
        }
        return this
    }
    
    /**
     * Adds an AND condition combining multiple filters.
     *
     * @param builderAction A lambda that configures the AND conditions.
     * @return This query builder instance.
     */
    fun and(builderAction: MongoSQueryBuilder.() -> Unit): MongoSQueryBuilder {
        val andBuilder = MongoSQueryBuilder()
        andBuilder.builderAction()
        if (andBuilder.filters.isNotEmpty()) {
            filters.add(Filters.and(andBuilder.filters))
        }
        return this
    }
    
    /**
     * Adds ascending sort order for a field.
     *
     * @param field The field name to sort by.
     * @return This query builder instance.
     */
    fun sortAsc(field: String): MongoSQueryBuilder {
        sorts.add(Sorts.ascending(field))
        return this
    }
    
    /**
     * Adds descending sort order for a field.
     *
     * @param field The field name to sort by.
     * @return This query builder instance.
     */
    fun sortDesc(field: String): MongoSQueryBuilder {
        sorts.add(Sorts.descending(field))
        return this
    }
    
    /**
     * Sets the number of documents to skip.
     *
     * @param skip The number of documents to skip.
     * @return This query builder instance.
     */
    fun skip(skip: Int): MongoSQueryBuilder {
        this.skipValue = skip
        return this
    }
    
    /**
     * Sets the maximum number of documents to return.
     *
     * @param limit The maximum number of documents.
     * @return This query builder instance.
     */
    fun limit(limit: Int): MongoSQueryBuilder {
        this.limitValue = limit
        return this
    }
    
    /**
     * Builds the final query filter.
     *
     * @return The combined filter or null if no filters were added.
     */
    fun buildFilter(): Bson? {
        return when {
            filters.isEmpty() -> null
            filters.size == 1 -> filters[0]
            else -> Filters.and(filters)
        }
    }
    
    /**
     * Builds the final sort specification.
     *
     * @return The combined sort specification or null if no sorts were added.
     */
    fun buildSort(): Bson? {
        return when {
            sorts.isEmpty() -> null
            sorts.size == 1 -> sorts[0]
            else -> Sorts.orderBy(sorts)
        }
    }
    
    /**
     * Gets the skip value.
     *
     * @return The skip value or null if not set.
     */
    fun getSkip(): Int? = skipValue
    
    /**
     * Gets the limit value.
     *
     * @return The limit value or null if not set.
     */
    fun getLimit(): Int? = limitValue
    
    /**
     * Creates a query builder for pagination.
     *
     * @param page The page number (1-based).
     * @param pageSize The number of items per page.
     * @return A new query builder with pagination settings.
     */
    fun paginate(page: Int, pageSize: Int): MongoSQueryBuilder {
        require(page > 0) { "Page number must be greater than 0" }
        require(pageSize > 0) { "Page size must be greater than 0" }
        
        val skipAmount = (page - 1) * pageSize
        return skip(skipAmount).limit(pageSize)
    }
    
    companion object {
        /**
         * Creates a new query builder instance.
         *
         * @return A new MongoSQueryBuilder instance.
         */
        fun create(): MongoSQueryBuilder = MongoSQueryBuilder()
        
        /**
         * Creates a query builder with an initial configuration.
         *
         * @param builderAction A lambda that configures the query builder.
         * @return A configured MongoSQueryBuilder instance.
         */
        fun build(builderAction: MongoSQueryBuilder.() -> Unit): MongoSQueryBuilder {
            val builder = MongoSQueryBuilder()
            builder.builderAction()
            return builder
        }
    }
}