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
	_ "embed"
	"errors"
	"fmt"
	"github.com/rs/zerolog/log"
	"io"
	"io/ioutil"
	"os"
	"os/exec"
	"runtime"
	"strconv"
	"time"
)

//go:embed exec/gosaxon-transformer.exe
var goSaxonExecutableWindows []byte

//go:embed exec/gosaxon-transformer.lnx
var goSaxonExecutableLinux []byte

//go:embed exec/gosaxon-transformer.mac
var goSaxonExecutableMac []byte

type Transformer struct {
	tmpExecutor *os.File
	cmd         *exec.Cmd
	running     bool
}

func NewTransformer() *Transformer {
	return &Transformer{}
}

func (m *Transformer) Start(executablePath string, debug bool) (string, error) {
	if executablePath == "" {
		// Dump the embedded executable to a temp file.
		var err error
		switch runtime.GOOS {
		case "windows":
			log.Debug().Msg("Using embedded Windows transformer binary")
			m.tmpExecutor, err = ioutil.TempFile(os.TempDir(), "gosaxon-executor-*.exe")
			if err != nil {
				return "", errors.New("got error creating temp file for executable: " + err.Error())
			}
			log.Debug().Str("executablePath", m.tmpExecutor.Name()).Msg("Path of transformer binary")
			err = ioutil.WriteFile(m.tmpExecutor.Name(), goSaxonExecutableWindows, os.ModeTemporary)
			if err != nil {
				return "", errors.New("got error dumping executable to the filesystem: " + err.Error())
			}
		case "linux":
			log.Debug().Msg("Using embedded Linux transformer binary")
			m.tmpExecutor, err = ioutil.TempFile(os.TempDir(), "gosaxon-executor-*.lnx")
			if err != nil {
				return "", errors.New("got error creating temp file for executable: " + err.Error())
			}
			log.Debug().Str("executablePath", m.tmpExecutor.Name()).Msg("Path of transformer binary")
			err = ioutil.WriteFile(m.tmpExecutor.Name(), goSaxonExecutableLinux, os.ModeTemporary)
			if err != nil {
				return "", errors.New("got error dumping executable to the filesystem: " + err.Error())
			}
			cmd := exec.Command("chmod", "+x", m.tmpExecutor.Name())
			err := cmd.Run()
			if err != nil {
				return "", errors.New("got error making linux executor executable: " + err.Error())
			}
		case "darwin":
			log.Debug().Msg("Using embedded MacOS transformer binary")
			m.tmpExecutor, err = ioutil.TempFile(os.TempDir(), "gosaxon-executor-*.mac")
			if err != nil {
				return "", errors.New("got error creating temp file for executable: " + err.Error())
			}
			log.Debug().Str("executablePath", m.tmpExecutor.Name()).Msg("Path of transformer binary")
			err = ioutil.WriteFile(m.tmpExecutor.Name(), goSaxonExecutableMac, os.ModeTemporary)
			if err != nil {
				return "", errors.New("got error dumping executable to the filesystem: " + err.Error())
			}
			cmd := exec.Command("chmod", "+x", m.tmpExecutor.Name())
			err := cmd.Run()
			if err != nil {
				return "", errors.New("got error making mac os executor executable: " + err.Error())
			}
		}
		// Free the handle so we can start the process.
		err = m.tmpExecutor.Close()
		if err != nil {
			return "", errors.New("got error closing file for executable: " + err.Error())
		}

		// Update the executor path to the temp file
		executablePath = m.tmpExecutor.Name()
	} else {
		log.Debug().Str("executablePath", executablePath).Msg("Using provided transformer binary")
	}

	// Prepare a command to run the gosaxon-executor
	log.Debug().Msg("Starting transformer executable")
	m.cmd = exec.Command(executablePath, strconv.FormatBool(debug))

	// Get access to StdOut and StdErr of the previously created command.
	stdout, err := m.cmd.StdoutPipe()
	if err != nil {
		return "", errors.New("got error accessing stdout pipe: " + err.Error())
	}
	stderr, err := m.cmd.StderrPipe()
	if err != nil {
		return "", errors.New("got error accessing stderr pipe: " + err.Error())
	}

	// Actually start the executor.
	err = m.cmd.Start()
	if err != nil {
		return "", errors.New("got error starting process: " + err.Error())
	}

	log.Debug().Msg("Transformer started")

	// Start a go routine to output any content from the processes std-err output.
	m.running = true
	go func() {
		buf := make([]byte, 1024)
		for m.running {
			time.Sleep(time.Millisecond * 10)
			readBytes, err := stderr.Read(buf)
			if err != nil {
				if errors.Is(err, io.EOF) {
					// Stop running as the stream is closed.
					m.running = false
				} else {
					_, _ = fmt.Fprintf(os.Stderr, "got error reading from stderr: %s\n", err)
				}
			} else {
				_, _ = fmt.Fprintf(os.Stderr, "read from stderr: %s\n", string(buf[0:readBytes]))
			}
		}
	}()

	// Read 24 bytes as these contain the 3 times 8 bytes with the hex encoded port numbers.
	buf := make([]byte, 1024)
	curPos := 0
	for curPos < 24 {
		readBytes, err := stdout.Read(buf[curPos:])
		if err != nil {
			return "", errors.New("got error reading from stdout: " + err.Error())
		}
		curPos += readBytes
	}

	log.Debug().Msg("Transformer connected")

	return string(buf[0:24]), nil
}

func (m *Transformer) Stop() error {
	m.running = false

	log.Debug().Msg("Stopping transformer")

	// Kill the executor, if it's still running.
	if m.cmd.ProcessState == nil || !m.cmd.ProcessState.Exited() {
		log.Debug().Msg("Killing transformer")
		_ = m.cmd.Process.Kill()
	}

	// If we were using the temp executor, delete it after being finished with the transformation.
	if m.tmpExecutor != nil {
		log.Debug().Msg("Cleaning up temporary executor binary")
		// Delete the executor file. (Give the OS max 2 seconds to clear the locks)
		tries := 0
		err := os.Remove(m.tmpExecutor.Name())
		for err != nil && tries < 200 {
			time.Sleep(time.Millisecond * 10)
			err = os.Remove(m.tmpExecutor.Name())
			tries++
		}
		if err != nil {
			return errors.New("error deleting temp executor: " + err.Error())
		}
		log.Debug().Msg("Temporary executor binary cleaned")
	}

	return nil
}
