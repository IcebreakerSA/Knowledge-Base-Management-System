# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Spring Boot 3.5.0** application built with **Maven** that serves as a knowledge management and AI-powered note-taking system. The application integrates with **PostgreSQL** (with vector extension for AI features), **Redis** for caching/sessions, and **LangChain4j** for AI/ML capabilities.

## Quick Start Commands

### Build & Run
- `mvn clean install` - Build the project
- `mvn spring-boot:run` - Run the application
- `mvn test` - Run tests

### Database Setup
The application requires PostgreSQL with the `vector` extension. SQL initialization script is located at `sql/complete_database_init.sql`.

### Development
- Java 17+ is required
- The application uses MyBatis Plus for ORM
- Sa-Token for authentication
- Lombok for reducing boilerplate code

## Architecture Overview

### Core Components

**Controllers** (`com.ldd.initialization.controller`):
- `NoteController` - CRUD operations for notes, tagging, and knowledge base integration
- `NotebookController` - Notebook management
- `ChatController` - AI chat interface with streaming support and rate control
- `DocumentController` - Document upload and processing
- `SharedKnowledgeBaseController` - Collaborative knowledge base management

**Services** (`com.ldd.initialization.service`):
- Standard service layer pattern with interfaces and implementations
- AI-powered services for document processing and chat
- Integration with vector database for semantic search

**Data Access** (`com.ldd.initialization.mapper`):
- MyBatis Plus mappers for PostgreSQL entities
- Entities include: User, Note, Notebook, NoteTag, SharedKnowledgeBase, KnowledgeBaseFile, KnowledgeBaseMember, FileInfo

**Configuration** (`com.ldd.initialization.config`):
- `PgVectorConfig` - PostgreSQL vector database configuration for AI embeddings
- `SaTokenConfiguration` - Authentication setup
- `ChatStreamConfig` - Rate limiting and streaming configuration
- Various other Spring Boot auto-configurations

### Key Technical Features

**AI/ML Integration**:
- LangChain4j integration for chat models (OpenAI and Ollama support)
- Vector database (PostgreSQL pgvector) for semantic search
- Document parsing (PDF, Markdown) with Apache Tika
- Streaming chat with rate control and adaptive delay

**Authentication & Authorization**:
- Sa-Token for login/auth
- Role-based access control
- User isolation for data privacy

**File Management**:
- X-File-Storage for file uploads
- Support for multiple file types
- Integration with knowledge bases

**Real-time Features**:
- WebFlux for reactive programming
- Streaming chat responses
- Rate-controlled output

## Development Guidelines

### Code Structure
- Follow the standard layered architecture: Controller → Service → Mapper → Domain
- DTOs for request/response, VOs for view models
- Use Lombok annotations (@Data, @Service, @Component, etc.)
- Exception handling via centralized `ExceptionHandlerConfigure`

### Database
- Use PostgreSQL with vector extension enabled
- MyBatis Plus for database operations
- All tables have `create_time` and `update_time` timestamps
- Soft delete pattern using `status` fields

### AI Features
- Vector embeddings stored in `documents` table
- Content retrieval uses similarity search with configurable thresholds
- Chat services support both streaming and simple responses

### Testing
- Unit tests for services and controllers
- Integration tests for database operations
- Mock external AI services for reliable testing

## Configuration

### Application Properties
Key configuration areas:
- `spring.datasource` - PostgreSQL connection
- `spring.redis` - Redis for sessions/caching
- AI model configurations (OpenAI/Ollama endpoints)
- File storage settings
- Sa-Token configuration

### Rate Control
Chat streaming includes sophisticated rate control:
- Base delay between chunks
- Adaptive delay based on content length
- Smooth transmission with windowing
- Backpressure handling

## Common Development Tasks

### Adding a New Entity
1. Create domain class in `domain/` package
2. Create mapper interface in `mapper/` package
3. Create service interface and implementation
4. Create controller with REST endpoints
5. Add corresponding SQL table (if needed)

### Adding AI Features
1. Use `PgVectorEmbeddingStore` for vector operations
2. Create content retriever beans
3. Integrate with LangChain4j chat models
4. Consider rate control for streaming endpoints

### Authentication
- Use `@SaCheckLogin` annotation for protected endpoints
- Access current user via `StpUtil.getLoginId()`
- Use `UserUtils.getCurrentUserId()` for service layer