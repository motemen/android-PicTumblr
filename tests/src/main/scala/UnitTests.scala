package net.tokyoenvious.droid.pictumblr.tests

import junit.framework.Assert._
import _root_.android.test.AndroidTestCase

class UnitTests extends AndroidTestCase {
  def testPackageIsCorrect {
    assertEquals("net.tokyoenvious.droid.pictumblr", getContext.getPackageName)
  }
}