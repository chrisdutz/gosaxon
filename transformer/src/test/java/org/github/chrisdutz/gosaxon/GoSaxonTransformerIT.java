/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.github.chrisdutz.gosaxon;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;

public class GoSaxonTransformerIT {

    @Test
    public void testTransformer() {
        try {
            String buildDirectory = System.getProperty("buildDirectory");
            if (buildDirectory == null) {
                buildDirectory = new File("target").getAbsolutePath();
            }
            if (!buildDirectory.endsWith(File.separator)) {
                buildDirectory += File.separator;
            }
            System.out.println("Running in buildDirectory: " + buildDirectory);

            // Load the input xml from the classpath
            InputStream xmlDataStream = getClass().getResourceAsStream("/M-0008_A-10D9-11-2951-O000A.xml");
            InputStream xsltDataStream = getClass().getResourceAsStream("/test.xslt");

            // Create a new process using the fat-jar we created in packaging.
            Runtime rt = Runtime.getRuntime();
            System.out.println("Transformation process: Starting");
            Process proc = rt.exec(buildDirectory + "application/gosaxon-transformer.exe");
            System.out.println("Transformation process: Started");

            // Get access to the input- and output-streams.
            InputStream stdout = proc.getInputStream();
            InputStream stderr = proc.getErrorStream();
            // Start a thread dumping all stderr output to the system.err output.
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    while (true) {
                        if (stderr.available() > 0) {
                            int readBytes = stderr.read(buffer);
                            System.err.write(buffer, 0, readBytes);
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }).start();

            // Wait till the sockets are started and the socket numbers have been sent back.
            while (stdout.available() < 16) {
                Thread.sleep(10);
            }
            // Parse the socket numbers for XML and XSLT payload.
            byte[] socketNumberHex = new byte[8];
            if (stdout.read(socketNumberHex) != 8) {
                System.out.println("Error reading xml socket number");
            }
            int xmlSocketNumber = Integer.parseInt(new String(socketNumberHex), 16);
            if (stdout.read(socketNumberHex) != 8) {
                System.out.println("Error reading xml socket number");
            }
            int xsltSocketNumber = Integer.parseInt(new String(socketNumberHex), 16);
            if (stdout.read(socketNumberHex) != 8) {
                System.out.println("Error reading xml socket number");
            }
            int outputSocketNumber = Integer.parseInt(new String(socketNumberHex), 16);

            // Connect to the sockets.
            Socket xmlSocket = new Socket("localhost", xmlSocketNumber);
            Socket xsltSocket = new Socket("localhost", xsltSocketNumber);
            Socket outputSocket = new Socket("localhost", outputSocketNumber);

            // Start a new Thread for writing the XML into the process.
            System.out.println("Transformation process: Sending XSLT");
            DataOutputPump xsltInputPump = new DataOutputPump(xsltDataStream, xsltSocket.getOutputStream(), "xslt-intput");
            xsltInputPump.run();
            xsltSocket.close();

            System.out.println("Transformation process: Sending XML");
            DataOutputPump xmlInputPump = new DataOutputPump(xmlDataStream, xmlSocket.getOutputStream(), "xml-input");
            xmlInputPump.run();
            xmlSocket.close();

            // Read the output from the process and dump it into a file.
            System.out.println("Outputting to: " + buildDirectory + "output.xml");
            FileOutputStream fileOutputStream = new FileOutputStream(buildDirectory + "output.xml");
            new Thread(() -> {
                try {
                    InputStream inputStream = outputSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    while (!outputSocket.isClosed()) {
                        if (inputStream.available() > 0) {
                            int readBytes = inputStream.read(buffer);
                            fileOutputStream.write(buffer, 0, readBytes);
                            fileOutputStream.flush();
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }).start();

            // Wait for the other side to tell us it's done.
            while (stdout.available() < 7) {
                Thread.sleep(100);
            }
            byte[] buf = new byte[7];
            stdout.read(buf);
            String result = new String(buf);
            if ("SUCCESS".equals(result)) {
                System.out.println("SUCCESS");
            } else if ("FAILURE".equals(result)) {
                System.out.println("FAILURE");
                Assertions.fail("Got a failure from the transformer");
            } else {
                System.out.println("UNDEFINED: " + result);
                Assertions.fail("Got an unexpected result from the transformer");
            }

            outputSocket.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.out.println("Done");
    }

    public static class DataOutputPump implements Runnable {

        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final String streamName;

        public DataOutputPump(InputStream inputStream, OutputStream outputStream, String streamName) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.streamName = streamName;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                while (inputStream.available() > 0) {
                    int readBytes = inputStream.read(buffer);
                    outputStream.write(buffer, 0, readBytes);
                }
                outputStream.flush();
                System.out.println("Finished sending " + streamName);
            } catch (IOException ioe) {
                throw new RuntimeException("Got error pumping input to output for stream: " + streamName, ioe);
            }
        }
    }

}
