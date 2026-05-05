# ElasticPom

Unified full-stack application for semantic search over scientific papers.

## Overview

ElasticPom ingests scientific paper datasets, stores metadata in MongoDB, and indexes them in Elasticsearch to enable fast and semantic search. Users can query papers using natural language and retrieve the most relevant results.

## Architecture

Data ingestion → MongoDB → Elasticsearch → Backend → Frontend

## Tech Stack

- Python 3.11 (data ingestion)
- Java 21 + Spring Boot (backend)
- MongoDB 8.2.6
- Elasticsearch 9.3.2
- React + JavaScript (frontend)
- Docker

## Dataset

- arXiv dataset  
  https://www.kaggle.com/datasets/Cornell-University/arxiv/data

## Features

- Centralized search across scientific papers
- Relevance-based ranking
- Natural language queries

## Setup

### Clone repository
```bash
git clone https://github.com/Gabriel-Gerhardt/ElasticPom.git
cd ElasticPom
```

### Start infrastructure (MongoDB + Elasticsearch)
```bash
cd backend
docker compose up
```

### Data ingestion
```bash
cd ingestor
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python3 main.py
```

### Backend
```bash
cd backend
./gradlew clean build
./gradlew bootRun
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Access

- most relevant papers:  http://localhost:8080/api/papers/most-relevant/?page-size=10&page=0

## Notes

- Ensure Docker is running before starting services
- Elasticsearch and Mongo indexing depends on ingestion step
- Default ports:
  - Backend: 8080  
  - Frontend: 5173  
  - MongoDB: 27017  
  - Elasticsearch: 9200  

## Contact

- LinkedIn: https://www.linkedin.com/in/gabriel-gerhardt-0a8b852b9/  
- Email: gabrielgerhardt27@gmail.com  
- GitHub: https://github.com/Gabriel-Gerhardt
