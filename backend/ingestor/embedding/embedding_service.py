from sentence_transformers import SentenceTransformer

MODEL_NAME = "all-MiniLM-L6-v2"


class EmbeddingService:
    def __init__(self):
        self._model = SentenceTransformer(MODEL_NAME)

    def embed(self, text: str) -> list[float]:
        return self._model.encode(text).tolist()

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        return [v.tolist() for v in self._model.encode(texts)]
