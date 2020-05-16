import os
import numpy as np
import tensorflow as tf

from distutils.version import StrictVersion
from matplotlib import pyplot as plt
from PIL import Image

from .utils import label_map_util
from .utils import visualization_utils as vis_util
from .utils import ops as utils_ops

from DjangoServer.settings import MEDIA_ROOT

BASE_DIR = 'C:\\Users\\ikime\\Desktop\\TensorFlow\\workspace'
MODEL_NAME = BASE_DIR + '\\training_demo\\trained-inference-graphs\\output_inference_graph_v1.pb'
PATH_TO_FROZEN_GRAPH = MODEL_NAME + '\\frozen_inference_graph.pb'
PATH_TO_LABELS = os.path.join(BASE_DIR, 'training_demo\\annotations', 'label_map.pbtxt')


class ImageDetector:
    def __init__(self, image):
        if StrictVersion(tf.__version__) < StrictVersion('1.9.0'):
            raise ImportError('Please upgrade your TensorFlow installation to v1.9.* or later!')
        self.__image = image

    def process_image(self):
        detection_graph = tf.Graph()
        with detection_graph.as_default():
            od_graph_def = tf.GraphDef()
            with tf.gfile.GFile(PATH_TO_FROZEN_GRAPH, 'rb') as fid:
                serialized_graph = fid.read()
                od_graph_def.ParseFromString(serialized_graph)
                tf.import_graph_def(od_graph_def, name='')

        image = Image.open(os.path.join(MEDIA_ROOT, self.__image))
        image_np = self._load_image_into_numpy_array(image)
        output_dict = self._run_inference_for_single_image(image_np, detection_graph)
        return output_dict['detection_scores'][0]

    @staticmethod
    def _load_image_into_numpy_array(image):
        (im_width, im_height) = image.size
        return np.array(image.getdata()).reshape(
            (im_height, im_width, 3)).astype(np.uint8)

    @staticmethod
    def _run_inference_for_single_image(image, graph):
        with graph.as_default():
            config = tf.ConfigProto()
            config.gpu_options.allow_growth = True

            with tf.Session(config=config) as sess:
                ops = tf.get_default_graph().get_operations()
                all_tensor_names = {output.name for op in ops for output in op.outputs}
                tensor_dict = {}
                for key in [
                    'num_detections', 'detection_boxes', 'detection_scores',
                    'detection_classes', 'detection_masks'
                ]:
                    tensor_name = key + ':0'
                    if tensor_name in all_tensor_names:
                        tensor_dict[key] = tf.get_default_graph().get_tensor_by_name(
                            tensor_name)
                if 'detection_masks' in tensor_dict:
                    detection_boxes = tf.squeeze(tensor_dict['detection_boxes'], [0])
                    detection_masks = tf.squeeze(tensor_dict['detection_masks'], [0])
                    real_num_detection = tf.cast(tensor_dict['num_detections'][0], tf.int32)
                    detection_boxes = tf.slice(detection_boxes, [0, 0], [real_num_detection, -1])
                    detection_masks = tf.slice(detection_masks, [0, 0, 0], [real_num_detection, -1, -1])
                    detection_masks_reframed = utils_ops.reframe_box_masks_to_image_masks(
                        detection_masks, detection_boxes, image.shape[0], image.shape[1])
                    detection_masks_reframed = tf.cast(
                        tf.greater(detection_masks_reframed, 0.5), tf.uint8)
                    tensor_dict['detection_masks'] = tf.expand_dims(
                        detection_masks_reframed, 0)
                image_tensor = tf.get_default_graph().get_tensor_by_name('image_tensor:0')

                # Run inference
                output_dict = sess.run(tensor_dict,
                                       feed_dict={image_tensor: np.expand_dims(image, 0)})

                # all outputs are float32 numpy arrays, so convert types as appropriate
                output_dict['num_detections'] = int(output_dict['num_detections'][0])
                output_dict['detection_classes'] = output_dict[
                    'detection_classes'][0].astype(np.uint8)
                output_dict['detection_boxes'] = output_dict['detection_boxes'][0]
                output_dict['detection_scores'] = output_dict['detection_scores'][0]
                if 'detection_masks' in output_dict:
                    output_dict['detection_masks'] = output_dict['detection_masks'][0]
        return output_dict
