# AI-Powered Interview Assistance Platform

An intelligent backend service that audits resumes and provides deep technical feedback using AI integration.

## 🛠 Tech Stack

| Technology | Version | Description |
|------------|---------|-------------|
| Java | 21 | Primary development language |
| Spring Boot | 3.4.0+ | Core application framework |
| Spring AI | 1.0.0-M5 | AI orchestration & integration |
| PostgreSQL + pgvector | 14+ | Relational + Vector storage |
| Redis | 6+ | Caching & Message Queue |
| Apache Tika | 2.9.2 | Document parsing and extraction |
| iText 8 | 8.0.5 | PDF generation |
| MapStruct | 1.6.3 | Type-safe bean mapping |

## ✨ Feature Modules

### 📄 Resume Management Module ✅

- **Multi-format Support**: PDF, DOCX, DOC, TXT
- **Real-time Progress Tracking**: Status display (Pending/Processing/Completed/Failed)
- **Auto Retry Mechanism**: Automatic retry on failure (up to 3 attempts)
- **Duplicate Detection**: Content-based hash matching
- **PDF Report Export**: Downloadable analysis reports

### 🎤 Mock Interview Module (Coming Soon)

- **Personalized Questions**: Generate interview questions based on resume
- **Real-time Q&A Interaction**: Interactive interview simulation
- **Targeted Feedback**: Provide specific improvement suggestions
- **Async Assessment Reports**: Generate comprehensive evaluation reports
- **Performance Trends**: Visualize interview performance over time
- **Interview Statistics**: Dashboard with key metrics
- **PDF Report Export**: Downloadable interview reports

### 📚 Knowledge Base Management Module (Coming Soon)

- **Multi-format Support**: PDF, DOCX, DOC, TXT, Markdown
- **Document Upload & Auto-chunking**: Intelligent document segmentation
- **Asynchronous Vectorization**: Background embedding processing
- **RAG (Retrieval-Augmented Generation)**: Enhanced context-aware responses
- **SSE Streaming**: Real-time streaming responses
- **Intelligent Q&A**: Context-aware dialogue system
- **Knowledge Statistics**: Usage analytics and insights

## 🗺️  Todo

- [ ] Non-blocking resume processing 
- [ ] frontend integration
- [ ] Mock Interview Module
- [ ] Knowledge Base Management
- [ ] Multi-language Support
- [ ] Advanced Analytics Dashboard
