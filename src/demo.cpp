#include <iostream>
#include <opencv2/opencv.hpp>
#include <fstream>
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
using namespace std;

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


void handle_rgb(std::string path)
{
  //    float eye_close_v = 0.75f;
  std::string image_file = path;
  std::string image_file_save = path;
  cv::Mat frame = cv::imread(image_file);
  cv::Mat frame_raw=cv::imread(image_file);
  cv::Mat ROI_right,ROI_left,ROI_face;
  cv::Rect face_select;

  int w = frame.cols;
  int h = frame.rows;

  //  int eye_keypoints[8]={};

  FaceSDKConfig config = getConfig(w, h);
  facesdk_init(config);

  char data[w * h * 3];
  memcpy(data, (char *)frame.data, w * h * 3);
  facesdk_readModelFromFile(ModelType::Detect, "/home/zs/face_landmark/models/face_detect.bin", ImageFormat::RGB);
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

  facesdk_readModelFromFile(ModelType::Landmark, "/home/zs/face_landmark/models/face_landmark2d.bin", ImageFormat::RGB);
  sdkFaces faces2 = facesdk_landmark();
  int index = 0;
  //  for (int i = 0; i < facesdk_landmark.size(); i++) {
  //                          Rect r = faceLandmarks.get(i).getBoundingBox();
  //  }
  for (int j = 0; j < faces2.face_count; j++)
  {
    index++;
    //        for (int i = 0; i < 424; i = i + 2)
    ////    for (int i = 234; i < 252; i = i + 2)
    //    {

    //      cv::Point pt(faces2.info[j].landmarks[i], faces2.info[j].landmarks[i + 1]);
    //      //      cv::putText(frame,to_string(i),pt,cv::FONT_HERSHEY_SIMPLEX,0.05,cv::Scalar(0,0,255),2,8);
    //      cv::circle(frame, pt, 1, cv::Scalar(255, 0, 0), 1);
    //    }

    //    for (int i = 202; i < 205; i = i + 4)
    //    {

    //      cv::Point pt(faces2.info[j].landmarks[i], faces2.info[j].landmarks[i + 1]);
    //      cv::putText(frame,to_string(i),pt,cv::FONT_HERSHEY_SIMPLEX,0.5,cv::Scalar(0,0,255),0.02,8);
    ////      cv::circle(frame, pt, 1, cv::Scalar(255, 0, 0), 1);
    //    }
    //    std::cout << std::endl;

    //    eye_keypoints[0]=101;
    //    eye_keypoints[1]=105;
    //    eye_keypoints[2]=109;
    //    eye_keypoints[3]=113;

    cv::Point pt_le1(faces2.info[j].landmarks[202]-10, faces2.info[j].landmarks[203]+10);
    cv::Point pt_le2(faces2.info[j].landmarks[220]+10, faces2.info[j].landmarks[203]-10);

    cv::rectangle(frame, pt_le1, pt_le2, cv::Scalar(255, 0, 0), 2);
//    std::cout << pt_le1.x<<", "<<pt_le1.y << std::endl;

    cv::Point pt_re1(faces2.info[j].landmarks[252]-10, faces2.info[j].landmarks[235]+10);
    cv::Point pt_re2(faces2.info[j].landmarks[234]+10, faces2.info[j].landmarks[235]-10);

    cv::rectangle(frame, pt_re1, pt_re2, cv::Scalar(255, 0, 0), 2);


    cv::Rect left_select = cv::Rect(pt_le1.x, pt_le2.y,
                                    abs(pt_le2.x-pt_le1.x), abs(pt_le2.y-pt_le1.y));

    cv::Rect right_select = cv::Rect(pt_re1.x, pt_re2.y,
                                     abs(pt_re2.x-pt_re1.x), abs(pt_re2.y-pt_re1.y));



    ROI_left = frame_raw(left_select);
    ROI_right = frame_raw(right_select);
    ROI_face = frame_raw(face_select);

    //        if (faces2.info[j].lefteye_close_state < eye_close_v)
    //        {
    //            std::cout << "左眼开" << std::endl;
    //            cv::putText(frame, "left eye open",
    //                        cv::Point(10, 20 + index * 30), cv::FONT_HERSHEY_DUPLEX, 1, cv::Scalar(0, 255, 255), 2);
    //        }
    //        else
    //        {
    //            std::cout << "左眼关" << std::endl;
    //            cv::putText(frame, "left eye close",
    //                        cv::Point(10, 20 + index * 30), cv::FONT_HERSHEY_DUPLEX, 1, cv::Scalar(0, 255, 255), 2);
    //        }

    //        if (faces2.info[j].righteye_close_state < eye_close_v)
    //        {
    //            std::cout << "右眼开" << std::endl;
    //            cv::putText(frame, "right eye open",
    //                        cv::Point(10, 50 + index * 30), cv::FONT_HERSHEY_DUPLEX, 1, cv::Scalar(0, 255, 255), 2);
    //        }
    //        else
    //        {
    //            std::cout << "右眼关" << std::endl;
    //            cv::putText(frame, "right eye close",
    //                        cv::Point(10, 50 + index * 30), cv::FONT_HERSHEY_DUPLEX, 1, cv::Scalar(0, 255, 255), 2);
    //        }
    //        std::cout << std::endl;
  }
  //    facesdk_readModelFromFile(ModelType::Attribution, "/home/zs/face_landmark/models/face_attr.bin", ImageFormat::RGB);
  //    sdkFaces faces3 = facesdk_attribute();
  //    std::cout << faces3.face_count << std::endl;
  //    for (int i = 0; i < faces3.face_count; i++)
  //    {
  //        std::cout << "age: " << faces3.info[i].attribution.age << std::endl;
  //        std::cout << "gender: " << faces3.info[i].attribution.gender << std::endl;
  //        std::cout << "glasses: " << faces3.info[i].attribution.glasses << std::endl;
  //        std::cout << "smile: " << faces3.info[i].attribution.smile << std::endl;
  //        std::cout << "beauty_man_look: " << faces3.info[i].attribution.beauty_man_look << std::endl;
  //        std::cout << "beauty_woman_look: " << faces3.info[i].attribution.beauty_woman_look << std::endl;
  //    }
  std::string save_name="/home/zs/face_landmark/output/"+path.erase(0,59);
  image_file_save.erase(0,59);
  image_file_save.erase(image_file_save.length()-4,image_file_save.length());
  //  std::cout << image_file_save<<", "<<image_file_save.length() << std::endl;

  std::string save_name_righteye="/home/zs/face_landmark/output/"+image_file_save+"_right_eye.jpg";
  std::string save_name_lefteye="/home/zs/face_landmark/output/"+image_file_save+"_left_eye.jpg";
  std::string save_name_face="/home/zs/face_landmark/output/"+image_file_save+"_face.jpg";


  std::cout << save_name_righteye<< std::endl;
  cv::imwrite(save_name, frame);

  cv::imwrite(save_name_righteye, ROI_right);
  cv::imwrite(save_name_lefteye, ROI_left);
  cv::imwrite(save_name_face, ROI_face);
  //  cv::imwrite("/home/zs/face_landmark/output/output.jpg", frame);
  facesdk_release();
}


int main(int argc, char **argv)
{

  std::string img_dir = "/home/zs/face_landmark/sample/src/FaceDemo/resources/image";
  if(cv::utils::fs::exists(img_dir))
  {
    std::cout << "该文件夹存在" << std::endl;
  }

  // 获取当前文件夹下指定格式的文件
  std::vector<cv::String> img_lists;
  cv::utils::fs::glob(img_dir, "*.jpg", img_lists);
  for(auto name:img_lists)
  {
//    std::cout << name << std::endl;
    //    std::string image_file = name;
    ////    std::string image_file2="/home/zs/face_landmark/sample/src/FaceDemo/resources/image/";
    //    image_file.erase(0,59);
    //    //     std::cout << "/home/zs/face_landmark/output/"+image_file << std::endl;
    //    std::cout << image_file<<", "<<image_file.length() << std::endl;
    //    image_file.erase(image_file.length()-5,image_file.length());
    //    std::cout << image_file<<", "<<image_file.length() << std::endl;
    handle_rgb(name);
  }


  //  handle_rgb(argv[1]);
  return 0;
}
