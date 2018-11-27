package ai.apptuit.metrics.jinsight.modules.logback;

import static ai.apptuit.metrics.jinsight.modules.logback.ErrorFingerprint.fromThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class ErrorFingerprintTests {

  private static final boolean DEBUG = false;

  @Test
  public void testBasics() throws InterruptedException {

    Exception[] exceptions = new Exception[]{null};
    Thread t = new Thread(new ExceptionGenerator(exceptions));
    t.start();
    t.join();

    ErrorFingerprint fingerprint = fromThrowable(exceptions[0]);
    assertNotNull(fingerprint);
    assertEquals(MalformedURLException.class.getName(), fingerprint.getErrorFullName());
    assertEquals(MalformedURLException.class.getSimpleName(), fingerprint.getErrorSimpleName());
    assertEquals("8e9bd2d175676d38eae43b7c44b12cc9", fingerprint.getChecksum());
    assertEquals("MalformedURLException#8e9b", fingerprint.toString());
  }

  @Test
  public void testNull() {
    ErrorFingerprint fingerprint1 = fromThrowable(null);
    assertNull(fingerprint1);
  }

  @Test
  public void testLineNumbersAndMessagesAreIgnored() {

    Exception e1 = null, e2 = null;
    try {
      new URL("asdfasdf");
    } catch (MalformedURLException e) {
      e1 = e;
    }

    try {
      new URL("pqrst");
    } catch (MalformedURLException e) {
      e2 = e;
    }

    assertEquals(fromThrowable(e1), fromThrowable(e2));
  }

  @Test
  public void testNestedException() {

    MalformedURLException exception = null;
    try {
      new URL("asdfasdf");
    } catch (MalformedURLException e) {
      exception = e;
    }

    ErrorFingerprint f1 = fromThrowable(new RuntimeException(exception));
    ErrorFingerprint f2 = fromThrowable(new RuntimeException(exception));
    ErrorFingerprint f3 = fromThrowable(new RuntimeException());

    assertEquals(f1, f2);
    assertNotEquals(f1, f3);
  }

  @Test
  public void testCircularReferenceException() {
    final ErrorFingerprint f1 = fromThrowable(new CircularReferenceException());
    final ErrorFingerprint f2 = fromThrowable(new CircularReferenceException());
    assertEquals(f1, f2);
  }

  @Test
  public void testExceptionsInADynamicProxy() {
    ErrorFingerprint f1 = fromThrowable(_createExceptionInDynamicProxy());
    ErrorFingerprint f2 = fromThrowable(_createExceptionInDynamicProxy());
    assertEquals(f1, f2);
  }

  private static Exception _createExceptionInDynamicProxy() {
    ClassLoader cl = new URLClassLoader(new URL[]{});
    List instance = (List) Proxy.newProxyInstance(cl, new Class[]{List.class}, new MyProxy());
    try {
      instance.size();
    } catch (RuntimeException e) {
      if (DEBUG) {
        e.printStackTrace(System.out);
      }
      return e;
    }
    return null;
  }


  @Test
  public void testExceptionsFromReflectionInflation() {

    Exception beforeInflation = _createReflectedException(false);

    assertTrue(_checkStackTrace(beforeInflation, "sun.reflect.NativeMethodAccessorImpl.invoke0"));
    assertFalse(_checkStackTrace(beforeInflation, "sun.reflect.GeneratedMethodAccessor"));

    //https://blogs.oracle.com/buck/entry/inflation_system_properties
    for (int i = 0; i < 100; i++) {
      _createReflectedException(true);
    }

    Exception afterInflation = _createReflectedException(false);
    assertTrue(_checkStackTrace(afterInflation, "sun.reflect.GeneratedMethodAccessor"));

    assertEquals(fromThrowable(beforeInflation), fromThrowable(afterInflation));
  }

  private static Exception _createReflectedException(boolean silent) {
    boolean debug = !silent && DEBUG;
    try {
      Class<?> aClass = ErrorFingerprintTests.class.getClassLoader().loadClass("java.util.ArrayList");
      Method aMethod = aClass.getMethod("get", int.class);
      aMethod.invoke(aClass.newInstance(), 0);
    } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
      printWhenDebugEnabled(debug, e);
    } catch (InvocationTargetException e) {
      printWhenDebugEnabled(debug, e);
      if (!silent) {
        return e;
      }
    }
    return null;
  }

  private static void printWhenDebugEnabled(boolean debug, Exception e) {
    if (debug) {
      e.printStackTrace(System.out);
    }
  }


  private static boolean _checkStackTrace(Exception e, String stringToSearch) {
    StringWriter stringWriter = new StringWriter(32 * 1024);

    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    printWriter.flush();
    printWriter.close();

    return stringWriter.getBuffer().toString().indexOf(stringToSearch) > 0;
  }


  private static class CircularReferenceException extends Exception {

    public CircularReferenceException() {
    }

    @Override
    public synchronized Throwable getCause() {
      return this;
    }
  }

  private static class MyProxy
      implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (DEBUG) {
        System.out.println("Invoking: " + method);
      }
      throw new NullPointerException();
    }
  }

  private static class ExceptionGenerator implements Runnable {

    private final Exception[] exceptions;

    public ExceptionGenerator(Exception[] exceptions) {
      this.exceptions = exceptions;
    }

    @Override
    public void run() {
      exceptions[0] = new MalformedURLException();
    }
  }
}
