from rest_framework import serializers
from .models import UploadImage

class ImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = UploadImage
        fields = '__all__'
