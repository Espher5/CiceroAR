import os
import numpy as np
import tensorflow as tf

from distutils.version import StrictVersion
from PIL import Image
from .utils import label_map_util
from DjangoServer.settings import MEDIA_ROOT


BASE_DIR = 'C:\\Users\\ikime\\Desktop\\TensorFlow\\workspace'
MODEL_NAME = BASE_DIR + '\\training_demo\\trained-inference-graphs\\output_inference_graph_v1.pb'
PATH_TO_FROZEN_GRAPH = MODEL_NAME + '\\frozen_inference_graph.pb'
PATH_TO_LABELS = os.path.join(BASE_DIR, 'training_demo\\annotations', 'label_map.pbtxt')


"""
Class encapsulating the image detection component
The __init__ method receives the name of the image to process with the loaded 
frozen inference graph
"""


class ImageDetector:
    def __init__(self, image):
        if StrictVersion(tf.__version__) < StrictVersion('1.9.0'):
            raise ImportError('Please upgrade your TensorFlow installation to v1.9.* or later!')
        self.__image = image

    """
    Runs the processing function and returns the results
    
    @:returns the tuple created by _run_inference_for_single_image()
    """
    def process_image(self):
        detection_graph = tf.Graph()
        with detection_graph.as_default():
            od_graph_def = tf.compat.v1.GraphDef()
            with tf.io.gfile.GFile(PATH_TO_FROZEN_GRAPH, 'rb') as fid:
                serialized_graph = fid.read()
                od_graph_def.ParseFromString(serialized_graph)
                tf.import_graph_def(od_graph_def, name='')

        image = Image.open(os.path.join(MEDIA_ROOT, self.__image))
        image_np = self._load_image_into_numpy_array(image)
        return self._run_inference_for_single_image(image_np, detection_graph)

    """
    Converts an image into a numpy array
    
    @:param image the image to convert    
    @:returns the numpy array containing the image
    """
    @staticmethod
    def _load_image_into_numpy_array(image):
        (im_width, im_height) = image.size
        return np.array(image.getdata()).reshape((im_height, im_width, 3)).astype(np.uint8)

    """
    Wrapper function to call the model and cleanup the outputs
    
    @:param image the target image
    @:param graph the detection graph used to process the image   
    @:returns a tuple containing the highest detection score 
        and the corresponding class name detected for the image
    """
    @staticmethod
    def _run_inference_for_single_image(image, graph):
        category_index = label_map_util.create_category_index_from_labelmap(PATH_TO_LABELS, use_display_name=True)

        with graph.as_default():
            config = tf.compat.v1.ConfigProto()
            config.gpu_options.allow_growth = True

            with tf.compat.v1.Session(config=config) as session:
                ops = tf.compat.v1.get_default_graph().get_operations()
                all_tensor_names = {output.name for op in ops for output in op.outputs}
                tensor_dict = {}
                for key in ['detection_scores', 'detection_classes']:
                    tensor_name = key + ':0'
                    if tensor_name in all_tensor_names:
                        tensor_dict[key] = tf.compat.v1.get_default_graph().get_tensor_by_name(tensor_name)
                image_tensor = tf.compat.v1.get_default_graph().get_tensor_by_name('image_tensor:0')

                output_dict = session.run(tensor_dict, feed_dict={image_tensor: np.expand_dims(image, 0)})
                output_dict['detection_classes'] = output_dict['detection_classes'][0].astype(np.uint8)
                output_dict['detection_scores'] = output_dict['detection_scores'][0]

        return output_dict['detection_scores'][0], \
               [category_index.get(output_dict['detection_classes'][0]).get('name')]
