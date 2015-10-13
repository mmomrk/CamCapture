import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;


/**
 * Created by mrk on 10/8/2015.
 */
public class Capturer
{
  String path = "/cv/";
  String outFileName = "omg.jpg";
  String inFileName = "img.jpg";
  String cam = "camera_";

  Mat imageIn;
  Mat imageOut;
  Mat imageCam;

  VideoCapture cap;
  int device = 0;

  public Mat getCamMat()
  {
    System.out.println("Capturer.getCamMat");
    pollCam();
    return imageCam;
  }

  private int pollCam()
  {
    System.out.println("Inside Capturer.pollCam");
    cap.open(device);
    cap.grab();
    cap.read(imageCam);
    Imgcodecs.imwrite(path + cam + outFileName, imageCam);
    cap.isOpened();

    if (cap.isOpened() == false)
    {
      System.out.println("cap is closed");
      return -1;
    }

    return 0;
  }

  public  Capturer()
  {
    System.out.println("Inside Capturer constructor. opening image. Device is "+device);
    imageIn = Imgcodecs.imread(path+inFileName);
    imageOut = new Mat( imageIn.rows(),imageIn.cols(),imageIn.type());
    imageCam = new Mat( 480,640,imageIn.type());
    cap = new VideoCapture();
  }

  public void run()
  {
    System.out.println("Inside Capturer.run ");

    pollCam();

  }

  public void finish()
  {
    cap.release();
  }


}
