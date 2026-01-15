# 🏗️ SpringWithLLM Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                            │
│                     (Web, Mobile, Desktop, CLI)                         │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 │ HTTP/REST
                                 │
┌────────────────────────────────▼────────────────────────────────────────┐
│                         API GATEWAY LAYER                               │
│                      (Spring Boot Controllers)                          │
│                                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │   /chat      │  │ /chat/stream │  │  /history    │                 │
│  │   endpoint   │  │   endpoint   │  │   endpoint   │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 │
┌────────────────────────────────▼────────────────────────────────────────┐
│                        SERVICE LAYER                                    │
│                     (Business Logic)                                    │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │              AIModelService                                       │  │
│  │  - Route requests to appropriate AI provider                     │  │
│  │  - Handle conversation history                                   │  │
│  │  - Manage streaming responses                                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
┌───────────────────▼─────────────┐  ┌────────▼────────────────────────┐
│   SPRING AI FRAMEWORK            │  │  DATA PERSISTENCE LAYER         │
│   (ChatClient API)               │  │  (Spring Data JPA)              │
│                                  │  │                                 │
│  ┌────────────────────────────┐ │  │  ┌──────────────────────────┐  │
│  │  Generic ChatModel         │ │  │  │ ConversationHistory      │  │
│  │  Interface                 │ │  │  │ Repository               │  │
│  └────────────────────────────┘ │  │  └──────────────────────────┘  │
└──────────────┬──────────────────┘  └────────┬────────────────────────┘
               │                               │
               │                               │
    ┌──────────┴─────────────┐                │
    │                        │                 │
    │   AI Provider Layer    │                 │
    │                        │                 │
┌───▼────┐ ┌────▼────┐ ┌────▼────┐      ┌────▼─────┐
│ OpenAI │ │Anthropic│ │  Azure  │      │PostgreSQL│
│  GPT-4 │ │ Claude  │ │ OpenAI  │      │ Database │
└────────┘ └─────────┘ └─────────┘      └──────────┘

┌────▼────┐ ┌────▼────┐ ┌────▼────┐
│ Vertex  │ │ Bedrock │ │ Ollama  │
│   AI    │ │   AI    │ │  Local  │
│ Gemini  │ │ Claude  │ │ Llama2  │
└─────────┘ └─────────┘ └─────────┘
```

---

## 📐 Component Description

### 1. **Client Layer**
- Web, Mobile, and Desktop applications
- Communicates via REST API
- Supports both synchronous and streaming responses
- Session-based conversation tracking

### 2. **API Gateway Layer (Controllers)**

#### AIController
Handles HTTP requests with the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ai/chat` | POST | Synchronous chat completion |
| `/api/ai/chat/stream` | POST | Streaming chat with SSE |
| `/api/ai/history/{sessionId}` | GET | Retrieve conversation history |
| `/api/ai/health` | GET | Health check endpoint |

**Features:**
- Session management via headers
- Request validation
- Response transformation
- Error handling

### 3. **Service Layer**

#### AIModelService
Core business logic with responsibilities:

- **Provider Routing**: Dynamically select AI provider based on request
- **Conversation Management**: Track and persist chat history
- **Response Processing**: Handle both sync and streaming responses
- **Error Handling**: Graceful degradation and meaningful error messages

**Key Methods:**
- `chat(ChatRequest, String sessionId)` - Synchronous completion
- `chatStream(ChatRequest, String sessionId)` - Streaming completion
- `getConversationHistory(String sessionId)` - History retrieval
- `getChatModel(AIProvider)` - Provider selection logic

### 4. **Spring AI Framework**

#### ChatClient API
- **Fluent Interface**: Builder pattern for chat interactions
- **Prompt Management**: User and system message handling
- **Streaming Support**: Reactive streams via Project Reactor
- **Provider Abstraction**: Unified interface across providers

#### Generic ChatModel Interface
- Abstraction layer over different AI providers
- Consistent API regardless of underlying model
- Automatic configuration via Spring Boot auto-configuration

### 5. **AI Provider Layer**

| Provider | Models | Configuration |
|----------|--------|---------------|
| **OpenAI** | GPT-4, GPT-3.5-turbo | API Key |
| **Anthropic** | Claude 3.5 Sonnet | API Key |
| **Azure OpenAI** | GPT-4 | API Key + Endpoint |
| **Google Vertex AI** | Gemini Pro | Project ID + Location |
| **Amazon Bedrock** | Claude, Titan | AWS Credentials |
| **Ollama** | Llama 2, Mistral, etc. | Base URL |

**Spring Boot Auto-Configuration:**
- Automatic bean creation based on configuration
- Optional dependencies (graceful handling if not configured)
- Environment-based configuration

### 6. **Data Persistence Layer**

#### Spring Data JPA
- Repository pattern for data access
- Hibernate ORM for object-relational mapping
- PostgreSQL as the database engine

#### ConversationHistoryRepository
```java
List<ConversationHistory> findBySessionIdOrderByTimestampAsc(String sessionId)
```

**Schema:**
```sql
conversation_history
- id (BIGSERIAL)
- session_id (VARCHAR)
- user_message (TEXT)
- ai_response (TEXT)
- provider (VARCHAR)
- timestamp (TIMESTAMP)
- tokens_used (INTEGER)
```

---

## 🔄 Data Flow

### Synchronous Chat Flow

