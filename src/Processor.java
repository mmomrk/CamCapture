import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.image.ImageProducer;

import static java.lang.Math.*;
import static org.opencv.core.Core.*;



/**
 * Created by mrk on 10/11/2015.
 */
public class Processor
{
  final String path = "/cv/";
  final double centerFactorX = .3;
  final double centerFactorY = .3;

  Mat outputImageT;

  private double moduloVect(Point p1)
  {
    return sqrt(p1.x*p1.x + p1.y*p1.y);
  }

  private double scalarMult(Point p1,Point p2)
  {
    return p1.x*p2.x + p1.y+p2.y;
  }

  private Point multiplyVectScal(Point vect,double scal)
  {
    return new Point(vect.x*scal,vect.y*scal);
  }

  private Point sumVect(Point v1, Point v2)
  {
    return new Point(v1.x+v2.x,v1.y+v2.y);
  }

  public double[] getAlphaRho(Point p1,Point p2, Size imgSize)    //todo testit
  {
    double deltaX = p2.x - p1.x;
    double deltaY = p2.y - p1.y;

    double k = deltaY/deltaX;

    double alpha = atan(k);

    Point center = new Point(imgSize.width/2,imgSize.height/2);

    Point dirVect = new Point(deltaX,deltaY);

    Point bVect = new Point(center.x - p1.x, center.y - p1.y);

    Point bProjectionVect = multiplyVectScal(bVect,scalarMult(dirVect,bVect)/moduloVect(dirVect)/moduloVect(bVect));

    double rho = moduloVect(sumVect(bVect,multiplyVectScal(bProjectionVect,-1)));

    double[] retval = new double[2];

    retval[0] = alpha;
    retval[1] = rho;

    return retval;
  }


  private boolean pointInside(Point point,Point firstGuard,Point secondGuard)
  {
    double leftX = firstGuard.x < secondGuard.x ? firstGuard.x : secondGuard.x;
    double rightX = firstGuard.x < secondGuard.x ? secondGuard.x:firstGuard.x;
    double topY = firstGuard.y < secondGuard.y ? firstGuard.y : secondGuard.y;
    double bottomY = firstGuard.y < secondGuard.y ? secondGuard.y : firstGuard.y;

    if (leftX < point.x & point.x<rightX & topY < point.y & point.y < bottomY)
    {
      return true;
    }
    return false;
  }

  private Point flipXY(Point flipIt)
  {
    return new Point(flipIt.y,flipIt.x);
  }

  private boolean pointsAround(Point p1,Point p2,Point middle)
  { //middle y is ignored!
    double maxX = p1.x > p2.x ? p1.x : p2.x;
    double minX = p1.x < p2.x ? p1.x : p2.x;  //I guess there is a way for this to look beautiful
    if (minX < middle.x & middle.x < maxX)
    {
      return  true;
    }
    return false;
  }

  private int lineInTheCenter(Point p1,Point p2, Size maxXY)
  {
    int leftX = (int) (maxXY.width * centerFactorX);  //assume  that factor is less than .5 and more than 0. actuallly it is like 1-factor. the less it is the bigger the area
    int rightX = (int) (maxXY.width * (1 - centerFactorX));
    int topY = (int) (maxXY.height * (centerFactorY));  //y goes down, hence this
    int bottomY = (int) (maxXY.height * (1-centerFactorY));

    Point firstGuard = new Point(leftX,topY);
    Point secondGuard = new Point(rightX,bottomY);

    if (pointInside(p1,firstGuard,secondGuard) | pointInside(p2,firstGuard,secondGuard))
    {
      return 1;
    }

    double deltaX = p2.x - p1.x;
    double deltaY = p2.y - p1.y;

    Point pw1,pw2;

    if (abs(deltaY/deltaX) < 1)
    {
      pw1 = p1;
      pw2 = p2;
    } else
    {
      pw1 = flipXY(p1);
      pw2 = flipXY(p2);

      int temp = leftX;
      leftX = topY;
      topY = temp;

      temp = rightX;
      rightX = bottomY;
      bottomY = temp;
      deltaX = pw2.x - pw1.x;
      deltaY = pw2.y - pw1.y;
    }
    double k = deltaY/deltaX;
    double b = pw1.y - pw1.x*k;

    double y0 = b + k*leftX;
    double y1 = b + k*rightX;

    if ( pointsAround(pw1,pw2,new Point(leftX,0))  &  (topY < y0 & y0 < bottomY) )
    {
      System.out.println("2 type");
      return 2;
    }
    if ( pointsAround(pw1,pw2,new Point(rightX,0))  &   (topY < y1 & y1 < bottomY) )
    {
      System.out.println("2 type");
      return 2;
    }

    return 0;

  }

