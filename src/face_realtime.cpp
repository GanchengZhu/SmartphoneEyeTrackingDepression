#include <ros/ros.h>
#include <sensor_msgs/PointCloud2.h>
#include <sensor_msgs/Image.h>
// PCL specific includes
#include <pcl_conversions/pcl_conversions.h>
#include <pcl/point_cloud.h>
#include <pcl/point_types.h>
#include <pcl/filters/voxel_grid.h>
#include <pcl/io/pcd_io.h>

#include <iostream>
#include <pcl/visualization/cloud_viewer.h>
#include <pcl/filters/statistical_outlier_removal.h>
#include <pcl/ModelCoefficients.h>
#include <pcl/sample_consensus/method_types.h>
#include <pcl/sample_consensus/model_types.h>
#include <pcl/segmentation/sac_segmentation.h>
#include <pcl/filters/extract_indices.h>
#include <pcl/common/centroid.h>
#include <pcl/io/pcd_io.h>
#include <pcl/point_types.h>
#include <pcl/common/common.h>


#include <pcl/ModelCoefficients.h>
#include <pcl/filters/voxel_grid.h>
#include <pcl/features/normal_3d.h>
#include <pcl/kdtree/kdtree.h>
#include <pcl/sample_consensus/method_types.h>
#include <pcl/sample_consensus/model_types.h>
#include <pcl/segmentation/sac_segmentation.h>
#include <pcl/segmentation/extract_clusters.h>

#include <pcl/filters/passthrough.h>
//openCV includes
//#include <image_transport/image_transport.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/core/core.hpp>
//#include <cv_bridge/cv_bridge.h>
#include <sensor_msgs/image_encodings.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <pcl/common/centroid.h>
#include <geometry_msgs/Pose.h>
#include <pcl/common/transforms.h>

#include <opencv2/opencv.hpp>

#include <streambuf>
#include "tenginekit_api.h"
#include <chrono>
#include <stdio.h>
#include <sys/types.h>
#include <dirent.h>
#include <opencv2/core/utils/filesystem.hpp>
#include <algorithm>
#include <iostream>
#include <string>



#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

using namespace cv;


class face_realtime
{
  cv::Mat cv_image;
  ros::NodeHandle nh;
  ros::Publisher pub_1;
  ros::Publisher pub_2;
  ros::Publisher pub_3;
  ros::Subscriber sub_1;
  ros::Subscriber sub_2;


public:

  face_realtime();

private:

  void image_callback(const sensor_msgs::ImageConstPtr& img_ptr);

};

face_realtime::face_realtime()
{

  sub_2=nh.subscribe<sensor_msgs::Image>("/camera/rgb/image_rect_color", 1, &face_realtime::image_callback,this);

}

FaceSDKConfig getConfig(int w, int h)
{
  FaceSDKConfig config;
  config.img_w = w;
  config.img_h = h;
  config.screen_w = w;
  config.screen_h = h;
  config.input_format = ImageFormat::BGR;
  config.mode = FaceSDKMode::Normal;
  config.thread_num = 2;
  return config;
}

void face_realtime::image_callback(const sensor_msgs::ImageConstPtr& img_ptr)
{
  cv::Mat frame;
  cv_bridge::CvImagePtr cv_ptr;
  cv_ptr = cv_bridge::toCvCopy(img_ptr);
  frame=cv_ptr->image;

  cv::Rect face_select;

  int w = frame.cols;
  int h = frame.rows;


  FaceSDKConfig config = getConfig(w, h);
  facesdk_init(config);

  char data[w * h * 3];
  memcpy(data, (char *)frame.data, w * h * 3);
  facesdk_readModelFromFile(ModelType::Detect, "/home/robotics-attention/face_landmark/models/face_detect.bin", ImageFormat::RGB);
  sdkFaces faces = facesdk_detect(data);
  std::cout << "faces:" << faces.face_count << std::endl;
  for (int i = 0; i < faces.face_count; i++)
  {
    cv::Point pt1(faces.info[i].face_box.x1-5, faces.info[i].face_box.y1-30);
    cv::Point pt2(faces.info[i].face_box.x2, faces.info[i].face_box.y2-20);
    cv::rectangle(frame, pt1, pt2, cv::Scalar(255, 0, 0), 2);
    face_select=cv::Rect(pt1.x, pt1.y,
                         abs(pt2.x-pt1.x), abs(pt2.y-pt1.y));;

    std::cout << "face_select:" << face_select.x << ", "<<face_select.y<<
                 ", "<<face_select.width<<", "<<face_select.height<< std::endl;
  }

  facesdk_readModelFromFile(ModelType::Landmark, "/home/robotics-attention/face_landmark/models/face_landmark2d.bin", ImageFormat::RGB);
  sdkFaces faces2 = facesdk_landmark();
  int index = 0;
  //  for (int i = 0; i < facesdk_landmark.size(); i++) {
  //                          Rect r = faceLandmarks.get(i).getBoundingBox();
  //  }
  for (int j = 0; j < faces2.face_count; j++)
  {
    index++;
    for (int i = 0; i < 424; i = i + 2)
    {

      cv::Point pt(faces2.info[j].landmarks[i], faces2.info[j].landmarks[i + 1]);
      //      cv::putText(frame,to_string(i),pt,cv::FONT_HERSHEY_SIMPLEX,0.05,cv::Scalar(0,0,255),2,8);
      cv::circle(frame, pt, 1, cv::Scalar(255, 0, 0), 1);
    }

//    for (int i = 202; i < 205; i = i + 4)
//    {

//      cv::Point pt(faces2.info[j].landmarks[i], faces2.info[j].landmarks[i + 1]);
//      cv::putText(frame,to_string(i),pt,cv::FONT_HERSHEY_SIMPLEX,0.5,cv::Scalar(0,0,255),0.02,8);
//      //      cv::circle(frame, pt, 1, cv::Scalar(255, 0, 0), 1);
//    }

  }

  cv::imshow("face",frame);
  cv::waitKey(3);


  facesdk_release();

  //  imwrite("2.bmp",imgRGB);

}




int main(int argc, char** argv)
{
  ros::init(argc, argv, "face_realtime");
  face_realtime tracker;
  ros::spin();
}
