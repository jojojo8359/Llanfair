# Llanfair

---

[From the (dead) homepage](http://jenmaarai.com/llanfair/en/):

> Llanfair is a free software that helps speedrunners keep track of their run. Released in August 2012, its capacity for customization and its portability allowed it to garner some recognition in the scene. Developed in Java, Llanfair can run on Windows, MacOS, or Unix.

This version is a fork of [gered's](https://github.com/gered) [fork](https://github.com/gered/Llanfair). This version aims to fix some bugs in his fork, as well as add support for auto-splitting in source games using my [fork](https://github.com/jojojo8359/SourceAutoRecord) of [SourceAutoRecord](https://github.com/NeKzor/SourceAutoRecord), a speedrunning plugin for Source engine games (e.g. Portal, Portal 2, Half-Life 2, & The Stanley Parable).

From gered's fork:
>The original author Xavier "Xunkar" Sencert was kind enough to release the sources (see [here](https://twitter.com/Xunkar/status/671042537134624768) and [here](https://twitter.com/Xunkar/status/671099823563632641)) when I asked. Here I will be extending the original application as best I can by adding some missing features here and there and fixing bugs as needed.
>
>Note that Xunkar has started working on Llanfair v2.0 which is a complete rewrite. You can [check it's progress here](https://github.com/xunkar/llanfair).

## Download

Check the [releases page](https://github.com/jojojo8359/Llanfair/releases) for downloadable JARs.

Llanfair requires Java 7 or later (you are encouraged to use the most recent version of Java).

Downloaded JARs can be run from the command line via something similar to:

```
$ java -jar /path/to/Llanfair.jar
```

## Major Changes / Fixes

The main changes from v1.4.3 (the last official release from Xunkar) are as follows:

* Optional world record display, via run data from speedrun.com. Contributed by [4ilo](https://github.com/4ilo).
* Enhancements to JNativeHook support for global key events. Llanfair will prompt with an error
  if the hook could not be registered instead of failing silently. Additionally on some OS's you 
  may see your OS prompt you with some kind of accessibility permissions request.
* Choice between global or non-global hotkeys.
* Human-readable config and splits file formats (XML). This change is almost entirely based on work
  Xunkar had started after release of v1.4.3.
* Support for a delayed/negative run start time. Useful if you want to start the run at a time more convenient for you
  but before any of the segments should start (e.g. to skip initial loading, fadeouts, etc).
* "Sum of best" time display option.
* Attempt counter showing: the number of total attempts, number of completed runs and a per-session attempt counter.
* Additional font and colour customization settings.
* Coloring of split time deltas using slightly different color shades based on if you're gaining/losing time while 
  already ahead/behind.
* Run goal text setting has been changed to a more generic run sub-title setting.
* By default the config file is saved under `$user_home/.llanfair/` and the default location
  to save/load splits is `$user_home/.llanfair/splits/` (though you can of course also choose
  whatever other location you like).
* Ensure application settings are saved when a Mac user quits via Cmd+Q.
* Saved splits are now saved with a default `.lfs` file extension.
* Fix that prevents existing splits files from being accidentally overwritten when choosing "New" option from menu (after you already had a splits file open), and then choose "Save."
* User setting to control amount of files shown in the "Open Recent" menu list.
* Option to set a different default splits file directory (this is merely an additional convenience, most people probably won't use this).
* Other minor bug fixes.

## TODO

* Bug fixing
* Some UI cleanups, especially in the Edit Run dialog and Settings dialog.
* Even more font/color customization options?
* ...
