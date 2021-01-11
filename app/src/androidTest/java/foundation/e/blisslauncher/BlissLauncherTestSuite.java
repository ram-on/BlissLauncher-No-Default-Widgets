package foundation.e.blisslauncher;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


import foundation.e.blisslauncher.features.launcher.LauncherActivityUiTest;

@RunWith(Suite::class)
@Suite.SuiteClasses(
/* Here list all you tests classes that are part of the suite
* eg: 
* mainActivityTest::class,     * secondActivityTest::class,
* serviceTest::class
*/
    LauncherActivityUiTest::class
)
class BlissLauncherTestSuite
