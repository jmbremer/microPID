/*
 * Copyright 2008 blue media labs ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bluemedialabs.io;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Writes all data to two underlying output streams. This class can be used to
 * direct a single output stream such as <code>System.out</code> to two other
 * output stream, for example, the screen and a file. 
 * 
 * @author J. Marco Bremer
 * @version 0.1
 */
public class SplitOutputStream extends OutputStream {

    private OutputStream out1;
    private OutputStream out2;
    
    
    public SplitOutputStream(OutputStream out1, OutputStream out2) {
        assert (out1 != null && out2 != null);
        this.out1 = out1;
        this.out2 = out2;
    }
    
    public void write(int b) throws IOException {
        out1.write(b);
        out2.write(b);
    }
   
   public void write(byte[] b) throws IOException {
       out1.write(b);
       out2.write(b);
   }
  
   public void write(byte[] b, int off, int len) throws IOException {
       out1.write(b, off, len);
       out2.write(b, off, len);
   }
  
   public void flush() throws IOException {
      out1.flush();
      out2.flush();
   }
  
   public void close() throws IOException {
       out1.close();
       out2.close();
   }
    
}