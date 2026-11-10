package util;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javax.swing.SwingUtilities;

/**
 * The intro video (JavaFX embedded in Swing through JFXPanel).
 *
 * Purpose: plays intro.mp4 inside the kiosk window when the app starts and
 * every time the flow loops back to the beginning. A click or touch anywhere
 * skips it. KioskMain decides where to go next via play(Runnable onFinished).
 *
 * The panel is created once by KioskMain and replayed; the MediaPlayer is
 * created fresh for each playback and disposed afterwards, so looping
 * never leaks memory.
 */
public class IntroVideoPanel extends JFXPanel {

    private MediaPlayer player;
    private Runnable onFinished;

    /** The kiosk background colour as a JavaFX colour (comes from UiColors). */
    private static final javafx.scene.paint.Color FX_BACKGROUND =
        javafx.scene.paint.Color.rgb(UiColors.BACKGROUND.getRed(),
            UiColors.BACKGROUND.getGreen(), UiColors.BACKGROUND.getBlue());

    public IntroVideoPanel() {
        setBackground(UiColors.BACKGROUND);
        Platform.setImplicitExit(false);   // keep the FX engine alive across replays

        // One click / touch anywhere skips the video
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                finish();
            }
        });
    }

    /**
     * Plays intro.mp4 and calls {@code onFinished} on the Swing thread when
     * the video ends OR is skipped.
     */
    public void play(Runnable onFinished) {
        this.onFinished = onFinished;
        stop();   // clean up any previous playback first

        final String mediaUrl = findVideo();
        if (mediaUrl == null) {
            finish();   // no video found anywhere: skip instead of hanging on black
            return;
        }

        if (getWidth() > 0 && getHeight() > 0) {
            startVideo(mediaUrl);
        } else {
            // The kiosk starts with the intro, so the window is not laid out
            // yet: start as soon as this panel has its real size, so the
            // scene (and therefore the video) is created at the right size.
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    removeComponentListener(this);
                    if (onFinished != null) {   // still wanted (not skipped meanwhile)
                        startVideo(mediaUrl);
                    }
                }
            });
        }
    }

    /** Creates a fresh player for one playback. */
    private void startVideo(String mediaUrl) {
        final int width = Math.max(1, getWidth());
        final int height = Math.max(1, getHeight());

        // JavaFX code must run on the FX thread
        Platform.runLater(() -> {
            player = new MediaPlayer(new Media(mediaUrl));
            player.setOnEndOfMedia(this::finish);
            player.setOnError(this::finish);   // if it can't play, continue anyway

            MediaView view = new MediaView(player);
            Scene videoScene = new Scene(new Group(view), width, height,
                FX_BACKGROUND);
            // The video stretches to the scene, and the scene was sized to
            // this panel - so the video fills the window.
            view.fitWidthProperty().bind(videoScene.widthProperty());
            view.fitHeightProperty().bind(videoScene.heightProperty());
            setScene(videoScene);
            player.play();
        });
    }

    /** Stops playback and frees the player (call after every playback). */
    public void stop() {
        final int width = Math.max(1, getWidth());
        final int height = Math.max(1, getHeight());

        Platform.runLater(() -> {
            if (player != null) {
                player.setOnEndOfMedia(null);
                player.setOnError(null);
                player.dispose();   // release video memory before the next loop
                player = null;
            }
            // Leave the plain background behind, no half-drawn last frame
            setScene(new Scene(new Group(), width, height, FX_BACKGROUND));
        });
    }

    /** Single exit point: video finished or clicked. Always returns to Swing. */
    private void finish() {
        Runnable done = onFinished;
        onFinished = null;          // a second click can't fire the callback twice
        stop();
        if (done != null) {
            SwingUtilities.invokeLater(done);   // back to the Swing thread
        }
    }

    // -------------------------------------------------------------
    // Video lookup (class path first, project files as a safety net)
    // -------------------------------------------------------------

    /**
     * Finds intro.mp4: classpath resource first (not an absolute path),
     * then the project-relative file, in case the IDE didn't copy the
     * video into the output folder.
     */
    private static String findVideo() {
        String[] resources = {
            "/intro.mp4",                            // copied next to the classes
            "/public/assets/animations/intro.mp4"    // project root on the classpath
        };
        for (String resource : resources) {
            java.net.URL url = IntroVideoPanel.class.getResource(resource);
            if (url != null) {
                return url.toExternalForm();
            }
        }

        String[] files = {
            "public/assets/animations/intro.mp4",                  // run from the project folder
            "GUDO.Restobar/public/assets/animations/intro.mp4"     // run from the parent folder
        };
        for (String file : files) {
            java.io.File f = new java.io.File(file);
            if (f.exists()) {
                return f.toURI().toString();
            }
        }
        return null;
    }
}
