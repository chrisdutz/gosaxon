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

package main

import (
	"fmt"
	"github.com/chrisdutz/gosaxon/library/pkg/gosaxon"
	"io/ioutil"
	"log"
	"os"
)

func main() {
	fmt.Printf("Starting Transformer")

	file, err := os.Open("C:\\Projects\\Apache\\PLC4X\\Documents\\KNX\\knx-content\\M-0008\\M-0008_A-F020-30-B4ED.xml")
	if err != nil {
		log.Fatal(err)
	}
	xml, err := ioutil.ReadAll(file)
	if err != nil {
		log.Fatal(err)
	}

	xslt := "<xsl:stylesheet version=\"2.0\"\n                xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n\n    <xsl:template match=\"/\">\n        <output>\n            <xsl:apply-templates select=\"*\"/>\n        </output>\n    </xsl:template>\n\n    <xsl:template match=\"node() | @*\">\n        <xsl:copy-of select=\".\"/>\n    </xsl:template>\n\n</xsl:stylesheet>"
	fmt.Printf("Sending XSLT:\n%s", xslt)

	result, err := gosaxon.Transform([]byte(xml), []byte(xslt))
	if err != nil {
		fmt.Printf("got error: %s", err.Error())
		return
	}
	fmt.Printf("got result: \n%s", string(result))
}
