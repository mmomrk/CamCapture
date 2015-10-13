import org.opencv.core.*;

public class CamCapture
{
  static Processor processor;
  static Capturer capturer;
  public static void main(String[] args)
  {
    System.out.println("Starting Cam Capture program");

    // Load the native library.
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    capturer = new Capturer();
    processor = new Processor();
    processor.doTheJob(capturer.getCamMat());


    capturer.finish();
    return;
  }
}