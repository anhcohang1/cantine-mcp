# Cantine MCP — Java 25

Petit serveur MCP Java exposant deux outils en lecture seule :

- `get_canteen_balance` → `30.00 EUR`
- `get_canteen_max_debt` → `-20.00 EUR`

Le serveur utilise Java 25, Spring Boot et le SDK Java officiel de Model Context Protocol.

## Compiler

```bash
mvn clean package
```

## Lancer

```bash
java -jar target/cantine-mcp-1.0.0.jar
```

Le point d'entrée MCP Streamable HTTP est :

```text
http://localhost:8080/mcp
```

## Tester avec MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

Choisir **Streamable HTTP** puis utiliser `http://localhost:8080/mcp`.

## Architecture

```text
ChatGPT / client MCP
        |
        | Streamable HTTP
        v
      /mcp
        |
        +-- get_canteen_balance      -> 30.00 EUR
        +-- get_canteen_max_debt     -> -20.00 EUR
```

Pour connecter ChatGPT à distance, l'endpoint MCP devra ensuite être rendu accessible en HTTPS.
