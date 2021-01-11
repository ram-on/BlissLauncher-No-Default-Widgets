package foundation.e.blisslauncher;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


import foundation.e.blisslauncher.features.launcher.LauncherActivityUiTest;

@RunWith(Suite.class)
/* Here list all you tests classes that are part of the suite
 * eg:
 * mainActivityTest.class,
 * secondActivityTest.class,
 * serviceTest.class
 */
@Suite.SuiteClasses(
    LauncherActivityUiTest.class
)
public class BlissLauncherTestSuite{

}
