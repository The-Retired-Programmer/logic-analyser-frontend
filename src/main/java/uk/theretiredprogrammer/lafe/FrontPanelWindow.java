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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import static javafx.scene.paint.Color.DARKGREY;
import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.RED;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.NumberStringConverter;
import static uk.theretiredprogrammer.lafe.ProbeCommands.ProbeState.STATE_IDLE;

public class FrontPanelWindow {

    private final Class clazz;
    private final Stage stage;
    private Rectangle2D windowsize;
    private Text messagenode;
    private ProbeCommands probecommands;
    private ProbeConfiguration config;
    private FrontPanelController controller;

    public FrontPanelWindow(Stage stage, FrontPanelController controller) {
        this.clazz = FrontPanelWindow.class;
        this.stage = stage;
        this.controller = controller;
        this.probecommands = controller.getProbeCommands();
        this.config = controller.getProbeConfiguration();
        setDefaultWindowWidth(400);
        LafePreferences.applyWindowSizePreferences(stage, clazz, windowsize);
        stage.setScene(buildScene(controller));
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Logic Analyser");
        stage.setOnHiding(e -> ExecuteAndCatch.run(() -> saveWindowSizePreferences()));
        stage.show();
    }
    
    private Scene buildScene(FrontPanelController controller) {
        BorderPane borderpane = new BorderPane();
        //pane.setContextMenu(contextmenu);
        borderpane.setCenter(new ScrollPane(buildEmptySampleDisplay()));
        borderpane.setBottom(buildConfiguration());
        borderpane.setLeft(buildControls());
        borderpane.setTop(messagenode = new Text());
        return new Scene(borderpane);
    }

    public void close() {
        stage.close();
    }

    public void writestatusmessage(String message) {
        messagenode.setText(message);
    }
    
    public final void checkifprobeconnected() {
        messagenode.setText(isprobeconnected()? "Probe Connected": "Probe connection failed");
    }

    public void reset() {
        LafePreferences.clearWindowSizePreferences(clazz);
        LafePreferences.applyWindowSizePreferences(stage, clazz, windowsize);
    }

    private void setDefaultWindowWidth(double width) {
        Rectangle2D screenbounds = Screen.getPrimary().getVisualBounds();
        if (width > screenbounds.getWidth()) {
            windowsize = screenbounds;
            return;
        }
        windowsize = new Rectangle2D(
                screenbounds.getMinX(),
                screenbounds.getMinY(),
                width,
                screenbounds.getHeight()
        );
    }

    private void saveWindowSizePreferences() {
        LafePreferences.saveWindowSizePreferences(stage, clazz);
    }
    
    // -------------------------------------------------------------------------
    //
    // controls panel
    //
    // -------------------------------------------------------------------------
    
    private ProbeCommands.ProbeState state = STATE_IDLE;
    private Lamp connectedlamp;

    public VBox buildControls() {
        VBox vbox = new VBox();
        vbox.getChildren().addAll(
                connectedlamp = new Lamp("Probe connected", RED),
                    new Lamp("Probe Sampling", RED),
                    new StopGoButton()
        );
        return vbox;
    }

    private boolean isprobeconnected() {
//        try {
//            CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(() -> doconnectedcheck());
//            while (!completableFuture.isDone()) {
//                Platform.runLater( () -> probecommands.displaymessage("Waiting to connect to probe"));
//            }
//            Platform.runLater( () -> probecommands.displaymessage(""));
//            return completableFuture.get();
//        } catch (InterruptedException | ExecutionException ex) {
//            return false;
//        }
        return doconnectedcheck();
    }
    
