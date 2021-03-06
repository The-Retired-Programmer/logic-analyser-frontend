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

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    
    private Controller controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ExecuteAndCatch.run(() -> startworker(stage)); 
    }
    
    @Override
    public void stop() {
        controller.close();
    }
    
    private void startworker(Stage stage) {
        controller = new Controller();
        Window window = new Window(stage, controller);
        controller.open(window);
    }
}
