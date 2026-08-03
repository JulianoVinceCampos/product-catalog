# Product Catalog API

REST API em **Java 17 + Spring Boot 3** para gerenciamento de catálogo de produtos com busca full-text, cache distribuído e observabilidade completa.

---

## Stack

| Componente     | Tecnologia               | Versão |
|----------------|--------------------------|--------|
| Runtime        | Java + Spring Boot       | 17 / 3.2 |
| Banco de dados | MySQL + JPA + Flyway     | 8.0 |
| Busca          | Elasticsearch            | 8.13 |
| Cache          | Redis + Spring Cache     | 7 |
| Métricas       | Micrometer + Prometheus  | - |
| Mapeamento     | MapStruct                | 1.5.5 |
| Contêineres    | Docker + Docker Compose  | - |

---

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) >= 24
- Pelo menos **4 GB de RAM** para os containers
- (Opcional) Java 17 + Maven 3.9 para desenvolvimento local

---

## 1. Rodar com Docker (recomendado)

```bash
# 1. Clone o projeto
git clone https://github.com/seu-usuario/product-catalog.git
cd product-catalog

# 2. Suba todos os serviços (MySQL + Redis + Elasticsearch + App)
docker compose up -d

# 3. Acompanhe os logs da aplicação
docker compose logs -f app
# Aguarde: "Started ProductCatalogApplication in X seconds"

# 4. Verifique a saúde
curl http://localhost:8080/actuator/health
```

Para parar:
```bash
docker compose down          # Para os containers (dados persistem)
docker compose down -v       # Para e remove todos os volumes (limpa tudo)
```

---

## 2. Rodar em desenvolvimento local (sem Docker para a app)

```bash
# 1. Suba apenas as dependências
docker compose up -d mysql redis elasticsearch

# 2. Aguarde os serviços (30-60s)
docker compose ps

# 3. Rode a aplicação com profile local (logs coloridos)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# OU se tiver Maven instalado:
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 3. Seed de dados de exemplo

Popula o banco com 10 produtos de amostra:

```bash
# Com Docker rodando:
docker compose exec app java -Dspring.profiles.active=prod,seed -jar app.jar

# Em desenvolvimento local:
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,seed
```

---

## 4. Endpoints da API

### Base URL: `http://localhost:8080`

| Método   | Endpoint                   | Descrição                        | Status |
|----------|----------------------------|----------------------------------|--------|
| POST     | /api/products              | Criar produto                    | 201    |
| GET      | /api/products/{id}         | Buscar por ID (com cache Redis)  | 200    |
| PUT      | /api/products/{id}         | Atualizar produto                | 200    |
| DELETE   | /api/products/{id}         | Soft delete                      | 204    |
| GET      | /api/products              | Listar com filtros e paginação   | 200    |
| POST     | /api/products/{id}/image   | Upload de imagem (multipart)     | 200    |
| GET      | /api/search/products       | Busca full-text (Elasticsearch)  | 200    |

#### Parâmetros de /api/products:
- `category`: filtro por categoria
- `status`: ACTIVE ou INACTIVE
- `page`: número da página (padrão: 0)
- `size`: itens por página (padrão: 20, máx: 100)

#### Parâmetros de /api/search/products:
- `q`: texto de busca (name + description)
- `category`: filtro exato
- `minPrice` / `maxPrice`: faixa de preço
- `status`: ACTIVE ou INACTIVE
- `sort` - campo: price ou createdAt
- `order`: asc ou desc
- `page` / `size`: paginação

---

## 5. Testando com cURL

### Criar produto:
```bash
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-001",
    "name": "MacBook Pro 14 M3",
    "description": "Apple M3 Pro chip, 18GB RAM, 512GB SSD",
    "price": 10999.99,
    "category": "Notebooks",
    "status": "ACTIVE"
  }'
```

Salve o `id` retornado:
```bash
export ID="cole-o-uuid-aqui"
```

### Buscar por ID:
```bash
curl -s http://localhost:8080/api/products/$ID
```

### Atualizar:
```bash
curl -s -X PUT http://localhost:8080/api/products/$ID \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-001",
    "name": "MacBook Pro 14 M3 - Atualizado",
    "description": "Descrição atualizada",
    "price": 9499.99,
    "category": "Notebooks",
    "status": "ACTIVE"
  }'
```

### Busca full-text (aguarde ~1s após criar):
```bash
curl -s "http://localhost:8080/api/search/products?q=macbook"
curl -s "http://localhost:8080/api/search/products?category=Notebooks&minPrice=5000&sort=price&order=asc"
```

### Listar com paginação:
```bash
curl -s "http://localhost:8080/api/products?page=0&size=10"
curl -s "http://localhost:8080/api/products?category=Notebooks&status=ACTIVE"
```

### Deletar (soft delete):
```bash
curl -s -X DELETE http://localhost:8080/api/products/$ID
# 204 No Content: produto ainda existe no banco mas invisível
```

### Testar validações (espera 422):
```bash
# Nome muito curto
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"ERR-001","name":"AB","price":99.99}'

# Preço zero
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"ERR-002","name":"Produto Válido","price":0}'
```

