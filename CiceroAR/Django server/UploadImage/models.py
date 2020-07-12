from django.db import models


def get_filename(instance, filename):
    extension = filename.split('.')[-1]
    return 'media.jpg'


class UploadImage(models.Model):
    image = models.ImageField(upload_to=get_filename, blank=False, null=False)

    def __str__(self):
        return self.image.name
