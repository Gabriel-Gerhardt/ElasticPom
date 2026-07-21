from kafka import Serializer

class KeyKafkaSerializer(Serializer):
    def serialize(self, data):
        return data
