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

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Little application that opens two sockets (one for XML data and one for XSLT data) and
 * Uses Saxon to transform the xml into any output based on the XSL transformation.
 */
public class GoSaxonTransformer {

    private ServerSocket xmlSocket;
    private ServerSocket xsltSocket;
    private ServerSocket outputSocket;
    private Thread worker;
    private boolean debug;
    private boolean running;

    public GoSaxonTransformer(boolean debug) {
        try {
            this.debug = debug;

            // Create two sockets for the xml and xslt on random ports.
            xmlSocket = new ServerSocket(0);
            xsltSocket = new ServerSocket(0);
            outputSocket = new ServerSocket(0);

            worker = new Thread(() -> {
                try {
                    // Accept the XML socket first and then the XSLT socket.
                    if (debug) {
                        System.err.println("Waiting for client connections");
                    }
                    Socket xmlSocketConnection = xmlSocket.accept();
                    Socket xsltSocketConnection = xsltSocket.accept();
                    Socket outputSocketConnection = outputSocket.accept();
                    if (debug) {
                        System.err.println("Client connected");
                    }

                    // Get the streams and make sure the BOM is skipped.
                    InputStream xmlInputStream = new BufferedInputStream(xmlSocketConnection.getInputStream());
                    OutputStream xmlOutputStream = outputSocketConnection.getOutputStream();
                    InputStream xsltInputStream = new BufferedInputStream(xsltSocketConnection.getInputStream());

                    if (debug) {
                        System.err.println("Starting Transformation");
                    }
                    transform(xmlInputStream, xsltInputStream, xmlOutputStream);
                    if (debug) {
                        System.err.println("Finished Transformation");
                    }

                    running = false;
                    if (debug) {
                        System.err.println("Done");
                    }
                } catch (Exception e) {
                    System.err.println("Got error executing transformation thread: " + e.getMessage());
                    System.out.print("FAILURE");
                }
            });
            running = true;
            try {
                if (debug) {
                    System.err.println("Starting transformer thread");
                }
                worker.start();
                if (debug) {
                    System.err.println("Transformer thread started");
                }
            } catch (Exception e) {
                System.err.println("Got error executing transformation thread: " + e.getMessage());
                System.out.print("FAILURE");
            }
        } catch (Exception e) {
            System.err.println("Got error executing setting up transformer: " + e.getMessage());
            System.out.print("FAILURE");
        }
    }

    public void stop() {
        try {
            xmlSocket.close();
            xsltSocket.close();
            outputSocket.close();
        } catch (IOException e) {
            System.err.println("Got error executing stopping transformer: " + e.getMessage());
            System.out.print("FAILURE");
        }
    }

    public int[] getPorts() {
        return new int[]{
            xmlSocket.getLocalPort(),
            xsltSocket.getLocalPort(),
            outputSocket.getLocalPort()
        };
    }

    public void transform(InputStream xmlInputStream, InputStream xsltInputStream, OutputStream xmlOutputStream) {
        try {
            if (debug) {
                System.err.println("Starting Cleaning input ...");
            }
            // TODO: We should probably do something with this ... but what?
            /*String xmlEncoding = */
            getEncodingFromBomAndRemoveBom(xmlInputStream);
            // TODO: We should probably do something with this ... but what?
            /*String xsltEncoding = */
            getEncodingFromBomAndRemoveBom(xsltInputStream);
            if (debug) {
                System.err.println("Finished Cleaning input");
            }

            if (debug) {
                System.err.println("Starting XSLT ...");
            }
            // Transform the input using the embedded XSLT.
            TransformerFactory transformerFactory = new net.sf.saxon.TransformerFactoryImpl();
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsltInputStream));
            transformer.setErrorListener(new ErrorListener() {
                public void warning(TransformerException exception) {
                    System.err.println("Got Warning: " + exception.getMessage());
                }

                public void error(TransformerException exception) {
                    System.err.println("Got Error: " + exception.getMessage());
                }

                public void fatalError(TransformerException exception) {
                    System.err.println("Got Fatal Error: " + exception.getMessage());
                }
            });
            transformer.transform(new StreamSource(xmlInputStream), new StreamResult(xmlOutputStream));
            if (debug) {
                System.err.println("Finished XSLT");
            }