---

## 6. Rodar os testes

### Testes unitários (rápido, sem Docker):
```bash
./mvnw test
# Tempo: < 30 segundos
```

### Testes de integração (requer Docker):
```bash
./mvnw failsafe:integration-test failsafe:verify
# Tempo: 2-5 minutos (Testcontainers sobe MySQL + Redis + ES automaticamente)
```

### Todos os testes:
```bash
./mvnw verify
```

### Relatório de cobertura:
```bash
./mvnw test jacoco:report
# Abrir: target/site/jacoco/index.html
```

---

## 7. Observabilidade

```bash
# Saúde completa (MySQL + Redis + Elasticsearch)
curl http://localhost:8080/actuator/health

# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus

# Counters de negócio
curl http://localhost:8080/actuator/metrics/product.created
curl http://localhost:8080/actuator/metrics/product.deleted
```

### Verificar cache Redis:
```bash
# Monitor em tempo real
docker compose exec redis redis-cli monitor

# Em outro terminal, faça 2 requests ao mesmo produto:
curl http://localhost:8080/api/products/$ID   # MISS - vai ao banco
curl http://localhost:8080/api/products/$ID   # HIT - servido pelo Redis
```

### Verificar Elasticsearch:
```bash
# Documentos indexados
curl "http://localhost:9200/products/_search?pretty&size=5"

# Busca manual
curl -X POST "http://localhost:9200/products/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{"query":{"match":{"name":"macbook"}}}'
```

---

## 8. Estrutura do projeto

```
product-catalog/
├── src/main/java/com/catalog/
│   ├── ProductCatalogApplication.java
│   ├── config/
│   │   ├── AsyncConfig.java          # Thread pool @Async
│   │   ├── AwsConfig.java            # S3 client (se storage.provider=s3)
│   │   ├── DatabaseSeeder.java       # Seed de dados (profile=seed)
│   │   ├── RedisConfig.java          # CacheManager + RedisTemplate
│   │   ├── RequestLoggingFilter.java # CorrelationId + latency log
│   │   └── SearchCacheAspect.java    # AOP cache para busca ES
│   ├── controller/
│   │   ├── ProductController.java    # CRUD /api/products
│   │   └── SearchController.java    # GET /api/search/products
│   ├── domain/
│   │   ├── event/ProductChangedEvent.java
│   │   └── model/
│   │       ├── Product.java          # JPA Entity (soft delete)
│   │       ├── ProductDocument.java  # ES Document
│   │       └── ProductStatus.java   # Enum ACTIVE/INACTIVE
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ProductRequest.java
│   │   │   └── ProductSearchFilter.java
│   │   └── response/
│   │       ├── ApiErrorResponse.java
│   │       ├── PageResponse.java
│   │       └── ProductResponse.java
│   ├── exception/
│   │   ├── DuplicateSkuException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ProductNotFoundException.java
│   │   └── StorageException.java
│   ├── mapper/ProductMapper.java     # MapStruct (compile-time)
│   ├── repository/
│   │   ├── elasticsearch/
│   │   │   ├── ProductElasticsearchRepository.java
│   │   │   └── ProductSearchRepository.java
│   │   └── jpa/ProductJpaRepository.java
│   └── service/
│       ├── ProductService.java       # Interface
│       ├── impl/
│       │   ├── ElasticsearchSyncListener.java  # Async sync pós-commit
│       │   └── ProductServiceImpl.java
│       └── storage/
│           ├── LocalStorageService.java  # Dev stub
│           ├── S3StorageService.java     # AWS S3
│           └── StorageService.java      # Interface
├── src/main/resources/
│   ├── application.yml
│   ├── logback-spring.xml
│   └── db/migration/V1__create_products_table.sql
├── src/test/
│   ├── java/com/catalog/
│   │   ├── controller/ProductControllerTest.java   # @WebMvcTest
│   │   ├── integration/
│   │   │   ├── AbstractIntegrationTest.java        # Testcontainers base
│   │   │   └── ProductApiIntegrationTest.java
│   │   └── service/ProductServiceTest.java         # Mockito unit tests
│   └── resources/application-integration-test.yml
├── docker-compose.yml
├── Dockerfile
├── mvnw
├── pom.xml
├── api.http
└── README.md
```

---

## 9. Decisões de arquitetura

**Soft delete**: produtos deletados ficam no banco com `deleted=true`, invisíveis às queries normais. Preserva integridade histórica.

**@TransactionalEventListener(AFTER_COMMIT)**: Elasticsearch só é sincronizado após o commit da transação MySQL. Elimina risco de indexar dados que serão revertidos.

**MapStruct**: Mapeamento gerado em tempo de compilação. Zero reflexão em runtime, erros detectados no build.

**AOP para cache de busca**: O aspecto `SearchCacheAspect` intercepta chamadas de busca e aplica cache Redis com chave composta por todos os parâmetros do filtro.

**Batch fetch no search**: Após busca no ES, os IDs são coletados e o banco é consultado com `findAllById()`, evitando N+1 queries.
