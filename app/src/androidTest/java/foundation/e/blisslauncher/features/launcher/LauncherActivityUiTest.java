package foundation.e.blisslauncher.features.launcher;


import android.view.View;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiAutomatorInstrumentationTestRunner;
import androidx.test.uiautomator.UiDevice;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import foundation.e.blisslauncher.R;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
//instrument --package foundation.e.blisslauncher.features.launcher --class LauncherActivityUiTest --runner


@RunWith(AndroidJUnit4ClassRunner.class)
public class LauncherActivityUiTest {

    private UiDevice device;
    @Rule
    public ActivityTestRule<LauncherActivity> mLauncherActivityTestRule =
            new ActivityTestRule<LauncherActivity>(LauncherActivity.class);



    @Test
    public void displayLeftPanelTest() throws Exception{
        //Swipe on the main view to display the left panel
        onView(withId(R.id.appGrid)).perform(ViewActions.swipeRight());

        //Check every element that should be here are here: Search bar, app suggestion, edit widget buttons and weather widget

        //Check app suggestion is displayed
        onView(allOf(withId(R.id.suggestedAppGrid), hasChildCount(4))).check(matches(isDisplayed()));

        //check edit widget button is displayed
        onView(withId(R.id.edit_widgets_button)).check(matches(isDisplayed()));

        //check that search_input is displayed
        //Note: it was a pain to find the "not(withParent(withParent(withParent(withId(swipe_search_container))))"
        //because it told that was more than one match with other trials, but in the "run" window , there was only one
        //I had to add breakpoint and analyse Object "editText" to find the other one then to find for difference between both...
        final Matcher searchInputMatcher = allOf(withId(R.id.search_input), not( withParent( withParent( withParent( withId(R.id.swipe_search_container ) ) ) ) ) );
        ViewInteraction editText = onView(searchInputMatcher);

        editText.check(matches(withHint("Search")))
        .check(matches( isDisplayed() ) );

        //check that weather widget is displayed
        onView(withId(R.id.weather_info_layout)).check(matches(isDisplayed()));
    }

    /**
     * This test requires Calendar App to be installed on the device
     * @throws Exception
     */
    @Test
    public void searchCalendarAppTest() throws Exception{
        final String Calendar_app_name = "Calendar";

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());




        //Swipe on the main view to display the left panel
        onView(withId(R.id.appGrid)).perform(ViewActions.swipeRight());
        final Matcher notChildOfSwipeSearchContainer = not( withParent( withParent( withParent( withId(R.id.swipe_search_container ) ) ) ) );
        final Matcher searchInputMatcher = allOf(withId(R.id.search_input), notChildOfSwipeSearchContainer );


        //Check there is 4  app  suggested
        final ViewInteraction appGrid = onView(allOf(withId(R.id.suggestedAppGrid), withParent(withParent(withId(R.id.used_apps_layout) ) ), isDisplayed() ) );
        appGrid.check(matches(hasChildCount(4)));



        //Click on "search input bar"
        final ViewInteraction editText = onView(searchInputMatcher);
        editText.perform(ViewActions.click());

        final String checkKeyboardCmd = "dumpsys input_method | grep mInputShown";

        //check soft keyboard displayed
        boolean softKeyboardShown = false;
        try {
            softKeyboardShown = device.executeShellCommand(checkKeyboardCmd).contains("mInputShown=true");
        } catch ( IOException e) {
            System.out.println(e.toString());
            throw new RuntimeException("Keyboard check failed", e);
        }

        Assert.assertTrue(softKeyboardShown);

        //Type "Calendar" into the textBox
        editText.perform(ViewActions.typeText(Calendar_app_name));

        //Check the value is set in input
        editText.check(matches(withText(Calendar_app_name)));



        //check the clear button is present
        onView(allOf(withId(R.id.clearSuggestionImageView), notChildOfSwipeSearchContainer)  )
                .check(matches( not(withEffectiveVisibility(Visibility.GONE))));


        //close soft keyboard
        closeSoftKeyboard();

        Thread.sleep(5000); //required to wait for update of UI



        //Check that there is only one app show in grid
        final ViewInteraction appGrid2 = onView(allOf(withId(R.id.suggestedAppGrid), withParent(withParent(withId(R.id.used_apps_layout) ) ), isDisplayed() ) );
        appGrid2.check(matches(hasChildCount(1))); //DOESN't WORK


        // Check that Calendar icon is clickable
        final ViewInteraction appLabel = onView(
            allOf(
                withId( R.id.app_label),
                withText(Calendar_app_name),
                withParent(
                    withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class) )
                ), isDisplayed()
            ) ) ;

        appLabel.check(matches(withParent(withChild(allOf(withId(R.id.app_icon), isClickable())))));
    }

}
