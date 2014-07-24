package demo;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.feature.shapes.FitData;
import boofcv.alg.feature.shapes.ShapeFittingOps;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FilePart;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import demo.model.User;
import demo.security.Authenticator;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
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

public class Main extends Application {

    private Stage stage;
    private User loggedUser;

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
            stage.setResizable(false);
            gotoLogin();
            stage.getIcons().add(new Image("demo/favicon.png"));
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
                WinDef.HWND cHwnd = user32.FindWindow(null, "Netflix - Google Chrome");
                WinDef.HWND fHwnd = user32.FindWindow(null, "Netflix - Mozilla Firefox");

                List<WinDef.RECT> rois = new ArrayList<WinDef.RECT>();

                if (user32.IsWindowVisible(cHwnd)) {
                    user32.GetWindowRect(cHwnd, cRect);
                    rois.add(cRect);
                }

                if (user32.IsWindowVisible(fHwnd)) {
                    user32.GetWindowRect(fHwnd, fRect);
                    rois.add(fRect);
                }

                for (int i = 0; i < rois.size(); i++) {

                    Rectangle rect = new Rectangle();
                    rect.setRect(rois.get(i).toRectangle().getX() + rois.get(i).toRectangle().getWidth() / 2,
                            rois.get(i).toRectangle().getY() + rois.get(i).toRectangle().getHeight() / 2,
                            rois.get(i).toRectangle().getWidth() / 2,
                            rois.get(i).toRectangle().getHeight() / 2);

                    try {
                        BufferedImage image = new Robot().createScreenCapture(rect);
                        mouseClick(detect(image), rect, currentFocusedHwnd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Rectangle fullRect = new Rectangle(new Point((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth() - Toolkit.getDefaultToolkit().getScreenSize().getWidth()/4), (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() - Toolkit.getDefaultToolkit().getScreenSize().getHeight()/4)),
                        new Dimension((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/4, (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()/4));

                try {
                    BufferedImage image = new Robot().createScreenCapture(fullRect);
                    mouseClick(detect(image), fullRect, currentFocusedHwnd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        executor.scheduleAtFixedRate(periodicTask, 0, 12, TimeUnit.SECONDS);
    }

    public static void stopLoop() {

        executor.shutdown();

    }

    public static void mouseClick(Point2D_F64 relativePosition, Rectangle offset, WinDef.HWND refocus) throws AWTException {
        if (relativePosition == null) {
            return;
        }

        System.out.println("Clicking -> " + (offset.getX() + relativePosition.getX()) + ","
                + (offset.getY() + relativePosition.getY()));

        java.awt.Point current = MouseInfo.getPointerInfo().getLocation();
        Robot robot = new Robot();
        robot.mouseMove((int)(offset.getX() + relativePosition.getX()), (int)(offset.getY() + relativePosition.getY()));
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        user32.SetForegroundWindow(refocus);
        robot.mouseMove((int)current.getX(), (int)current.getY());
    }

    public static Point2D_F64 detect(BufferedImage source) {
        //BufferedImage source = UtilImageIO.loadImage("src/sight.jpg");
        ImageFloat32 input = ConvertBufferedImage.convertFromSingle(source, null, ImageFloat32.class);

        ImageUInt8 binary = new ImageUInt8(input.width, input.height);

        double mean = ImageStatistics.mean(input);

        ThresholdImageOps.threshold(input, binary, (float) mean, true);

        ImageUInt8 grey = BinaryImageOps.erode8(binary, null);
        grey = BinaryImageOps.dilate8(grey, null);

        List<Contour> contours = BinaryImageOps.contour(grey, 8, null);

        Graphics2D g2 = source.createGraphics();
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.GREEN);

        for (Contour c : contours) {
            FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(c.external, 0, false, null);

            if (isCircle(c)) {
                VisualizeShapes.drawEllipse(ellipse.shape, g2);
                //System.out.println("yes circle: " + ellipse.shape.center);

                MultiSpectral<ImageFloat32> inputHsv = ConvertBufferedImage.convertFromMulti(source, null, true,ImageFloat32.class);
                MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class, inputHsv.width, inputHsv.height,3);

                ColorHsv.rgbToHsv_F32(inputHsv, hsv);

                float maxDist2 = 0.4f*0.4f;

                ImageFloat32 H = hsv.getBand(0);
                ImageFloat32 S = hsv.getBand(1);

                float adjustUnits = (float)(Math.PI/2.0);

                float hue = 0.15f;
                float saturation = 0.9f;

                BufferedImage output = new BufferedImage(inputHsv.width, inputHsv.height, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < hsv.height; y++) {
                    for (int x = 0; x < hsv.width; x++) {
                        float dh = UtilAngle.dist(H.unsafe_get(x, y), hue);
                        float ds = (S.unsafe_get(x, y)-saturation)*adjustUnits;

                        float dist2 = dh*dh + ds*ds;
                        if (dist2 <= maxDist2) {
                            output.setRGB(x, y, source.getRGB(x, y));
                        }
                    }
                }

                ImageFloat32 result = ConvertBufferedImage.convertFromSingle(output, null, ImageFloat32.class);

                ImageUInt8 binaryResult = new ImageUInt8(result.width, result.height);

                mean = ImageStatistics.mean(result);

                ThresholdImageOps.threshold(result, binaryResult, (float)mean, true);

                //BinaryImageOps.erode8(binaryResult, binaryResult);
                BinaryImageOps.dilate8(binaryResult, binaryResult);

                contours = BinaryImageOps.contour(binaryResult, 8, null);

                //ShowImages.showWindow(source, "Binary Blob Contours");
                for (Contour d : contours) {
                    List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(d.external, false,
                            4, Math.PI/2, 0);

                    g2.setColor(Color.BLUE);
                    for (List<Point2D_I32> internal : c.internal) {
                        vertexes = ShapeFittingOps.fitPolygon(internal, false, 4, Math.PI/2, 0);
                        VisualizeShapes.drawPolygon(vertexes, true, g2);

                        if ((internal.get(0).getX() - ellipse.shape.getCenter().getX()) < 100) {

                            if (isTriangle(internal)) {
                                //System.out.println("yes triangle: " + ellipse.shape.center);
                                return ellipse.shape.getCenter();
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static double GetAngleOfLineBetweenTwoPoints(Point2D_I32 pointA, Point2D_I32 pointB) {
        double xDiff = pointB.x - pointA.x;
        double yDiff = pointB.y - pointA.y;

        return Math.toDegrees(Math.atan2(yDiff, xDiff));
    }

    public static boolean isTriangle(List<Point2D_I32> points) {
        Point2D_I32 top = new Point2D_I32(points.get(0).getX(), points.get(0).getY()),
                right = new Point2D_I32(points.get(0).getX(), points.get(0).getY()),
                bottom = new Point2D_I32(points.get(0).getX(), points.get(0).getY());

        for (Point2D_I32 p : points) {
            if (top.getY() > p.getY()) {
                top.set(p.getX(), p.getY());
            }

            if (right.getX() < p.getX()) {
                right.set(p.getX(), p.getY());
            }

            if (bottom.getY() < p.getY()) {
                bottom.set(p.getX(), p.getY());
            }
        }

        double topToBottom = GetAngleOfLineBetweenTwoPoints(top, bottom);
        double topToRight = GetAngleOfLineBetweenTwoPoints(top, right);
        double bottomToRight = GetAngleOfLineBetweenTwoPoints(bottom, right);

        if ((topToBottom > 80 && topToBottom < 100) &&
            Math.abs(top.getY() - bottom.getY()) > 10) {

            return true;
        }

        return false;
    }

    public static boolean isCircle(Contour contour) {
        int left = contour.external.get(0).getX(),
                top = contour.external.get(0).getY(),
                right = contour.external.get(0).getX(),
                bottom = contour.external.get(0).getY();

        for (Point2D_I32 p : contour.external) {
            if (left > p.getX()) {
                left = p.getX();
            } else if (right < p.getX()) {
                right = p.getX();
            }

            if (top > p.getY()) {
                top = p.getY();
            } else if (bottom < p.getY()) {
                bottom = p.getY();
            }
        }

        int horizontalDelta = Math.abs(left - right);
        int verticalDelta = Math.abs(top - bottom);

        if ((horizontalDelta > 30 && horizontalDelta < 200) &&
                (verticalDelta > 30 && verticalDelta < 200) &&
                (Math.abs(horizontalDelta - verticalDelta)) <= 2) {

            return true;
        }

        return false;
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

    private boolean isSwitchOn = false;

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
        final Scene scene = new Scene(page);

        try {
            scene.lookup("#switch").setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    isSwitchOn = !isSwitchOn;
                    //System.out.println("rect clicked: " + isSwitchOn);

                    if (isSwitchOn) {
                        scene.lookup("#offState").setVisible(false);
                        scene.lookup("#onState").setVisible(true);

                        runLoop();
                    } else {
                        scene.lookup("#offState").setVisible(true);
                        scene.lookup("#onState").setVisible(false);

                        stopLoop();
                    }
                }
            });
        } catch (Exception ignored) {}

        stage.setScene(scene);

        return (Initializable) loader.getController();
    }
}
