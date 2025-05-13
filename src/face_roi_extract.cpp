#include <iostream>
#include <opencv2/opencv.hpp>
#include <fstream>
#include <streambuf>
#include "tenginekit_api.h"
#include <chrono>


#include <stdio.h>
#include <sys/types.h>
#include <dirent.h>
//#include <opencv2/core/utils/filesystem.hpp>
#include <algorithm>
#include <iostream>
#include <string>
#include <sys/types.h>
#include <dirent.h> //windows开发工具没有这个头文件
#include <unistd.h>
#include <string.h>

#include <string>
#include<sys/io.h>
#include<queue>

using namespace std;
using namespace cv;

#include <iostream>
#include <string>
using namespace std;
template <class T>
int getArrSize(T& arr){
  return sizeof(arr) / sizeof(arr[0]);
}

template <class T>
int getArrLength(T& arr){
  return end(arr) - begin(arr);
}


vector<int> face_gride(12);
char line_face_gride[256];
ofstream face_gride_text;
//ifstream face_gride_text("/home/robotics-attention/face_landmark/output/face_gride_data_save.txt");

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

//旋转函数 旋转方向为逆时针为正
cv::Mat RotateImg(cv::Mat image, double angle)
{
  /*
  对旋转的进行改进，由于图形是一个矩形，旋转后的新图像的形状是一个原图像的外接矩形
  因此需要重新计算出旋转后的图形的宽和高
  */
  int width = image.cols;
  int height = image.rows;

  double radian= angle * CV_PI / 180.;//角度转换为弧度
  double width_rotate = fabs(width*cos(radian))+fabs(height*sin(radian));
  double height_rotate= fabs(width*sin(radian)) + fabs(height*cos(radian));

  //旋转中心 原图像中心点
  cv::Point2f center((float)width / 2.0, (float)height/ 2.0);
  //旋转矩阵
  Mat m1 = cv::getRotationMatrix2D(center, angle, 1.0);
  //m1为2行3列通道数为1的矩阵
  //变换矩阵的中心点相当于平移一样 原图像的中心点与新图像的中心点的相对位置
  m1.at<double>(0, 2) += (width_rotate - width) / 2.;
  m1.at<double>(1, 2) += (height_rotate - height) / 2.;
  Mat imgOut;
  if (image.channels() == 1)
  {
    cv::warpAffine(image, imgOut, m1,cv::Size(width_rotate,height_rotate), cv::INTER_LINEAR, 0, Scalar(255));
  }
  else if (image.channels() == 3)
  {
    cv::warpAffine(image, imgOut, m1,cv::Size(width_rotate, height_rotate), cv::INTER_LINEAR, 0, Scalar(255, 255, 255));
  }
  return imgOut;
}