            // Signal the client that we're done.
            System.out.print("SUCCESS");

            // Stop after transformation.
            stop();
        } catch (IOException e) {
            System.err.println("Got IOException: " + e.getMessage());
            System.out.print("FAILURE");
        } catch (TransformerConfigurationException e) {
            System.err.println("Got TransformerConfigurationException: " + e.getMessage());
            System.out.print("FAILURE");
        } catch (TransformerException e) {
            System.err.println("Got TransformerException: " + e.getMessage());
            System.out.print("FAILURE");
        }
    }

    private String getEncodingFromBomAndRemoveBom(InputStream inputStream) throws IOException {
        inputStream.mark(4);
        while (inputStream.available() < 4) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
        if (inputStream.available() >= 2) {
            int first = inputStream.read();
            int second = inputStream.read();
            if ((first == 0xEF) && (second == 0xBB)) {
                if ((inputStream.available() >= 1) && (inputStream.read() == 0xBF)) {
                    return "UTF-8";
                }
            } else if ((first == 0xFF) && (second == 0xFE)) {
                if ((inputStream.available() >= 2) && (inputStream.read() == 0x00) && (inputStream.read() == 0x00)) {
                    return "UTF-32 (lE)";
                } else {
                    return "UTF-16 (LE)";
                }
            } else if ((first == 0xFE) && (second == 0xFF)) {
                return "UTF-16 (BE)";
            } else if ((first == 0x00) && (second == 0x00)) {
                if ((inputStream.available() >= 2) && (inputStream.read() == 0xFE) && (inputStream.read() == 0xFF)) {
                    return "UTF-32 (BE)";
                }
            } else if ((first == 0x2B) && (second == 0x2F)) {
                if ((inputStream.available() >= 1) && (inputStream.read() == 0x76)) {
                    return "UTF-7";
                }
            } else if ((first == 0xF7) && (second == 0x64)) {
                if ((inputStream.available() >= 1) && (inputStream.read() == 0x4C)) {
                    return "UTF-1";
                }
            } else if ((first == 0xDD) && (second == 0x73)) {
                if ((inputStream.available() >= 2) && (inputStream.read() == 0x66) && (inputStream.read() == 0x73)) {
                    return "EBCDIC";
                }
            } else if ((first == 0x0E) && (second == 0xFE)) {
                if ((inputStream.available() >= 1) && (inputStream.read() == 0xFF)) {
                    return "SCSU";
                }
            } else if ((first == 0xFB) && (second == 0xEE)) {
                if ((inputStream.available() >= 1) && (inputStream.read() == 0x28)) {
                    return "BOCU-1";
                }
            } else if ((first == 0x84) && (second == 0x31)) {
                if ((inputStream.available() >= 2) && (inputStream.read() == 0x95) && (inputStream.read() == 0x33)) {
                    return "GB-18030";
                }
            }
        }
        // There was no BOM, reset the read position.
        inputStream.reset();
        return null;
    }


    public static void main(String[] args) {
        boolean debug = false;
        if (args.length == 1) {
            debug = Boolean.parseBoolean(args[0]);
        }

        if (debug) {
            System.err.println("Starting ...");
        }
        try {
            GoSaxonTransformer transformer = new GoSaxonTransformer(debug);
            // Tell the caller the port numbers.
            int[] ports = transformer.getPorts();
            System.out.printf("%08X", ports[0]);
            System.out.printf("%08X", ports[1]);
            System.out.printf("%08X", ports[2]);

            if (debug) {
                System.err.println("Started");
            }
            // Wait till the transformer terminates.
            while (transformer.running) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (debug) {
                System.err.println("Finished");
            }
        } catch (Exception e) {
            System.err.println("Got Exception: " + e.getMessage());
        }
    }

}
