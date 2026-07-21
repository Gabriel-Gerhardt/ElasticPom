from kafka import Serializer

class JsonKafkaSerializer(Serializer):
    def serialize(self, data):
        return data