void handle_rgb(std::string path, string name)
{
  //    float eye_close_v = 0.75f;
  std::string image_file = path;
  std::string image_file_save = path;
  cv::Mat frame = cv::imread(image_file);
//  frame=RotateImg(frame,90);

  cv::Mat frame_raw=cv::imread(image_file);
//  frame_raw=RotateImg(frame_raw,90);

  cv::Mat ROI_right,ROI_left,ROI_face;
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
  vector<double> face_area(3);
  for (int i = 0; i < faces.face_count; i++)
  {
    int h_face,w_face;
    h_face=abs(faces.info[i].face_box.y1-faces.info[i].face_box.y2+30);
    w_face=abs(faces.info[i].face_box.x1-5-faces.info[i].face_box.x2);

    cv::Point pt1(faces.info[i].face_box.x1-5+(w_face-h_face)/2, faces.info[i].face_box.y1+10);
    cv::Point pt2(faces.info[i].face_box.x2-(w_face-h_face)/2, faces.info[i].face_box.y2-20);
    cv::rectangle(frame, pt1, pt2, cv::Scalar(255, 0, 0), 2);
    face_select=cv::Rect(pt1.x, pt1.y,
                         abs(pt2.x-pt1.x), abs(pt2.y-pt1.y));;

    std::cout << "face_select:" << face_select.x << ", "<<face_select.y<<
                 ", "<<face_select.width<<", "<<face_select.height<< std::endl;

    std::cout << "face_size:" << faces.info[i].face_box.x1-5 << ", "<<faces.info[i].face_box.x2<<
                 ", "<<faces.info[i].face_box.y1+10<<", "<<faces.info[i].face_box.y2-20<< std::endl;
    face_area[i]=face_select.area();
  }

  facesdk_readModelFromFile(ModelType::Landmark, "/home/robotics-attention/face_landmark/models/face_landmark2d.bin", ImageFormat::RGB);
  sdkFaces faces2 = facesdk_landmark();
  int index = 0;

  if(faces2.face_count<2)
  {
    for (int j = 0; j < faces2.face_count; j++)
    {
      index++;
      int h_right,h_left;
      h_left=abs(faces2.info[j].landmarks[202]-faces2.info[j].landmarks[220]-40);
      h_right=abs(faces2.info[j].landmarks[252]-faces2.info[j].landmarks[234]-40);

      cv::Point pt_le1(faces2.info[j].landmarks[202]-20, faces2.info[j].landmarks[203]+h_left/2);
      cv::Point pt_le2(faces2.info[j].landmarks[220]+20, faces2.info[j].landmarks[203]-h_left/2);

      cv::rectangle(frame, pt_le1, pt_le2, cv::Scalar(255, 0, 0), 2);

      cv::Point pt_re1(faces2.info[j].landmarks[252]-20, faces2.info[j].landmarks[235]+h_right/2);
      cv::Point pt_re2(faces2.info[j].landmarks[234]+20, faces2.info[j].landmarks[235]-h_right/2);

      cv::rectangle(frame, pt_re1, pt_re2, cv::Scalar(255, 0, 0), 2);


      cv::Rect left_select = cv::Rect(pt_le1.x, pt_le2.y,
                                      abs(pt_le2.x-pt_le1.x), abs(pt_le2.y-pt_le1.y));

      cv::Rect right_select = cv::Rect(pt_re1.x, pt_re2.y,
                                       abs(pt_re2.x-pt_re1.x), abs(pt_re2.y-pt_re1.y));





      ROI_left = frame_raw(left_select);
      ROI_right = frame_raw(right_select);
      ROI_face = frame_raw(face_select);

      face_gride[0]=face_select.height;
      face_gride[1]=face_select.width;
      face_gride[2]=face_select.x;
      face_gride[3]=face_select.y;

      face_gride[4]=left_select.height;
      face_gride[5]=left_select.width;
      face_gride[6]=left_select.x;
      face_gride[7]=left_select.y;

      face_gride[8]=right_select.height;
      face_gride[9]=right_select.width;
      face_gride[10]=right_select.x;
      face_gride[11]=right_select.y;
    }
  }

  else{
    int face_index=0;
    if(face_area[0]>face_area[1])
    {face_index=0;}
    else
    {face_index=1;}

    std::cout << "face_area:" << face_area[face_index] << ", "<<face_area[0]<<
                 ", "<<face_area[1]<< std::endl;



    //    index++;
    int h_right,h_left;
    h_left=abs(faces2.info[face_index].landmarks[202]-faces2.info[face_index].landmarks[220]-40);
    h_right=abs(faces2.info[face_index].landmarks[252]-faces2.info[face_index].landmarks[234]-40);

    cv::Point pt_le1(faces2.info[face_index].landmarks[202]-20, faces2.info[face_index].landmarks[203]+h_left/2);
    cv::Point pt_le2(faces2.info[face_index].landmarks[220]+20, faces2.info[face_index].landmarks[203]-h_left/2);

    cv::rectangle(frame, pt_le1, pt_le2, cv::Scalar(255, 0, 0), 2);

    cv::Point pt_re1(faces2.info[face_index].landmarks[252]-20, faces2.info[face_index].landmarks[235]+h_right/2);
    cv::Point pt_re2(faces2.info[face_index].landmarks[234]+20, faces2.info[face_index].landmarks[235]-h_right/2);

    cv::rectangle(frame, pt_re1, pt_re2, cv::Scalar(255, 0, 0), 2);


    cv::Rect left_select = cv::Rect(pt_le1.x, pt_le2.y,
                                    abs(pt_le2.x-pt_le1.x), abs(pt_le2.y-pt_le1.y));

    cv::Rect right_select = cv::Rect(pt_re1.x, pt_re2.y,
                                     abs(pt_re2.x-pt_re1.x), abs(pt_re2.y-pt_re1.y));

    int h_face,w_face;
    h_face=abs(faces.info[face_index].face_box.y1-faces.info[face_index].face_box.y2+30);
    w_face=abs(faces.info[face_index].face_box.x1-5-faces.info[face_index].face_box.x2);

    cv::Point pt1(faces.info[face_index].face_box.x1-5+(w_face-h_face)/2, faces.info[face_index].face_box.y1+10);
    cv::Point pt2(faces.info[face_index].face_box.x2-(w_face-h_face)/2, faces.info[face_index].face_box.y2-20);

    face_select=cv::Rect(pt1.x, pt1.y,
                         abs(pt2.x-pt1.x), abs(pt2.y-pt1.y));;



    ROI_left = frame_raw(left_select);
    ROI_right = frame_raw(right_select);
    ROI_face = frame_raw(face_select);

    face_gride[0]=face_select.width;
    face_gride[1]=face_select.height;

    face_gride[2]=face_select.x;
    face_gride[3]=face_select.y;

    face_gride[4]=left_select.width;
    face_gride[5]=left_select.height;
    face_gride[6]=left_select.x;
    face_gride[7]=left_select.y;

    face_gride[8]=right_select.width;
    face_gride[9]=right_select.height;
    face_gride[10]=right_select.x;
    face_gride[11]=right_select.y;

  }



  std::string save_name="/home/robotics-attention/face_landmark/output/"+path.erase(0,59);
  std::cout <<"save_name:"<< save_name<< std::endl;

  image_file_save.erase(0,59);
  image_file_save.erase(image_file_save.length()-4,image_file_save.length());

  string target="/image";
  int pos = image_file_save.find(target);
  int n = target.size();
  image_file_save = image_file_save.erase(pos,n);


  //  std::string save_name_righteye="/home/robotics-attention/face_landmark/output/face_image/"+image_file_save+"_right_eye.jpg";
  //  std::string save_name_lefteye="/home/robotics-attention/face_landmark/output/left_eye_image/"+image_file_save+"_left_eye.jpg";
  //  std::string save_name_face="/home/robotics-attention/face_landmark/output/right_eye_image/"+image_file_save+"_face.jpg";


  string target3="resources/";
  int pos3 = image_file_save.find(target3);
  int n3 = target3.size();
  image_file_save = image_file_save.erase(pos3,n3);

  std::string save_name_righteye="/home/robotics-attention/face_landmark/output/right_eye_image/"+image_file_save+".jpg";
  std::string save_name_lefteye="/home/robotics-attention/face_landmark/output/left_eye_image/"+image_file_save+".jpg";
  std::string save_name_face="/home/robotics-attention/face_landmark/output/face_image/"+image_file_save+".jpg";

  std::cout << save_name_face<< std::endl;
  std::cout << save_name_lefteye<< std::endl;
  std::cout << save_name_righteye<< std::endl;


  string target2=name+"/";
  int pos2 = image_file_save.find(target2);
  int n2 = target2.size();
  image_file_save = image_file_save.erase(pos2,n2);

//  string target4=name+"image";
//  int pos4 = image_file_save.find(target4);
//  int n4 = target4.size();
////  image_file_save = image_file_save.erase(pos4,n4);

  face_gride_text.open("/home/robotics-attention/face_landmark/output/face_gride/"+name+"/face_gride_data_save.txt",ios::app|ios::out);
  face_gride_text<<image_file_save<<' '<<face_gride[0]<<' '<<face_gride[1]<<' '<<face_gride[2]
                <<' '<<face_gride[3]<<' '<<face_gride[4]<<' '
               <<face_gride[5]<<' '<<face_gride[6]<<' '
              <<face_gride[7]<<' '<<face_gride[8]<<' '
             <<face_gride[9]<<' '<<face_gride[10]<<' '
            <<face_gride[11]<<endl;
  face_gride_text.close();


  std::cout <<"*******************************************"<< std::endl;
//  cv::imwrite(save_name, frame_raw);

//    cv::imwrite(save_name_righteye, ROI_right);
//    cv::imwrite(save_name_lefteye, ROI_left);
//    cv::imwrite(save_name_face, ROI_face);
  //  int test[]={1,2,3,4,5};
  //  std::end(test)-begin(test);


  facesdk_release();
}



