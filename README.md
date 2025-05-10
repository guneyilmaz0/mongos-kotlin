# MongoDB Wrapper

A lightweight and easy-to-use Java wrapper for MongoDB operations.

## Features

- Simple connection setup
- Streamlined document retrieval operations
- Support for basic and complex data types
- Fluent API for common MongoDB operations

## Requirements

- Java 21 or higher
- MongoDB server
- Gradle 8.0+ (for building)

## Installation

### Gradle

Add the repository and dependency to your `build.gradle` file:

```gradle
repositories {
    mavenCentral()
    // Add repository if not published to Maven Central
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.guneyilmaz0:mongos-kotlin:VERSION'
}
```

### Maven

Add the dependency to your `pom.xml` file:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.guneyilmaz0</groupId>
    <artifactId>mongos-kotlin</artifactId>
    <version>VERSION</version>
</dependency>
```

## Quick Start

- Kotlin
```kotlin
    // Initialize and connect to MongoDB
    val db = MongoS("myDatabase")

    // Store data
    db.set("users", "user123", User("John Doe", 25))

    // Retrieve data
    val user: User? = db.get<User>("users", "user123")

    // Check if document exists
    val exists = db.exists("users", "user123")

    // Retrieve a list of objects
    val allUsers: List<User>? = db.getList("users", "userList", User::class.java)
```

## Documentation

### Database Class Methods

| Method                        | Description                                          |
|-------------------------------|------------------------------------------------------|
| `isConnected()`               | Checks if connected to the database                  |
| `get<T>(collection, id)`      | Retrieves a document as a raw object                 |
| `set(collection, id, object)` | Stores an object in the specified collection         |

## Building from Source

```bash
git clone https://github.com/guneyilmaz0/mongos.git
cd mongos
./gradlew build
```

## Testing

Run the test suite with:

```bash
./gradlew test
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

- GitHub: [guneyilmaz0](https://github.com/guneyilmaz0)