package util;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
 * Purpose: plays intro.mp4 (from the project's public/assets/animations
 * folder) inside the kiosk window when the app starts and every time the
 * flow loops back to the beginning. The video always runs to the end;
 * pressing the window or screen does NOT skip it. KioskMain decides where
 * to go next via play(Runnable onFinished).
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
    }

    /**
     * Plays intro.mp4 and calls {@code onFinished} on the Swing thread when
     * the video ends (or cannot be played).
     */
    public void play(Runnable onFinished) {
        this.onFinished = onFinished;
        stop();   // clean up any previous playback first

        final String mediaUrl = findVideo();
        if (mediaUrl == null) {
            finish();   // no video found: continue instead of hanging on the background
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
                    if (onFinished != null) {
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
            view.setFitWidth(width);           // the video stretches to
            view.setFitHeight(height);         // fill the whole window
            Scene videoScene = new Scene(new Group(view), width, height, FX_BACKGROUND);
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

    /** Single exit point: the video ended or failed. Always returns to Swing. */
    private void finish() {
        Runnable done = onFinished;
        onFinished = null;          // can only fire once per playback
        stop();
        if (done != null) {
            SwingUtilities.invokeLater(done);   // back to the Swing thread
        }
    }

    // -------------------------------------------------------------
    // Video lookup: the project's public/assets/animations folder
    // -------------------------------------------------------------

    /**
     * Finds intro.mp4 in the project's public/assets/animations folder.
     * A project-relative path, never a hardcoded absolute one.
     */
    private static String findVideo() {
        String[] files = {
            "public/assets/animations/intro.mp4",                 // run from the project folder
            "GUDO.Restobar/public/assets/animations/intro.mp4"    // run from the parent folder
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
