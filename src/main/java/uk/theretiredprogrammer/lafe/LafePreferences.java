/*
 * Copyright 2021 Richard Linsdale (richard at theretiredprogrammer.uk).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.theretiredprogrammer.lafe;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

public class LafePreferences {

    // window sizes
    private static final String WINDOW_WIDTH = "windowWidth";
    private static final String WINDOW_HEIGHT = "windowHeight";
    private static final String WINDOW_X_POS = "windowXPos";
    private static final String WINDOW_Y_POS = "windowYPos";
    private static final String WINDOW_MAXIMIZED = "windowMaximized";

    public static void applyWindowSizePreferences(Stage stage, Class clazz, Rectangle2D windowsize) {
        String windowname = clazz.getSimpleName();
        try {
            Preferences packagePreferences = Preferences.userNodeForPackage(clazz);
            if (packagePreferences.nodeExists(windowname)) {
                Preferences stagePreferences = packagePreferences.node(windowname);
                boolean wasMaximized = stagePreferences.getBoolean(WINDOW_MAXIMIZED, false);
                if (wasMaximized) {
                    stage.setMaximized(true);
                } else {
                    stage.setX(stagePreferences.getDouble(WINDOW_X_POS, windowsize.getMinX()));
                    stage.setY(stagePreferences.getDouble(WINDOW_Y_POS, windowsize.getMinY()));
                    stage.setWidth(stagePreferences.getDouble(WINDOW_WIDTH, windowsize.getWidth()));
                    stage.setHeight(stagePreferences.getDouble(WINDOW_HEIGHT, windowsize.getHeight()));
                }
            } else {
                stage.setX(windowsize.getMinX());
                stage.setY(windowsize.getMinY());
                stage.setWidth(windowsize.getWidth());
                stage.setHeight(windowsize.getHeight());
            }
        } catch (BackingStoreException ex) {
            throw new Failure("Could not access preferences for window " + windowname, ex);
        }
    }

    public static void saveWindowSizePreferences(Stage stage, Class clazz) {
        String windowname = clazz.getSimpleName();
        try {
            Preferences stagePreferences = Preferences.userNodeForPackage(clazz).node(windowname);
            if (stage.isMaximized()) {
                stagePreferences.putBoolean(WINDOW_MAXIMIZED, true);
            } else {
                stagePreferences.putBoolean(WINDOW_MAXIMIZED, false);
                stagePreferences.putDouble(WINDOW_X_POS, stage.getX());
                stagePreferences.putDouble(WINDOW_Y_POS, stage.getY());
                stagePreferences.putDouble(WINDOW_WIDTH, stage.getWidth());
                stagePreferences.putDouble(WINDOW_HEIGHT, stage.getHeight());
            }
            stagePreferences.flush();
        } catch (BackingStoreException ex) {
            throw new Failure("Could not flush preferences for window " + windowname, ex);
        }
    }

    public static void clearWindowSizePreferences(Class clazz) {
        String windowname = clazz.getSimpleName();
        try {
            Preferences stagePreferences = Preferences.userNodeForPackage(clazz).node(windowname);
            stagePreferences.removeNode();
            stagePreferences.flush();
        } catch (BackingStoreException ex) {
            throw new Failure("Could not flush preferences for window " + windowname, ex);
        }
    }
}
