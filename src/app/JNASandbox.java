package app;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.*;

import java.awt.*;

// jna-3.5.1.jar and platform-3.5.1.jar

public class JNASandbox {

    public static void main(String[] args) {

        HWND cHwnd = User32.INSTANCE.FindWindow
                (null, "Netflix - Google Chrome");
        RECT cRect = new RECT();

        HWND fHwnd = User32.INSTANCE.FindWindow
                (null, "Netflix - Mozilla Firefox");

        RECT fRect = new RECT();

        if (cHwnd == null && fHwnd == null) {

            System.out.println("Netflix is not running");

        }

        if (User32.INSTANCE.IsWindowVisible(cHwnd)) {

            System.out.println("Chrome: " + User32.INSTANCE.IsWindowVisible(cHwnd));
            User32.INSTANCE.GetWindowRect(cHwnd, cRect);
            System.out.println(cRect.toString());

        }

        if (User32.INSTANCE.IsWindowVisible(fHwnd)) {

            System.out.println("Firefox: " + User32.INSTANCE.IsWindowVisible(fHwnd));
            User32.INSTANCE.GetWindowRect(fHwnd, fRect);
            System.out.println(fRect.toString());

        }

        HWND currentFocusHwnd = User32.INSTANCE.GetForegroundWindow();

        User32.INSTANCE.SetForegroundWindow(currentFocusHwnd);

    }
}