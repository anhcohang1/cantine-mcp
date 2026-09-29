# Cantine MCP — Java 25

Petit serveur MCP Java exposant deux outils en lecture seule :

- `get_canteen_balance` → `30.00 EUR`
- `get_canteen_max_debt` → `-20.00 EUR`

Le serveur utilise Java 25, Spring Boot et le SDK Java de Model Context Protocol.

## Architecture

```text
Client MCP (curl / ChatGPT)
        |
        | Streamable HTTP
        v
http://localhost:8080/mcp
        |
        +-- get_canteen_balance  -> 30.00 EUR
        +-- get_canteen_max_debt -> -20.00 EUR
```

## 1. Récupérer le projet

```bash
git clone --branch master https://github.com/anhcohang1/cantine-mcp.git
cd cantine-mcp
```

Si le dépôt est déjà présent :

```bash
git switch master
git pull
```

## 2. Vérifier Java et Maven

```bash
java --version
mvn --version
```

Le projet utilise Java 25. Il faut également utiliser un Maven récent (3.9.x recommandé).

Exemple de configuration testée :

```text
Java version: 25.0.2
Apache Maven 3.9.x
```

Attention : Maven 3.0.4 est trop ancien et peut notamment tenter d'accéder à Maven Central en HTTP au lieu de HTTPS.

## 3. Compiler

À la racine du projet :

```bash
mvn clean package
```

Le JAR généré est :

```text
target/cantine-mcp-1.0.0.jar
```

## 4. Démarrer le serveur MCP

```bash
java -jar target/cantine-mcp-1.0.0.jar
```

Le serveur doit rester actif. Les logs doivent notamment indiquer :

```text
Tomcat started on port 8080 (http) with context path '/'
Started CantineApplication
```

L'endpoint MCP Streamable HTTP est :

```text
http://localhost:8080/mcp
```

Conserver ce terminal ouvert.

## 5. Ouvrir un deuxième terminal

Tous les tests suivants sont effectués dans un autre terminal pendant que l'application Java continue de tourner.

Un simple :

```bash
curl -i http://localhost:8080/mcp
```

peut retourner une erreur indiquant que `text/event-stream` et un identifiant de session MCP sont requis. C'est normal : un client MCP doit d'abord initialiser une session.

## 6. Initialiser le protocole MCP

Envoyer le message MCP `initialize` :

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "capabilities": {},
      "clientInfo": {
        "name": "curl-test",
        "version": "1.0"
      }
    }
  }'
```

Une réponse correcte ressemble à :

```text
HTTP/1.1 200
Mcp-Session-Id: <SESSION_ID>
...
{"jsonrpc":"2.0","id":1,"result":{
  "protocolVersion":"2025-06-18",
  "capabilities":{"logging":{},"tools":{"listChanged":false}},
  "serverInfo":{"name":"cantine-mcp","version":"1.0.0"}
}}
```

Noter la valeur du header `Mcp-Session-Id`. Elle sera utilisée dans les requêtes suivantes.

Dans les exemples ci-dessous, remplacer `<SESSION_ID>` par cette valeur.

## 7. Lister les outils MCP

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'Mcp-Session-Id: <SESSION_ID>' \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'
```

La réponse est envoyée en SSE (`text/event-stream`) et doit contenir les deux outils :

```text
get_canteen_balance
get_canteen_max_debt
```

Par exemple :

```text
event: message
data: {"jsonrpc":"2.0","id":2,"result":{"tools":[...]}}
```

## 8. Appeler `get_canteen_balance`

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'Mcp-Session-Id: <SESSION_ID>' \
  -d '{
    "jsonrpc":"2.0",
    "id":3,
    "method":"tools/call",
    "params":{
      "name":"get_canteen_balance",
      "arguments":{}
    }
  }'
```

Résultat attendu :

```text
event: message
data: {"jsonrpc":"2.0","id":3,"result":{"content":[{"type":"text","text":"Solde du compte cantine : 30.00 EUR"}],"isError":false}}
```

Le point important est :

```text
Solde du compte cantine : 30.00 EUR
```

avec :

```text
"isError":false
```

## 9. Appeler `get_canteen_max_debt`

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'Mcp-Session-Id: <SESSION_ID>' \
  -d '{
    "jsonrpc":"2.0",
    "id":4,
    "method":"tools/call",
    "params":{
      "name":"get_canteen_max_debt",
      "arguments":{}
    }
  }'
```

Résultat attendu :

```text
Dette maximale autorisée : -20.00 EUR
```

## 10. Ce qui a été testé de bout en bout

Après ces commandes, le chemin complet est validé :

```text
curl
  |
  | HTTP + JSON-RPC / MCP
  v
/mcp (Streamable HTTP)
  |
  v
McpConfig
  |
  +--> get_canteen_balance
  |        |
  |        v
  |    CantineService.getBalance()
  |        |
  |        +--> 30.00 EUR
  |
  +--> get_canteen_max_debt
           |
           v
       CantineService.getMaxDebt()
           |
           +--> -20.00 EUR
```

`curl` joue ici le rôle du client MCP. Lorsqu'un client comme ChatGPT est connecté au serveur, il effectue le même type d'échanges MCP : initialisation, découverte des outils et appels `tools/call`.

## Test optionnel avec MCP Inspector

Si Node.js et `npx` sont installés :

```bash
npx @modelcontextprotocol/inspector
```

Choisir **Streamable HTTP** puis utiliser :

```text
http://localhost:8080/mcp
```

MCP Inspector n'est pas nécessaire pour les tests décrits ci-dessus : `curl` suffit.

## Connexion à ChatGPT

Les tests locaux utilisent `localhost:8080`. Pour qu'un client ChatGPT qui ne partage pas la même machine puisse appeler le serveur, l'endpoint MCP doit être rendu accessible au client, typiquement via une URL HTTPS ou un mécanisme de tunnel approprié.
