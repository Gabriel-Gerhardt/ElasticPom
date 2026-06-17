"""
Acceptance tests for filter_data_parser.py and the FILTERS constant.

These tests validate:
- FILTERS has exactly the three expected filters
- Each filter has the required fields (filtername, order, type)
- type values are exactly "option" or "range" — no other values
- order values are 1, 2, 3 (ascending, no gaps, no duplicates)
- filtername values are clean logical names (subjects / creators / date),
  with no ".keyword" suffix — the backend now resolves the keyword
  sub-field internally, so the ingestor no longer needs to know about it.
- No filter is missing any required field
"""

import pytest
from mongo.filter_data_parser import FILTERS

VALID_TYPES = {"option", "range"}
REQUIRED_FIELDS = {"filtername", "order", "type"}


class TestFiltersConstant:
    def test_filters_is_a_list(self):
        assert isinstance(FILTERS, list), "FILTERS must be a list"

    def test_filters_has_exactly_three_entries(self):
        assert len(FILTERS) == 3, f"Expected 3 filters, got {len(FILTERS)}"

    def test_all_filters_have_required_fields(self):
        for f in FILTERS:
            missing = REQUIRED_FIELDS - set(f.keys())
            assert not missing, f"Filter {f} is missing fields: {missing}"

    def test_all_type_values_are_valid(self):
        for f in FILTERS:
            assert f["type"] in VALID_TYPES, (
                f"Filter '{f['filtername']}' has invalid type '{f['type']}'; "
                f"must be one of {VALID_TYPES}"
            )

    def test_order_values_are_1_2_3(self):
        orders = sorted(f["order"] for f in FILTERS)
        assert orders == [1, 2, 3], f"Expected orders [1,2,3], got {orders}"

    def test_order_values_are_unique(self):
        orders = [f["order"] for f in FILTERS]
        assert len(orders) == len(set(orders)), "Duplicate order values found"

    def test_filternames_are_unique(self):
        names = [f["filtername"] for f in FILTERS]
        assert len(names) == len(set(names)), "Duplicate filtername values found"

    def test_expected_filternames_present(self):
        names = {f["filtername"] for f in FILTERS}
        expected = {"subjects", "creators", "date"}
        assert names == expected, f"Expected filternames {expected}, got {names}"

    def test_no_filtername_has_keyword_suffix(self):
        """Filternames must be clean logical names; the backend resolves
        .keyword sub-fields internally now, so callers shouldn't need to."""
        for f in FILTERS:
            assert not f["filtername"].endswith(".keyword"), (
                f"filtername '{f['filtername']}' should not have a .keyword suffix"
            )

    def test_subjects_has_type_option(self):
        subjects = next(f for f in FILTERS if f["filtername"] == "subjects")
        assert subjects["type"] == "option"

    def test_creators_has_type_option(self):
        creators = next(f for f in FILTERS if f["filtername"] == "creators")
        assert creators["type"] == "option"

    def test_date_has_type_range(self):
        date = next(f for f in FILTERS if f["filtername"] == "date")
        assert date["type"] == "range"

    def test_subjects_has_order_1(self):
        subjects = next(f for f in FILTERS if f["filtername"] == "subjects")
        assert subjects["order"] == 1

    def test_creators_has_order_2(self):
        creators = next(f for f in FILTERS if f["filtername"] == "creators")
        assert creators["order"] == 2

    def test_date_has_order_3(self):
        date = next(f for f in FILTERS if f["filtername"] == "date")
        assert date["order"] == 3

    def test_no_extra_unexpected_fields(self):
        """Filters should not contain extra fields beyond the required three."""
        for f in FILTERS:
            extra = set(f.keys()) - REQUIRED_FIELDS
            assert not extra, (
                f"Filter '{f['filtername']}' has unexpected extra fields: {extra}"
            )

    def test_filtername_values_are_strings(self):
        for f in FILTERS:
            assert isinstance(f["filtername"], str), (
                f"filtername must be a string, got {type(f['filtername'])}"
            )

    def test_order_values_are_integers(self):
        for f in FILTERS:
            assert isinstance(f["order"], int), (
                f"order must be an int, got {type(f['order'])} for '{f['filtername']}'"
            )

    def test_type_values_are_strings(self):
        for f in FILTERS:
            assert isinstance(f["type"], str), (
                f"type must be a string, got {type(f['type'])} for '{f['filtername']}'"
            )
