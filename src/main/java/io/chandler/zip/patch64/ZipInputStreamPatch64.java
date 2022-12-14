package io.chandler.zip.patch64;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Some ZIP stream records are encoded with ZIP64 fields despite data being under 0xFFFFFFFF bytes long
 * ZipInputStream tries to read a Zip64 data descriptor as if it were Zip32 and throws a recognizable error:
 *   java.util.zip.ZipException: invalid entry size (expected 0 but got N bytes)
 *      at java.util.zip.ZipInputStream.readEnd(ZipInputStream.java:384)
 *      
 * This patched ZipInputStream will catch the exception and manually advance the stream to the next entry
 * 
 * @author cjgriscom
 *
 */
public class ZipInputStreamPatch64 extends ZipInputStream {
  
  private ZipEntry ze;
  
  public ZipInputStreamPatch64(InputStream in) { super(in); }
  public ZipInputStreamPatch64(InputStream in, Charset charset) { super(in, charset); }
  
  @Override
  public ZipEntry getNextEntry() throws IOException {
    return ze = super.getNextEntry(); 
  }
    
  @Override public int read() throws IOException {
    try {
      return super.read();
    } catch (ZipException e) {
      if (matchHotfixZip64(e)) {
        hotfixZip64(this, ze);
        return 0;
      }
      else throw e;
    }
  }
    
  @Override public int read(byte[] b) throws IOException {
    return this.read(b, 0, b.length);
  }
    
  @Override public int read(byte[] b, int off, int len) throws IOException {
    try {
      return super.read(b, off, len);
    } catch (ZipException e) {
      if (matchHotfixZip64(e)) {
        hotfixZip64(this, ze);
        return 0;
      }
      else throw e;
    }
  }
  
  private static boolean matchHotfixZip64(ZipException e) {
    return e.getMessage().startsWith("invalid entry size (expected 0 ");
  }

  private static void hotfixZip64(ZipInputStream zis, ZipEntry ze) throws IOException {
    try {
      // Close the entry manually
      Field f = ZipInputStream.class.getDeclaredField("entryEOF");
      f.setAccessible(true);
      f.set(zis, true);
      f = ZipInputStream.class.getDeclaredField("entry");
      f.setAccessible(true);
      f.set(zis, null);
      
      // Grab pushback stream
      Field inF = FilterInputStream.class.getDeclaredField("in"); inF.setAccessible(true);
      PushbackInputStream in = (PushbackInputStream) inF.get(zis);
      
      // Read 8 extra bytes to compensate misalignment, and use them to validate uncompressed size
      long usize = 0;
      for (int i = 0; i < 8; i++) {
        int byt = in.read(); // Read 8 extra bytes to compensate footer
        if (byt < 0) throw new EOFException();
        usize >>>= 8;
        usize |= ((long)(byt & 0xFF)) << 56;
      }
      
      // Correct ZipEntry's size
      ze.setSize(usize);

      // Grab the Inflater
      f = InflaterInputStream.class.getDeclaredField("inf");
      f.setAccessible(true);
      Inflater inf = (Inflater) f.get(zis);
      
      // Re-check for errors
      if (ze.getSize() != inf.getBytesWritten()) {
        throw new ZipException("invalid entry size (expected " + ze.getSize() +
                               " but got " + inf.getBytesWritten() + " bytes)");
      }
      if (ze.getCompressedSize() != inf.getBytesRead()) {
        throw new ZipException("invalid entry compressed size (expected " + ze.getCompressedSize() +
                               " but got " + inf.getBytesRead() + " bytes)");
      }
      
      // Verify CRC manually
      f = ZipInputStream.class.getDeclaredField("crc");
      f.setAccessible(true);
      CRC32 crc = (CRC32) f.get(zis);
      if (ze.getCrc() != crc.getValue()) {
        throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(ze.getCrc()) +
                               " but got 0x" + Long.toHexString(crc.getValue()) + ")");
      }
      
    } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
      throw new IOException("Failed to correct ZipInputStream bug", e);
    }
  }
}
