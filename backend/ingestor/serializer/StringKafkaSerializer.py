from kafka import Serializer

class StringKafkaSerializer(Serializer):
    def serialize(self, topic, data):
        return data.encode("utf-8") if data else None
