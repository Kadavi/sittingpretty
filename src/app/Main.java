package app;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FilePart;
import com.ning.http.client.Response;
import com.sun.jna.platform.win32.*;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    static ScheduledExecutorService executor;

    static { System.load("C:/Users/Grant Dawson/Documents/opencv/build/java/x64/opencv_java246.dll"); }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("app.fxml"));
        primaryStage.setTitle("Polite Stare");
        primaryStage.setWidth(250);
        primaryStage.setHeight(480);

        Scene scene = new Scene(new Group());
        final ToggleGroup group = new ToggleGroup();

        ToggleButton toggle = new ToggleButton("", new ImageView("app/psyduck.jpg"));

        toggle.setToggleGroup(group);

        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            public void changed(ObservableValue<? extends Toggle> ov, Toggle toggle, Toggle new_toggle) {

                if (toggle == null && new_toggle != null) {
                    runLoop();
                } else {
                    stopLoop();
                }

            }
        });

        HBox hbox = new HBox();

        hbox.getChildren().add(toggle);

        ((Group) scene.getRoot()).getChildren().add(hbox);

        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void runLoop() {
        System.out.println("running");

        executor = Executors.newSingleThreadScheduledExecutor();

        Runnable periodicTask = new Runnable() {
            public void run() {

                WinDef.HWND currentFocusedHwnd = User32.INSTANCE.GetForegroundWindow();

                WinDef.HWND cHwnd = User32.INSTANCE.FindWindow
                        (null, "Netflix - Google Chrome");
                WinDef.RECT cRect = new WinDef.RECT();

                WinDef.HWND fHwnd = User32.INSTANCE.FindWindow
                        (null, "Netflix - Mozilla Firefox");
                WinDef.RECT fRect = new WinDef.RECT();

                if (cHwnd == null && fHwnd == null) {

                    System.out.println("Netflix is not running");

                }

                List<WinDef.RECT> rois = new ArrayList<WinDef.RECT>();

                if (User32.INSTANCE.IsWindowVisible(cHwnd)) {

                    System.out.println("Chrome: " + User32.INSTANCE.IsWindowVisible(cHwnd));
                    System.out.println(cRect.toString());
                    User32.INSTANCE.GetWindowRect(cHwnd, cRect);
                    rois.add(cRect);

                }

                if (User32.INSTANCE.IsWindowVisible(fHwnd)) {

                    System.out.println("Firefox: " + User32.INSTANCE.IsWindowVisible(fHwnd));
                    System.out.println(fRect.toString());
                    User32.INSTANCE.GetWindowRect(fHwnd, fRect);
                    rois.add(fRect);

                }

                String result = null;

                File file = new File("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/sight.jpg");

                for (int i = 0 ; i < rois.size(); i++) {

                    Rectangle rect = new Rectangle();
                    rect.setRect(rois.get(i).toRectangle().getX() + rois.get(i).toRectangle().getWidth()/2,
                            rois.get(i).toRectangle().getY() + rois.get(i).toRectangle().getHeight()/2,
                            rois.get(i).toRectangle().getWidth()/2,
                            rois.get(i).toRectangle().getHeight()/2);

                    try {

                        BufferedImage image = new Robot().createScreenCapture(rect);

                        ImageIO.write(image, "jpg", file);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

                    AsyncHttpClient client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().build());
                    AsyncHttpClient.BoundRequestBuilder builder = client.preparePost("http://localhost:8080/");

                    builder.setHeader("Content-Type", "multipart/form-data");
                    builder.addParameter("email", "coedry@gmail.com");
                    //builder.addParameter("key", "#$@#$");
                    builder.addBodyPart(new FilePart("fileupload", file, fileTypeMap.getContentType(file), "UTF-8"));

                    try {

                        result = builder.execute().get().getResponseBody();

                        if (!result.isEmpty()) {

                            System.out.println(result);

                            if (result.contains(","))
                                mouseClick(result.split(","), rect, currentFocusedHwnd);

                        } else {

                            System.out.println("\nwiff");

                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                    }
                }
            }
        };

        executor.scheduleAtFixedRate(periodicTask, 0, 10, TimeUnit.SECONDS);
    }

    public static void stopLoop() {
        System.out.println("stopping");

        executor.shutdown();
    }

    public static void mouseClick(String[] relativePosition, Rectangle offset, WinDef.HWND refocus) throws AWTException {
        if (relativePosition.length > 2) return;

        java.awt.Point current = MouseInfo.getPointerInfo().getLocation();

        Robot robot = new Robot();

        robot.mouseMove((int)(offset.getX() + (Double.parseDouble(relativePosition[0]))),
                (int)(offset.getY() + Double.parseDouble(relativePosition[1])));

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(200);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.mouseMove((int)current.getX()+1, (int)current.getY()+1);
        robot.delay(5);
        robot.mouseMove((int)current.getX(), (int)current.getY());
        System.out.println("Clicking: (" + ((offset.getX() + (Double.parseDouble(relativePosition[0])))) + "," + (offset.getY() + Double.parseDouble(relativePosition[1])) + ")");

        User32.INSTANCE.SetForegroundWindow(refocus);

    }

    /* Client-only experimentation code. */

    public static void DetectCircle(String path) {

        List<Point> totalCircles = new ArrayList(),
                  totalTriangles = new ArrayList();

        Mat src = Highgui.imread(path, CvType.CV_32FC4);
        //src = src.submat(new Rect(320, 320, 300, 300));

        //Highgui.imwrite("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/cropped.jpg", src);

        Mat circles = new Mat();

        Mat dst = Mat.zeros(src.size(), CvType.CV_32FC4);

        Mat triangleResult = Mat.zeros(src.size(), CvType.CV_32FC4),
              circleResult = Mat.zeros(src.size(), CvType.CV_32FC4);

        Core.inRange(src, new Scalar(205,205,205), new Scalar(245,245,245), circleResult); // White circles

        Imgproc.GaussianBlur(circleResult, circleResult, new Size(7, 7), 7);

        Highgui.imwrite("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/circleResultOne.jpg", circleResult);

        Core.inRange(src, new Scalar(210,210,230), new Scalar(235,235,240), triangleResult); // White circles

        Imgproc.GaussianBlur(triangleResult, triangleResult, new Size(19, 19), 19);

        Core.addWeighted(circleResult, 1.0, triangleResult, 1.0, 50.0, circleResult);

        Highgui.imwrite("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/circleResultTwo.jpg", circleResult);

        Core.inRange(src, new Scalar(15,25,150), new Scalar(65,80,240), triangleResult); // Red masses


        Highgui.imwrite("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/triangleResult.jpg", triangleResult);

        int iCannyUpperThreshold = 20;
        int iMinDistance = 20;
        int iMinRadius = 5;
        int iMaxRadius = 100;
        int iAccumulator = 50;

        Imgproc.HoughCircles(circleResult, circles, Imgproc.CV_HOUGH_GRADIENT,
                1, iMinDistance, iCannyUpperThreshold, iAccumulator,
                iMinRadius, iMaxRadius);

        if (circles.cols() > 0) {
            for (int x = 0; x < circles.cols(); x++)
            {
                double vCircle[] = circles.get(0,x);

                if (vCircle == null)
                    break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int)Math.round(vCircle[2]);

                Core.circle(dst, pt, radius, new Scalar(255, 255, 255), 1);
                totalCircles.add(pt);
                System.out.println("Circle: " + pt);

            }
        }

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        MatOfPoint2f approx = new MatOfPoint2f();

        Imgproc.findContours(triangleResult, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);

        Imgproc.drawContours(dst, contours, 0, new Scalar(255,0,0), 2);

        for (int i = 0 ; i < contours.size() ; i++) {

            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approx, 0, true);

            if (approx.toList().size() > 1)  {

                int averageCenterX = 0, averageCenterY = 0;

                for (int j = 0 ; j < approx.toList().size() ; j++) {

                    averageCenterX += approx.toList().get(j).x;
                    averageCenterY += approx.toList().get(j).y;

                }

                totalTriangles.add(new Point(averageCenterX / approx.toList().size(),
                        averageCenterY / approx.toList().size()));

                System.out.println("Triangle: " + approx.toList());

            }

        }

        for (int i = 0; i < totalCircles.size(); i++) {

            for (int j = 0; j < totalTriangles.size(); j++) {

                double distanceX = totalCircles.get(i).x - totalTriangles.get(j).x;
                double distanceY = totalCircles.get(i).y - totalTriangles.get(j).y;

                if (Math.abs(distanceX) < 10 && Math.abs(distanceY) < 10) {

                    System.out.println("\nMatch!\n" +
                    "(Circle) " + totalCircles.get(i).x + " " + totalCircles.get(i).y + "\n" +
                    "(Triangle) " + totalTriangles.get(j).x + " " + totalTriangles.get(j).y);

                }

            }

        }

        Highgui.imwrite("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/YOW.jpg", dst);

    }

    public static void main(String[] args) {
/*
        BufferedImage image = null;

        try {
            image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        } catch (AWTException e) {
            e.printStackTrace();
        }

        try {
            ImageIO.write(image, "jpg", new File("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/sight.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        DetectCircle("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/sight.jpg");
*/
        launch(args);
    }
}
