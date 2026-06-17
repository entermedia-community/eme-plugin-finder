# EME-LIB Plugins Directory Structure

## Overview
EME-LIB uses a modular plugin architecture with 11 main plugin directories under `plugins/`.

## Plugin Categories

### Core Plugins
| Plugin | Purpose | Key Directories |
|--------|---------|-----------------|
| `system` | Core system functionality | code/, defaultdata/, docs/, lib/ |
| `finder` | Data entry and search | code/, html/find/ |
| `catalog` | Content management | html/, events/, lists/ |
| `authentication` | User authentication | code/, html/ |
| `mediadb` | Media database services | html/services/, mcp.old/ |

### Feature Plugins
| Plugin | Purpose | Key Directories |
|--------|---------|-----------------|
| `community` | Community features | html/community/, blog/, blockfind/ |
| `informatics` | Informatics tools | code/, html/ |
| `manager` | Multi-database management | code/, html/ |
| `openedit` | Editing toolbar | code/, webapp/ |

### UI/Theme Plugins
| Plugin | Purpose | Key Directories |
|--------|---------|-----------------|
| `eme` | Main site theme | code/, html/ |

## Standard Plugin Structure
Most plugins follow this pattern:
```
<plugin>/
├── code/          # Java source code
├── html/          # HTML templates and configuration
│   └── src/
│       └── plugin.xml  # Spring bean definitions
├── lib/           # JAR libraries
└── work/          # Temporary work files
```

## Special Cases

### openedit
```
<plugin>/
├── code/
├── html/
└── webapp/        # Separate webapp directory
```

### mediadb
```
<plugin>/
├── code/
├── html/
│   └── services/  # API services
│       └── ai/    # AI services
└── mcp.old/       # Legacy MCP files
```

## Fallback Order
1. `Website/plugins/_` (user's plugins)
2. `EME-LIB/plugins/_` (this repository)

## Key Configuration Files
- `plugins/<name>/html/src/plugin.xml` - Bean definitions
- `plugins/catalog/html/data/lists/aiskill/*.xml` - Automation steps
- `plugins/system/lib/*.jar` - Core libraries
