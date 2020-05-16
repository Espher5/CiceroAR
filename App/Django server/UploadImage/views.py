from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework import status

from .serializers import ImageSerializer
from ImageProcessing.ImageDetector import ImageDetector


class ImageUploadView(APIView):
    parser_classes = (MultiPartParser, )

    @staticmethod
    def post(request):
        image_serializer = ImageSerializer(data=request.data)

        if image_serializer.is_valid():
            image_serializer.save()
            image_detector = ImageDetector('La-nascita-di-Venere-Botticelli.jpg')
            confidence = image_detector.process_image()
            print(confidence)

            return Response(image_serializer.data, status=status.HTTP_201_CREATED)
        else:
            return Response(image_serializer.data, status=status.HTTP_400_BAD_REQUEST)