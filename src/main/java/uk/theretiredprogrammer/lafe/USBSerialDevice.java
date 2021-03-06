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

import com.fazecast.jSerialComm.SerialPort;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

public class USBSerialDevice implements Closeable {

    public static USBSerialDevice selectCommPort(Consumer<String> displaystatus) {
        List<String> picoPorts = new ArrayList<>();
        String selected = null;
        while (selected == null) {
            var commPorts = SerialPort.getCommPorts();
            picoPorts.clear();
            for (SerialPort port : commPorts) {
                if (port.getDescriptivePortName().equals("Pico (Dial-In)")) {
                    picoPorts.add("/dev/" + port.getSystemPortName());
                }
            }
            if (picoPorts.size() == 1) {
                selected = picoPorts.get(0);
            } else if (picoPorts.isEmpty()) {
                selected = Window.buildAndShowNoPicoProbeDialog();
            } else {
                selected = Window.buildAndShowManyPicoProbeDialog(picoPorts, (p) -> setLocate(p, displaystatus), (d) -> clearLocate(d, displaystatus));
            }
        }
        USBSerialDevice usbdevice = setLocate(selected, displaystatus);
        return usbdevice;
    }

    private static USBSerialDevice setLocate(String path, Consumer<String> displaystatus) {
        USBSerialDevice usbdevice = new USBSerialDevice(path, displaystatus);
        usbdevice.sendCommandAndHandleResponse("f-1", (s) -> false);
        return usbdevice;
    }

    private static void clearLocate(USBSerialDevice usbdevice, Consumer<String> displaystatus) {
        usbdevice.sendCommandAndHandleResponse("f-0", (s) -> false);
        usbdevice.close();
    }

    private SerialPort commPort;
    private OutputStream out;
    private InputStream in;
    private final Consumer<String> displaystatus;
    private final String path;

    public USBSerialDevice(String path, Consumer<String> displaystatus) {
        this.displaystatus = displaystatus;
        this.path = path;
        open();
    }
    
    public final void open() {
        this.commPort = SerialPort.getCommPort(path);
        commPort.openPort();
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        out = commPort.getOutputStream();
        in = commPort.getInputStream();
    }

    @Override
    public void close() {
        try {
            in.close();
            out.close();
            commPort.closePort();
        } catch (IOException ex) {
            throw new Failure("Failure during USBSerialDevice.close()", ex);

        }
    }

    public void write(char c) {
        try {
            out.write(c);
        } catch (IOException ex) {
            throw new Failure("write(" + c + ") failed ", ex);
        }
    }

    public void writeln(String s) throws IOException {
        for (int val : s.toCharArray()) {
            if (val >= 32 && val <= 126) {
                out.write(val);
            }
        }
        out.write(10); // newline
    }

    public String readln() throws IOException {
        String response = "";
        int c = 0;
        while (c != '\n') {
            c = in.read();
            if (c >= 32 && c <= 126) {
                response += (char) c;
            }
        }
        return response;
    }

    // higher level probe specific command poll/response functions
    public synchronized boolean sendCommandAndHandleResponse(String s, Function<String, Boolean> responselinehandler) {
        sendcommand(s);
        return handleResponse(responselinehandler);
    }

    private void sendcommand(String s) {
        IOException ioex;
        System.out.println("W: " + s);
        try {
            writeln(s);
            return;
        } catch (IOException ex) {
            ioex = ex;
        }
        try {
            System.out.println("W: !");
            writeln("!"); // attempt to send an abandon command
        } catch (IOException ex) {
        }
        throw new Failure("Attempting to Send Command", ioex);
    }

    private boolean handleResponse(Function<String, Boolean> responselinehandler) {
        try {
            while (true) {
                String response = readln();
                if (response.startsWith("**DEBUG:")) {
                    displayStatus(response);
                } else {
                    System.out.println("R: " + response);
                    if (response.startsWith("Y")) {
                        displayStatus(response, 2);
                        return true;
                    }
                    if (response.startsWith("N")) {
                        displayStatus(response, 2);
                        return false;
                    }
                    if (!responselinehandler.apply(response)) {
                        return false;
                    }
                }
            }
        } catch (IOException ex) { // treat IOException as a N response
            return false;
        }
    }

    private void displayStatus(String message, int startindex) {
        if (message.length() > startindex) {
            displayStatus(message.substring(startindex));
        }
    }

    private void displayStatus(String message) {
        Platform.runLater(() -> displaystatus.accept(message));
    }
}
