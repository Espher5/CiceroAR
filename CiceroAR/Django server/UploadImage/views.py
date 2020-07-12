import os
import logging

from DjangoServer.settings import MEDIA_ROOT

from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework import status

from .serializers import ImageSerializer
from ImageProcessing.ImageDetector import ImageDetector

from django.http import JsonResponse


logger = logging.getLogger(__name__)


class ImageUploadView(APIView):
    parser_classes = (MultiPartParser, )

    @staticmethod
    def post(request):
        image_serializer = ImageSerializer(data=request.data)

        if image_serializer.is_valid():

            """
            Creates an instance of the ImageDetector class,
            runs the detection on the received image and logs the result
            """
            image_serializer.save()
            image_detector = ImageDetector('media.jpg')
            detection_result = image_detector.process_image()
            logging.info(detection_result)

            """
            Deletes the received image and returns a JSON response 
            containing the title of the image or None, in case nothing was detected
            """
            os.remove(os.path.join(MEDIA_ROOT, 'media.jpg'))
            return JsonResponse({'title': detection_result[1][0]}, safe=False) if detection_result[0] > 0.99 else JsonResponse({'title': None}, safe=False)
        else:
            return Response(image_serializer.data, status=status.HTTP_400_BAD_REQUEST)
