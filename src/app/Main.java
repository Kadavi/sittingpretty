package app;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FilePart;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    static ScheduledExecutorService executor;
    
    static User32 user32 = User32.INSTANCE;

    static {

        System.load(System.getProperty("user.dir") + "/src/app/opencv_java246.dll");

    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("app.fxml"));
        primaryStage.setTitle("Polite Stare");
        primaryStage.setWidth(250);
        primaryStage.setHeight(480);

        Scene scene = new Scene(new Group());
        final ToggleGroup group = new ToggleGroup();

        ToggleButton toggle = new ToggleButton("", new ImageView("app/sight.jpg"));

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

        executor = Executors.newSingleThreadScheduledExecutor();

        Runnable periodicTask = new Runnable() {

            public void run() {

                String result = null;

                WinDef.HWND currentFocusedHwnd = user32.GetForegroundWindow();

                WinDef.RECT cRect = new WinDef.RECT();
                WinDef.RECT fRect = new WinDef.RECT();
                WinDef.HWND cHwnd = user32.FindWindow
                        (null, "Netflix - Google Chrome");
                WinDef.HWND fHwnd = user32.FindWindow
                        (null, "Netflix - Mozilla Firefox");

                List<WinDef.RECT> rois = new ArrayList<WinDef.RECT>();

                if (user32.IsWindowVisible(cHwnd)) {

                    //System.out.println("Chrome: " + user32.IsWindowVisible(cHwnd));
                    //System.out.println(cRect.toString());
                    user32.GetWindowRect(cHwnd, cRect);
                    rois.add(cRect);

                }

                if (user32.IsWindowVisible(fHwnd)) {

                    //System.out.println("Firefox: " + user32.IsWindowVisible(fHwnd));
                    //System.out.println(fRect.toString());
                    user32.GetWindowRect(fHwnd, fRect);
                    rois.add(fRect);

                }

                File file = new File("C:/Users/Grant Dawson/IdeaProjects/sittingpretty/src/app/sight.jpg");

                for (int i = 0; i < rois.size(); i++) {

                    Rectangle rect = new Rectangle();
                    rect.setRect(rois.get(i).toRectangle().getX() + rois.get(i).toRectangle().getWidth() / 2,
                            rois.get(i).toRectangle().getY() + rois.get(i).toRectangle().getHeight() / 2,
                            rois.get(i).toRectangle().getWidth() / 2,
                            rois.get(i).toRectangle().getHeight() / 2);

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

                        if (!result.isEmpty() && result.contains(",")) {

                            mouseClick(result.split(","), rect, currentFocusedHwnd);

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

        executor.shutdown();

    }

    public static void mouseClick(String[] relativePosition, Rectangle offset, WinDef.HWND refocus) throws AWTException {

        if (relativePosition.length > 2) return;

        java.awt.Point current = MouseInfo.getPointerInfo().getLocation();

        Robot robot = new Robot();

        robot.mouseMove((int) (offset.getX() + (Double.parseDouble(relativePosition[0]))),
                (int) (offset.getY() + Double.parseDouble(relativePosition[1])));

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(200);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.mouseMove((int) current.getX() + 1, (int) current.getY() + 1);
        robot.delay(5);
        robot.mouseMove((int) current.getX(), (int) current.getY());

        user32.SetForegroundWindow(refocus);

    }

    public static void main(String[] args) {

        launch(args);

    }
}