```
1. Client → POST /api/ai/chat
   ↓
2. AIController receives request
   - Validates ChatRequest
   - Generates session ID if not provided
   ↓
3. AIModelService.chat()
   - Selects ChatModel based on provider
   - Creates ChatClient
   ↓
4. Spring AI ChatClient
   - Builds prompt
   - Calls AI provider API
   ↓
5. AI Provider (OpenAI/Anthropic/etc.)
   - Processes request
   - Returns response
   ↓
6. AIModelService
   - Receives response
   - Saves to database via repository
   ↓
7. Response → Client
   - Returns ChatResponse JSON
```

### Streaming Chat Flow

```
1. Client → POST /api/ai/chat/stream
   ↓
2. AIController receives request
   - Returns Flux<String> (reactive stream)
   ↓
3. AIModelService.chatStream()
   - Creates ChatClient with streaming
   ↓
4. Spring AI ChatClient
   - Opens streaming connection
   - Returns Flux<String>
   ↓
5. AI Provider
   - Streams tokens in real-time
   ↓
6. Client receives Server-Sent Events
   - Token-by-token delivery
   ↓
7. On completion
   - Full response saved to database
```

---

## 🎯 Key Design Patterns

### 1. **Strategy Pattern**
- AI provider selection based on enum
- Runtime provider switching
- Easy to add new providers

### 2. **Repository Pattern**
- Data access abstraction
- Separation of concerns
- Testability

### 3. **Builder Pattern**
- ChatClient fluent API
- DTO construction
- Readable and maintainable code

### 4. **Dependency Injection**
- Spring IoC container
- Loose coupling
- Easy mocking for tests

### 5. **Reactive Streams**
- Streaming responses with Flux
- Backpressure handling
- Non-blocking I/O

---

## 🔐 Security Architecture

### API Key Management
```
Environment Variables → application.yml → Spring AI Auto-Config
```

- Never stored in code
- Loaded at runtime
- Provider-specific configuration

### Data Security
- SQL injection prevention via JPA
- Input validation on all endpoints
- Parameterized queries

### Network Security
- CORS configuration for web clients
- HTTPS recommended for production
- Session-based authentication ready

---

## 📊 Scalability Considerations

### Horizontal Scaling
- Stateless design (session data in database)
- No in-memory state (except transient)
- Load balancer friendly

### Database Optimization
- Indexed session_id for fast lookups
- Connection pooling (HikariCP)
- Prepared statements

### Caching Strategy
- Redis integration ready
- Session cache for conversation history
- Response caching for common queries

### Async Processing
- Reactive streams for streaming
- CompletableFuture for async operations
- Thread pool management

---

## 🛠️ Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Framework** | Spring Boot | 3.2.1 |
| **Language** | Java | 21 |
| **AI Framework** | Spring AI | 1.0.0-M4 |
| **Database** | PostgreSQL | 14+ |
| **ORM** | Hibernate/JPA | 6.x |
| **Build Tool** | Maven | 3.8+ |
| **Reactive** | Project Reactor | 3.x |
| **Logging** | SLF4J + Logback | 2.x |

---

## 🔍 Observability

### Logging
- Structured logging with SLF4J
- Log levels: DEBUG for Spring AI, INFO for app
- Request/Response logging
- Error tracking with stack traces

### Monitoring (Ready for Integration)
- Spring Boot Actuator endpoints
- Metrics collection (Micrometer)
- Health checks
- Custom metrics for AI usage

### Tracing (Ready for Integration)
- Distributed tracing with OpenTelemetry
- Request correlation IDs
- Provider latency tracking

---

## 🚀 Deployment Architecture

### Development
```
Developer Machine
├── Java 21 JDK
├── Maven
├── PostgreSQL (Docker)
└── Ollama (optional)
```

### Production
```
Load Balancer
    │
    ├─── App Instance 1 (Docker/K8s)
    ├─── App Instance 2 (Docker/K8s)
    └─── App Instance N (Docker/K8s)
         │
         └─── PostgreSQL (RDS/Cloud SQL)
```

### Container Deployment
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/springwithllm-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🔄 Future Enhancements

### Planned Features
- [ ] Vector database integration (PGVector, Pinecone)
- [ ] Retrieval Augmented Generation (RAG)
- [ ] Function calling / Tool use
- [ ] Image generation endpoints
- [ ] Audio transcription
- [ ] Structured outputs with POJOs
- [ ] Conversation memory with context window
- [ ] Rate limiting and quotas
- [ ] Multi-tenancy support
- [ ] GraphQL API

### Extensibility Points
- Custom AI providers via interface implementation
- Plugin system for middleware/advisors
- Custom embeddings models
- Vector store adapters

---

## 📈 Performance Metrics

### Expected Performance
- **Latency**: < 100ms (app overhead)
- **Throughput**: 1000+ req/s
- **Database**: < 10ms per query
- **Memory**: ~512MB base + request processing

### AI Provider Latency (Typical)
- OpenAI: 1-3 seconds
- Anthropic: 1-2 seconds
- Azure: 1-3 seconds
- Vertex AI: 1-3 seconds
- Bedrock: 2-4 seconds
- Ollama: 0.5-2 seconds (local)

---

## 🎓 Best Practices Implemented

1. **Separation of Concerns**: Clear layer boundaries
2. **Dependency Injection**: Loose coupling via Spring
3. **Error Handling**: Comprehensive exception handling
4. **Configuration Management**: Externalized configuration
5. **Logging**: Structured logging throughout
6. **Testing**: Unit tests ready structure
7. **Documentation**: Inline comments and README
8. **Code Style**: Consistent formatting with Lombok
9. **Security**: API keys in environment variables
10. **Scalability**: Stateless design

---

## 📚 References

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Project Reactor](https://projectreactor.io/docs)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

**Architecture Version**: 1.0.0  
**Last Updated**: January 14, 2026
