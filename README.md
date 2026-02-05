# Document Export Plugin for Bloomreach CMS

A comprehensive document export plugin for Bloomreach CMS that enables exporting CMS documents in multiple formats with configurable options.

## Features

### Supported Export Formats

- **JSON** - Structured JSON with document properties and metadata
- **CSV** - Tabular comma-separated values format
- **TSV** - Tabular tab-separated values format
- **XML** - Well-formed XML with full document structure
- **PDF** - Professional PDF documents with configurable page size, orientation, and styling (using Apache FOP)
- **HTML** - Semantic HTML with optional CSS styling and metadata

### Configuration Options

Each export format offers format-specific configuration options:

| Format | Options |
|--------|---------|
| JSON | Pretty-print option |
| CSV | Header row, quoting options |
| TSV | Header row, quoting options |
| XML | Pretty-print, namespace declarations, metadata inclusion |
| PDF | Page size (A4/Letter/Legal), orientation (Portrait/Landscape), font size (Small/Medium/Large), metadata toggle |
| HTML | Include metadata, include CSS styling |

## Architecture

The plugin follows Bloomreach CMS patterns with a 3-layer architecture:

### Layer 1: Repository (Backend)
- `ExportXxxWorkflow` interfaces define the export contract
- `ExportXxxWorkflowImpl` classes implement the actual export logic
- Located in `repository/src/main/java/org/onehippo/forge/exportjson/repository/workflow/`

### Layer 2: CMS (Frontend)
- `ExportJsonPlugin` registers export buttons in the document editor
- Dialog classes (`ExportXxxDialog`) provide user configuration options
- Download pages (`ExportXxxDownloadPage`) handle file downloads
- Located in `cms/src/main/java/org/onehippo/forge/exportjson/frontend/`

### Layer 3: Configuration
- Workflows registered via YAML in `repository/src/main/resources/hcm-config/workflow/exportjson-workflow.yaml`
- Single plugin handles all export formats

## Installation

### Prerequisites

- Bloomreach CMS 16.0.0 or higher
- Java 11+
- Maven 3.6+

### Build

```bash
mvn clean install
```

### Deployment

1. Deploy the JAR files to your Bloomreach instance:
   - `repository/target/exportjson-repository-*.jar` → `WEB-INF/lib/`
   - `cms/target/exportjson-cms-*.jar` → `WEB-INF/lib/`

2. Restart your CMS application

3. Export buttons will appear in the document editor for all configured documents

## Usage

### Exporting a Document

1. Open any document in the Bloomreach CMS editor
2. Look for export buttons in the document actions area (JSON, CSV, TSV, XML, PDF, HTML)
3. Click the desired export format button
4. A dialog will appear with format-specific options
5. Configure options as needed
6. Click "Download" to save the file

### Example Exports

**PDF Export:**
- Select page size: A4, Letter, or Legal
- Choose orientation: Portrait or Landscape
- Pick font size: Small (9pt), Medium (11pt), Large (13pt)
- Toggle metadata inclusion

**HTML Export:**
- Include document metadata (created, modified, author)
- Include professional CSS styling

## Configuration

### System Properties

For PDF export using Apache FOP, no additional JVM properties are required. The plugin uses Apache FOP 2.8 which handles font management efficiently.

### Custom Export Formats

To add a new export format:

1. Create workflow interface: `ExportXxxWorkflow.java`
2. Create workflow implementation: `ExportXxxWorkflowImpl.java`
3. Register in `exportjson-workflow.yaml`
4. Create dialog: `ExportXxxDialog.java`, `.html`, `.css`
5. Create download page: `ExportXxxDownloadPage.java`
6. Add to `ExportJsonPlugin.java`
7. Update `ExportJsonPlugin.properties`

## Dependencies

### Core Dependencies

- **jackson-databind** (2.15.2) - JSON/XML serialization
- **commons-csv** (1.10.0) - CSV/TSV handling
- **fop** (2.8) - PDF generation (Apache FOP)

### Bloomreach CMS

- hippo-repository-api
- hippo-cms-api
- hippo-addon-workflow

## Project Structure

```
document-export/
├── repository/                 # Backend workflow implementations
│   ├── src/main/java/
│   │   └── org/onehippo/forge/exportjson/repository/
│   │       ├── workflow/      # Workflow interfaces and implementations
│   │       └── *Utils.java    # Utility classes
│   ├── src/main/resources/
│   │   └── hcm-config/
│   │       └── workflow/      # Workflow YAML configuration
│   └── pom.xml
├── cms/                        # Frontend UI and dialogs
│   ├── src/main/java/
│   │   └── org/onehippo/forge/exportjson/frontend/
│   │       ├── dialog/        # Export option dialogs
│   │       ├── pages/         # Download page handlers
│   │       └── plugins/       # Main plugin class
│   └── pom.xml
├── tests/                      # Integration tests
├── demo/                       # Demo CMS instance
└── pom.xml                     # Parent POM
```

## Building from Source

### Build All Modules

```bash
mvn clean install
```

### Build Specific Module

```bash
mvn clean install -pl repository
mvn clean install -pl cms
```

### Run Tests

```bash
mvn clean verify
```

### Generate Site Documentation

```bash
mvn clean site:site
```

## License

Apache License 2.0 - See LICENSE file for details

## Contributing

Contributions are welcome! Please ensure:

1. Code follows existing patterns and conventions
2. All tests pass: `mvn clean verify`
3. Code is properly commented
4. Changes include relevant test coverage

## Support

For issues, feature requests, or questions:

- GitHub Issues: https://github.com/bloomreach-forge/document-export/issues
- Bloomreach Forge: https://github.com/bloomreach-forge

## Changelog

### Version 8.0.1-SNAPSHOT

- Initial release
- Support for JSON, CSV, TSV, XML exports
- PDF export using Apache FOP
- HTML export with optional CSS styling
- Configurable export options per format
- Multi-page PDF support
- Professional HTML generation with metadata