    private boolean doconnectedcheck() {
        try {
            if (probecommands.ping()) {
                connectedlamp.changeColour(GREEN);
            }
            if (probecommands.getState()) {
                state = probecommands.getLastStateResponse();
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
        //
//        
        //
//        int number = 20;
//        Thread newThread = new Thread(() -> {
//            System.out.println("Factorial of " + number + " is: " + factorial(number));
//        });
//        newThread.start();

    public class Lamp extends Group {

        private final Circle colourlens;

        public Lamp(String label, Color colour) {
            colourlens = new Circle(40, 20, 10, colour);
            this.getChildren().addAll(
                    new Circle(40, 20, 15, DARKGREY),
                    colourlens,
                    new Text(label)
            );
        }

        public void changeColour(Color newcolour) {
            colourlens.setFill(newcolour);
        }
    }

    public class StopGoButton extends Group {

        private final Button button;

        public StopGoButton() {
            button = new Button(getButtonText());
            button.relocate(0, 20);
            this.getChildren().add(button);
            button.setOnAction((ev) -> buttonpressed(ev));
        }

        private String getButtonText() {
            switch (state) {
                case STATE_IDLE -> {
                    return "Start Sampling";
                }
                case STATE_SAMPLING -> {
                    return "Stop Sampling";
                }
                case STATE_STOPPING_SAMPLING -> {
                    return "Stopping  in Progress";
                }
                case STATE_SAMPLING_DONE -> {
                    return "Get Sample";
                }
                default ->
                    throw new IllegalProgramStateFailure("illegal state");
            }
        }

        public void buttonpressed(Event ev) {
            switch (state) {
                case STATE_IDLE:
                try {
                    // start sampling
                    probecommands.start();
                } catch (IOException ex) {
                    // failure to start
                    return;
                }
                break;
                case STATE_SAMPLING:
                try {
                    // stop sampling request
                    probecommands.stop();
                } catch (IOException ex) {
                    // failure to stop
                    return;
                }
                break;
                case STATE_STOPPING_SAMPLING:

                    break;
                case STATE_SAMPLING_DONE:
                    Map<Integer, List<String>> samples = new HashMap<>();
                    try {
                        probecommands.data(samples);
                    } catch (IOException ex) {
                        throw new Failure(ex);
                    }
                    controller.setData(samples);
            }
            try {
                if (probecommands.getState()) {
                    state = probecommands.getLastStateResponse();
                }
            } catch (IOException ex) {
                throw new Failure(ex);
            }
            button.setText(getButtonText());
        }
    }
    
    // -------------------------------------------------------------------------
    //
    //    the configuration panel
    //
    // -------------------------------------------------------------------------

    private HBox buildConfiguration() {
        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(
                labelledNode("Sample Source", combineFields(
                        labelledNode("First Pin", integerField(config.firstpin, 3)),
                        labelledNode("Pins", integerField(config.pins, 3)))),
                labelledNode("Sampling Speed", combineFields(
                        labelledNode("speed", integerField(config.speed, 10)),
                        labelledNode("units", hzunitsSelectionField(config.speedunit)),
                        labelledNode("multiplier", integerField(config.speedmultiplier, 3)))),
                labelledNode("Start Trigger", combineFields(
                        labelledNode("Enable", checkboxField(config.st_enabled)),
                        labelledNode("Pin", integerField(config.st_pin, 3)),
                        labelledNode("Pin Level", triggerSelectionField(config.st_level)))),
                labelledNode("Sample Size (bits)", integerField(config.samplesize, 7)),
                labelledNode("Sample End Mode", sampleendmodeSelectionField(config.sampleendmode)),
                labelledNode("Event Trigger", combineFields(
                        labelledNode("Enable", checkboxField(config.et_enabled)),
                        labelledNode("Pin", integerField(config.et_pin, 3)),
                        labelledNode("Pin Level", triggerSelectionField(config.et_level))))
        );
        return hbox;
    }

    private VBox labelledNode(String label, Node node) {
        VBox display = new VBox(2);
        display.setAlignment(Pos.CENTER);
        display.getChildren().addAll(new Label(label), node);
        return display;
    }

    private Node combineFields(Node... fields) {
        HBox display = new HBox(0);
        display.setAlignment(Pos.CENTER);
        display.getChildren().addAll(fields);
        return display;
    }

    private TextField integerField(IntegerProperty property, int size) {
        TextField intfield = new TextField();
        intfield.setPrefColumnCount(size);
        TextFormatter<Number> textformatter = new TextFormatter<>(new NumberStringConverter(), 0.0, integerFilter);
        intfield.setTextFormatter(textformatter);
        textformatter.valueProperty().bindBidirectional(property);
        return intfield;
    }

    static UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        if (change.getControlNewText().matches("-?([0-9]*)?")) {
            return change;
        }
        return null;
    };

    private ComboBox<ProbeConfiguration.HzUnits> hzunitsSelectionField(ObjectProperty<ProbeConfiguration.HzUnits> value) {
        ComboBox<ProbeConfiguration.HzUnits> cbox = new ComboBox<>();
        cbox.getItems().addAll(ProbeConfiguration.HzUnits.values());
        cbox.valueProperty().bindBidirectional(value);
        return cbox;
    }

    private ComboBox<ProbeConfiguration.Trigger> triggerSelectionField(ObjectProperty<ProbeConfiguration.Trigger> value) {
        ComboBox<ProbeConfiguration.Trigger> cbox = new ComboBox<>();
        cbox.getItems().addAll(ProbeConfiguration.Trigger.values());
        cbox.valueProperty().bindBidirectional(value);
        return cbox;
    }

    private ComboBox<ProbeConfiguration.SampleEndMode> sampleendmodeSelectionField(ObjectProperty<ProbeConfiguration.SampleEndMode> value) {
        ComboBox<ProbeConfiguration.SampleEndMode> cbox = new ComboBox<>();
        cbox.getItems().addAll(ProbeConfiguration.SampleEndMode.values());
        cbox.valueProperty().bindBidirectional(value);
        return cbox;
    }

    private CheckBox checkboxField(BooleanProperty value) {
        CheckBox booleanfield = new CheckBox();
        booleanfield.setSelected(value.get());
        booleanfield.selectedProperty().bindBidirectional(value);
        return booleanfield;
    }
    
    // -------------------------------------------------------------------------
    //
    //  the sample display canvas
    //
    // -------------------------------------------------------------------------
    
    private Canvas sampledisplaycanvas;

    private Canvas buildEmptySampleDisplay() {
        return sampledisplaycanvas = new Canvas(500.0, 500.0);
    }

    public void refreshSampleDislay() {
        Map<Integer, List<String>> samples = controller.getSamples();
        int numbersamples = samples.size();
        expectedsamplesize = config.samplesize.get();
        // calculate layout
        int margin = 20;
        int height = 500;
        int maxsampleheight = 220;
        int calcsampleheight = (height - margin) / numbersamples;
        if (calcsampleheight > maxsampleheight) {
            calcsampleheight = maxsampleheight;
        }
        hscale = 5;
        int width = expectedsamplesize * hscale + 2 * margin;
        // set the required dimensions for the canvas and clear it
        sampledisplaycanvas.setWidth(width);
        sampledisplaycanvas.setHeight(height);
        sampledisplaycanvas.getGraphicsContext2D().clearRect(0, 0, sampledisplaycanvas.getWidth(), sampledisplaycanvas.getHeight());
        int count = 0;
        for (var es : samples.entrySet()) {
            int topofsample = calcsampleheight * count++ + margin;
            drawSample(es.getKey(), es.getValue(), margin, topofsample, topofsample + calcsampleheight - margin);
        }
    }

    // sample drawing variables
    
    private int hstart;
    private int hscale;
    private int highpos;
    private int lowpos;
    private int expectedsamplesize;
    private double[] xpos;
    private double[] ypos;
    private int insertat;

    private void drawSample(int pin, List<String> sample, int hstart, int highpos, int lowpos) {
        xpos = new double[expectedsamplesize];
        ypos = new double[expectedsamplesize];
        insertat = 0;
        this.hstart = hstart;
        this.highpos = highpos;
        this.lowpos = lowpos;
        sample.forEach(segment -> buildSamplesegment(segment));
        GraphicsContext gc = sampledisplaycanvas.getGraphicsContext2D();
        gc.setStroke(RED);
        gc.setLineWidth(2.0);
        gc.strokePolyline(xpos, ypos, insertat);
    }

    private void buildSamplesegment(String samplesegment) {
        for (int cptr = 0; cptr < samplesegment.length(); cptr++) {
            switch (samplesegment.charAt(cptr)) {
                case 'H' -> {
                    insertHigh();
                }
                case 'L' -> {
                    insertLow();
                }
                default ->
                    cptr = decoderle(cptr, samplesegment);
            }
        }
    }

    private int decoderle(int cptr, String s) {
        char c = s.charAt(cptr);
        int count = 0;
        while ('0' <= c && c <= '9') {
            count = count * 10 + (int) (c - '0');
            c = s.charAt(++cptr);
        }
        switch (c) {
            case 'H' ->
                insertHigh(count);
            case 'L' ->
                insertLow(count);
            default ->
                throw new Failure("Badly encoded RLE data: " + c);
        }
        return cptr;
    }

    private void insertHigh() {
        insertHigh(1);
    }

    private void insertHigh(int width) {
        insert(highpos, width);
    }

    private void insertLow() {
        insertLow(1);
    }

    private void insertLow(int width) {
        insert(lowpos, width);
    }

    private void insert(int vpos, int width) {
        xpos[insertat] = hstart;
        ypos[insertat++] = vpos;
        hstart += hscale * width;
        xpos[insertat] = hstart;
        ypos[insertat++] = vpos;
    }
}
