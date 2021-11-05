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

package gosaxon

import (
	"encoding/binary"
	"encoding/hex"
	"errors"
	"fmt"
	"net"
	"strconv"
)

type Client struct {
	executablePath string
	debug          bool
}

func NewClient() *Client {
	return &Client{
		debug: false,
	}
}

func NewClientWithDebug() *Client {
	return &Client{
		debug: true,
	}
}

func NewClientWithExecutable(executablePath string) *Client {
	return &Client{
		executablePath: executablePath,
	}
}

func (m *Client) Transform(inputXml []byte, stylesheet []byte) ([]byte, error) {
	transformer := NewTransformer()
	// First we have to start the transformer so we can communicate with it.
	portInformation, err := transformer.Start(m.executablePath, m.debug)
	if err != nil {
		return nil, errors.New(fmt.Sprintf("error starting transformer executable: %s", err.Error()))
	}

	// The information on the port numbers is encoded in the response string.
	// Each is 8 characters long encoded as a hex string.
	xmlPortBytes, err := hex.DecodeString(portInformation[0:8])
	if err != nil {
		return nil, errors.New("got error decoding the xml port number: " + err.Error())
	}
	xmlPort := binary.BigEndian.Uint32(xmlPortBytes)
	xsltPortBytes, err := hex.DecodeString(portInformation[8:16])
	if err != nil {
		return nil, errors.New("got error decoding the xslt port number: " + err.Error())
	}
	xsltPort := binary.BigEndian.Uint32(xsltPortBytes)
	outPortBytes, err := hex.DecodeString(portInformation[16:24])
	if err != nil {
		return nil, errors.New("got error decoding the out port number: " + err.Error())
	}
	outPort := binary.BigEndian.Uint32(outPortBytes)

	// Open sockets to 127.0.0.1 on the given ports
	xmlConn, err := net.Dial("tcp", "127.0.0.1:"+strconv.Itoa(int(xmlPort)))
	if err != nil {
		return nil, errors.New("got error opening connection to xml port: " + err.Error())
	}
	xsltConn, err := net.Dial("tcp", "127.0.0.1:"+strconv.Itoa(int(xsltPort)))
	if err != nil {
		return nil, errors.New("got error opening connection to xslt port: " + err.Error())
	}
	outConn, err := net.Dial("tcp", "127.0.0.1:"+strconv.Itoa(int(outPort)))
	if err != nil {
		return nil, errors.New("got error opening connection to out port: " + err.Error())
	}

	// Send the content
	var bytesWritten int
	bytesWritten, err = xsltConn.Write(stylesheet)
	if err != nil {
		return nil, errors.New("error writing stylesheet: " + err.Error())
	} else if bytesWritten != len(stylesheet) {
		return nil, errors.New(fmt.Sprintf("error writing all bytes of stylesheet. Only %d of %d bytes were writeren", bytesWritten, len(stylesheet)))
	}
	bytesWritten, err = xmlConn.Write(inputXml)
	if err != nil {
		return nil, errors.New("error writing xml document: " + err.Error())
	} else if bytesWritten != len(inputXml) {
		return nil, errors.New(fmt.Sprintf("error writing all bytes of xml document. Only %d of %d bytes were writeren", bytesWritten, len(inputXml)))
	}

	// As soon as the content is sent, we have to close the connection in order to make saxon do it's work.
	err = xsltConn.Close()
	if err != nil {
		return nil, errors.New("error closing stylesheet socket: " + err.Error())
	}
	err = xmlConn.Close()
	if err != nil {
		return nil, errors.New("error writing xml document socket: " + err.Error())
	}

	// Read input until we get an error as Saxon will close the connection after sending the last byte.
	// REMARK: don't use the ioutils.ReadAll as this will fail.
	var response []byte
	for true {
		buf := make([]byte, 1024)
		read, err := outConn.Read(buf)
		if err != nil {
			break
		}
		response = append(response, buf[0:read]...)
	}

	err = transformer.Stop()
	if err != nil {
		return nil, err
	}

	return response, nil
}
