/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.apptuit.metrics.jinsight.modules.logback;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author Rajiv Shivane
 */
public class ErrorFingerprint {

  private static boolean DEBUG = false;

  private String fqClassName;
  private String className;
  private String checksum;
  private String body;

  private ErrorFingerprint(String fqClassName, String className, String checksum) {
    this.fqClassName = fqClassName;
    this.className = className;
    this.checksum = checksum;
  }

  public String getChecksum() {
    return checksum;
  }

  public String getErrorFullName() {
    return fqClassName;
  }

  public String getErrorSimpleName() {
    return className;
  }

  @Override
  public String toString() {
    return className + "#" + checksum.substring(0, 4);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ErrorFingerprint that = (ErrorFingerprint) o;
    return checksum.equals(that.checksum);
  }

  @Override
  public int hashCode() {
    return checksum.hashCode();
  }


  /**
   * @return The fingerprint. Maybe null if there is an error computing the fingerprint
   */
  public static ErrorFingerprint fromThrowable(Throwable t) {
    IThrowable wrapper = (t == null) ? null : new ThrowableWrapper(t);
    return fromIThrowable(wrapper);
  }

  public static ErrorFingerprint fromIThrowableProxy(IThrowableProxy proxy) {
    IThrowable wrapper = (proxy == null) ? null : new ThrowableProxyWrapper(proxy);
    return fromIThrowable(wrapper);
  }

  public static ErrorFingerprint fromIThrowable(IThrowable t) {
    try {
      return _fromThrowable(t);
    } catch (Throwable thr) {
      return null;
    }
  }

  private static ErrorFingerprint _fromThrowable(IThrowable t) throws NoSuchAlgorithmException, IOException {
    if (t == null) {
      return null;
    }

    MessageDigest md = MessageDigest.getInstance("MD5");
    OutputStream outputStream;
    if (DEBUG) {
      outputStream = new ByteArrayOutputStream(2 * 1024);
    } else {
      outputStream = NULL_OUTPUT_STREAM;
    }
    Writer writer = new OutputStreamWriter(new DigestOutputStream(outputStream, md), StandardCharsets.UTF_8);
    printStackTrace(t, writer);
    writer.flush();
    writer.close();

    ErrorFingerprint fingerprint = new ErrorFingerprint(t.getClassName(), t.getSimpleName(), toHex(md.digest()));
    if (DEBUG) {
      fingerprint.body = new String(((ByteArrayOutputStream) outputStream).toByteArray(), StandardCharsets.UTF_8);
      System.out.println(fingerprint.body);
    }
    return fingerprint;
  }

  private static String toHex(byte[] hash) {
    StringBuilder sb = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      sb.append(String.format("%02x", b & 0xff));
    }

    return sb.toString();
  }

  private static void printStackTrace(IThrowable t, Writer writer) throws IOException {
    printStackTrace(t, writer, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private static void printStackTrace(IThrowable t, Writer writer, Set<IThrowable> causes) throws IOException {
    if (causes.contains(t)) {
      writer.write("\t[CIRCULAR REFERENCE:" + t + "]\n");
      return;
    }

    if (causes.size() > 0) {
      writer.write("Caused by: ");
    }
    writer.write(t.getClassName());
    writer.write('\n');

    Iterator<StackTraceElement> trace = t.getStackTrace();
    while (trace.hasNext()) {
      StackTraceElement traceElement = trace.next();
      printStackTraceElement(writer, traceElement);
    }

    IThrowable cause = t.getCause();
    if (cause != null) {
      causes.add(t);
      printStackTrace(cause, writer, causes);
    }

  }

  private static void printStackTraceElement(Writer writer, StackTraceElement traceElement) throws IOException {
    String className = traceElement.getClassName();
    if (className.startsWith("com.sun.proxy.$Proxy")) {
      className = "com.sun.proxy.$Proxy$$";
    } else {

            /*
            Handle reflection inflation         //https://blogs.oracle.com/buck/entry/inflation_system_properties
            Fake NativeMethodAccessor and GeneratedMethodAccessor stack elements to look alike

            NativeMethodAccessor elements
                at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
                at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.lang.reflect.Method.invoke(Method.java:497)

            GeneratedMethodAccessor elements
                at sun.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
                at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.lang.reflect.Method.invoke(Method.java:497)
            */

      if (className.equals("sun.reflect.NativeMethodAccessorImpl")) {
        if (traceElement.getMethodName().equals("invoke0")) {
          return;
        }
        className = "sun.reflect.$$MethodAccessor$$";
      } else if (className.startsWith("sun.reflect.GeneratedMethodAccessor")) {
        className = "sun.reflect.$$MethodAccessor$$";
      }
    }
    writer.write("\tat " + className + "." + traceElement.getMethodName() + "\n");
  }

  private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
    @Override
    public void write(byte[] b) {
    }

    @Override
    public void write(byte[] b, int off, int len) {
    }

    @Override
    public void write(int b) {
    }
  };

  public interface IThrowable {

    String getClassName();

    String getSimpleName();

    Iterator<StackTraceElement> getStackTrace();

    IThrowable getCause();

  }

  private static class ThrowableWrapper implements IThrowable {

    private final Throwable actual;

    public ThrowableWrapper(Throwable t) {
      if (t == null) {
        throw new NullPointerException();
      }
      this.actual = t;
    }

    @Override
    public String getClassName() {
      return actual.getClass().getName();
    }

    @Override
    public String getSimpleName() {
      return actual.getClass().getSimpleName();
    }

    @Override
    public Iterator<StackTraceElement> getStackTrace() {
      StackTraceElement[] elements = actual.getStackTrace();
      return new Iterator<StackTraceElement>() {
        private int pos = 0;

        @Override
        public boolean hasNext() {
          return elements.length > pos;
        }

        @Override
        public StackTraceElement next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return elements[pos++];
        }
      };
    }

    @Override
    public IThrowable getCause() {
      Throwable cause = actual.getCause();
      if (cause == null) {
        return null;
      }
      return new ThrowableWrapper(cause);
    }
  }

  private static class ThrowableProxyWrapper implements IThrowable {

    private final IThrowableProxy proxy;

    public ThrowableProxyWrapper(IThrowableProxy proxy) {
      if (proxy == null) {
        throw new NullPointerException();
      }
      this.proxy = proxy;
    }

    @Override
    public String getClassName() {
      return proxy.getClassName();
    }

    @Override
    public String getSimpleName() {
      String className = proxy.getClassName();
      int idx = className.lastIndexOf('.');
      return idx < 0 ? className : className.substring(idx + 1);
    }

    @Override
    public Iterator<StackTraceElement> getStackTrace() {
      StackTraceElementProxy[] proxyArray = proxy.getStackTraceElementProxyArray();
      return new Iterator<StackTraceElement>() {
        private int pos = 0;

        @Override
        public boolean hasNext() {
          return proxyArray.length > pos;
        }

        @Override
        public StackTraceElement next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return proxyArray[pos++].getStackTraceElement();
        }
      };
    }

    @Override
    public IThrowable getCause() {
      IThrowableProxy cause = proxy.getCause();
      if (cause == null) {
        return null;
      }
      return new ThrowableProxyWrapper(cause);
    }
  }
}
