package util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A small JavaFX video panel for kiosk pages (JavaFX embedded in Swing).
 *
 * Purpose: plays an .mp4 once each time {@link #play()} is called and then
 * keeps the finished picture on screen until {@link #stop()}. The clips end
 * with a blank last frame, so the player stops itself at 90% of the clip.
 *
 * Every playback gets its own brand-new JFXPanel and MediaPlayer, and both
 * are thrown away by stop(). Nothing is reused between visits, so a visit
 * can never inherit a stale or blank state from an earlier one - the
 * animation behaves the same no matter how many times the page is opened.
 * If a playback fails or never starts, it is restarted automatically.
 */
public class VideoPanel extends JPanel {

    /** The clip stops at this fraction, before its blank last frame. */
    private static final double FREEZE_AT = 0.90;

    /** A start that has not begun playing after this long is restarted. */
    private static final int START_TIMEOUT_MILLIS = 1500;
    private static final int MAX_ATTEMPTS = 5;

    /** The kiosk background colour as a JavaFX colour (comes from UiColors). */
    private static final javafx.scene.paint.Color FX_BACKGROUND =
        javafx.scene.paint.Color.rgb(UiColors.BACKGROUND.getRed(),
            UiColors.BACKGROUND.getGreen(), UiColors.BACKGROUND.getBlue());

    private final String path;
    private final int width;
    private final int height;

    /**
     * Identifies the latest play()/stop() call. Every delayed step checks it
     * first, so a step from an older call can never start or replace a player.
     */
    private volatile int request;

    /** The current JavaFX panel (Swing thread only). */
    private JFXPanel fxPanel;
    /** Marks the current attempt, so callbacks of older attempts are ignored. */
    private AtomicBoolean attemptMark;
    /** The current player (FX thread only). */
    private MediaPlayer player;

    /** @param path project-relative .mp4 path, e.g. public/assets/animations/x.mp4 */
    public VideoPanel(String path, int width, int height) {
        super(new BorderLayout());
        this.path = path;
        this.width = width;
        this.height = height;
        Platform.setImplicitExit(false);   // removing a JFXPanel must not shut JavaFX down
        setBackground(UiColors.BACKGROUND);
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
    }

    /** Plays the clip from the start and freezes on its finished picture. */
    public void play() {
        start(++request, 1);
    }

    /** Stops playback and removes the video; the panel goes back to the background. */
    public void stop() {
        request++;   // cancels anything still waiting to start
        release();
    }

    /**
     * Waits until the panel is really on screen and laid out, then starts a
     * fresh playback in a fresh JFXPanel.
     */
    private void start(int id, int attempt) {
        if (id != request) {
            return;   // the page was left (or reopened) in the meantime
        }
        if (!isShowing() || getWidth() <= 0 || getHeight() <= 0) {
            Timer wait = new Timer(20, e -> start(id, attempt));
            wait.setRepeats(false);
            wait.start();
            return;
        }

        final String mediaUrl = findVideo();
        if (mediaUrl == null) {
            return;   // no clip found: leave the plain background
        }

        release();   // drop whatever the previous playback left behind

        final JFXPanel panel = new JFXPanel();
        panel.setPreferredSize(new Dimension(width, height));
        fxPanel = panel;
        add(panel, BorderLayout.CENTER);
        validate();   // give the panel its real size before its scene is set

        final AtomicBoolean playing = new AtomicBoolean(false);
        attemptMark = playing;

        // JavaFX code must run on the FX thread
        Platform.runLater(() -> {
            if (id != request) {
                return;
            }
            disposePlayer();
            try {
                final Media media = new Media(mediaUrl);
                final MediaPlayer playback = new MediaPlayer(media);
                player = playback;
                playback.setMute(true);

                MediaView view = new MediaView(playback);
                view.setFitWidth(width);
                view.setFitHeight(height);
                view.setPreserveRatio(true);
                panel.setScene(new Scene(new Group(view), width, height, FX_BACKGROUND));

                Runnable retry = () -> retryLater(id, attempt, playing);
                media.setOnError(retry);
                playback.setOnError(retry);
                playback.setOnPlaying(() -> playing.set(true));
                playback.setOnReady(() -> {
                    if (player == playback) {
                        // stop at 90%: the player then keeps showing that picture
                        playback.setStopTime(media.getDuration().multiply(FREEZE_AT));
                        playback.play();
                    }
                });
            } catch (RuntimeException e) {
                retryLater(id, attempt, playing);
            }
        });

        // Watchdog: if the clip never began playing, start over.
        Timer watchdog = new Timer(START_TIMEOUT_MILLIS, e -> {
            if (!playing.get()) {
                retryLater(id, attempt, playing);
            }
        });
        watchdog.setRepeats(false);
        watchdog.start();
    }

    /** Swing thread: tries again unless the page changed or we gave up. */
    private void retryLater(int id, int attempt, AtomicBoolean mark) {
        if (attempt < MAX_ATTEMPTS) {
            SwingUtilities.invokeLater(() -> {
                if (mark == attemptMark) {   // ignore callbacks of older attempts
                    start(id, attempt + 1);
                }
            });
        }
    }

    /** Swing thread: removes the current JFXPanel and frees its player. */
    private void release() {
        if (fxPanel != null) {
            remove(fxPanel);
            fxPanel = null;
            revalidate();
            repaint();
        }
        Platform.runLater(this::disposePlayer);
    }

    /** Frees the current player (FX thread only). */
    private void disposePlayer() {
        if (player != null) {
            player.dispose();
            player = null;
        }
    }

    // -------------------------------------------------------------
    // Video lookup: the project's public/assets/animations folder
    // -------------------------------------------------------------

    /** Finds the clip from the project folder or from its parent folder. */
    private String findVideo() {
        String[] files = {
            path,                       // run from the project folder
            "GUDO.Restobar/" + path     // run from the parent folder
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