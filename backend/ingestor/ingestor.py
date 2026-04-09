import pandas as pd
class Ingestor:
    def __init__(self):
        pass

    def run(self, path):
        for chunk in pd.read_json(path, lines=True, chunksize=1000):
            yield chunk


