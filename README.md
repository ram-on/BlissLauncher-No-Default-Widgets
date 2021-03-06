# BlissLauncher - No Default Widgets Fork

[BlissLauncher](https://gitlab.e.foundation/e/os/BlissLauncher) comes with 2 widgets that are pre-installed and the user is unable to remove them (as they are hard-coded).  

This fork removes the Search & Suggested Apps widget and Weather Panel widgets.  Additionally this fork give you the freedom
to enable or disable any of the above widgets.

## Download

Download APK:  https://github.com/ram-on/BlissLauncher-No-Default-Widgets/releases/download/v1.3.1/BlissLauncher-No-Default-Widgets-AndroidR-v1.3.2.apk


## Screenshots

Screenshot without the Search & Suggested Apps widget and Weather Panel widgets:

<img src="https://i.imgur.com/Cbyf3WJ.png" alt="No Widgets" height="700px"/>

Screenshot with some custom widgets (added via the "Edit" button):

<img src="https://i.imgur.com/yFmY5Kg.png" alt="Custom Widgets" height="700px"/>

Screenshot with the Search & Suggested Apps widget enabled (refer to [here](#enable--disable-default-widgets)):

<img src="https://i.imgur.com/FTiAi0w.png" alt="Search & Suggested Apps Widget" height="700px"/>

Screenshot with the Weather Panel widget enabled (refer to [here](#enable--disable-default-widgets)):

<img src="https://i.imgur.com/TvPegT0.png" alt="Weather Panel Widget" height="700px"/>


## Enable / Disable Default Widgets

```bash
git clone https://github.com/ram-on/BlissLauncher-No-Default-Widgets.git
cd BlissLauncher-No-Default-Widgets
git checkout NoDefaultWidgets
```

Then open `app/src/main/java/foundation/e/blisslauncher/features/launcher/LauncherActivity.java` and change the boolean values
of the `DISABLE_SEARCH_AND_APP_SUGGESTIONS` and `DISABLE_WEATHER_PANEL` constants to enable/disable these widgets.
