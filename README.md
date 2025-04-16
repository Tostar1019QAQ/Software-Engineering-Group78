# Software-Engineering-Group78

# Before devlopment (important!)

## Quick Start

### Run with Maven

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/finance-tracker.git
   cd finance-tracker
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn exec:java
   ```



## Development Setup

1. **IDE Configuration**
   - Import as Maven project
   - Ensure JDK 17 is configured
   - Set project encoding to UTF-8

2. **Project Structure**
   ```
   src/
   ├── main/
   │   ├── java/
   │   │   └── com/group78/financetracker/
   │   │       ├── model/
   │   │       ├── service/
   │   │       └── ui/
   │   └── resources/
   └── test/
       └── java/
   ```

3. **Build Configuration**
   - Maven is used for dependency management
   - All dependencies are specified in `pom.xml`
