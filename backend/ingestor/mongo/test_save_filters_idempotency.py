"""
Integration tests for MongoIntegration.save_filters() idempotency.

The unit/edge-case tests in test_mongo_integration.py and
test_save_filters_edge_cases.py each call save_filters() exactly once per
test. None of them prove what happens on a SECOND consecutive ingestion run
with the same FILTERS list (the real-world steady-state case once
"contributors" has already been purged) -- specifically: does calling
save_filters() twice in a row with an unchanged FILTERS list produce the
same upsert/delete shape both times (no duplication, no flapping, no growing
delete_many calls), and does delete_many's $nin list stay identical across
runs?
"""

from unittest.mock import MagicMock, patch
import pytest


with patch("pymongo.MongoClient"):
    from mongo.mongo_integration import MongoIntegration
    from mongo.filter_data_parser import FILTERS


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


class TestSaveFiltersIdempotency:
    def test_calling_twice_with_same_filters_produces_identical_op_shapes(self, integration):
        """Two consecutive runs with the same FILTERS list must each upsert
        the same 3 ops with the same _id/$set shape -- no duplication or
        drift between runs (e.g. accumulating extra ops on a shared list)."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        inst.save_filters(FILTERS)
        first_call_ops = mock_filters_col.bulk_write.call_args[0][0]
        first_ids = sorted(op._filter["_id"] for op in first_call_ops)

        inst.save_filters(FILTERS)
        second_call_ops = mock_filters_col.bulk_write.call_args[0][0]
        second_ids = sorted(op._filter["_id"] for op in second_call_ops)

        assert mock_filters_col.bulk_write.call_count == 2
        assert len(first_call_ops) == 3
        assert len(second_call_ops) == 3
        assert first_ids == second_ids == ["creators", "date", "subjects"]

    def test_calling_twice_issues_delete_many_with_same_nin_list_both_times(self, integration):
        """delete_many's $nin filter must be derived fresh from the current
        FILTERS each call, so back-to-back runs with an unchanged FILTERS
        list issue the exact same $nin set both times rather than growing,
        shrinking, or otherwise flapping."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        inst.save_filters(FILTERS)
        inst.save_filters(FILTERS)

        assert mock_filters_col.delete_many.call_count == 2
        first_nin = mock_filters_col.delete_many.call_args_list[0][0][0]["_id"]["$nin"]
        second_nin = mock_filters_col.delete_many.call_args_list[1][0][0]["_id"]["$nin"]
        assert sorted(first_nin) == sorted(second_nin) == ["creators", "date", "subjects"]

    def test_second_run_with_fewer_filters_purges_the_removed_one(self, integration):
        """Simulates a FILTERS list shrinking between two consecutive runs
        (e.g. mid-migration away from a filter): the second run's delete_many
        must reflect only the filters present in THAT run, purging anything
        upserted by the first run that is no longer current."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        first_run_filters = [
            {"filtername": "subjects", "order": 1, "type": "option"},
            {"filtername": "creators", "order": 2, "type": "option"},
            {"filtername": "date", "order": 3, "type": "range"},
        ]
        inst.save_filters(first_run_filters)

        second_run_filters = [
            {"filtername": "subjects", "order": 1, "type": "option"},
            {"filtername": "date", "order": 2, "type": "range"},
        ]
        inst.save_filters(second_run_filters)

        second_nin = mock_filters_col.delete_many.call_args_list[1][0][0]["_id"]["$nin"]
        assert sorted(second_nin) == ["date", "subjects"]
        assert "creators" not in second_nin

    def test_calling_twice_does_not_mutate_the_shared_filters_constant(self, integration):
        """save_filters() must not mutate the FILTERS list/dicts it's given
        (e.g. by injecting "_id" into the original dict in place in a way
        that leaks into the next call or other consumers of FILTERS)."""
        inst, mock_db = integration
        mock_filters_col = MagicMock()
        mock_db.__getitem__.return_value = mock_filters_col

        snapshot_before = [dict(f) for f in FILTERS]

        inst.save_filters(FILTERS)
        inst.save_filters(FILTERS)

        assert FILTERS == snapshot_before
