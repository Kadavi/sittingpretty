package demo;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FilePart;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import demo.model.User;
import demo.security.Authenticator;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;

public class Main extends Application {

    private Stage stage;
    private User loggedUser;
    private final double MINIMUM_WINDOW_WIDTH = 500.0;
    private final double MINIMUM_WINDOW_HEIGHT = 500.0;

    static ScheduledExecutorService executor;
    static User32 user32 = User32.INSTANCE;

    public static void main(String[] args) {
        Application.launch(Main.class, (java.lang.String[])null);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            stage = primaryStage;
            stage.setTitle("Polite Stare");
            stage.setMinWidth(MINIMUM_WINDOW_WIDTH);
            stage.setMinHeight(MINIMUM_WINDOW_HEIGHT);
            gotoLogin();
            primaryStage.show();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void stop() {
        stopLoop();
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
                    AsyncHttpClient.BoundRequestBuilder builder = client.preparePost("http://ec2-54-241-86-115.us-west-1.compute.amazonaws.com:8080/greatbench/");

                    builder.setHeader("Content-Type", "multipart/form-data");
                    builder.addParameter("email", "coedry@gmail.com");
                    builder.addParameter("key", "#$@#$");
                    builder.addBodyPart(new FilePart("fileupload", file, fileTypeMap.getContentType(file), "UTF-8"));

                    System.out.println("Sending");

                    try {

                        result = builder.execute().get().getResponseBody();

                        if (!result.isEmpty() && result.contains(",")) {

                            System.out.println("Clicking");
                            mouseClick(result.split(","), rect, currentFocusedHwnd, false);

                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                    }
                }

                try {

                    BufferedImage image = new Robot().createScreenCapture(new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit().getScreenSize()));

                    ImageIO.write(image, "jpg", file);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

                AsyncHttpClient client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().build());
                AsyncHttpClient.BoundRequestBuilder builder = client.preparePost("http://ec2-54-241-86-115.us-west-1.compute.amazonaws.com:8080/greatbench/");

                builder.setHeader("Content-Type", "multipart/form-data");
                builder.addParameter("email", "coedry@gmail.com");
                builder.addParameter("key", "#$@#$");
                builder.addBodyPart(new FilePart("fileupload", file, fileTypeMap.getContentType(file), "UTF-8"));

                System.out.println("Sending" + System.nanoTime());

                try {

                    result = builder.execute().get().getResponseBody();

                    if (!result.isEmpty() && result.contains(",")) {

                        System.out.println("Clicking");
                        mouseClick(result.split(","), new Rectangle(), currentFocusedHwnd, true);

                    }

                } catch (Exception e) {

                    e.printStackTrace();

                }
            }
        };

        executor.scheduleAtFixedRate(periodicTask, 0, 10, TimeUnit.SECONDS);

    }

    public static void stopLoop() {

        executor.shutdown();

    }

    public static void mouseClick(String[] relativePosition, Rectangle offset, WinDef.HWND refocus, boolean isFull) throws AWTException {

        if (relativePosition.length > 2) return;

        java.awt.Point current = MouseInfo.getPointerInfo().getLocation();

        int x = (int) (offset.getX() + Double.parseDouble(relativePosition[0]));
        int y = (int) (offset.getY() + Double.parseDouble(relativePosition[1]));

        Robot robot = new Robot();
        if ((isFull && y > 300) || !isFull) {
            robot.mouseMove(x, y);
        }

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(200);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(5);
        robot.mouseMove((int) current.getX() + 1, (int) current.getY() + 1);

        user32.SetForegroundWindow(refocus);

    }

    public User getLoggedUser() {
        return loggedUser;
    }
        
    public boolean userLogin(String userId, String password){
        if (Authenticator.validate(userId, password)) {
            loggedUser = User.of(userId);
            gotoProfile();
            System.out.println(Authenticator.token);
            return true;
        } else {
            return false;
        }
    }
    
    void userLogout(){
        loggedUser = null;
        gotoLogin();
    }
    
    private void gotoProfile() {
        try {
            ProfileController profile = (ProfileController) replaceSceneContent("Profile.fxml");
            profile.setApp(this);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void gotoLogin() {
        try {
            LoginController login = (LoginController) replaceSceneContent("Login.fxml");
            login.setApp(this);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Initializable replaceSceneContent(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        InputStream in = Main.class.getResourceAsStream(fxml);
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        loader.setLocation(Main.class.getResource(fxml));
        AnchorPane page;
        try {
            page = (AnchorPane) loader.load(in);
        } finally {
            in.close();
        } 
        Scene scene = new Scene(page, 500, 500);
        stage.setScene(scene);
        stage.sizeToScene();
        return (Initializable) loader.getController();
    }
}
