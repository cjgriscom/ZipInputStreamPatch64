package io.chandler.zip.patch64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.zip.ZipException;

import org.junit.jupiter.api.Test;

public class ZipInputStreamPatch64Test {

  @Test
  public void testProblemZip64() throws Exception {
    try (ZipInputStreamPatch64 zis = new ZipInputStreamPatch64(getClass().getResourceAsStream("/Content.zip"))) {
      assertEquals("Content.txt", zis.getNextEntry().getName());
      assertNull(zis.getNextEntry());
    }

    try (ZipInputStreamPatch64 zis = new ZipInputStreamPatch64(getClass().getResourceAsStream("/ContentTwo.zip"))) {
      zis.getNextEntry();
      zis.closeEntry();
      zis.getNextEntry();
      zis.read(new byte[191]);
      zis.read();
      assertNull(zis.getNextEntry());
    }
  }

  @Test
  public void testProblemZip64BadCRC() throws Exception {
    try (ZipInputStreamPatch64 zis = new ZipInputStreamPatch64(getClass().getResourceAsStream("/ContentTwo_WithBadCRC.zip"))) {
      assertEquals("Content.txt", zis.getNextEntry().getName());
      try {
        zis.closeEntry();
        assertTrue(false);
      } catch (ZipException e) {
        assertEquals("invalid entry CRC (expected 0xdeadbeef but got 0xbb2aaedb)", e.getMessage());
      }
    }
  }
}
