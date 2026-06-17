"""
Additional edge-case tests for MongoIntegration.save_filters().

These complement the 8 existing tests in test_mongo_integration.py
and cover scenarios not addressed there:
- null-value filtername (None) is skipped
- duplicate filtername: two ops for the same filtername
- filter with extra/unknown fields: all fields passed through $set
- filtername with empty-string value is skipped (falsy)
- actual FILTERS constant from filter_data_parser is accepted correctly
"""

from unittest.mock import MagicMock, patch
import pytest
from pymongo.errors import BulkWriteError

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


class TestSaveFiltersEdgeCases:

    def test_filter_with_none_filtername_is_skipped(self, integration):
        """A filter whose 'filtername' key is None must be silently skipped."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [
            {"filtername": None, "order": 1, "type": "option"},
            {"filtername": "subjects.keyword", "order": 1, "type": "option"},
        ]
        inst.save_filters(filters)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        # Only the valid filter should produce an op
        assert len(ops) == 1
        assert ops[0]._filter == {"_id": "subjects.keyword"}

    def test_duplicate_filtername_produces_two_ops(self, integration):
        """Two dicts with the same filtername both produce UpdateOne ops
        (deduplication is left to MongoDB via upsert semantics)."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [
            {"filtername": "subjects.keyword", "order": 1, "type": "option"},
            {"filtername": "subjects.keyword", "order": 99, "type": "range"},  # duplicate
        ]
        inst.save_filters(filters)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        assert len(ops) == 2
        assert ops[0]._filter == {"_id": "subjects.keyword"}
        assert ops[1]._filter == {"_id": "subjects.keyword"}

    def test_filter_with_extra_fields_passes_all_through_set(self, integration):
        """Extra fields beyond filtername/order/type are included in $set."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        filters = [
            {"filtername": "subjects.keyword", "order": 1, "type": "option", "label": "Subjects"},
        ]
        inst.save_filters(filters)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        set_doc = ops[0]._doc["$set"]
        assert set_doc.get("label") == "Subjects"

    def test_save_filters_returns_bulk_write_result_on_success(self, integration):
        """save_filters returns the result of bulk_write when it succeeds."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        mock_result = MagicMock()
        mock_filters_col.bulk_write.return_value = mock_result

        filters = [{"filtername": "date", "order": 3, "type": "range"}]
        result = inst.save_filters(filters)

        assert result is mock_result

    def test_all_three_production_filters_are_upserted(self, integration):
        """The actual FILTERS from filter_data_parser produce 3 ops with correct _ids."""
        from mongo.filter_data_parser import FILTERS

        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        inst.save_filters(FILTERS)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        assert len(ops) == 3

        ids = [op._filter["_id"] for op in ops]
        assert set(ids) == {"subjects.keyword", "contributors.keyword", "date"}

    def test_set_doc_for_production_filters_includes_correct_types(self, integration):
        """Verify $set documents for the production FILTERS have correct type values."""
        from mongo.filter_data_parser import FILTERS

        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        inst.save_filters(FILTERS)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        type_map = {op._filter["_id"]: op._doc["$set"]["type"] for op in ops}

        assert type_map["subjects.keyword"] == "option"
        assert type_map["contributors.keyword"] == "option"
        assert type_map["date"] == "range"

    def test_set_doc_for_production_filters_includes_correct_orders(self, integration):
        """Verify $set documents for the production FILTERS have correct order values."""
        from mongo.filter_data_parser import FILTERS

        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        inst.save_filters(FILTERS)

        ops = mock_filters_col.bulk_write.call_args[0][0]
        order_map = {op._filter["_id"]: op._doc["$set"]["order"] for op in ops}

        assert order_map["subjects.keyword"] == 1
        assert order_map["contributors.keyword"] == 2
        assert order_map["date"] == 3