  private Mat drawRectangleLimiter (Mat inputImage)
  {
    Point p1 = new Point(inputImage.size().width*centerFactorX,inputImage.size().height*centerFactorY);
    Point p2 = new Point(inputImage.size().width*(1-centerFactorX),inputImage.size().height*(1-centerFactorY));
    Imgproc.rectangle(inputImage,p1,p2,new Scalar(50,50,50));
    return inputImage;
  }

  public void linesFind(Mat inputImage)
  {
    System.out.println("Processor.linesFind");

    int thresh1 = 50;
    int thresh2 = 150;

    Mat[] edgesA = new Mat[3];
    for (int t=0;t<3;t++){
      edgesA[t] = new Mat();
    }
    Imgproc.Canny(inputImage,edgesA[0],thresh1,thresh2);
    Imgcodecs.imwrite(path+"canny-raw0.jpg",edgesA[0]);

    Imgproc.Canny(bilateral(inputImage),edgesA[1],thresh1,thresh2);
    Imgcodecs.imwrite(path+"canny-bilat1.jpg",edgesA[1]);

    Imgproc.Canny(median(inputImage),edgesA[2],thresh1,thresh2);
    Imgcodecs.imwrite(path+"canny-median2.jpg",edgesA[2]);


    int rho = 1;
    float theta = (float) (3.14/180);
    int thresh = 30;
    int minLenght = 40;
    int maxGap = 20;

//    Mat lines = new Mat( inputImage.rows(),inputImage.cols(), CvType.CV_8UC1);
    Mat lines = new Mat();
    Mat edges;
    for (int it=0; it<3;it++)
    {
      edges = edgesA[it].clone();

      Imgproc.HoughLinesP(edges, lines, rho, theta, thresh, minLenght, maxGap);

      double[][] linesA = new double[lines.rows()][4];
//      System.out.println(lines.dump());
      for (int i = 0; i < lines.rows(); i++)
      {
        linesA[i] = lines.get(i, 0);

      }

      Mat linedImage = new Mat();
      linedImage = inputImage.clone();
      linedImage = drawRectangleLimiter(linedImage);
      for (double[] line : linesA)
      {
        Point p1 = new Point(line[0], line[1]);
        Point p2 = new Point(line[2], line[3]);
//        System.out.println(lineInTheCenter(p1,p2,inputImage.size()));
        Scalar color = new Scalar(255,25,255);
        switch (lineInTheCenter(p1, p2, inputImage.size()))
        {
          case 0:
            color = new Scalar(0,0,150);
            break;
          case 1:
            color = new Scalar(250, 50, 120);
                    break;
          case 2:
            color = new Scalar(25, 250, 120);
            System.out.println(p1+""+p2);
        }
        Imgproc.line(linedImage, p1, p2,  color , 2);
      }
      switch (it)
      {
        case 0:
          Imgcodecs.imwrite(path + "lined-raw.jpg", linedImage);
          break;
        case 1:
          Imgcodecs.imwrite(path + "lined-bilat.jpg", linedImage);
          break;
        case 2:
          Imgcodecs.imwrite(path + "lined-median.jpg", linedImage);
          break;
      }

    }
  }

  public void doTheJob(Mat inputImage)
  {
    Imgcodecs.imwrite(path + "pre-processed.jpg", inputImage);
//    bilateral(inputImage);
//    median(inputImage);
    linesFind(inputImage);
  }

  public Mat bilateral(Mat inputImage)
  {
    System.out.println("Processor.bilateral");
    int d = 21;
    int sigmaColor = 40;
    int sigmaSpace = 20;

    outputImageT = new Mat( inputImage.rows(),inputImage.cols(),inputImage.type());

    Imgproc.bilateralFilter(inputImage,outputImageT,d,sigmaColor,sigmaSpace);
    Imgcodecs.imwrite(path+"bilateral.jpg",outputImageT);
    return outputImageT;
  }

  public Mat median(Mat inputImage)
  {
    System.out.println("Processor.median");
    int d=21;

    outputImageT = new Mat( inputImage.rows(),inputImage.cols(),inputImage.type());

    Imgproc.medianBlur(inputImage, outputImageT, d);
    Imgcodecs.imwrite(path+"median.jpg",outputImageT);
    return outputImageT;
  }




}
