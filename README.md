# BlissLauncher - No Default Widgets Fork

[BlissLauncher](https://gitlab.e.foundation/e/os/BlissLauncher) comes with 2 widgets that are pre-installed and the user is unable to remove them (as they are hard-coded).  

This fork removes the Search & Suggested Apps widget and Weather Panel widgets.  Additionally this fork give you the freedom
to enable or disable any of the above widgets.

## Screenshots 


## Enable / Disable Default Widgets

```bash
git clone https://github.com/ram-on/BlissLauncher-No-Default-Widgets.git
cd BlissLauncher-No-Default-Widgets
git checkout NoDefaultWidgets
```

Then open `app/src/main/java/foundation/e/blisslauncher/features/launcher/LauncherActivity.java` and change the boolean values
of the `DISABLE_SEARCH_AND_APP_SUGGESTIONS` and `DISABLE_WEATHER_PANEL` constants to enable/disable these widgets.
