package app;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FilePart;
import com.ning.http.client.Response;
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

                String result = null;

                try {

                    BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

                    ImageIO.write(image, "jpg", new File("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/sight.jpg"));

                } catch (Exception e) {
                    e.printStackTrace();
                }

                File file = new File("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/sight.jpg");

                MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

                AsyncHttpClient client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().build());
                AsyncHttpClient.BoundRequestBuilder builder = client.preparePost("http://localhost:8080/");

                builder.setHeader("Content-Type", "multipart/form-data");

                builder.addBodyPart(new FilePart("fileupload", file, fileTypeMap.getContentType(file), "UTF-8"));

                try {

                    result = builder.execute().get().getResponseBody();

                    if (!result.isEmpty()) {

                        System.out.println(result);

                        mouseClick(result.split(","));

                    } else {

                        System.out.println("wiff");

                    }

                } catch (Exception e) {

                    e.printStackTrace();

                }
            }
        };

        executor.scheduleAtFixedRate(periodicTask, 0, 10, TimeUnit.SECONDS);
    }

    public static void stopLoop() {
        System.out.println("stopping");

        executor.shutdown();
    }

    public static void mouseClick(String[] pointStrings) throws AWTException {
        System.out.println("clicking");

        java.awt.Point current = MouseInfo.getPointerInfo().getLocation();

        Robot robot = new Robot();

        robot.mouseMove((int)Double.parseDouble(pointStrings[0]), (int)Double.parseDouble(pointStrings[1]));

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(300);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.mouseMove((int)current.getX(), (int)current.getY());
    }

    public static void DetectCircle() {

        List<Point> totalCircles = new ArrayList(),
                  totalTriangles = new ArrayList();

        Mat src = Highgui.imread("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/screenshot.jpg", CvType.CV_32F);

        Mat circles = new Mat();

        Mat dst = Mat.zeros(src.size(), CvType.CV_32F);

        Mat triangleResult = Mat.zeros(src.size(), CvType.CV_32F),
              circleResult = Mat.zeros(src.size(), CvType.CV_32F);

        Core.inRange(src, new Scalar(200,200,200), new Scalar(240,240,240), circleResult); // White circles
        Core.inRange(src, new Scalar(0,0,175), new Scalar(10,50,250), triangleResult); // Red masses

        Imgproc.GaussianBlur(circleResult, circleResult, new Size(9, 9), 2, 2);

        int iCannyUpperThreshold = 10;
        int iMinDistance = 100;
        int iMinRadius = 20;
        int iMaxRadius = 50;
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

                Core.circle(dst, pt, radius, new Scalar(255, 255, 255), 10);
                totalCircles.add(pt);
                //System.out.println("Circle: " + pt);

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

                //System.out.println("Triangle: " + approx.toList());

            }

        }

        for (int i = 0; i < totalCircles.size(); i++) {

            for (int j = 0; j < totalTriangles.size(); j++) {

                double distanceX = totalCircles.get(i).x - totalTriangles.get(j).x;
                double distanceY = totalCircles.get(i).y - totalTriangles.get(j).y;

                if (Math.abs(distanceX) < 10 && Math.abs(distanceY) < 10) {

                    System.out.println(totalCircles.get(i).x + " " + totalCircles.get(i).y);

                }

            }

        }

        Highgui.imwrite("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/YOW.jpg", dst);

    }

    public static void main(String[] args) {

        //DetectCircle();

        launch(args);
    }
}
