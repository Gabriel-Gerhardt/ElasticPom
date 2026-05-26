from itertools import islice
from oaipmh_scythe import Scythe

class Ingestor:
    def __init__(self, oai_identifier : str, metadata_prefix : str, _set : str):
        self.oai_identifier = oai_identifier
        self.metadata_prefix = metadata_prefix
        self._set = _set


    def run(self):
        with Scythe(self.oai_identifier) as scythe:
            if self._set is not None and self._set!= "":
                records = scythe.list_records(metadata_prefix=self.metadata_prefix, set_=self._set)
            else:
                records = scythe.list_records(metadata_prefix=self.metadata_prefix)
            while True:
                batch = list(islice(records, 1000))
                if not batch:
                    break
                yield batch