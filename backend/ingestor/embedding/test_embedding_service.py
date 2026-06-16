from unittest.mock import MagicMock, patch
import numpy as np
import pytest

# Patch SentenceTransformer at import time so no model download is needed
with patch("sentence_transformers.SentenceTransformer") as _mock_cls:
    _mock_cls.return_value = MagicMock()
    from embedding.embedding_service import EmbeddingService


@pytest.fixture
def service():
    with patch("embedding.embedding_service.SentenceTransformer") as mock_cls:
        mock_model = MagicMock()
        mock_cls.return_value = mock_model
        svc = EmbeddingService()
        svc._model = mock_model
        yield svc


def _fake_vector(n=384):
    return np.zeros(n, dtype=np.float32)


class TestEmbed:
    def test_returns_list_of_floats(self, service):
        service._model.encode.return_value = _fake_vector()
        result = service.embed("some text")
        assert isinstance(result, list)
        assert len(result) == 384
        assert all(isinstance(v, float) for v in result)

    def test_calls_model_encode_with_text(self, service):
        service._model.encode.return_value = _fake_vector()
        service.embed("hello world")
        service._model.encode.assert_called_once_with("hello world")

    def test_returns_correct_values(self, service):
        vec = np.array([0.1, 0.2, 0.3] + [0.0] * 381, dtype=np.float32)
        service._model.encode.return_value = vec
        result = service.embed("text")
        assert result[0] == pytest.approx(0.1, abs=1e-6)
        assert result[1] == pytest.approx(0.2, abs=1e-6)
        assert result[2] == pytest.approx(0.3, abs=1e-6)


class TestEmbedBatch:
    def test_returns_list_of_lists(self, service):
        service._model.encode.return_value = np.zeros((3, 384), dtype=np.float32)
        result = service.embed_batch(["a", "b", "c"])
        assert isinstance(result, list)
        assert len(result) == 3
        assert all(isinstance(v, list) for v in result)

    def test_each_vector_is_384_dims(self, service):
        service._model.encode.return_value = np.zeros((2, 384), dtype=np.float32)
        result = service.embed_batch(["x", "y"])
        assert all(len(v) == 384 for v in result)

    def test_calls_encode_with_all_texts(self, service):
        texts = ["foo", "bar"]
        service._model.encode.return_value = np.zeros((2, 384), dtype=np.float32)
        service.embed_batch(texts)
        service._model.encode.assert_called_once_with(texts)

    def test_empty_batch(self, service):
        service._model.encode.return_value = np.zeros((0, 384), dtype=np.float32)
        result = service.embed_batch([])
        assert result == []
