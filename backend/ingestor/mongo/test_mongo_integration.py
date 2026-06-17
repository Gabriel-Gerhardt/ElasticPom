from unittest.mock import MagicMock, patch, call
import pytest
from pymongo.errors import BulkWriteError

# Patch MongoClient so no real DB connection is made
with patch("pymongo.MongoClient"):
    from mongo.mongo_integration import MongoIntegration


@pytest.fixture
def integration():
    with patch("pymongo.MongoClient") as mock_client_cls:
        mock_client = MagicMock()
        mock_client_cls.return_value = mock_client

        mock_db = MagicMock()
        mock_client.__getitem__.return_value = mock_db

        mock_collection = MagicMock()
        mock_db.__getitem__.return_value = mock_collection

        inst = MongoIntegration.__new__(MongoIntegration)
        inst.client = mock_client
        inst.db = mock_db
        inst.collection = mock_collection
        inst.unique_key = "paper_id"

        yield inst, mock_db


class TestSaveFilters:
    def test_upserts_on_underscore_id(self, integration):
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [{"filtername": "subjects", "order": 1, "type": "option"}]
        inst.save_filters(filters)

        mock_filters_col.bulk_write.assert_called_once()
        ops = mock_filters_col.bulk_write.call_args[0][0]
        assert len(ops) == 1

        # The UpdateOne filter must use _id = filtername (not {"filtername": ...})
        op_filter = ops[0]._filter
        assert op_filter == {"_id": "subjects"}, (
            f"Expected upsert filter {{'_id': 'subjects'}}, got {op_filter}"
        )

    def test_document_includes_id_field(self, integration):
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [{"filtername": "date", "order": 3, "type": "range"}]
        inst.save_filters(filters)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        # The $set document must include _id so MongoDB stores filtername as _id
        set_doc = ops[0]._doc["$set"]
        assert set_doc["_id"] == "date"
        assert set_doc["filtername"] == "date"
        assert set_doc["order"] == 3
        assert set_doc["type"] == "range"

    def test_skips_filters_without_filtername(self, integration):
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [
            {"filtername": "subjects", "order": 1, "type": "option"},
            {"order": 2, "type": "option"},  # missing filtername
        ]
        inst.save_filters(filters)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        assert len(ops) == 1

    def test_returns_none_for_empty_list(self, integration):
        inst, mock_db = integration
        result = inst.save_filters([])
        assert result is None

    def test_returns_none_when_all_filters_lack_filtername(self, integration):
        inst, mock_db = integration
        result = inst.save_filters([{"order": 1, "type": "option"}])
        assert result is None

    def test_multiple_filters_upserted(self, integration):
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [
            {"filtername": "subjects", "order": 1, "type": "option"},
            {"filtername": "creators", "order": 2, "type": "option"},
            {"filtername": "date",     "order": 3, "type": "range"},
        ]
        inst.save_filters(filters)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        assert len(ops) == 3
        ids = [op._filter["_id"] for op in ops]
        assert ids == ["subjects", "creators", "date"]

    def test_bulk_write_error_is_caught_and_returned(self, integration):
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        error_details = {"writeErrors": [{"code": 11000}]}
        mock_filters_col.bulk_write.side_effect = BulkWriteError(error_details)

        filters = [{"filtername": "subjects", "order": 1, "type": "option"}]
        result = inst.save_filters(filters)

        assert result == error_details

    def test_bulk_write_called_with_ordered_false(self, integration):
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [{"filtername": "subjects", "order": 1, "type": "option"}]
        inst.save_filters(filters)

        _, kwargs = mock_filters_col.bulk_write.call_args
        assert kwargs.get("ordered") is False
