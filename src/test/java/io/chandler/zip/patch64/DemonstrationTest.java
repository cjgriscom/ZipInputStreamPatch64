package io.chandler.zip.patch64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Field;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

public class DemonstrationTest {
  
  @Test public void readProblematicZip() throws Exception {
    
    try (InputStream fileStream = getClass().getResourceAsStream("/ContentTwo.zip");
         InputStream bufferedStream = new BufferedInputStream(fileStream);
         ZipInputStream zipStream = new ZipInputStream(bufferedStream)) {
      
      ZipEntry entry = zipStream.getNextEntry();
      System.out.println("Entry name: " + entry.getName());
      System.out.println("Entry compressed size: " + entry.getCompressedSize());
      System.out.println("Entry uncompressed size: " + entry.getSize());
      
      // Consume all bytes
      StringBuilder content = new StringBuilder();
      try {
        for (int b; (b = zipStream.read()) >= 0;) content.append((char) b);
        
      } catch (Exception e) {
        
        System.err.println("Stream failed at byte " + content.length());
        System.out.println(" ** New entry compressed size: " + entry.getCompressedSize());
        System.out.println(" ** New entry uncompressed size: " + entry.getSize());
        e.printStackTrace();
        
        System.out.println("Recovering...");
        
        // Steps to recover from error condition
        if ((e instanceof ZipException) && e.getMessage().startsWith("invalid entry size (expected 0 ")) {
          // Grab pushback stream
          Field inF = FilterInputStream.class.getDeclaredField("in"); inF.setAccessible(true);
          PushbackInputStream in = (PushbackInputStream) inF.get(zipStream);
                      
          for (int i = 0; i < 8; i++) in.read(); // Read 8 extra bytes to compensate footer
          
          // Close the entry manually
          Field f = ZipInputStream.class.getDeclaredField("entryEOF");
          f.setAccessible(true);
          f.set(zipStream, true);
          f = ZipInputStream.class.getDeclaredField("entry");
          f.setAccessible(true);
          f.set(zipStream, null);
        }
        
        // Check if stream advanced properly
        ZipEntry entry2 = zipStream.getNextEntry();
        assertNotNull(entry2);
        assertEquals("Abcdefg.txt", entry2.getName());
        
      }
      
      
    }
    
  }
}