void GetFileNames(string path,std::vector<cv::String>& filenames)
{
    DIR *pDir;
    struct dirent* ptr;
    if(!(pDir = opendir(path.c_str()))){
        cout<<"Folder doesn't Exist!"<<endl;
        return;
    }
    while((ptr = readdir(pDir))!=0) {
        if (strcmp(ptr->d_name, ".") != 0 && strcmp(ptr->d_name, "..") != 0){
            filenames.push_back(path + "/" + ptr->d_name);
    }
    }
    closedir(pDir);
}




int main(int argc, char **argv)
{



  string ss2;
  ss2=argv[1];




  std::string img_dir = "/home/robotics-attention/face_landmark/sample/src/FaceDemo/resources/image/"+ss2+"/image";

//  if((ptr = readdir(dir)) != NULL)
//  {
//    std::cout << "该文件夹存在" << std::endl;
    face_gride_text.open("/home/robotics-attention/face_landmark/output/face_gride/"+ss2=+"/face_gride_data_save.txt",ios::app|ios::out);
    face_gride_text<<"image_name " <<"face_Width "<<"face_Height "
                  <<"face_x "<<"face_y "
                 <<"lefteye_Width "<<"lefteye_Height "
                <<"lefteye_x "<<"lefteye_y "
               <<"righteye_Width "<<"righteye_Height "
              <<"righteye_x "<<"righteye_y "<<endl;
    face_gride_text.close();

//  }


  // 获取当前文件夹下指定格式的文件
  std::vector<cv::String> img_lists;

   GetFileNames(img_dir,img_lists);

  for(auto name:img_lists)
  {
    std::cout << name << std::endl;
    handle_rgb(name,ss2);
  }




  return 0;
}
