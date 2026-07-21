from kafka import Serializer
import json

class JsonKafkaSerializer(Serializer):
    def serialize(self, topic, data):
        return json.dumps(data).encode("utf-8")