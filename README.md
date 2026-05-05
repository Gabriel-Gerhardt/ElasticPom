## ElasticPom

Unified scientific papers full-stack application

## How It Works

Ingest data from sources to mount the papers on Mongo and save them for search in Elastic.
The user search papers with natural language, it returns the most relevant of them.

## Diagram

<Project C4 or sequencial diagram>
  
## Tech Stack



## Datasets

The current datasets being used for ingestion are:

arXiv dataset: https://www.kaggle.com/datasets/Cornell-University/arxiv/data

## Use Cases

Allows centralized search of many scientific papers
Shows the most relevant papers
Allow semantic search

## Set-up

### Github
```bash
git clone https://github.com/Gabriel-Gerhardt/ElasticPom.git
cd ElasticPom
```

### Docker
```bash
cd backend
docker compose up
```

### Ingestion
```bash
cd ingestor
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python3 main.py
```

### Web
```bash
cd backend
./gradlew clean build
./gradlew bootRun
cd ..
cd frontend
npm install
npm run dev
```


Access:

    Most relevant papers pagination: http://localhost:8080/api/papers/most-relevant/?page-size=10&page=0
    

## Contact
[LinkedIn](https://www.linkedin.com/in/gabriel-gerhardt-0a8b852b9/)

[Gmail](mailto:gabrielgerhardt27@gmail.com)

[GitHub](https://github.com/Gabriel-Gerhardt)

