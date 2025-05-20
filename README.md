# Finance Tracker by Group78

A comprehensive personal finance management application built with Java and Swing.

## Features

- 📊 **Dashboard**: Get a quick overview of your financial status
- 💰 **Transaction Management**: Track income and expenses with customizable categories
- 📅 **Budget Planning**: Set and monitor category-based budgets
- 📝 **Bill Management**: Never miss a payment deadline
- 📈 **Data Visualization**: View your financial data through interactive charts
- 🧠 **AI-Powered Analysis**: Receive intelligent financial insights and recommendations
- 📱 **Reports**: Generate comprehensive financial reports with just a click
- 📤 **Data Import/Export**: Support for CSV and Excel formats

## Requirements

- Java 17 or higher
- 4GB RAM (minimum)
- 100MB free disk space
- Internet connection (for AI features)

## Installation

### Option 1: Pre-built JAR

1. Download the latest JAR file from the releases page
2. Double-click the JAR file to run, or use:
   ```
   java -jar finance-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

### Option 2: Build from Source

1. Clone the repository:
   ```
   git clone https://github.com/Tostar1019QAQ/Software-Engineering-Group78.git
   cd finance-tracker
   ```

2. Build with Maven:
   ```
   mvn clean package
   ```

3. Run the application:
   ```
   java -jar target/finance-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## Configuration

### DeepSeek API Key

For AI-powered features, you'll need a DeepSeek API key:

1. Register at [DeepSeek API](https://deepseek.com)
2. Generate an API key
3. In the application, go to the AI tab and enter your API key in the settings section

### Data Directory

By default, the application stores data in the following locations:

- **Windows**: `%USERPROFILE%\FinanceTracker\data`
- **macOS/Linux**: `$HOME/FinanceTracker/data`

Reports are saved to the `output` folder in the application directory.

## Testing

The project uses JUnit 5 for unit testing and follows Test-Driven Development (TDD) principles.

### Test-Driven Development Approach

Our development process follows the TDD cycle:

1. **Write a failing test** - Define the expected behavior before implementation
2. **Make the test pass** - Write the minimum code needed to pass the test
3. **Refactor** - Improve the code while ensuring tests still pass

### Test Structure

The test suite consists of multiple test classes:

- **ReportGeneratorTest** - Tests for report generation functionality
- **TransactionServiceTest** - Tests for transaction management
- **AIServiceTest** - Tests for AI-powered analysis features

### Running Tests

To run the test suite:

```
mvn test
```

### Test Coverage

To generate test coverage reports:

```
mvn jacoco:report
```

The coverage report will be available at `target/site/jacoco/index.html`.

## Development

### Project Structure

```
finance-tracker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── group78/
│   │   │           └── financetracker/
│   │   │               ├── model/       # Data models
│   │   │               ├── service/     # Business logic
│   │   │               ├── ui/          # UI components
│   │   │               └── util/        # Utility classes
│   │   └── resources/  # Application resources
│   └── test/           # Test classes
└── pom.xml            # Maven configuration
```

### Building and Testing

To build the project:
```
mvn clean package
```

To run tests:
```
mvn test
```

To generate JavaDocs:
```
mvn javadoc:javadoc
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## Acknowledgments

- [FlatLaf](https://www.formdev.com/flatlaf/) - Modern Swing look and feel
- [JFreeChart](https://www.jfree.org/jfreechart/) - Chart visualization library
- [Apache POI](https://poi.apache.org/) - Excel file format support
- [DeepSeek AI](https://deepseek.com) - AI-powered financial insights

---

Developed by Group 78 - Software Engineering Project 2025
